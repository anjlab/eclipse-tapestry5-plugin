package com.anjlab.eclipse.tapestry5.internal.visitors;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeLiteral;

import com.anjlab.eclipse.tapestry5.DeclarationReference.ASTNodeReference;
import com.anjlab.eclipse.tapestry5.EclipseUtils;
import com.anjlab.eclipse.tapestry5.TapestryModule;
import com.anjlab.eclipse.tapestry5.TapestryModule.ObjectCallback;
import com.anjlab.eclipse.tapestry5.TapestryService;
import com.anjlab.eclipse.tapestry5.TapestryService.ServiceDefinition;
import com.anjlab.eclipse.tapestry5.TapestryUtils;

public class TapestryServiceCapturingVisitor extends ASTVisitor
{
    private final IProgressMonitor monitor;
    private final TapestryModule tapestryModule;
    private final ObjectCallback<TapestryService, RuntimeException> serviceFound;
    
    private ServiceDefinition serviceDefinition;

    public TapestryServiceCapturingVisitor(IProgressMonitor monitor,
            TapestryModule tapestryModule,
            ObjectCallback<TapestryService, RuntimeException> serviceFound)
    {
        this.monitor = monitor;
        this.tapestryModule = tapestryModule;
        this.serviceFound = serviceFound;
    }

    private ServiceDefinition serviceDefinition()
    {
        if (serviceDefinition == null)
        {
            serviceDefinition = new ServiceDefinition();
        }
        return serviceDefinition;
    }

    private boolean analyzeInvocationChain(MethodInvocation node)
    {
        if (node.getExpression() instanceof MethodInvocation)
        {
            visit((MethodInvocation) node.getExpression());
        }
        else
        {
            //  Unsupported method chain, drop captured service definition
            serviceDefinition = null;
        }
        return false;
    }

    @Override
    public boolean visit(MethodInvocation node)
    {
        if (monitor.isCanceled())
        {
            return false;
        }
        
        String identifier = node.getName().getIdentifier();
        
        if ("withMarker".equals(identifier))
        {
            //  Copy annotations from module class
            serviceDefinition().addMarkers(tapestryModule.markers());
            for (Object arg : node.arguments())
            {
                if (arg instanceof TypeLiteral)
                {
                    serviceDefinition().addMarker(
                            EclipseUtils.toClassName(tapestryModule.getEclipseProject(), (TypeLiteral) arg));
                }
            }
            return analyzeInvocationChain(node);
        }
        else if ("preventReloading".equals(identifier))
        {
            serviceDefinition().setPreventReloading(true);
            return analyzeInvocationChain(node);
        }
        else if ("preventDecoration".equals(identifier))
        {
            serviceDefinition().setPreventDecoration(true);
            return analyzeInvocationChain(node);
        }
        else if ("eagerLoad".equals(identifier))
        {
            serviceDefinition().setEagerLoad(true);
            return analyzeInvocationChain(node);
        }
        else if ("scope".equals(identifier))
        {
            if (node.arguments().size() == 1)
            {
                serviceDefinition().setScope(
                        EclipseUtils.evalExpression(
                                tapestryModule.getEclipseProject(), node.arguments().get(0)));
            }
            return analyzeInvocationChain(node);
        }
        else if ("withId".equals(identifier))
        {
            if (node.arguments().size() == 1)
            {
                serviceDefinition().setId(
                        EclipseUtils.evalExpression(
                                tapestryModule.getEclipseProject(), node.arguments().get(0)));
            }
            return analyzeInvocationChain(node);
        }
        else if ("withSimpleId".equals(identifier))
        {
            serviceDefinition().setSimpleId(true);
            return analyzeInvocationChain(node);
        }
        else if ("bind".equals(identifier))
        {
            ASTNode intf = null;
            ASTNode impl = null;
            
            switch (node.arguments().size()) {
            case 2:
                //  Interface, Implementation
                intf = (ASTNode) node.arguments().get(0);
                impl = (ASTNode) node.arguments().get(1);
                break;
            case 1:
                //  Interface
                intf = (ASTNode) node.arguments().get(0);
                break;
            }
            
            if (intf instanceof TypeLiteral)
            {
                ServiceDefinition definition = serviceDefinition();
                
                definition.setIntfClass(EclipseUtils.toClassName(tapestryModule.getEclipseProject(), (TypeLiteral) intf));
                
                if (impl instanceof TypeLiteral)
                {
                    definition.setImplClass(EclipseUtils.toClassName(tapestryModule.getEclipseProject(), (TypeLiteral) impl));
                }
                
                if (definition.isSimpleId())
                {
                    if (StringUtils.isNotEmpty(definition.getImplClass()))
                    {
                        definition.setId(TapestryUtils.getSimpleName(definition.getImplClass()));
                    }
                }
                else if (StringUtils.isEmpty(definition.getId()))
                {
                    definition.setId(TapestryUtils.getSimpleName(definition.getIntfClass()));
                }
                
                copyMarkersFrom(definition.getIntfClass());
                copyMarkersFrom(definition.getImplClass());

                serviceFound.callback(new TapestryService(
                        tapestryModule,
                        definition,
                        new ASTNodeReference(tapestryModule.getModuleClass(), node)));
            }
            
            serviceDefinition = null;
            
            return false;
        }
        
        return super.visit(node);
    }

    private void copyMarkersFrom(String className)
    {
        if (StringUtils.isEmpty(className))
        {
            return;
        }

        IType type = EclipseUtils.findTypeDeclaration(tapestryModule.getEclipseProject(), className);

        if (type == null)
        {
            return;
        }

        try
        {
            List<String> markers = tapestryModule.readMarkerAnnotation(type);

            serviceDefinition().addMarkers(markers);
        }
        catch (JavaModelException e)
        {
            // Ignore
        }
    }
}