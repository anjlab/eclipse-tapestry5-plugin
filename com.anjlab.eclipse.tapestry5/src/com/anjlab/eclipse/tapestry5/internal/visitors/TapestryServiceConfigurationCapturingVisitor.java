package com.anjlab.eclipse.tapestry5.internal.visitors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeLiteral;

import com.anjlab.eclipse.tapestry5.EclipseUtils;
import com.anjlab.eclipse.tapestry5.TapestryModule;
import com.anjlab.eclipse.tapestry5.internal.DeclarationCapturingScope;
import com.anjlab.eclipse.tapestry5.internal.DeclarationCapturingScope.Declaration;
import com.anjlab.eclipse.tapestry5.internal.DeclarationCapturingScope.InjectedDeclaration;

public abstract class TapestryServiceConfigurationCapturingVisitor extends ASTVisitor
{

    protected final IProgressMonitor monitor;
    protected final TapestryModule tapestryModule;
    protected final DeclarationCapturingScope declarations;
    
    private Object name;
    private Object value;

    private boolean captureConfiguration;
    private boolean captureOrderedConfiguration;
    private boolean captureMappedConfiguration;
    
    public TapestryServiceConfigurationCapturingVisitor(IProgressMonitor monitor, TapestryModule tapestryModule)
    {
        this.monitor = monitor;
        this.tapestryModule = tapestryModule;
        this.declarations = new DeclarationCapturingScope();
    }

    public TapestryServiceConfigurationCapturingVisitor usesConfiguration()
    {
        this.captureConfiguration = true;
        return this;
    }
    
    public TapestryServiceConfigurationCapturingVisitor usesOrderedConfiguration()
    {
        this.captureOrderedConfiguration = true;
        return this;
    }
    
    public TapestryServiceConfigurationCapturingVisitor usesMappedConfiguration()
    {
        this.captureMappedConfiguration = true;
        return this;
    }
    
    @Override
    public boolean visit(MethodDeclaration node)
    {
        //  Capture method arguments in a scope
        declarations.enterScope();
        for (Object arg : node.parameters())
        {
            if (arg instanceof SingleVariableDeclaration)
            {
                SingleVariableDeclaration variableDeclaration = (SingleVariableDeclaration) arg;
                
                String className = EclipseUtils.toClassName(tapestryModule.getEclipseProject(), variableDeclaration.getType());
                
                if (StringUtils.isNotEmpty(className))
                {
                    declarations.add(new Declaration(
                            variableDeclaration,
                            variableDeclaration.getName().getIdentifier(),
                            className));
                }
            }
        }
        return super.visit(node);
    }

    @Override
    public void endVisit(MethodDeclaration node)
    {
        super.endVisit(node);
        declarations.exitScope();
    }

    @Override
    public boolean visit(MethodInvocation node)
    {
        if (monitor.isCanceled())
        {
            return false;
        }
        
        this.name = null;
        this.value = null;
        
        //  org.apache.tapestry5.ioc.Configuration<T>
        //  org.apache.tapestry5.ioc.OrderedConfiguration<T>
        //  org.apache.tapestry5.ioc.MappedConfiguration<K,V>
        
        if (!isTapestryConfigurationMethod(node))
        {
            return false;
        }
        
        if (!(node.getExpression() instanceof SimpleName))
        {
            return false;
        }
        
        Object type = resolve(node.getExpression());
        
        if (type instanceof InjectedDeclaration)
        {
            type = ((InjectedDeclaration) type).type;
        }
        
        if (!(type instanceof IType))
        {
            return false;
        }
        
        String className = ((IType) type).getFullyQualifiedName();
        
        int argc = node.arguments().size();
        
        if (StringUtils.equals("org.apache.tapestry5.ioc.Configuration", className)
                && captureConfiguration)
        {
            if (argc == 1)
            {
                this.value = resolve(node.arguments().get(0));
                
                configurationAddOverride(node, this.value);
                
                return false;
            }
        }
        else if (StringUtils.equals("org.apache.tapestry5.ioc.OrderedConfiguration", className)
                && captureOrderedConfiguration)
        {
            if (argc >= 2)
            {
                this.name = resolve(node.arguments().get(0));
                
                if (!(this.name instanceof String))
                {
                    //  Contribution id must be String
                    return false;
                }
                
                this.value = resolve(node.arguments().get(1));
                
                orderedConfigurationAddOverride(
                        node,
                        (String) this.name,
                        this.value,
                        resolveConstraints(node, 2));
                
                return false;
            }
        }
        else if (StringUtils.equals("org.apache.tapestry5.ioc.MappedConfiguration", className)
                && captureMappedConfiguration)
        {
            if (argc == 2)
            {
                this.name = resolve(node.arguments().get(0));
                this.value = resolve(node.arguments().get(1));
                
                mappedConfigurationAddOverride(node, this.name, this.value);
                
                return false;
            }
        }
        
        return false;
    }

    private String[] resolveConstraints(MethodInvocation node, int constraintsArgumentIndex)
    {
        int argc = node.arguments().size();
        
        List<String> constraints = new ArrayList<String>();
        
        if (argc == constraintsArgumentIndex + 1)
        {
            //  XXX May be: String, or String[], or chain of OrderConstraintBuilder
            Object arg = node.arguments().get(constraintsArgumentIndex);
            
            if ((arg instanceof String) || (arg instanceof StringLiteral))
            {
                Object constraint = resolve(arg);
                
                if (constraint instanceof String)
                {
                    constraints.add((String) constraint);
                }
            }
        }
        else
        {
            //  Varargs
            for (int i = constraintsArgumentIndex; i < argc; i++)
            {
                Object arg = node.arguments().get(i);
                
                Object constraint = resolve(arg);
                
                if (constraint instanceof String)
                {
                    constraints.add((String) constraint);
                }
            }
        }
        
        return constraints.toArray(new String[constraints.size()]);
    }

    private Object resolve(Object arg)
    {
        if (arg instanceof TypeLiteral)
        {
            String className = EclipseUtils.toClassName(tapestryModule.getEclipseProject(), (TypeLiteral) arg);
            
            if (StringUtils.isNotEmpty(className))
            {
                return EclipseUtils.findTypeDeclaration(tapestryModule.getEclipseProject(), className);
            }
        }
        else if (arg instanceof SimpleName)
        {
            String identifier = ((SimpleName) arg).getIdentifier();
            Declaration declaration = declarations.findClosest(identifier);
            if (declaration != null && StringUtils.isNotEmpty(declaration.className))
            {
                //  Create injected resource, it may be:
                //  - @InjectService("id"),
                //  - with zero-or-many @Marker annotations,
                //  - or @Inject @Symbol(name)
                
                IType type = EclipseUtils.findTypeDeclaration(tapestryModule.getEclipseProject(), declaration.className);
                
                return type == null ? null : new InjectedDeclaration(declaration, type);
                
                //  This may be an interface name of @Inject'ed service.
                //  Interface name is enough right now, but it may be handy
                //  to find corresponding implementation class to implement additional features.
                //  Finding an implementation is not a trivial operation:
                //  - a service may be injected by ID,
                //  - or using marker annotations,
                //  and we may not have enough information at this time,
                //  because not all modules could be discovered yet.
                //  We should keep the declaration reference to find this information later.
            }
        }
        else if (arg instanceof ClassInstanceCreation)
        {
            String className = EclipseUtils.toClassName(
                    tapestryModule.getEclipseProject(), ((ClassInstanceCreation) arg).getType());
            
            if (StringUtils.isNotEmpty(className))
            {
                return EclipseUtils.findTypeDeclaration(tapestryModule.getEclipseProject(), className);
            }
        }
        
        //  Try evaluating
        return EclipseUtils.evalExpression(tapestryModule.getEclipseProject(), arg);
    }

    private boolean isTapestryConfigurationMethod(MethodInvocation node)
    {
        return Arrays.binarySearch(TapestryModule.CONFIGURATION_METHODS, node.getName().toString()) >= 0;
    }

    protected boolean isAddOrOverrideInvocation(MethodInvocation node)
    {
        return Arrays.binarySearch(TapestryModule.ADD_OVERRIDE, node.getName().toString()) >= 0;
    }

    protected boolean isAddOrOverrideInstanceInvocation(MethodInvocation node)
    {
        return Arrays.binarySearch(TapestryModule.ADD_OVERRIDE_INSTANCE, node.getName().toString()) >= 0;
    }

    protected boolean isOverride(MethodInvocation node)
    {
        return Arrays.binarySearch(TapestryModule.OVERRIDES, node.getName().toString()) >= 0;
    }

    protected void mappedConfigurationAddOverride(MethodInvocation node, Object key, Object value)
    {
        //  Do nothing, concrete visitors should override this method
    }

    protected void orderedConfigurationAddOverride(MethodInvocation node, String id, Object value, String[] constraints)
    {
        //  Do nothing, concrete visitors should override this method
    }

    protected void configurationAddOverride(MethodInvocation node, Object value)
    {
        //  Do nothing, concrete visitors should override this method
    }
}