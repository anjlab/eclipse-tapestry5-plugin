package com.anjlab.eclipse.tapestry5;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeLiteral;

import com.anjlab.eclipse.tapestry5.DeclarationReference.ASTNodeReference;
import com.anjlab.eclipse.tapestry5.TapestryService.ServiceDefinition;

public abstract class TapestryModule
{
    private TapestryProject project;
    private IType moduleClass;
    private List<ModuleReference> references = new ArrayList<ModuleReference>();
    
    private boolean sourceAvailable;
    private boolean appModule;
    private boolean tapestryCoreModule;
    
    public static abstract class ModuleReference
    {
        abstract String getLabel();
        
        @Override
        public String toString()
        {
            return getLabel();
        }
    }
    
    public TapestryModule(TapestryProject project, IType moduleClass)
    {
        this.project = project;
        this.moduleClass = moduleClass;
    }
    
    public static TapestryModule createTapestryModule(TapestryProject project, IType moduleClass, ObjectCallback<TapestryModule, RuntimeException> moduleCreated)
    {
        final TapestryModule module;
        
        if (moduleClass.getResource() != null)
        {
            module = new LocalTapestryModule(project, moduleClass);
        }
        else
        {
            module = new JarTapestryModule(project, moduleClass);
        }
        
        if (moduleCreated != null)
        {
            moduleCreated.callback(module);
        }
        
        return module;
    }
    
    public TapestryProject getProject()
    {
        return project;
    }
    
    public IProject getEclipseProject()
    {
        return moduleClass.getJavaProject().getProject();
    }
    
    public ModuleReference getReference()
    {
        return references.get(0);
    }
    
    public void setReference(ModuleReference reference)
    {
        addReference(reference);
    }
    
    public void addReference(ModuleReference reference)
    {
        this.references.add(reference);
    }
    
    public List<ModuleReference> references()
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
        monitor.subTask("Analyzing " + moduleClass.getFullyQualifiedName() + "...");
        
        findSubModules(monitor);
        
        findLibraryMappings(monitor);
        
        findJavaScriptStacks(monitor);
        
        findComponents(monitor);
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
            IAnnotation annotation = TapestryUtils.findAnnotation(
                    moduleClass.getAnnotations(), "org.apache.tapestry5.ioc.annotations.SubModule");
            
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
                                createTapestryModule(
                                        project,
                                        subModuleClass,
                                        new ObjectCallback<TapestryModule, RuntimeException>()
                                        {
                                            @Override
                                            public void callback(TapestryModule obj)
                                            {
                                                obj.setReference(new ModuleReference()
                                                {
                                                    @Override
                                                    public String getLabel()
                                                    {
                                                        return "via @SubModule of " + getName();
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
        
        CompilationUnit compilationUnit = getModuleClassCompilationUnit();
        
        if (compilationUnit == null)
        {
            return;
        }
        
        compilationUnit.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(MethodInvocation node)
            {
                if (monitor.isCanceled())
                {
                    return false;
                }
                
                if (Arrays.binarySearch(ADD_OVERRIDE_INSTANCE, node.getName().toString()) < 0
                        || node.arguments().size() != 2)
                {
                    return super.visit(node);
                }
                
                Object typeArg = node.arguments().get(1);
                
                if (!(typeArg instanceof TypeLiteral))
                {
                    return super.visit(node);
                }
                
                String className = EclipseUtils.toClassName((TypeLiteral) typeArg);
                
                if (className == null)
                {
                    return super.visit(node);
                }
                
                IType declaration = EclipseUtils.findTypeDeclaration(getEclipseProject(), className);
                
                if (declaration == null)
                {
                    return super.visit(node);
                }
                
                try
                {
                    String[] interfaceNames = declaration.getSuperInterfaceNames();
                    
                    for (String interfaceName : interfaceNames)
                    {
                        if (TapestryUtils.isTapestryJavaScriptStackInterface(interfaceName))
                        {
                            Object stackExpr = node.arguments().get(0);
                            String stackName = EclipseUtils.evalExpression(getEclipseProject(), stackExpr);
                            
                            javaScriptStacks.add(new JavaScriptStack(
                                    stackName,
                                    declaration,
                                    Arrays.binarySearch(OVERRIDES, node.getName().toString()) >= 0,
                                    new ASTNodeReference(getModuleClass(), node)));
                        }
                    }
                }
                catch (JavaModelException e)
                {
                    Activator.getDefault().logWarning(
                            "Unable to get super interfaces of " + declaration);
                }
                
                return super.visit(node);
            }
        });
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

    private static final String[] ADD_OVERRIDE = { "add", "override" };
    private static final String[] ADD_OVERRIDE_INSTANCE = { "addInstance", "overrideInstance" };
    private static final String[] OVERRIDES = { "override", "overrideInstance" };
    
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
        
        compilationUnit.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(MethodInvocation node)
            {
                if (monitor.isCanceled())
                {
                    return false;
                }
                
                if (Arrays.binarySearch(ADD_OVERRIDE, node.getName().toString()) < 0
                        || node.arguments().size() != 1)
                {
                    return super.visit(node);
                }
                
                Object arg = node.arguments().get(0);
                
                if (!(arg instanceof ClassInstanceCreation))
                {
                    return super.visit(node);
                }
                
                ClassInstanceCreation creation = (ClassInstanceCreation) arg;
                
                Type creationType = creation.getType();
                
                if (creationType.isSimpleType())
                {
                    Name name = ((SimpleType) creationType).getName();
                    
                    if (name.isSimpleName())
                    {
                        if ("LibraryMapping".equals(((SimpleName) name).getIdentifier())
                                && creation.arguments().size() == 2)
                        {
                            Object prefixExpr = creation.arguments().get(0);
                            Object packageExpr = creation.arguments().get(1);
                            
                            String prefix = EclipseUtils.evalExpression(project.getProject(), prefixExpr);
                            String pkg = "".equals(prefix)
                                       ? TapestryUtils.getAppPackage(project.getProject())
                                       : EclipseUtils.evalExpression(project.getProject(), packageExpr);
                            
                            if (prefix != null && !StringUtils.isEmpty(pkg))
                            {
                                libraryMappings.add(new LibraryMapping(prefix, pkg, new ASTNodeReference(getModuleClass(), node)));
                            }
                            else
                            {
                                Activator.getDefault().logWarning(
                                        "Unable to evaluate LibraryMapping(" + prefixExpr + ", " + packageExpr + ")");
                            }
                        }
                    }
                }
                
                return super.visit(node);
            }
        });
    }
    
    private volatile List<TapestryService> services;
    
    public List<TapestryService> services()
    {
        if (services == null)
        {
            findServices(new NullProgressMonitor());
        }
        return services;
    }
    
    private synchronized void findServices(final IProgressMonitor monitor)
    {
        if (services != null)
        {
            return;
        }
        
        services = new ArrayList<TapestryService>();
        
        enumModuleMethods();
        
        visitBindInvocations();
    }

    private void visitBindInvocations()
    {
        final CompilationUnit compilationUnit = getModuleClassCompilationUnit();
        
        if (compilationUnit == null)
        {
            return;
        }
        
        compilationUnit.accept(new ASTVisitor()
        {
            private TapestryService.ServiceDefinition serviceDefinition;
            
            private TapestryService.ServiceDefinition serviceDefinition()
            {
                if (serviceDefinition == null)
                {
                    serviceDefinition = new TapestryService.ServiceDefinition();
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
                String identifier = node.getName().getIdentifier();
                
                if ("withMarker".equals(identifier))
                {
                    for (Object arg : node.arguments())
                    {
                        if (arg instanceof TypeLiteral)
                        {
                            serviceDefinition().addMarker(
                                    EclipseUtils.toClassName((TypeLiteral) arg));
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
                                        getEclipseProject(), node.arguments().get(0)));
                    }
                    return analyzeInvocationChain(node);
                }
                else if ("withId".equals(identifier))
                {
                    if (node.arguments().size() == 1)
                    {
                        serviceDefinition().setId(
                                EclipseUtils.evalExpression(
                                        getEclipseProject(), node.arguments().get(0)));
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
                        TapestryService.ServiceDefinition definition = serviceDefinition();
                        
                        definition.setIntfClass(EclipseUtils.toClassName((TypeLiteral) intf));
                        
                        if (impl instanceof TypeLiteral)
                        {
                            definition.setImplClass(EclipseUtils.toClassName((TypeLiteral) impl));
                        }
                        
                        if (definition.isSimpleId())
                        {
                            if (StringUtils.isNotEmpty(definition.getImplClass()))
                            {
                                definition.setId(TapestryUtils.simpleName(definition.getImplClass()));
                            }
                        }
                        else if (StringUtils.isEmpty(definition.getId()))
                        {
                            definition.setId(TapestryUtils.simpleName(definition.getIntfClass()));
                        }
                        
                        services.add(new TapestryService(
                                TapestryModule.this,
                                definition,
                                new ASTNodeReference(getModuleClass(), node)));
                    }
                    
                    serviceDefinition = null;
                    
                    return false;
                }
                
                return super.visit(node);
            }
        });
    }

    private void enumModuleMethods()
    {
        try
        {
            for (IMethod method : getModuleClass().getMethods())
            {
                // TODO builder methods
                // TODO contributions
                // TODO @Startup
                // TODO decorations
                // TODO advises
                
                if (isServiceBuilderMethod(method))
                {
                    addServiceFromBuilderMethod(method);
                }
            }
        }
        catch (JavaModelException e)
        {
            Activator.getDefault().logError(
                    "Error enumerating builder methods for " + getModuleClass().getFullyQualifiedName(), e);
        }
    }

    private void addServiceFromBuilderMethod(IMethod method) throws JavaModelException
    {
        IAnnotation annotation = TapestryUtils.findAnnotation(
                method.getAnnotations(), "org.apache.tapestry5.ioc.annotations.ServiceId");
        
        String typeName = EclipseUtils.resolveTypeNameForMember(moduleClass, method, method.getReturnType());
        
        final AtomicReference<String> serviceId = new AtomicReference<String>();
        
        if (annotation != null)
        {
            TapestryUtils.readValueFromAnnotation(
                    annotation,
                    "value",
                    getEclipseProject(),
                    AST.newAST(AST.JLS8),
                    new ObjectCallback<String, JavaModelException>()
                    {
                        @Override
                        public void callback(String value) throws JavaModelException
                        {
                            serviceId.set(value);
                        }
                    });
        }
        else
        {
            String id = method.getElementName().substring("build".length());
            
            if (StringUtils.isEmpty(id))
            {
                int lastDot = typeName.lastIndexOf('.');
                
                id = lastDot != -1 ? typeName.substring(lastDot + 1) : typeName;
            }
            
            serviceId.set(id);
        }
        
        //  TODO Check @Marker annotations
        
        services.add(new TapestryService(
                TapestryModule.this,
                new ServiceDefinition()
                    .setIntfClass(typeName)
                    .setId(serviceId.get()),
                new DeclarationReference.JavaElementReference(method)));
    }
    
    private boolean isServiceBuilderMethod(IMethod method)
    {
        return method.getElementName().startsWith("build");
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
        
        compilationUnit = EclipseUtils.parse(source);
        
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
                    componentContext = TapestryUtils.createTapestryContext((IFile) obj);
                }
                else if (obj instanceof IClassFile)
                {
                    IClassFile classFile = (IClassFile) obj;
                    
                    //  Ignore inner classes
                    if (!classFile.getElementName().contains("$"))
                    {
                        componentContext = TapestryUtils.createTapestryContext(classFile);
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

    protected abstract void enumJavaClassesRecursively(IProgressMonitor monitor, String rootPackage, ObjectCallback<Object, RuntimeException> callback);

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
