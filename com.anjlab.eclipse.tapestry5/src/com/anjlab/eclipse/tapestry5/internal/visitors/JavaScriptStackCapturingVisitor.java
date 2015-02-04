package com.anjlab.eclipse.tapestry5.internal.visitors;

import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;

import com.anjlab.eclipse.tapestry5.Activator;
import com.anjlab.eclipse.tapestry5.DeclarationReference.ASTNodeReference;
import com.anjlab.eclipse.tapestry5.EclipseUtils;
import com.anjlab.eclipse.tapestry5.JavaScriptStack;
import com.anjlab.eclipse.tapestry5.TapestryModule;
import com.anjlab.eclipse.tapestry5.TapestryModule.ObjectCallback;
import com.anjlab.eclipse.tapestry5.TapestryUtils;
import com.anjlab.eclipse.tapestry5.internal.DeclarationCapturingScope;
import com.anjlab.eclipse.tapestry5.internal.DeclarationCapturingScope.Declaration;

public class JavaScriptStackCapturingVisitor extends ASTVisitor
{
    private final IProgressMonitor monitor;
    private final TapestryModule tapestryModule;
    private final ObjectCallback<JavaScriptStack, RuntimeException> javaScriptStackFound;
    
    protected final DeclarationCapturingScope declarations = new DeclarationCapturingScope();

    public JavaScriptStackCapturingVisitor(IProgressMonitor monitor, TapestryModule tapestryModule,
            ObjectCallback<JavaScriptStack, RuntimeException> javaScriptStackFound)
    {
        this.monitor = monitor;
        this.tapestryModule = tapestryModule;
        this.javaScriptStackFound = javaScriptStackFound;
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
                
                declarations.add(new Declaration(
                        variableDeclaration.getName().getIdentifier(),
                        EclipseUtils.toClassName(tapestryModule.getEclipseProject(), variableDeclaration.getType()),
                        variableDeclaration));
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
        
        IType secondArgumentType = null;
        
        if (Arrays.binarySearch(TapestryModule.ADD_OVERRIDE_INSTANCE, node.getName().toString()) >= 0
                && node.arguments().size() == 2)
        {
            Object typeArg = node.arguments().get(1);
            
            if (typeArg instanceof TypeLiteral)
            {
                String className = EclipseUtils.toClassName(tapestryModule.getEclipseProject(), (TypeLiteral) typeArg);
                
                if (StringUtils.isNotEmpty(className))
                {
                    secondArgumentType = EclipseUtils.findTypeDeclaration(tapestryModule.getEclipseProject(), className);
                }
            }
        }
        else if (Arrays.binarySearch(TapestryModule.ADD_OVERRIDE, node.getName().toString()) >= 0
                && node.arguments().size() == 2)
        {
            Object instanceArg = node.arguments().get(1);
            
            //  Maybe one of:
            //  1) new JavaScriptStack()
            //  2) simpleName
            
            if (instanceArg instanceof SimpleName)
            {
                String identifier = ((SimpleName) instanceArg).getIdentifier();
                Declaration declaration = declarations.findClosest(identifier);
                if (declaration != null && StringUtils.isNotEmpty(declaration.className))
                {
                    secondArgumentType = EclipseUtils.findTypeDeclaration(tapestryModule.getEclipseProject(), declaration.className);
                    
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
            else if (instanceArg instanceof ClassInstanceCreation)
            {
                String className = EclipseUtils.toClassName(
                        tapestryModule.getEclipseProject(), ((ClassInstanceCreation) instanceArg).getType());
                
                if (StringUtils.isNotEmpty(className))
                {
                    secondArgumentType = EclipseUtils.findTypeDeclaration(tapestryModule.getEclipseProject(), className);
                }
            }
        }
        
        if (secondArgumentType == null)
        {
            return super.visit(node);
        }
        
        try
        {
            tryAddJavaScriptStack(node, secondArgumentType, secondArgumentType.getFullyQualifiedName());
            
            String[] interfaceNames = secondArgumentType.getSuperInterfaceNames();
            
            for (String interfaceName : interfaceNames)
            {
                tryAddJavaScriptStack(node, secondArgumentType, interfaceName);
            }
        }
        catch (JavaModelException e)
        {
            Activator.getDefault().logWarning(
                    "Unable to get super interfaces of " + secondArgumentType);
        }
        
        return super.visit(node);
    }

    private void tryAddJavaScriptStack(MethodInvocation node, IType type, String interfaceName)
    {
        if (TapestryUtils.isTapestryJavaScriptStackInterface(interfaceName))
        {
            Object stackExpr = node.arguments().get(0);
            String stackName = EclipseUtils.evalExpression(tapestryModule.getEclipseProject(), stackExpr);
            
            javaScriptStackFound.callback(new JavaScriptStack(
                    stackName,
                    type,
                    Arrays.binarySearch(TapestryModule.OVERRIDES, node.getName().toString()) >= 0,
                    new ASTNodeReference(tapestryModule.getModuleClass(), node)));
        }
    }
}