package com.anjlab.eclipse.tapestry5;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.Manifest;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJarEntryResource;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import com.anjlab.eclipse.tapestry5.TapestryModule.ModuleReference;

public class TapestryProject
{
    private IProject project;
    
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
        String appPackage = TapestryUtils.getAppPackage(project);
        String appName = TapestryUtils.getAppName(project);
        
        if (appName == null || appPackage == null)
        {
            return Collections.emptyList();
        }
        
        List<TapestryModule> modules = new ArrayList<TapestryModule>();
        
        addModule(modules, project, appPackage + ".services." + appName + "Module", new ModuleReference()
        {
            @Override
            public String getLabel()
            {
                return "Your Application's Module";
            }
        });
        addModule(modules, project, "org.apache.tapestry5.services.TapestryModule", new ModuleReference()
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
                findModules(modules, root);
            }
        }
        catch (CoreException e)
        {
            Activator.getDefault().logError("Error searching tapestry modules", e);
        }
        
        return modules;
    }

    private void findModules(List<TapestryModule> modules, final IPackageFragmentRoot root)
            throws CoreException
    {
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
                                        addModule(modules, project, className, new ModuleReference()
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

    private void addModule(List<TapestryModule> modules, IProject project, String moduleClassName, ModuleReference reference)
    {
        IType moduleType = EclipseUtils.findTypeDeclaration(project, moduleClassName);
        
        if (moduleType == null)
        {
            return;
        }
        
        addModule(modules, new TapestryModule(this, moduleType, reference));
    }

    private void addModule(List<TapestryModule> modules, TapestryModule module)
    {
        modules.add(module);
        
        for (TapestryModule subModule :  module.subModules())
        {
            addModule(modules, subModule);
        }
    }

}
