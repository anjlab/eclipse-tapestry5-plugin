package com.anjlab.eclipse.tapestry5;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IType;

import com.anjlab.eclipse.tapestry5.TapestryModule.ObjectCallback;
import com.anjlab.eclipse.tapestry5.watchdog.IEclipseClasspathListener;

public class TapestryModuleFactory implements IEclipseClasspathListener
{
    //  Cache for tapestry modules built from JARs: they're almost never change,
    //  but instantiating and initializing new module is a heavy operation.
    //
    //  Theoretically in workspace we may have two module classes with the same name,
    //  but sourced from JARs with different versions, depending on project's classpath.

    private Map<IType, Map<IProject, TapestryModule>> jarModules =
            new HashMap<IType, Map<IProject, TapestryModule>>();
    
    @Override
    public void classpathChanged(IFile classpath)
    {
        //  Reset cache when classpath updated
        
        Set<IType> emptyKeys = new HashSet<IType>();
        
        for (Entry<IType, Map<IProject, TapestryModule>> entry : jarModules.entrySet())
        {
            Map<IProject, TapestryModule> value = entry.getValue();
            
            value.remove(classpath.getProject());
            
            if (value.isEmpty())
            {
                emptyKeys.add(entry.getKey());
            }
        }
        
        for (IType key : emptyKeys)
        {
            jarModules.remove(key);
        }
    }
    
    public TapestryModule createTapestryModule(
            TapestryProject project,
            IType moduleClass,
            ObjectCallback<TapestryModule, RuntimeException> moduleCreated)
    {
        final TapestryModule module;
        
        if (moduleClass.getResource() != null)
        {
            module = new LocalTapestryModule(project, moduleClass);
            
            notifyModuleCreated(moduleCreated, module);
        }
        else
        {
            Map<IProject, TapestryModule> projectModules = jarModules.get(moduleClass);
            
            if (projectModules == null)
            {
                projectModules = new HashMap<IProject, TapestryModule>();
                
                jarModules.put(moduleClass, projectModules);
            }
            
            TapestryModule cachedModule = projectModules.get(project.getProject());
            
            if (cachedModule != null)
            {
                module = cachedModule;
            }
            else
            {
                module = new JarTapestryModule(project, moduleClass);
                
                projectModules.put(project.getProject(), module);
                
                notifyModuleCreated(moduleCreated, module);
            }
        }
        
        return module;
    }

    private void notifyModuleCreated(
            ObjectCallback<TapestryModule, RuntimeException> moduleCreated,
            final TapestryModule module)
    {
        if (moduleCreated != null)
        {
            moduleCreated.callback(module);
        }
    }
    
}
