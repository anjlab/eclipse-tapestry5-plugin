package com.anjlab.eclipse.tapestry5;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJarEntryResource;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;

import com.anjlab.eclipse.tapestry5.DeclarationReference.NonJavaReference;
import com.anjlab.eclipse.tapestry5.DeclarationReference.ProjectSettingsReference;
import com.anjlab.eclipse.tapestry5.TapestryService.ServiceDefinition;
import com.anjlab.eclipse.tapestry5.internal.CaseInsensitiveMap;
import com.anjlab.eclipse.tapestry5.internal.Orderable;
import com.anjlab.eclipse.tapestry5.internal.Orderer;
import com.anjlab.eclipse.tapestry5.internal.SymbolExpansion;
import com.anjlab.eclipse.tapestry5.templates.ProjectSettings;
import com.anjlab.eclipse.tapestry5.templates.ProjectSettings.TapestryModuleSettings;
import com.anjlab.eclipse.tapestry5.templates.ProjectSettings.TapestryServiceSettings;
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

        // Now when we know tapestry version for this project,
        // try to locate correct version of project settings

        final ProjectSettings projectSettings = TapestryUtils.readProjectSettings(this);

        for (Entry<String, TapestryModuleSettings> moduleEntry : projectSettings.getTapestryModules().entrySet())
        {
            String moduleClassName = moduleEntry.getKey();
            
            TapestryModule module = addModule(monitor, modules, project, moduleClassName,
                    new ObjectCallback<TapestryModule, RuntimeException>()
                    {
                        @Override
                        public void callback(TapestryModule module)
                        {
                            module.addReference(new TapestryModuleReference(new NonJavaReference(module), true)
                            {
                                @Override
                                public String getLabel()
                                {
                                    return "via " + projectSettings.getReferenceLabel();
                                }
                            });
                        }
                    });

            if (module != null)
            {
                TapestryModuleSettings moduleSettings = moduleEntry.getValue();
                
                for (Entry<String, TapestryServiceSettings> serviceEntry : moduleSettings.getTapestryServices().entrySet())
                {
                    String serviceLabel = serviceEntry.getKey();
                    TapestryServiceSettings serviceSettings = serviceEntry.getValue();
                    
                    if (StringUtils.isNotEmpty(serviceSettings.getDiscovery()))
                    {
                        //  Support just one discovery rule for now
                        if (serviceSettings.getDiscovery().equals("intf-impl-pattern"))
                        {
                            addServicesViaIntfImplPatternDiscovery(projectSettings, module, serviceLabel, serviceSettings, monitor);
                        }
                        else
                        {
                            Activator.getDefault().logWarning("Unsupported discovery '" + serviceSettings.getDiscovery());
                        }
                    }
                    else
                    {
                        //  Clone and add new service
                        
                        ServiceDefinition definition = serviceSettings.clone();
                        
                        String intfClass;
                        if (StringUtils.isEmpty(serviceSettings.getIntfClass()))
                        {
                            //  Treat serviceLabel as intfClass
                            intfClass = serviceLabel;
                        }
                        else
                        {
                            intfClass = serviceSettings.getIntfClass();
                        }
                        
                        if (StringUtils.isEmpty(definition.getId()))
                        {
                            definition.setId(TapestryUtils.getSimpleName(intfClass));
                        }
                        
                        definition.setIntfClass(intfClass);
                        
                        addService(projectSettings, module, definition);
                    }
                }
            }
            else
            {
                Activator.getDefault().logWarning("Tapestry module '" + moduleClassName + "' not found");
            }
        }

        // TODO Tapestry 5.4: Register javascriptModules as contributions to ModuleManager?

        findSymbols(monitor, projectSettings);

        markJavaScriptStackOverrides();
    }

    private void addServicesViaIntfImplPatternDiscovery(
            final ProjectSettings projectSettings,
            final TapestryModule module,
            final String serviceLabel,
            final TapestryServiceSettings serviceSettings,
            final IProgressMonitor monitor)
    {
        //  Note: This rule can only search between classes that are located
        //  in the same project/JAR as this module class
        
        if (StringUtils.isEmpty(serviceSettings.getIntfClass())
                || StringUtils.isEmpty(serviceSettings.getImplClass()))
        {
            Activator.getDefault().logError("Both intfClass and implClass must not be null with auto-discovery rule '"
                    + serviceSettings.getDiscovery()
                    + "' (" + projectSettings.getReferenceLabel()
                    + ", tapestryModule: " + module.getName()
                    + ", serviceLabel: " + serviceLabel
                    + ", intfClass: " + serviceSettings.getIntfClass()
                    + ", implClass: " + serviceSettings.getImplClass() + ")");
            return;
        }
        
        final Pattern intfPattern = Pattern.compile(serviceSettings.getIntfClass());
        
        module.enumJavaClassesRecursively(monitor, "", new ObjectCallback<Object, RuntimeException>()
        {
            @Override
            public void callback(Object obj) throws RuntimeException
            {
                String className;
                if (obj instanceof IFile)
                {
                    className = EclipseUtils.getClassName((IFile) obj);
                }
                else if (obj instanceof IClassFile)
                {
                    className = ((IClassFile) obj).getElementName();
                }
                else
                {
                    return;
                }
                
                Matcher matcher = intfPattern.matcher(className);
                
                if (matcher.find())
                {
                    ServiceDefinition definition = serviceSettings.clone();
                    
                    String intfClass = className;
                    String implClass = matcher.replaceAll(definition.getImplClass());
                    
                    //  Check if implClass actually exists
                    IType implClassType = EclipseUtils
                            .findTypeDeclaration(getProject(), IJavaSearchConstants.CLASS, implClass);
                    
                    if (implClassType == null)
                    {
                        Activator.getDefault().logWarning(
                                "Implementation class '" + implClass
                                + "' not found for service interface '" + intfClass + "'");
                        return;
                    }
                    
                    definition.setIntfClass(intfClass);
                    definition.setImplClass(implClass);
                    
                    definition.setId(StringUtils.isEmpty(definition.getId())
                            ? TapestryUtils.getSimpleName(className)
                            : matcher.replaceAll(definition.getId()));
                    
                    addService(projectSettings, module, definition);
                }
            }
        });
    }

    private void addService(ProjectSettings projectSettings, TapestryModule module, ServiceDefinition definition)
    {
        definition.resolveMarkers(module);
        
        TapestryService service = new TapestryService(
                module, definition, new ProjectSettingsReference(projectSettings));
        
        //  TODO Make sure to replace existing service that could be added via config previously
        //  (caching issue)
        module.addService(service);
    }

    private SymbolExpansion expansion;

    public String expandSymbols(String input) throws RuntimeException
    {
        if (expansion == null)
        {
            expansion = new SymbolExpansion(symbols());
        }
        
        return expansion.expandSymbols(input);
    }

    private Map<String, List<TapestrySymbol>> symbols;
    
    public Map<String, List<TapestrySymbol>> symbols()
    {
        if (symbols == null)
        {
            findSymbols(new NullProgressMonitor(), TapestryUtils.readProjectSettings(this));
        }
        
        return symbols;
    }
    
    private synchronized void findSymbols(IProgressMonitor monitor, ProjectSettings projectSettings)
    {
        if (symbols != null)
        {
            return;
        }
        
        final List<TapestryModule> modules = modules();
        
        monitor.beginTask("Resolving symbol providers", modules.size());
        
        final Orderer<TapestryService> orderer = new Orderer<TapestryService>();
        
        for (final TapestryModule module : modules)
        {
            monitor.subTask(module.getModuleClass().getFullyQualifiedName());
            
            List<Orderable<TapestryService>> symbolProviders = module.symbolProviders(monitor);
            
            for (Orderable<TapestryService> orderable : symbolProviders)
            {
                orderer.add(orderable);
            }
            
            monitor.worked(1);
        }
        
        final List<TapestryService> symbolProviders = orderer.getOrdered();
        
        monitor.beginTask("Resolving symbols", modules.size());
        
        final Map<TapestryService, List<TapestrySymbol>> providersToSymbols =
                new HashMap<TapestryService, List<TapestrySymbol>>();
        
        for (final TapestryModule module : modules)
        {
            monitor.subTask(module.getModuleClass().getFullyQualifiedName());
            
            module.buildProvidersToSymbolsMap(symbolProviders, monitor);
            
            for (final TapestryService symbolProvider : symbolProviders)
            {
                //  Values from the same provider may override each other
                
                List<TapestrySymbol> providerSymbols = module.symbolsFrom(symbolProvider, monitor);
                
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
                                    //  Clone symbol to keep cached version untouched 
                                    symbol = symbol.clone();
                                    
                                    symbol.setOverridden(true);
                                    
                                    providerSymbols.set(i, symbol);
                                }
                            }
                        }
                    }
                    
                    List<TapestrySymbol> symbols = providersToSymbols.get(symbolProvider);
                    
                    if (symbols == null)
                    {
                        symbols = new ArrayList<TapestrySymbol>();
                        providersToSymbols.put(symbolProvider, symbols);
                    }
                    
                    symbols.addAll(providerSymbols);
                }
            }
            
            monitor.worked(1);
        }
        
        // Override symbols using values from project settings

        Map<String, TapestryService> symbolProvidersLookup = new CaseInsensitiveMap<TapestryService>();

        for (TapestryService symbolProvider : symbolProviders)
        {
            symbolProvidersLookup.put(symbolProvider.getDefinition().getId(), symbolProvider);
        }

        for (Entry<String, Map<String, String>> additionalProviderSymbols : projectSettings.getSymbols().entrySet())
        {
            String providerName = additionalProviderSymbols.getKey();

            TapestryService symbolProvider = symbolProvidersLookup.get(providerName);

            if (symbolProvider == null)
            {
                Activator.getDefault().logWarning("Symbol provider '" + providerName + "' not found");
                continue;
            }

            List<TapestrySymbol> symbols = providersToSymbols.get(symbolProvider);

            if (symbols == null)
            {
                symbols = new ArrayList<TapestrySymbol>();
                providersToSymbols.put(symbolProvider, symbols);
            }

            Map<String, String> additionalSymbols = additionalProviderSymbols.getValue();

            for (Entry<String, String> symbolEntry : additionalSymbols.entrySet())
            {
                // Add to the top of the list to win during symbol expansion
                symbols.add(0,
                        new TapestrySymbol(
                                symbolEntry.getKey(),
                                symbolEntry.getValue(),
                                false,
                                new ProjectSettingsReference(projectSettings),
                                symbolProvider));
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
                                    && !(existing.getReference() instanceof ProjectSettingsReference)
                                    //  Some modules may be added conditionally, like QA/DEV/Production
                                    //  we should treat them all as equal, they souldn't override each other,
                                    //  because usually only one of them enabled at run-time
                                    && !existing.getReference().getTapestryModule().isConditional())
                            {
                                //  Clone symbol to keep cached version untouched 
                                symbol = symbol.clone();
                                
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

    public TapestryService findService(
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

    private TapestryModule addModule(IProgressMonitor monitor, List<TapestryModule> modules,
            IProject project, String moduleClassName, ObjectCallback<TapestryModule, RuntimeException> moduleCreated)
    {
        if (monitor.isCanceled())
        {
            return null;
        }
        
        monitor.subTask("Locating " + moduleClassName + "...");
        
        IType moduleClass = EclipseUtils.findTypeDeclaration(
                project, IJavaSearchConstants.CLASS, moduleClassName);
        
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
