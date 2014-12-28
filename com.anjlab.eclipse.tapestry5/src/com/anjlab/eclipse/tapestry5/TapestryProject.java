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

import com.anjlab.eclipse.tapestry5.TapestryModule.ModuleReference;
import com.anjlab.eclipse.tapestry5.TapestryModule.ObjectCallback;
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
        
        markOverrides();
    }

    private void markOverrides()
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
                if (stack.isOverrides())
                {
                    hasOverride = true;
                }
            }
            
            if (hasOverride)
            {
                for (JavaScriptStack stack : configs)
                {
                    if (!stack.isOverrides())
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
                            module.setReference(new ModuleReference()
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
                            obj.setReference(new ModuleReference()
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
                module.setReference(new ModuleReference()
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
        // t5.3
        addModule(monitor, modules, project, "org.apache.tapestry5.services.TapestryModule", coreObjectCallback);
        // t5.4
        addModule(monitor, modules, project, "org.apache.tapestry5.modules.TapestryModule", coreObjectCallback);
        
        addModule(monitor, modules, project, "org.apache.tapestry5.ioc.services.TapestryIOCModule", new ObjectCallback<TapestryModule, RuntimeException>()
        {
            @Override
            public void callback(TapestryModule module)
            {
                module.setReference(new ModuleReference()
                {
                    @Override
                    String getLabel()
                    {
                        return "Tapestry IoC Module";
                    }
                });
            }
        });
        
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
                                                        obj.setReference(new ModuleReference()
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
        
        TapestryModule module = TapestryModule.createTapestryModule(this, moduleClass, moduleCreated);
        
        addModule(monitor, modules, module);
        
        return module;
    }

    private void addModule(IProgressMonitor monitor, List<TapestryModule> modules, TapestryModule module)
    {
        if (monitor.isCanceled())
        {
            return;
        }
        
        int index = modules.indexOf(module);
        
        if (index != -1)
        {
            TapestryModule existingModule = modules.get(index);
            
            existingModule.addReference(module.getReference());
            
            return;
        }
        
        module.initialize(monitor);
        
        modules.add(module);
        
        for (TapestryModule subModule :  module.subModules())
        {
            addModule(monitor, modules, subModule);
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
            if (stack.isOverrides())
            {
                return stack;
            }
        }
        
        return stacks.isEmpty()
             ? null
             : stacks.get(0);
    }
}
