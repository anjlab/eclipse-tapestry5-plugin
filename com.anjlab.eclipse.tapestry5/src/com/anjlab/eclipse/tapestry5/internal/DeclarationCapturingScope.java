package com.anjlab.eclipse.tapestry5.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.NormalAnnotation;

import com.anjlab.eclipse.tapestry5.EclipseUtils;
import com.anjlab.eclipse.tapestry5.TapestryModule;
import com.anjlab.eclipse.tapestry5.TapestryService.Matcher;
import com.anjlab.eclipse.tapestry5.TapestryUtils;

public class DeclarationCapturingScope
{
    public static class Declaration
    {
        public final String name;
        public final String className;
        public final ASTNode node;

        public Declaration(ASTNode node, String name, String className)
        {
            this.name = name;
            this.className = className;
            this.node = node;
        }
    }

    public static class InjectedDeclaration extends Declaration
    {
        public final IType type;

        public InjectedDeclaration(ASTNode node, String name, String className, IType type)
        {
            super(node, name, className);
            this.type = type;
        }

        public InjectedDeclaration(Declaration declaration, IType type)
        {
            this(declaration.node, declaration.name, declaration.className, type);
        }

        public Matcher createMatcher(final TapestryModule module)
        {
            final AtomicReference<String> serviceId = new AtomicReference<String>();
            
            final List<String> markers = new ArrayList<String>();
            
            node.accept(new ASTVisitor()
            {
                @Override
                public boolean visit(NormalAnnotation node)
                {
                    if (TapestryUtils.isAnnotationEquals(
                            node, TapestryUtils.ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_INJECT_SERVICE))
                    {
                        List<?> values = node.values();
                        
                        for (Object value : values)
                        {
                            if (value instanceof MemberValuePair)
                            {
                                MemberValuePair pair = (MemberValuePair) value;
                                
                                if ("value".equals(pair.getName().getIdentifier()))
                                {
                                    serviceId.set(
                                            EclipseUtils.evalExpression(
                                                    module.getEclipseProject(),
                                                    pair.getValue()));
                                }
                            }
                            else
                            {
                                serviceId.set(
                                        EclipseUtils.evalExpression(
                                                module.getEclipseProject(),
                                                value));
                            }
                        }
                    }
                    
                    return super.visit(node);
                }
                
                @Override
                public boolean visit(MarkerAnnotation node)
                {
                    if (TapestryUtils.isAnnotationEquals(
                            node, TapestryUtils.ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_INJECT))
                    {
                        // @Inject is not a service marker
                        return super.visit(node);
                    }
                    
                    try
                    {
                        markers.add(
                                EclipseUtils.resolveTypeName(
                                        module.getModuleClass(),
                                        node.getTypeName().getFullyQualifiedName()));
                    }
                    catch (JavaModelException e)
                    {
                        //  Ignore
                    }
                    
                    return super.visit(node);
                }
            });
            
            AndMatcher matcher = new AndMatcher();
            
            matcher.add(new ServiceIntfMatcher(className));
            
            if (StringUtils.isNotEmpty(serviceId.get()))
            {
                matcher.add(new IdentityIdMatcher(serviceId.get()));
            }
            
            for (String marker : markers)
            {
                matcher.add(new MarkerMatcher(marker));
            }
            
            return matcher;
        }

        public boolean isServiceInjection()
        {
            final AtomicBoolean foundSymbolAnnotation = new AtomicBoolean(false);
            
            node.accept(new ASTVisitor()
            {
                @Override
                public boolean visit(NormalAnnotation node)
                {
                    if (TapestryUtils.isAnnotationEquals(
                            node, TapestryUtils.ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_SYMBOL))
                    {
                        foundSymbolAnnotation.set(true);
                    }
                    
                    return super.visit(node);
                }
            });

            return !foundSymbolAnnotation.get();
        }
    }
    
    private Stack<Map<String, Declaration>> scopes = new Stack<Map<String, Declaration>>();

    public DeclarationCapturingScope()
    {
        enterScope();
    }

    public void enterScope()
    {
        scopes.push(new HashMap<String, Declaration>());
    }

    public void exitScope()
    {
        scopes.pop();
    }

    public void add(Declaration declaration)
    {
        scopes.peek().put(declaration.name, declaration);
    }

    public Declaration findClosest(String name)
    {
        for (int i = scopes.size() - 1; i >= 0; i--)
        {
            Map<String, Declaration> scope = scopes.get(i);
            Declaration declaration = scope.get(name);
            if (declaration != null)
            {
                return declaration;
            }
        }
        return null;
    }
}
