package com.anjlab.eclipse.tapestry5;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJarEntryResource;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.MethodInvocation;

import com.anjlab.eclipse.tapestry5.DeclarationReference.ASTNodeReference;
import com.anjlab.eclipse.tapestry5.DeclarationReference.NonJavaReference;
import com.anjlab.eclipse.tapestry5.TapestryService.ServiceDefinition;
import com.anjlab.eclipse.tapestry5.internal.DeclarationCapturingScope.InjectedDeclaration;
import com.anjlab.eclipse.tapestry5.internal.Orderer;
import com.anjlab.eclipse.tapestry5.internal.visitors.TapestryServiceConfigurationCapturingVisitor;
import com.anjlab.eclipse.tapestry5.watchdog.WebXmlReader.WebXml;

public class TapestryProject
{
    private IProject project;
    
    private String tapestryVersion;
    
    private boolean tapestryVersionResolved;
    
    private volatile List<TapestryModule> modules;
    
    public TapestryProject(IProject project)
    {
        this.project = project;
    }
    
    public IProject getProject()
    {
        return project;
    }
    
    private static final Pattern VERSION_MAJOR_MINOR_PATTERN = Pattern.compile("^\\d+\\.\\d+");
    
    public String getTapestryVersionMajorMinor()
    {
        String version = getTapestryVersion();
        
        if (StringUtils.isEmpty(version))
        {
            return null;
        }
        
        Matcher matcher = VERSION_MAJOR_MINOR_PATTERN.matcher(version);
        
        if (matcher.find())
        {
            return matcher.group();
        }
        
        return null;
    }
    
    public String getTapestryVersion()
    {
        if (tapestryVersionResolved)
        {
            //  May be null
            return tapestryVersion;
        }
        
        for (TapestryModule module : modules())
        {
            if (module.isTapestryCoreModule())
            {
                TapestryFile file = module.findClasspathFileCaseInsensitive(
                        "META-INF/gradle/org.apache.tapestry/tapestry-core/project.properties");
                
                if (file instanceof JarEntryFile)
                {
                    try
                    {
                        Properties properties = new Properties();
                        properties.load(((JarEntryFile) file).getJarEntry().getContents());
                        tapestryVersion = properties.getProperty("version");
                    }
                    catch (Exception e)
                    {
                        Activator.getDefault().logError("Error reading tapestry version string", e);
                    }
                }
                
                break;
            }
        }
        
        tapestryVersionResolved = true;
        
        return tapestryVersion;
    }
    
    public List<TapestryModule> modules()
    {
        if (modules == null)
        {
            initialize(new NullProgressMonitor());
        }
        
        return modules;
    }

    public void initialize(IProgressMonitor monitor)
    {
        findModules(monitor);
        
        monitor.subTask("Resolving symbols...");
        
        findSymbols(monitor);
        
        markJavaScriptStackOverrides();
    }

    private Map<String, List<TapestrySymbol>> symbols;
    
    public Map<String, List<TapestrySymbol>> symbols()
    {
        if (symbols == null)
        {
            findSymbols(new NullProgressMonitor());
        }
        
        return symbols;
    }
    
    private synchronized void findSymbols(IProgressMonitor monitor)
    {
        if (symbols != null)
        {
            return;
        }
        
        //  Tapestry symbols are contributions to SymbolProviders.
        //  First we need to grab all contributions to SymbolSource,
        //  this way we will get all SymbolProviders.
        
        final TapestryService symbolSource = new TapestryService(
                null,
                new ServiceDefinition()
                    .setId("SymbolSource")
                    .setIntfClass("org.apache.tapestry5.ioc.services.SymbolSource"),
                null);
        
        final Orderer<TapestryService> orderer = new Orderer<TapestryService>();
        
        for (TapestryModule module : modules())
        {
            module.visitContributions(symbolSource, new TapestryServiceConfigurationCapturingVisitor(monitor, module)
            {
                @Override
                protected void orderedConfigurationAddOverride(MethodInvocation node, String id, Object value, String[] constraints)
                {
                    if (value instanceof IType)
                    {
                        ServiceDefinition definition = new ServiceDefinition()
                            .setId("SymbolProvider")
                            .setIntfClass("org.apache.tapestry5.ioc.services.SymbolProvider")
                            .setImplClass(((IType) value).getFullyQualifiedName());
                        
                        TapestryService symbolProvider = new TapestryService(
                                module, definition,
                                new ASTNodeReference(module, module.getModuleClass(), node));
                        
                        orderer.add(id, symbolProvider, constraints);
                    }
                    else if (value instanceof InjectedDeclaration)
                    {
                        InjectedDeclaration injectedValue = (InjectedDeclaration) value;
                        
                        if (injectedValue.isServiceInjection())
                        {
                            com.anjlab.eclipse.tapestry5.TapestryService.Matcher matcher = injectedValue.createMatcher(module);
                            
                            TapestryService symbolProvider = findService(matcher);
                            
                            if (symbolProvider != null)
                            {
                                orderer.add(id, symbolProvider, constraints);
                            }
                        }
                    }
                }
            }
            .usesOrderedConfiguration());
        }
        
        final List<TapestryService> symbolProviders = orderer.getOrdered();
        
        final Map<TapestryService, List<TapestrySymbol>> providersToSymbols =
                new HashMap<TapestryService, List<TapestrySymbol>>();
        
        for (TapestryModule module : modules())
        {
            for (TapestryService symbolProvider : symbolProviders)
            {
                module.visitContributions(symbolProvider, new TapestryServiceConfigurationCapturingVisitor(monitor, module)
                {
                    @Override
                    protected void mappedConfigurationAddOverride(MethodInvocation node, Object key, Object value)
                    {
                        if (!(key instanceof String))
                        {
                            return;
                        }
                        
                        String symbolName = (String) key;
                        
                        List<TapestrySymbol> providerSymbols = providersToSymbols.get(symbolProvider);
                        
                        if (providerSymbols == null)
                        {
                            providerSymbols = new ArrayList<TapestrySymbol>();
                            
                            providersToSymbols.put(symbolProvider, providerSymbols);
                        }
                        
                        if (value instanceof String)
                        {
                            providerSymbols.add(
                                    new TapestrySymbol(
                                            symbolName,
                                            (String) value,
                                            isOverride(node),
                                            new ASTNodeReference(module, module.getModuleClass(), node),
                                            symbolProvider));
                        }
                        else if (value instanceof InjectedDeclaration)
                        {
                            //  @Symbol?
                            System.out.println(value);
                        }
                        else if (value == null)
                        {
                            providerSymbols.add(
                                    new TapestrySymbol(
                                            symbolName,
                                            null,
                                            isOverride(node),
                                            new ASTNodeReference(module, module.getModuleClass(), node),
                                            symbolProvider));
                        }
                    }
                }.usesMappedConfiguration());
                
                //  Values from the same provider may override each other
                
                List<TapestrySymbol> providerSymbols = providersToSymbols.get(symbolProvider);
                
                if (providerSymbols != null)
                {
                    for (int i = 0; i < providerSymbols.size(); i++)
                    {
                        TapestrySymbol symbol = providerSymbols.get(i);
                        
                        for (int j = i + 1; j < providerSymbols.size(); j++)
                        {
                            TapestrySymbol other = providerSymbols.get(j);
                            
                            if (StringUtils.equals(symbol.getName(), other.getName()))
                            {
                                if (other.isOverride())
                                {
                                    symbol.setOverridden(true);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        symbols = new HashMap<String, List<TapestrySymbol>>();
        
        //  Symbol providers ordered by override rule,
        //  symbols in the first provider override values of the second,
        //  second overrides third, etc.
        
        for (TapestryService symbolProvider : symbolProviders)
        {
            List<TapestrySymbol> providerSymbols = providersToSymbols.get(symbolProvider);
            
            if (providerSymbols != null)
            {
                for (TapestrySymbol symbol : providerSymbols)
                {
                    List<TapestrySymbol> values = symbols.get(symbol.getName());
                    
                    if (values == null)
                    {
                        values = new ArrayList<TapestrySymbol>();
                        symbols.put(symbol.getName(), values);
                    }
                    else
                    {
                        for (TapestrySymbol existing : values)
                        {
                            if (!existing.isOverridden()
                                    //  Some modules may be added conditionally, like QA/DEV/Production
                                    //  we should treat them all as equal, they souldn't override each other,
                                    //  because usually only one of them enabled at run-time
                                    && !existing.getReference().getTapestryModule().isConditional())
                            {
                                symbol.setOverridden(true);
                                break;
                            }
                        }
                    }
                    
                    values.add(symbol);
                }
            }
        }
    }

    private TapestryService findService(
            com.anjlab.eclipse.tapestry5.TapestryService.Matcher matcher)
    {
        for (TapestryModule module : modules())
        {
            for (TapestryService service : module.services())
            {
                if (matcher.matches(service))
                {
                    return service;
                }
            }
        }
        return null;
    }

    private void markJavaScriptStackOverrides()
    {
        Map<String, List<JavaScriptStack>> stacks = new HashMap<String, List<JavaScriptStack>>();
        
        for (TapestryModule module : modules())
        {
            for (JavaScriptStack stack : module.javaScriptStacks())
            {
                List<JavaScriptStack> configs = stacks.get(stack.getName());
                
                if (configs == null)
                {
                    configs = new ArrayList<JavaScriptStack>();
                    stacks.put(stack.getName(), configs);
                }
                
                configs.add(stack);
            }
        }
        
        for (String stackName : stacks.keySet())
        {
            List<JavaScriptStack> configs = stacks.get(stackName);
            
            boolean hasOverride = false;
            
            for (JavaScriptStack stack : configs)
            {
                if (stack.isOverride())
                {
                    hasOverride = true;
                }
            }
            
            if (hasOverride)
            {
                for (JavaScriptStack stack : configs)
                {
                    if (!stack.isOverride())
                    {
                        stack.setOverridden(true);
                    }
                }
            }
        }
    }

    private synchronized void findModules(IProgressMonitor monitor)
    {
        if (modules != null)
        {
            return;
        }
        
        modules = new ArrayList<TapestryModule>();
        
        String appPackage = TapestryUtils.getAppPackage(project);
        
        if (appPackage == null)
        {
            return;
        }
        
        final WebXml webXml = Activator.getDefault().getWebXml(project);
        
        if (webXml == null)
        {
            return;
        }
        
        for (String filterName : webXml.getFilterNames())
        {
            final String localFilterName = filterName;
            
            TapestryModule appModule = addModule(monitor, modules, project,
                    appPackage + ".services." + StringUtils.capitalize(filterName) + "Module",
                    new ObjectCallback<TapestryModule, RuntimeException>()
                    {
                        @Override
                        public void callback(TapestryModule module)
                        {
                            module.setAppModule(true);
                            module.addReference(new TapestryModuleReference(new NonJavaReference(module), false)
                            {
                                @Override
                                public String getLabel()
                                {
                                    return "Your Application's Module (via " + webXml.getFilterClassName(localFilterName) + " in web.xml)";
                                }
                            });
                        }
            });
            
            if (appModule != null)
            {
                break;
            }
        }
        
        for (String paramName : webXml.getParamNames())
        {
            if (paramName.matches("tapestry\\.[^-]+-modules"))
            {
                final String tapestryModeModules = paramName;
                
                String modeModules = webXml.getParamValue(tapestryModeModules);
                
                for (String moduleClassName : modeModules.split(","))
                {
                    addModule(monitor, modules, project, moduleClassName.trim(), new ObjectCallback<TapestryModule, RuntimeException>()
                    {
                        @Override
                        public void callback(TapestryModule obj)
                        {
                            obj.addReference(new TapestryModuleReference(new NonJavaReference(obj), true)
                            {
                                @Override
                                public String getLabel()
                                {
                                    return "via " + tapestryModeModules + " in web.xml";
                                }
                            });
                        }
                    });
                }
            }
        }
        
        // Handle new t5.4 TapestryModule class location
        ObjectCallback<TapestryModule, RuntimeException> coreObjectCallback = new ObjectCallback<TapestryModule, RuntimeException>()
        {
            @Override
            public void callback(TapestryModule module)
            {
                module.setTapestryCoreModule(true);
                module.addReference(new TapestryModuleReference(new NonJavaReference(module), false)
                {
                    @Override
                    public String getLabel()
                    {
                        final String version = TapestryProject.this.getTapestryVersion();
                        
                        return "Tapestry Core Module" + (StringUtils.isEmpty(version) ? "" : " version " + version);
                    }
                });
            }
        };
        // T5.3
        addModule(monitor, modules, project, "org.apache.tapestry5.services.TapestryModule", coreObjectCallback);
        // T5.4
        addModule(monitor, modules, project, "org.apache.tapestry5.modules.TapestryModule", coreObjectCallback);
        
        final ObjectCallback<TapestryModule, RuntimeException> iocModuleCallback = new ObjectCallback<TapestryModule, RuntimeException>()
        {
            @Override
            public void callback(TapestryModule module)
            {
                module.addReference(new TapestryModuleReference(new NonJavaReference(module), false)
                {
                    @Override
                    public String getLabel()
                    {
                        return "Tapestry IoC Module";
                    }
                });
            }
        };
        
        //  T5.3
        addModule(monitor, modules, project, "org.apache.tapestry5.ioc.services.TapestryIOCModule", iocModuleCallback);
        //  T5.4
        addModule(monitor, modules, project, "org.apache.tapestry5.ioc.modules.TapestryIOCModule", iocModuleCallback);
        
        try
        {
            for (IPackageFragmentRoot root : JavaCore.create(project).getAllPackageFragmentRoots())
            {
                findModules(monitor, modules, root);
            }
        }
        catch (CoreException e)
        {
            Activator.getDefault().logError("Error searching tapestry modules", e);
        }
    }

    private void findModules(IProgressMonitor monitor, List<TapestryModule> modules, final IPackageFragmentRoot root)
            throws CoreException
    {
        monitor.subTask("Reading " + root.getElementName() + "...");
        
        for (Object obj : root.getNonJavaResources())
        {
            if (obj instanceof IJarEntryResource)
            {
                IJarEntryResource jarEntry = (IJarEntryResource) obj;
                
                if ("META-INF".equals(jarEntry.getName()))
                {
                    for (IJarEntryResource child : jarEntry.getChildren())
                    {
                        if ("MANIFEST.MF".equals(child.getName()))
                        {
                            InputStream contents = child.getContents();
                            
                            try
                            {
                                Manifest manifest = new Manifest(contents);
                                
                                String classes = manifest.getMainAttributes().getValue("Tapestry-Module-Classes");
                                
                                if (classes != null)
                                {
                                    for (String className : classes.split(","))
                                    {
                                        addModule(monitor, modules, project, className,
                                                new ObjectCallback<TapestryModule, RuntimeException>()
                                                {
                                                    @Override
                                                    public void callback(TapestryModule obj)
                                                    {
                                                        obj.addReference(new TapestryModuleReference(new NonJavaReference(obj), false)
                                                        {
                                                            @Override
                                                            public String getLabel()
                                                            {
                                                                return "via " + root.getElementName() + "/META-INF/MANIFEST.MF";
                                                            }
                                                        });
                                                    }
                                                });
                                    }
                                }
                            }
                            catch (IOException e)
                            {
                                if (contents != null)
                                {
                                    try { contents.close(); } catch (IOException t)  { }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private TapestryModule addModule(IProgressMonitor monitor, List<TapestryModule> modules, IProject project, String moduleClassName, ObjectCallback<TapestryModule, RuntimeException> moduleCreated)
    {
        if (monitor.isCanceled())
        {
            return null;
        }
        
        monitor.subTask("Locating " + moduleClassName + "...");
        
        IType moduleClass = EclipseUtils.findTypeDeclaration(project, moduleClassName);
        
        if (moduleClass == null)
        {
            return null;
        }
        
        TapestryModule module = Activator.getDefault()
                .getTapestryModuleFactory()
                .createTapestryModule(this, moduleClass, moduleCreated);
        
        addModule(monitor, modules, module, moduleCreated);
        
        return module;
    }

    private void addModule(IProgressMonitor monitor, List<TapestryModule> modules,
            TapestryModule module, ObjectCallback<TapestryModule, RuntimeException> moduleCreated)
    {
        if (monitor.isCanceled())
        {
            return;
        }
        
        int index = modules.indexOf(module);
        
        if (index != -1)
        {
            TapestryModule existingModule = modules.get(index);
            
            if (existingModule != module)
            {
                for (TapestryModuleReference reference : module.references())
                {
                    existingModule.addReference(reference);
                }
            }
            else
            {
                moduleCreated.callback(existingModule);
            }
            
            return;
        }
        
        module.initialize(monitor);
        
        modules.add(module);
        
        for (TapestryModule subModule :  module.subModules())
        {
            addModule(monitor, modules, subModule, moduleCreated);
        }
    }

    public boolean contains(IProject project)
    {
        for (TapestryModule module : modules())
        {
            if (project.equals(module.getModuleClass().getJavaProject().getProject()))
            {
                return true;
            }
        }
        return false;
    }

    public TapestryContext findComponentContext(String componentName) throws JavaModelException
    {
        String libraryPrefix = "";
        String componentNameWithoutPrefix = componentName;
        
        int index = componentName.indexOf('.');
        if (index < 0)
        {
            index = componentName.indexOf('/');
        }
        if (index >= 0)
        {
            libraryPrefix = componentName.substring(0, index);
            
            if (index + 1 >= componentName.length())
            {
                return null;
            }
            
            componentNameWithoutPrefix = componentName.substring(index + 1);
        }
        
        for (TapestryModule module : modules)
        {
            if (module.isAppModule())
            {
                TapestryContext context = findComponentContext(
                        module, TapestryUtils.getComponentsPackage(module.getEclipseProject()), componentName);
                
                if (context != null)
                {
                    return context;
                }
            }
            
            for (LibraryMapping mapping : module.libraryMappings())
            {
                if (libraryPrefix.equals(mapping.getPathPrefix()))
                {
                    TapestryContext context = findComponentContext(
                            module, mapping.getRootPackage() + ".components", componentNameWithoutPrefix);
                    
                    if (context != null)
                    {
                        return context;
                    }
                }
            }
        }
        
        if ("".equals(libraryPrefix))
        {
            return findComponentContext("core/" + componentNameWithoutPrefix);
        }
        return null;
    }

    private TapestryContext findComponentContext(TapestryModule module, String appPackage, String componentNameWithoutPrefix)
    {
        //  subpackage.componentName
        String componentPath = getComponentPath(module, appPackage, componentNameWithoutPrefix);
        
        //  TODO Look in module.getComponents() instead? It's cached
        TapestryFile file = module.findClasspathFileCaseInsensitive(componentPath);
        
        if (file == null)
        {
            File parentFile = new File(componentPath).getParentFile();
            
            if (parentFile != null)
            {
                //  subpackage.componentNameSubpackage
                componentPath = getComponentPath(module, appPackage, componentNameWithoutPrefix + parentFile.getName());
                
                file = module.findClasspathFileCaseInsensitive(componentPath);
                
                if (file == null)
                {
                    //  subpackage.subpackageComponentName
                    componentPath = getComponentPath(module, appPackage, prepend(componentNameWithoutPrefix, parentFile.getName()));
                    
                    file = module.findClasspathFileCaseInsensitive(componentPath);
                }
            }
        }
        
        return file != null ? file.getContext() : null;
    }

    private String prepend(String componentNameWithoutPrefix, String parentName)
    {
        StringBuilder builder = new StringBuilder(componentNameWithoutPrefix);
        int index = componentNameWithoutPrefix.lastIndexOf(".");
        return builder.insert(index + 1, parentName).toString();
    }

    protected String getComponentPath(TapestryModule module, String appPackage,
            String componentNameWithoutPrefix)
    {
        return TapestryUtils.joinPath(appPackage.replace('.', '/'),
                componentNameWithoutPrefix.replace('.', '/')
                    + (module instanceof LocalTapestryModule ? ".java" : ".class"));
    }

    public JavaScriptStack findStack(String stackName)
    {
        List<JavaScriptStack> stacks = new ArrayList<JavaScriptStack>();
        
        for (TapestryModule module : modules())
        {
            for (JavaScriptStack stack : module.javaScriptStacks())
            {
                if (stackName.equals(stack.getName()))
                {
                    stacks.add(stack);
                }
            }
        }
        
        //  Find first overridden stack (if any)
        for (JavaScriptStack stack : stacks)
        {
            if (stack.isOverride())
            {
                return stack;
            }
        }
        
        return stacks.isEmpty()
             ? null
             : stacks.get(0);
    }

    public LibraryMapping findLibraryMapping(String packageName)
    {
        List<LibraryMapping> mappings = new ArrayList<LibraryMapping>();
        
        for (TapestryModule module : modules())
        {
            for (LibraryMapping mapping : module.libraryMappings())
            {
                if (packageName.startsWith(mapping.getRootPackage()))
                {
                    mappings.add(mapping);
                }
            }
        }
        
        return mappings.size() > 0 ? mappings.get(0) : null;
    }
}
