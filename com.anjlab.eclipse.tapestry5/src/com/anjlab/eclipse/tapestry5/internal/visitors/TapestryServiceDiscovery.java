package com.anjlab.eclipse.tapestry5.internal.visitors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import com.anjlab.eclipse.tapestry5.Activator;
import com.anjlab.eclipse.tapestry5.DeclarationReference.JavaElementReference;
import com.anjlab.eclipse.tapestry5.EclipseUtils;
import com.anjlab.eclipse.tapestry5.ObjectCallback;
import com.anjlab.eclipse.tapestry5.TapestryModule;
import com.anjlab.eclipse.tapestry5.TapestryService;
import com.anjlab.eclipse.tapestry5.TapestryService.InstrumenterType;
import com.anjlab.eclipse.tapestry5.TapestryService.Matcher;
import com.anjlab.eclipse.tapestry5.TapestryService.ServiceDefinition;
import com.anjlab.eclipse.tapestry5.TapestryService.ServiceInstrumenter;
import com.anjlab.eclipse.tapestry5.TapestryUtils;
import com.anjlab.eclipse.tapestry5.internal.AndMatcher;
import com.anjlab.eclipse.tapestry5.internal.GlobPatternMatcher;
import com.anjlab.eclipse.tapestry5.internal.IdentityIdMatcher;
import com.anjlab.eclipse.tapestry5.internal.MarkerMatcher;
import com.anjlab.eclipse.tapestry5.internal.ServiceIntfMatcher;

public class TapestryServiceDiscovery
{
    private final IProgressMonitor monitor;
    private final TapestryModule tapestryModule;
    private final ObjectCallback<TapestryService, RuntimeException> serviceFound;
    private final ObjectCallback<ServiceInstrumenter, RuntimeException> advisorFound;
    private final ObjectCallback<ServiceInstrumenter, RuntimeException> contributorFound;
    private final ObjectCallback<ServiceInstrumenter, RuntimeException> decoratorFound;
    
    public TapestryServiceDiscovery(
            IProgressMonitor monitor,
            TapestryModule tapestryModule,
            ObjectCallback<TapestryService, RuntimeException> serviceFound,
            ObjectCallback<ServiceInstrumenter, RuntimeException> advisorFound,
            ObjectCallback<ServiceInstrumenter, RuntimeException> contributorFound,
            ObjectCallback<ServiceInstrumenter, RuntimeException> decoratorFound)
    {
        this.monitor = monitor;
        this.tapestryModule = tapestryModule;
        this.serviceFound = serviceFound;
        this.advisorFound = advisorFound;
        this.contributorFound = contributorFound;
        this.decoratorFound = decoratorFound;
    }

    public void run()
    {
        try
        {
            for (IMethod method : tapestryModule.getModuleClass().getMethods())
            {
                if (monitor.isCanceled())
                {
                    return;
                }
                
                try
                {
                    if (TapestryUtils.isServiceBuilderMethod(method))
                    {
                        addServiceFromBuilderMethod(method);
                    }
                    else if (TapestryUtils.isDecoratorMethod(method))
                    {
                        addServiceDecorator(method);
                    }
                    else if (TapestryUtils.isAdvisorMethod(method))
                    {
                        addServiceAdvisor(method);
                    }
                    else if (TapestryUtils.isContributorMethod(method))
                    {
                        addContributionMethod(method);
                    }
                    else if (TapestryUtils.isStartupMethod(method))
                    {
                        addStartupContributor(method);
                    }
                }
                catch (Exception e)
                {
                    Activator.getDefault().logError(
                            "Error handling method " + method.getElementName()
                            + " for " + tapestryModule.getModuleClass().getFullyQualifiedName(), e);
                }
            }
        }
        catch (Exception e)
        {
            Activator.getDefault().logError(
                    "Error enumerating methods for " + tapestryModule.getModuleClass().getFullyQualifiedName(), e);
        }
    }


    private void addStartupContributor(IMethod method) throws JavaModelException
    {
        contributorFound.callback(
                new ServiceInstrumenter()
                        .setType(InstrumenterType.CONTRIBUTOR)
                        .setId("RegistryStartup")
                        .setReference(new JavaElementReference(tapestryModule, method))
                        .setServiceMatcher(new IdentityIdMatcher("RegistryStartup"))
                        .setConstraints(extractConstraints(method)));
    }

    private void addContributionMethod(IMethod method) throws JavaModelException
    {
        IAnnotation annotation = TapestryUtils.findAnnotation(method.getAnnotations(),
                TapestryUtils.ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_CONTRIBUTE);
        
        String serviceInterface = annotation != null
                ? EclipseUtils.readFirstValueFromAnnotation(tapestryModule.getEclipseProject(), annotation, "value")
                : null;
        
        String id = annotation == null
                ? stripMethodPrefix(method, TapestryUtils.CONTRIBUTE_METHOD_NAME_PREFIX)
                : null;
        
        List<String> markers = extractMarkers(
                method,
                new HashSet<String>(Arrays.asList(
                    TapestryUtils.ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_CONTRIBUTE,
                    TapestryUtils.ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_ORDER,
                    TapestryUtils.ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_MATCH)));
        
        Matcher serviceMatcher = createServiceMatcherForConfigurationContribution(id, serviceInterface, markers);
        
        contributorFound.callback(
                new ServiceInstrumenter()
                        .setType(InstrumenterType.CONTRIBUTOR)
                        .setId(id)
                        .setReference(new JavaElementReference(tapestryModule, method))
                        .setServiceMatcher(serviceMatcher)
                        .setConstraints(extractConstraints(method)));
    }

    private void addServiceAdvisor(IMethod method) throws JavaModelException
    {
        advisorFound.callback(
                createInstrumenter(method,
                        TapestryUtils.ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_ADVISE,
                        TapestryUtils.ADVISE_METHOD_NAME_PREFIX,
                        InstrumenterType.ADVISOR));
    }

    private void addServiceDecorator(IMethod method) throws JavaModelException
    {
        decoratorFound.callback(
                createInstrumenter(method,
                        TapestryUtils.ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_DECORATE,
                        TapestryUtils.DECORATE_METHOD_NAME_PREFIX,
                        InstrumenterType.DECORATOR));
    }

    private ServiceInstrumenter createInstrumenter(
            IMethod method, String instrumenterAnnotation, String methodNamePrefix, InstrumenterType instrumenterType)
                    throws JavaModelException
    {
        IAnnotation annotation = TapestryUtils.findAnnotation(method.getAnnotations(),
                instrumenterAnnotation);
        
        String serviceInterface = annotation == null ? null
                : EclipseUtils.readFirstValueFromAnnotation(tapestryModule.getEclipseProject(), annotation, "serviceInterface");
        
        String id = annotation == null
                ? stripMethodPrefix(method, methodNamePrefix)
                : extractId(serviceInterface, EclipseUtils.readFirstValueFromAnnotation
                        (tapestryModule.getEclipseProject(), annotation, "id"));
        
        List<String> markers = extractMarkers(
                method,
                new HashSet<String>(Arrays.asList(
                    instrumenterAnnotation,
                    TapestryUtils.ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_ORDER,
                    TapestryUtils.ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_MATCH)));
        
        Matcher serviceMatcher = createMatcherForInstrumenter(method, id, markers);
        
        return new ServiceInstrumenter()
                .setType(instrumenterType)
                .setId(id)
                .setReference(new JavaElementReference(tapestryModule, method))
                .setServiceMatcher(serviceMatcher)
                .setConstraints(extractConstraints(method));
    }

    private String[] extractConstraints(IMethod method) throws JavaModelException
    {
        IAnnotation annotation = TapestryUtils.findAnnotation(method.getAnnotations(),
                TapestryUtils.ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_ORDER);
        
        return annotation == null
                ? null
                : EclipseUtils.readValuesFromAnnotation(
                        tapestryModule.getEclipseProject(), annotation, "value");
    }

    //  TODO Contribution doesn't use @Match, contributeXXX uses service-id matcher,
    //  with @Contribute annotation - only uses service interface & markers
    //  TODO Contribution also works with @Local, in this case should only match module's own services
    private Matcher createMatcherForInstrumenter(IMethod method, String serviceId, List<String> markers) throws JavaModelException
    {
        AndMatcher matcher = new AndMatcher();
        
        IAnnotation match = TapestryUtils.findAnnotation(method.getAnnotations(),
                TapestryUtils.ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_MATCH);
        
        if (match != null)
        {
            String[] patterns = EclipseUtils.readValuesFromAnnotation(
                    tapestryModule.getEclipseProject(), match, "value");
            
            for (String pattern : patterns)
            {
                matcher.add(new GlobPatternMatcher(pattern));
            }
        }
        
        if (markers.size() > 0)
        {
            for (String marker : markers)
            {
                matcher.add(new MarkerMatcher(marker));
            }
        }
        
        return matcher;
    }
    
    //  TODO Contribution doesn't use @Match, contributeXXX uses service-id matcher,
    //  with @Contribute annotation - only uses service interface & markers
    //  TODO Contribution also works with @Local, in this case should only match module's own services
    private Matcher createServiceMatcherForConfigurationContribution(String serviceId, String serviceIntf, List<String> markers) throws JavaModelException
    {
        AndMatcher matcher = new AndMatcher();
        
        if (StringUtils.isNotEmpty(serviceId))
        {
            matcher.add(new IdentityIdMatcher(serviceId));
        }
        
        if (StringUtils.isNotEmpty(serviceIntf))
        {
            matcher.add(new ServiceIntfMatcher(serviceIntf));
        }
        
        if (markers.size() > 0)
        {
            for (String marker : markers)
            {
                matcher.add(new MarkerMatcher(marker));
            }
        }
        
        return matcher;
    }

    private String extractId(String serviceInterface, String id)
    {
        return StringUtils.isNotEmpty(id)
             ? id
             : StringUtils.isNotEmpty(serviceInterface)
                 ? TapestryUtils.getSimpleName(serviceInterface)
                 : null;
    }

    private void addServiceFromBuilderMethod(IMethod method) throws JavaModelException
    {
        IAnnotation annotation = TapestryUtils.findAnnotation(
                method.getAnnotations(),
                TapestryUtils.ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_SERVICE_ID);
        
        String typeName = EclipseUtils.resolveTypeNameForMember(
                tapestryModule.getModuleClass(), method, method.getReturnType());
        
        final AtomicReference<String> serviceId = new AtomicReference<String>();
        
        if (annotation != null)
        {
            serviceId.set(EclipseUtils.readFirstValueFromAnnotation(
                    tapestryModule.getEclipseProject(), annotation, "value"));
        }
        else
        {
            String id = stripMethodPrefix(method, TapestryUtils.BUILD_METHOD_NAME_PREFIX);
            
            if (StringUtils.isEmpty(id))
            {
                id = TapestryUtils.getSimpleName(typeName);
            }
            
            serviceId.set(id);
        }
        
        serviceFound.callback(
                new TapestryService(
                        tapestryModule,
                        new ServiceDefinition()
                            .setIntfClass(typeName)
                            .setId(serviceId.get())
                            .addMarkers(tapestryModule.markers())
                            .addMarkers(tapestryModule.readMarkerAnnotation(method)),
                        new JavaElementReference(tapestryModule, method)));
    }

    private String stripMethodPrefix(IMethod method, String prefix)
    {
        return method.getElementName().substring(prefix.length());
    }

    private List<String> extractMarkers(IAnnotatable annotatable, Set<String> skipAnnotations)
                    throws JavaModelException
    {
        List<String> markers = new ArrayList<String>();
        
        for (IAnnotation annotation : annotatable.getAnnotations())
        {
            String typeName = EclipseUtils.resolveTypeName(
                    tapestryModule.getModuleClass(), annotation.getElementName());
            
            if (skipAnnotations.contains(typeName))
            {
                continue;
            }
            
            markers.add(typeName);
        }
        return markers;
    }

}
