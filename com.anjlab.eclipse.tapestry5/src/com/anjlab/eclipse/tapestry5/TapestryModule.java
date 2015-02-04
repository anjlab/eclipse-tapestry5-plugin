package com.anjlab.eclipse.tapestry5;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import com.anjlab.eclipse.tapestry5.DeclarationReference.ASTNodeReference;
import com.anjlab.eclipse.tapestry5.DeclarationReference.JavaElementReference;
import com.anjlab.eclipse.tapestry5.TapestryService.InstrumenterType;
import com.anjlab.eclipse.tapestry5.TapestryService.Matcher;
import com.anjlab.eclipse.tapestry5.TapestryService.ServiceDefinition;
import com.anjlab.eclipse.tapestry5.TapestryService.ServiceInstrumenter;
import com.anjlab.eclipse.tapestry5.internal.AndMatcher;
import com.anjlab.eclipse.tapestry5.internal.GlobPatternMatcher;
import com.anjlab.eclipse.tapestry5.internal.IdentityIdMatcher;
import com.anjlab.eclipse.tapestry5.internal.IdentityMatcher;
import com.anjlab.eclipse.tapestry5.internal.MarkerMatcher;
import com.anjlab.eclipse.tapestry5.internal.OrMatcher;
import com.anjlab.eclipse.tapestry5.internal.visitors.JavaScriptStackCapturingVisitor;
import com.anjlab.eclipse.tapestry5.internal.visitors.LibraryMappingCapturingVisitor;
import com.anjlab.eclipse.tapestry5.internal.visitors.TapestryServiceCapturingVisitor;

public abstract class TapestryModule
{
    private TapestryProject project;
    private IType moduleClass;
    private List<TapestryModuleReference> references = new ArrayList<TapestryModuleReference>();
    
    private boolean sourceAvailable;
    private boolean appModule;
    private boolean tapestryCoreModule;
    
    public TapestryModule(TapestryProject project, IType moduleClass)
    {
        this.project = project;
        this.moduleClass = moduleClass;
    }
    
    public TapestryProject getProject()
    {
        return project;
    }
    
    public IProject getEclipseProject()
    {
        return moduleClass.getJavaProject().getProject();
    }
    
    public void addReference(TapestryModuleReference reference)
    {
        if (!references.contains(reference))
        {
            references.add(reference);
        }
    }
    
    public List<TapestryModuleReference> references()
    {
        return Collections.unmodifiableList(references);
    }
    
    public IType getModuleClass()
    {
        return moduleClass;
    }
    
    public String getName()
    {
        return moduleClass.getElementName();
    }
    
    public void initialize(IProgressMonitor monitor)
    {
        subTask(monitor, "markers");
        
        findMarkers(monitor);
        
        subTask(monitor, "imported modules");
        
        findSubModules(monitor);
        
        subTask(monitor, "library mappings");
        
        findLibraryMappings(monitor);
        
        subTask(monitor, "components");
        
        findComponents(monitor);
        
        subTask(monitor, "services");
        
        findServices(monitor);
        
        subTask(monitor, "JavaScript stacks");
        
        findJavaScriptStacks(monitor);
    }

    private void subTask(IProgressMonitor monitor, String name)
    {
        monitor.subTask("Analyzing " + moduleClass.getFullyQualifiedName() + " (" + name + ")...");
    }
    
    private volatile List<String> markers;
    
    private synchronized void findMarkers(IProgressMonitor monitor)
    {
        if (markers != null)
        {
            return;
        }
        
        try
        {
            markers = readMarkerAnnotation(moduleClass);
        }
        catch (JavaModelException e)
        {
            Activator.getDefault().logError("Error getting markers for " + getName(), e);
        }
    }

    public List<String> readMarkerAnnotation(IAnnotatable annotatable) throws JavaModelException
    {
        List<String> markers = new ArrayList<String>();
        
        IAnnotation annotation = TapestryUtils.findAnnotation(annotatable.getAnnotations(),
                TapestryUtils.ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_MARKER);
        
        if (annotation != null)
        {
            String[] typeLiterals = EclipseUtils.readValuesFromAnnotation(getEclipseProject(), annotation, "value");
            
            for (String typeLiteral : typeLiterals)
            {
                String typeName = EclipseUtils.resolveTypeName(moduleClass, typeLiteral);
                
                markers.add(typeName);
            }
        }
        
        return markers;
    }

    public List<String> markers()
    {
        if (markers == null)
        {
            findMarkers(new NullProgressMonitor());
        }
        return markers;
    }
    
    private volatile List<TapestryModule> subModules;
    
    public List<TapestryModule> subModules()
    {
        if (subModules == null)
        {
            findSubModules(new NullProgressMonitor());
        }
        
        return subModules;
    }

    private synchronized void findSubModules(IProgressMonitor monitor)
    {
        if (subModules != null)
        {
            return;
        }
        
        subModules = new ArrayList<TapestryModule>();
        
        try
        {
            final IAnnotation annotation = findSubmoduleAnnotation();
            
            if (annotation == null)
            {
                return;
            }
            
            for (IMemberValuePair pair : annotation.getMemberValuePairs())
            {
                Object[] classes = pair.getValue().getClass().isArray()
                                 ? (Object[]) pair.getValue()
                                 : new Object[] { pair.getValue() };
                
                for (Object className : classes)
                {
                    if (monitor.isCanceled())
                    {
                        return;
                    }
                    
                    IType subModuleClass = EclipseUtils.findTypeDeclaration(
                            moduleClass.getJavaProject().getProject(), (String) className);
                    
                    if (subModuleClass != null)
                    {
                        subModules.add(
                                Activator.getDefault()
                                    .getTapestryModuleFactory()
                                    .createTapestryModule(
                                        project,
                                        subModuleClass,
                                        new ObjectCallback<TapestryModule, RuntimeException>()
                                        {
                                            @Override
                                            public void callback(TapestryModule obj)
                                            {
                                                obj.addReference(new TapestryModuleReference(new JavaElementReference(annotation))
                                                {
                                                    @Override
                                                    public String getLabel()
                                                    {
                                                        String annotationName = TapestryUtils.getSimpleName(annotation.getElementName());
                                                        return "via @" + annotationName + " of " + getName();
                                                    }
                                                });
                                            }
                                        }));
                    }
                }
            }
        }
        catch (JavaModelException e)
        {
            Activator.getDefault().logError("Error getting submodules for " + getName(), e);
        }
    }

    private IAnnotation findSubmoduleAnnotation() throws JavaModelException
    {
        IAnnotation annotation = TapestryUtils.findAnnotation(
                moduleClass.getAnnotations(),
                TapestryUtils.ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_SUB_MODULE);
        
        if (annotation == null)
        {
            annotation = TapestryUtils.findAnnotation(
                    moduleClass.getAnnotations(),
                    TapestryUtils.ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_IMPORT_MODULE);
        }
        
        return annotation;
    }
    
    private List<JavaScriptStack> javaScriptStacks;
    
    public List<JavaScriptStack> javaScriptStacks()
    {
        if (javaScriptStacks == null)
        {
            findJavaScriptStacks(new NullProgressMonitor());
        }
        return javaScriptStacks;
    }
    
    private synchronized void findJavaScriptStacks(final IProgressMonitor monitor)
    {
        if (javaScriptStacks != null)
        {
            return;
        }
        
        javaScriptStacks = new ArrayList<JavaScriptStack>();
        
        final ASTVisitor javaScriptStackCapturingVisitor = new JavaScriptStackCapturingVisitor(
                monitor, this, new ObjectCallback<JavaScriptStack, RuntimeException>()
                {
                    @Override
                    public void callback(JavaScriptStack stack)
                    {
                        javaScriptStacks.add(stack);
                    }
                });
        
        //  JavaScriptStackCapturingVisitor is a heavy handler,
        //  we don't want to parse every method declaration with it
        //  So only parse actual JavaScriptStackSource contributions
        
        final TapestryService javaScriptStackSource = new TapestryService(
                null,
                new ServiceDefinition()
                    .setId("JavaScriptStackSource")
                    .setIntfClass("org.apache.tapestry5.services.javascript.JavaScriptStackSource"),
                null);
        
        visitContributions(javaScriptStackSource, javaScriptStackCapturingVisitor);
    }

    private void visitContributions(final TapestryService targetService, final ASTVisitor visitor)
    {
        for (ServiceInstrumenter contributor : contributors())
        {
            if (contributor.getServiceMatcher().matches(targetService))
            {
                DeclarationReference contributionDeclaration = contributor.getReference();
                
                if (contributionDeclaration instanceof ASTNodeReference)
                {
                    ASTNode node = ((ASTNodeReference) contributionDeclaration).getNode();
                    
                    //  Find declaring method
                    while (!(node instanceof MethodDeclaration) && node != null)
                    {
                        node = node.getParent();
                    }
                    
                    if (node != null)
                    {
                        node.accept(visitor);
                    }
                }
                else if (contributionDeclaration instanceof JavaElementReference)
                {
                    final IJavaElement element = ((JavaElementReference) contributionDeclaration).getElement();
                    
                    if (element instanceof IMethod)
                    {
                        //  Parse source code of method definition
                        
                        final CompilationUnit unit = getModuleClassCompilationUnit();
                        
                        if (unit != null)
                        {
                            unit.accept(new ASTVisitor()
                            {
                                @Override
                                public boolean visit(MethodDeclaration node)
                                {
                                    if (node.getName().getIdentifier().equals(element.getElementName()))
                                    {
                                        node.accept(visitor);
                                    }
                                    return false;
                                }
                            });
                        }
                    }
                }
            }
        }
    }

    private volatile List<LibraryMapping> libraryMappings;

    public List<LibraryMapping> libraryMappings()
    {
        if (libraryMappings == null)
        {
            findLibraryMappings(new NullProgressMonitor());
        }
        return libraryMappings;
    }

    public static final String[] ADD_OVERRIDE = { "add", "override" };
    public static final String[] ADD_OVERRIDE_INSTANCE = { "addInstance", "overrideInstance" };
    public static final String[] OVERRIDES = { "override", "overrideInstance" };
    
    private synchronized void findLibraryMappings(final IProgressMonitor monitor)
    {
        if (libraryMappings != null)
        {
            return;
        }
        
        libraryMappings = new ArrayList<LibraryMapping>();
        
        CompilationUnit compilationUnit = getModuleClassCompilationUnit();
        
        if (compilationUnit == null)
        {
            return;
        }
        
        compilationUnit.accept(new LibraryMappingCapturingVisitor(monitor, this,
                new ObjectCallback<LibraryMapping, RuntimeException>()
                {
                    @Override
                    public void callback(LibraryMapping mapping)
                    {
                        libraryMappings.add(mapping);
                    }
                }));
    }
    
    private volatile List<TapestryService> services;
    private volatile List<ServiceInstrumenter> decorators;
    private volatile List<ServiceInstrumenter> advisors;
    private volatile List<ServiceInstrumenter> contributors;
    
    public List<TapestryService> services()
    {
        if (services == null)
        {
            findServices(new NullProgressMonitor());
        }
        return services;
    }
    
    public List<ServiceInstrumenter> decorators()
    {
        if (decorators == null)
        {
            findServices(new NullProgressMonitor());
        }
        return decorators;
    }
    
    public List<ServiceInstrumenter> advisors()
    {
        if (advisors == null)
        {
            findServices(new NullProgressMonitor());
        }
        return advisors;
    }
    
    public List<ServiceInstrumenter> contributors()
    {
        if (contributors == null)
        {
            findServices(new NullProgressMonitor());
        }
        return contributors;
    }
    
    private synchronized void findServices(final IProgressMonitor monitor)
    {
        if (services != null)
        {
            return;
        }
        
        services = new ArrayList<TapestryService>();
        
        decorators = new ArrayList<ServiceInstrumenter>();
        advisors = new ArrayList<ServiceInstrumenter>();
        contributors = new ArrayList<ServiceInstrumenter>();
        
        enumModuleMethods(monitor);
        
        visitBindInvocations(monitor);
    }

    private void visitBindInvocations(final IProgressMonitor monitor)
    {
        final CompilationUnit compilationUnit = getModuleClassCompilationUnit();
        
        if (compilationUnit == null)
        {
            return;
        }
        
        compilationUnit.accept(
                new TapestryServiceCapturingVisitor(monitor, this,
                        new ObjectCallback<TapestryService, RuntimeException>()
                        {
                            @Override
                            public void callback(TapestryService service)
                            {
                                services.add(service);
                            }
                        }));
    }

    private void enumModuleMethods(IProgressMonitor monitor)
    {
        try
        {
            for (IMethod method : getModuleClass().getMethods())
            {
                if (monitor.isCanceled())
                {
                    return;
                }
                
                try
                {
                    if (isServiceBuilderMethod(method))
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
                            + " for " + getModuleClass().getFullyQualifiedName(), e);
                }
            }
        }
        catch (Exception e)
        {
            Activator.getDefault().logError(
                    "Error enumerating methods for " + getModuleClass().getFullyQualifiedName(), e);
        }
    }

    private void addStartupContributor(IMethod method) throws JavaModelException
    {
        contributors.add(new ServiceInstrumenter()
            .setType(InstrumenterType.CONTRIBUTOR)
            .setId("RegistryStartup")
            .setReference(new JavaElementReference(method))
            .setServiceMatcher(new IdentityIdMatcher("RegistryStartup"))
            .setConstraints(extractConstraints(method)));
    }

    private void addContributionMethod(IMethod method) throws JavaModelException
    {
        IAnnotation annotation = TapestryUtils.findAnnotation(method.getAnnotations(),
                TapestryUtils.ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_CONTRIBUTE);
        
        String serviceInterface = annotation == null ? null
                : EclipseUtils.readFirstValueFromAnnotation(getEclipseProject(), annotation, "value");
        
        String id = annotation == null
                ? stripMethodPrefix(method, TapestryUtils.CONTRIBUTE_METHOD_NAME_PREFIX)
                : extractId(serviceInterface, null);
        
        List<String> markers = extractMarkers(method, new HashSet<String>(Arrays.asList(
                TapestryUtils.ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_CONTRIBUTE,
                TapestryUtils.ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_ORDER,
                TapestryUtils.ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_MATCH)));
        
        Matcher serviceMatcher = createMatcher(method, id, markers);
        
        contributors.add(new ServiceInstrumenter()
            .setType(InstrumenterType.CONTRIBUTOR)
            .setId(id)
            .setReference(new JavaElementReference(method))
            .setServiceMatcher(serviceMatcher)
            .setConstraints(extractConstraints(method)));
    }

    private void addServiceAdvisor(IMethod method) throws JavaModelException
    {
        advisors.add(
                createInstrumenter(method,
                        TapestryUtils.ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_ADVISE,
                        TapestryUtils.ADVISE_METHOD_NAME_PREFIX,
                        InstrumenterType.ADVISOR));
    }

    private void addServiceDecorator(IMethod method) throws JavaModelException
    {
        decorators.add(
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
                : EclipseUtils.readFirstValueFromAnnotation(getEclipseProject(), annotation, "serviceInterface");
        
        String id = annotation == null
                ? stripMethodPrefix(method, methodNamePrefix)
                : extractId(serviceInterface, EclipseUtils.readFirstValueFromAnnotation(getEclipseProject(), annotation, "id"));
        
        List<String> markers = extractMarkers(method, new HashSet<String>(Arrays.asList(
                instrumenterAnnotation,
                TapestryUtils.ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_ORDER,
                TapestryUtils.ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_MATCH)));
        
        Matcher serviceMatcher = createMatcher(method, id, markers);
        
        return new ServiceInstrumenter()
                .setType(instrumenterType)
                .setId(id)
                .setReference(new JavaElementReference(method))
                .setServiceMatcher(serviceMatcher)
                .setConstraints(extractConstraints(method));
    }

    private String[] extractConstraints(IMethod method) throws JavaModelException
    {
        IAnnotation annotation = TapestryUtils.findAnnotation(method.getAnnotations(),
                TapestryUtils.ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_ORDER);
        
        return annotation == null ? null : EclipseUtils.readValuesFromAnnotation(getEclipseProject(), annotation, "value");
    }

    private Matcher createMatcher(IMethod method, String serviceId, List<String> markers) throws JavaModelException
    {
        IAnnotation match = TapestryUtils.findAnnotation(method.getAnnotations(),
                TapestryUtils.ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_MATCH);
        
        Matcher patternMatcher;
        
        if (match != null)
        {
            patternMatcher = new OrMatcher();
            
            String[] patterns = EclipseUtils.readValuesFromAnnotation(getEclipseProject(), match, "value");
            
            for (String pattern : patterns)
            {
                ((OrMatcher) patternMatcher).add(new GlobPatternMatcher(pattern));
            }
        }
        else
        {
            patternMatcher = StringUtils.isNotEmpty(serviceId)
                    ? new IdentityIdMatcher(serviceId)
                    // Match everything, ambiguity would probably be resolved with markers
                    : new IdentityMatcher(true);
        }
        
        if (markers.size() > 0)
        {
            OrMatcher markerMatcher = new OrMatcher();
            
            for (String marker : markers)
            {
                markerMatcher.add(new MarkerMatcher(marker));
            }
            
            AndMatcher andMatcher = new AndMatcher();
            andMatcher.add(patternMatcher);
            andMatcher.add(markerMatcher);
            
            return andMatcher;
        }
        
        return patternMatcher;
    }

    private List<String> extractMarkers(IAnnotatable annotatable, Set<String> skipAnnotations) throws JavaModelException
    {
        List<String> markers = new ArrayList<String>();
        
        for (IAnnotation annotation : annotatable.getAnnotations())
        {
            String typeName = EclipseUtils.resolveTypeName(
                    moduleClass, annotation.getElementName());
            
            if (skipAnnotations.contains(typeName))
            {
                continue;
            }
            
            markers.add(typeName);
        }
        return markers;
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
        
        String typeName = EclipseUtils.resolveTypeNameForMember(moduleClass, method, method.getReturnType());
        
        final AtomicReference<String> serviceId = new AtomicReference<String>();
        
        if (annotation != null)
        {
            serviceId.set(EclipseUtils.readFirstValueFromAnnotation(getEclipseProject(), annotation, "value"));
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
        
        services.add(new TapestryService(
                TapestryModule.this,
                new ServiceDefinition()
                    .setIntfClass(typeName)
                    .setId(serviceId.get())
                    .addMarkers(markers())
                    .addMarkers(readMarkerAnnotation(method)),
                new JavaElementReference(method)));
    }

    private String stripMethodPrefix(IMethod method, String prefix)
    {
        return method.getElementName().substring(prefix.length());
    }
    
    private boolean isServiceBuilderMethod(IMethod method)
    {
        return method.getElementName().startsWith(TapestryUtils.BUILD_METHOD_NAME_PREFIX);
    }
    
    private CompilationUnit compilationUnit;
    
    private CompilationUnit getModuleClassCompilationUnit()
    {
        if (compilationUnit != null)
        {
            return compilationUnit;
        }
        
        IClassFile classFile = moduleClass.getClassFile();
        
        String source = null;
        
        try
        {
            source = classFile != null
                          ? classFile.getSource()
                          : moduleClass.getCompilationUnit().getSource();
        }
        catch (JavaModelException e)
        {
            Activator.getDefault().logError("Error getting source file", e);
        }
        
        this.sourceAvailable = source != null;
        
        if (!sourceAvailable)
        {
            return null;
        }
        
        compilationUnit = (CompilationUnit) EclipseUtils.parse(source, ASTParser.K_COMPILATION_UNIT);
        
        return compilationUnit;
    }

    private List<TapestryContext> components;
    
    public List<TapestryContext> getComponents()
    {
        if (components == null)
        {
            findComponents(new NullProgressMonitor());
        }
        return components;
    }
    
    public interface ObjectCallback<T, E extends Throwable>
    {
        void callback(T obj) throws E;
    }
    
    private synchronized void findComponents(IProgressMonitor monitor)
    {
        if (components != null)
        {
            return;
        }
        
        components = new ArrayList<TapestryContext>();
        
        if (intermediateComponents != null)
        {
            components.addAll(intermediateComponents);
        }
        
        ObjectCallback<Object, RuntimeException> componentClassFound = createComponentClassFoundHandler();
        
        for (LibraryMapping mapping : libraryMappings())
        {
            String componentsPackage = mapping.getRootPackage() + ".components";
            
            if (monitor.isCanceled())
            {
                return;
            }
            
            if (mapping.getPathPrefix().isEmpty() && isTapestryCoreModule())
            {
                //  This package is from the AppModule
                for (TapestryModule module : getProject().modules())
                {
                    if (module.isAppModule())
                    {
                        //  Components should go to AppModule by using its own componentClassFound handler
                        module.enumJavaClassesRecursively(monitor, componentsPackage);
                        break;
                    }
                }
            }
            else
            {
                enumJavaClassesRecursively(monitor, componentsPackage, componentClassFound);
            }
        }
    }
    
    private void enumJavaClassesRecursively(IProgressMonitor monitor, String componentsPackage)
    {
        enumJavaClassesRecursively(monitor, componentsPackage, createComponentClassFoundHandler());
    }

    private List<TapestryContext> intermediateComponents;

    private ObjectCallback<Object, RuntimeException> createComponentClassFoundHandler()
    {
        ObjectCallback<Object, RuntimeException> componentClassFound = new ObjectCallback<Object, RuntimeException>()
        {
            @Override
            public void callback(Object obj)
            {
                TapestryContext componentContext = null;
                
                if (obj instanceof IFile)
                {
                    componentContext = Activator.getDefault()
                            .getTapestryContextFactory()
                            .createTapestryContext((IFile) obj);
                }
                else if (obj instanceof IClassFile)
                {
                    IClassFile classFile = (IClassFile) obj;
                    
                    //  Ignore inner classes
                    if (!classFile.getElementName().contains("$"))
                    {
                        componentContext = Activator.getDefault()
                                .getTapestryContextFactory()
                                .createTapestryContext(classFile);
                    }
                }
                
                if (componentContext != null)
                {
                    if (components != null)
                    {
                        //  XXX Make this structure synchronized?
                        components.add(componentContext);
                    }
                    else
                    {
                        if (intermediateComponents == null)
                        {
                            intermediateComponents = new ArrayList<TapestryContext>();
                        }
                        
                        intermediateComponents.add(componentContext);
                    }
                }
            }
        };
        return componentClassFound;
    }

    protected abstract void enumJavaClassesRecursively(
            IProgressMonitor monitor, String rootPackage, ObjectCallback<Object, RuntimeException> callback);

    public abstract TapestryFile getModuleFile();

    public abstract boolean isReadOnly();
    
    public boolean isSourceAvailable()
    {
        return sourceAvailable;
    }

    public void setAppModule(boolean appModule)
    {
        this.appModule = appModule;
    }
    
    public boolean isAppModule()
    {
        return appModule;
    }

    public void setTapestryCoreModule(boolean tapestryCoreModule)
    {
        this.tapestryCoreModule = tapestryCoreModule;
    }
    
    public boolean isTapestryCoreModule()
    {
        return tapestryCoreModule;
    }
    
    public List<LibraryMapping> libraryMappings(String libraryPrefix) throws JavaModelException
    {
        List<LibraryMapping> result = new ArrayList<LibraryMapping>();
        for (LibraryMapping mapping : libraryMappings())
        {
            if (mapping.getPathPrefix().equals(libraryPrefix))
            {
                result.add(mapping);
            }
        }
        return result;
    }

    public abstract TapestryFile findClasspathFileCaseInsensitive(String path);

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof TapestryModule))
        {
            return false;
        }
        return this.moduleClass.equals(((TapestryModule) obj).moduleClass);
    }

    @Override
    public int hashCode()
    {
        return moduleClass.getFullyQualifiedName().hashCode();
    }
    
    public String getComponentName(TapestryContext context)
    {
        String packageName = context.getPackageName();
        String componentFullName = packageName + "." + context.getName();
        
        String componentsPackage;
        
        for (LibraryMapping mapping : libraryMappings())
        {
            if (!mapping.getPathPrefix().isEmpty() && packageName.startsWith(mapping.getRootPackage()))
            {
                componentsPackage = mapping.getRootPackage() + ".components";
                
                String pathPrefix = mapping.getPathPrefix().equals("core") ? "" : mapping.getPathPrefix() + ".";
                
                return pathPrefix + getComponentName(componentFullName, componentsPackage);
            }
        }
        
        componentsPackage = TapestryUtils.getComponentsPackage(getEclipseProject());
        
        return componentsPackage != null && packageName.startsWith(componentsPackage)
             ? getComponentName(componentFullName, componentsPackage)
             : componentFullName;
    }

    private String getComponentName(String componentFullName, String componentsPackage)
    {
        return componentFullName.substring((componentsPackage + ".").length());
    }
}
