package com.anjlab.eclipse.tapestry5;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IType;

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

    private Map<IType, Map<IProject, TapestryModule>> localModules = new HashMap<IType, Map<IProject, TapestryModule>>();

    //  TODO Monitor changes in config.json
    
    @Override
    public void classpathChanged(IFile classpath)
    {
        // Reset cache when classpath updated

        // TODO Be more intelligent and check if some dependencies with tapestry
        // modules remain the same

        clearCache(classpath.getProject());
    }

    public void clearCache(IProject project)
    {
        clearProjectCache(project, localModules);
        clearProjectCache(project, jarModules);
    }

    private void clearProjectCache(IProject project, Map<IType, Map<IProject, TapestryModule>> modulesCache)
    {
        Set<IType> emptyKeys = new HashSet<IType>();

        for (Entry<IType, Map<IProject, TapestryModule>> entry : modulesCache.entrySet())
        {
            Map<IProject, TapestryModule> value = entry.getValue();

            value.remove(project);

            if (value.isEmpty())
            {
                emptyKeys.add(entry.getKey());
            }
        }

        for (IType key : emptyKeys)
        {
            modulesCache.remove(key);
        }
    }

    private interface ModuleCreator
    {
        TapestryModule createModule(TapestryProject project, IType moduleClass);
    }

    public TapestryModule createTapestryModule(
            TapestryProject project,
            IType moduleClass,
            ObjectCallback<TapestryModule, RuntimeException> moduleCreated)
    {
        if (moduleClass.getResource() != null)
        {
            return getOrCreateModule(localModules, project, moduleClass, moduleCreated, new ModuleCreator()
            {
                @Override
                public TapestryModule createModule(TapestryProject project, IType moduleClass)
                {
                    return new LocalTapestryModule(project, moduleClass);
                }
            });
        }

        return getOrCreateModule(jarModules, project, moduleClass, moduleCreated, new ModuleCreator()
        {
            @Override
            public TapestryModule createModule(TapestryProject project, IType moduleClass)
            {
                return new JarTapestryModule(project, moduleClass);
            }
        });
    }

    private TapestryModule getOrCreateModule(
            Map<IType, Map<IProject, TapestryModule>> modulesCache,
            TapestryProject project,
            IType moduleClass,
            ObjectCallback<TapestryModule, RuntimeException> moduleCreated,
            ModuleCreator moduleCreator)
    {
        Map<IProject, TapestryModule> projectModules = modulesCache.get(moduleClass);

        if (projectModules == null)
        {
            projectModules = new HashMap<IProject, TapestryModule>();

            modulesCache.put(moduleClass, projectModules);
        }

        TapestryModule cachedModule = projectModules.get(project.getProject());

        if (cachedModule != null)
        {
            return cachedModule;
        }

        final TapestryModule module = moduleCreator.createModule(project, moduleClass);

        projectModules.put(project.getProject(), module);

        notifyModuleCreated(moduleCreated, module);

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

    public void localModuleChanged(IFile moduleClass)
    {
        Set<IType> emptyKeys = new HashSet<IType>();

        for (Entry<IType, Map<IProject, TapestryModule>> entry : localModules.entrySet())
        {
            Map<IProject, TapestryModule> value = entry.getValue();

            // Check if IType is from the same resource as moduleClass
            boolean found = false;
            for (TapestryModule module : value.values())
            {
                if (TapestryUtils.isModuleFile(moduleClass, module))
                {
                    found = true;
                    break;
                }
            }

            if (found)
            {
                emptyKeys.add(entry.getKey());
            }
        }

        for (IType key : emptyKeys)
        {
            localModules.remove(key);
        }
    }

    public void clearLocalCache(IProject project)
    {
        clearProjectCache(project, localModules);
    }
}
