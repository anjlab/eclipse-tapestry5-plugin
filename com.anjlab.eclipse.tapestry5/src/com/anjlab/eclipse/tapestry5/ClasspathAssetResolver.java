package com.anjlab.eclipse.tapestry5;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.anjlab.eclipse.tapestry5.TapestryContext.FileNameBuilder;

public class ClasspathAssetResolver implements AssetResolver
{
    @Override
    public TapestryFile resolveInWorkspace(String path)
    {
        //  Look in current tapestry project first
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        
        TapestryProject tapestryProject = Activator.getDefault().getTapestryProject(window);
        
        Set<IProject> visitedProjects = new HashSet<IProject>();
        
        if (tapestryProject != null)
        {
            for (TapestryModule module : tapestryProject.modules())
            {
                IProject project = module.getEclipseProject();
                
                //  Multiple modules can be from the same Eclipse project
                visitedProjects.add(project);
                
                TapestryFile file = module.findClasspathFileCaseInsensitive(path);
                
                if (file != null)
                {
                    return file;
                }
            }
        }
        
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        
        for (IProject project : projects)
        {
            if (!visitedProjects.add(project))
            {
                //  Already visited
                continue;
            }
            
            try
            {
                if (EclipseUtils.isJavaProject(project))
                {
                    IJavaProject javaProject = JavaCore.create(project);
                    
                    TapestryFile file = TapestryUtils.findFileInSourceFolders(javaProject, path);
                    
                    if (file != null)
                    {
                        return file;
                    }
                }
            }
            catch (CoreException e)
            {
                //  Ignore
            }
        }
        
        return null;
    }
    
    @Override
    public TapestryFile resolve(final String path, TapestryFile relativeTo) throws UnresolvableReferenceException
    {
        FileNameBuilder fileNameBuilder = new FileNameBuilder()
        {
            @Override
            public String getFileName(String fileName, String fileExtension)
            {
                int lastIndexOfDash = fileName.lastIndexOf('/');
                
                if (lastIndexOfDash <= 0)
                {
                    return path;
                }
                
                return fileName.substring(0, lastIndexOfDash) + '/' + path;
            }
        };
        
        return resolve(path, relativeTo, fileNameBuilder);
    }

    private TapestryFile resolve(final String path, TapestryFile relativeTo, FileNameBuilder fileNameBuilder)
            throws UnresolvableReferenceException
    {
        try
        {
            List<TapestryFile> files = relativeTo.getContext()
                    .findTapestryFiles(relativeTo, true, fileNameBuilder);
            
            if (!files.isEmpty())
            {
                return files.get(0);
            }
            
            throw createAssetException(path, null);
        }
        catch (Throwable t)
        {
            throw createAssetException(path, t);
        }
    }

    protected UnresolvableReferenceException createAssetException(final String path, Throwable cause)
    {
        return new UnresolvableReferenceException("Couldn't resolve classpath asset from path '" + path + "'", cause);
    }
}
