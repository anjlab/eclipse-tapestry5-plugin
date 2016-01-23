package com.anjlab.eclipse.tapestry5.internal.visitors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.search.IJavaSearchConstants;

import com.anjlab.eclipse.tapestry5.Activator;
import com.anjlab.eclipse.tapestry5.DeclarationReference.ASTNodeReference;
import com.anjlab.eclipse.tapestry5.EclipseUtils;
import com.anjlab.eclipse.tapestry5.ObjectCallback;
import com.anjlab.eclipse.tapestry5.TapestryModule;
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
            bind(node);
            
            serviceDefinition = null;
            
            return false;
        }
        
        return super.visit(node);
    }

    private void bind(MethodInvocation node)
    {
        String intfClass = null;
        String implClass = null;
        
        switch (node.arguments().size()) {
        case 2:
            //  Interface, Implementation
            intfClass = typeLiteralToClassName(node.arguments().get(0));
            implClass = typeLiteralToClassName(node.arguments().get(1));
            break;
        case 1:
            //  Check if it's actually an interface, or a non-interface class
            //  It it's an interface, then name of implementation class can be computed
            String className = typeLiteralToClassName(node.arguments().get(0));
            
            //  TODO Implement caching for type lookups
            IType type = EclipseUtils.findTypeDeclaration(
                    tapestryModule.getEclipseProject(), IJavaSearchConstants.CLASS_AND_INTERFACE, className);
            
            if (type == null)
            {
                //  Something is wrong with this service binding
                Activator.getDefault().logError("Unable to find java type: " + className);
                return;
            }
            
            try
            {
                if (type.isInterface())
                {
                    intfClass = type.getFullyQualifiedName();
                    implClass = intfClass + "Impl";
                }
                else
                {
                    implClass = type.getFullyQualifiedName();
                }
                
                break;
            }
            catch (JavaModelException e)
            {
                Activator.getDefault().logError("Unable to read java model", e);
                return;
            }
            default:
                Activator.getDefault().logWarning("Unexpected method signature: " + node);
                return;
        }
        
        ServiceDefinition definition = serviceDefinition();
        
        definition.setIntfClass(intfClass);
        definition.setImplClass(implClass);
        
        if (definition.isSimpleId())
        {
            if (StringUtils.isNotEmpty(definition.getImplClass()))
            {
                definition.setId(TapestryUtils.getSimpleName(definition.getImplClass()));
            }
        }
        else if (StringUtils.isEmpty(definition.getId()))
        {
            //  Try getting serviceId from @ServiceId & @Named annotations on implementation class
            //  see tapestry's ServiceBinderImpl#bind() for details
            
            String serviceId = readServiceIdFromAnnotations(definition.getImplClass());
            
            String classNameForServiceId = StringUtils.defaultIfEmpty(
                    //  In tapestry it's not possible to have null interface,
                    //  it's plugin's implementation detail --
                    //  should fallback to implClass in this case
                    definition.getIntfClass(),
                    definition.getImplClass());
            
            definition.setId(
                    StringUtils.defaultIfEmpty(
                            serviceId,
                            StringUtils.isNotEmpty(classNameForServiceId)
                                    ? TapestryUtils.getSimpleName(classNameForServiceId)
                                    : null));
        }
        
        if (StringUtils.isEmpty(definition.getId()))
        {
            //  Something is wrong with this service definition
            //  Maybe that was not ServiceBinder#bind(), but some other `bind` method?
            return;
        }
        
        definition.resolveMarkers(tapestryModule);
        
        serviceFound.callback(new TapestryService(
                tapestryModule,
                definition,
                new ASTNodeReference(tapestryModule, tapestryModule.getModuleClass(), node)));
    }

    private String readServiceIdFromAnnotations(String implClass)
    {
        try
        {
            IType implType = EclipseUtils.findTypeDeclaration(
                    tapestryModule.getEclipseProject(),
                    IJavaSearchConstants.CLASS,
                    implClass);
            
            if (implType == null)
            {
                return null;
            }
            
            IAnnotation serviceIdAnnotation =
                    TapestryUtils.findAnnotation(
                            implType.getAnnotations(),
                            TapestryUtils.ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_SERVICE_ID);
            
            if (serviceIdAnnotation != null)
            {
                return EclipseUtils.readFirstValueFromAnnotation(
                        tapestryModule.getEclipseProject(),
                        serviceIdAnnotation,
                        "value");
            }
            
            IAnnotation namedAnnotation =
                    TapestryUtils.findAnnotation(
                            implType.getAnnotations(),
                            TapestryUtils.JAVAX_INJECT_NAMED);
            
            if (namedAnnotation != null)
            {
                return StringUtils.trimToNull(
                        EclipseUtils.readFirstValueFromAnnotation(
                            tapestryModule.getEclipseProject(),
                            namedAnnotation,
                            "value"));
            }
        }
        catch (JavaModelException e)
        {
            Activator.getDefault().logError("Error determining ServiceId from implementation class", e);
        }
        
        return null;
    }

    private String typeLiteralToClassName(Object node)
    {
        if (node instanceof TypeLiteral)
        {
            return EclipseUtils.toClassName(tapestryModule.getEclipseProject(), (TypeLiteral) node);
        }
        return null;
    }
}