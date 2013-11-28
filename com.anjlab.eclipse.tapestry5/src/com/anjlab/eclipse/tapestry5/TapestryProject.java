package com.anjlab.eclipse.tapestry5;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Manifest;

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
import com.anjlab.eclipse.tapestry5.watchdog.WebXmlWatchdog.WebXml;

public class TapestryProject
{
    private IProject project;
    
    private volatile List<TapestryModule> modules;
    
    public TapestryProject(IProject project)
    {
        this.project = project;
    }
    
    public IProject getProject()
    {
        return project;
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
        
        TapestryModule appModule = null;
        
        final WebXml webXml = Activator.getDefault().getWebXml(project);
        
        if (webXml == null)
        {
            return;
        }
        
        for (String filterName : webXml.getFilterNames())
        {
            final String localFilterName = filterName;
            
            appModule = addModule(monitor, modules, project, appPackage + ".services." + filterName + "Module", new ModuleReference()
            {
                @Override
                public String getLabel()
                {
                    return "Your Application's Module (via " + webXml.getFilterClassName(localFilterName) + " in web.xml)";
                }
            });
            
            if (appModule != null)
            {
                appModule.setAppModule(true);
                break;
            }
        }
        
        addModule(monitor, modules, project, "org.apache.tapestry5.services.TapestryModule", new ModuleReference()
        {
            @Override
            public String getLabel()
            {
                return "Tapestry Core Module";
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
                                        addModule(monitor, modules, project, className, new ModuleReference()
                                        {
                                            @Override
                                            public String getLabel()
                                            {
                                                return "via " + root.getElementName() + "/META-INF/MANIFEST.MF";
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

    private TapestryModule addModule(IProgressMonitor monitor, List<TapestryModule> modules, IProject project, String moduleClassName, ModuleReference reference)
    {
        monitor.subTask("Locating " + moduleClassName + "...");
        
        IType moduleClass = EclipseUtils.findTypeDeclaration(project, moduleClassName);
        
        if (moduleClass == null)
        {
            return null;
        }
        
        TapestryModule module = TapestryModule.createTapestryModule(this, moduleClass, reference);
        
        addModule(monitor, modules, module);
        
        return module;
    }

    private void addModule(IProgressMonitor monitor, List<TapestryModule> modules, TapestryModule module)
    {
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
        String componentPath = getComponentPath(module, appPackage, componentNameWithoutPrefix);
        
        TapestryFile file = module.findJavaFileCaseInsensitive(componentPath);
        
        if (file == null)
        {
            File parentFile = new File(componentPath).getParentFile();
            
            if (parentFile != null)
            {
                componentPath = getComponentPath(module, appPackage, componentNameWithoutPrefix + parentFile.getName());
                
                file = module.findJavaFileCaseInsensitive(componentPath);
            }
        }
        
        return file != null ? file.getContext() : null;
    }

    protected String getComponentPath(TapestryModule module, String appPackage,
            String componentNameWithoutPrefix)
    {
        return TapestryUtils.joinPath(appPackage.replace('.', '/'),
                componentNameWithoutPrefix.replace('.', '/')
                    + (module instanceof LocalTapestryModule ? ".java" : ".class"));
    }

}
