package com.anjlab.eclipse.tapestry5;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

public class ContextAssetResolver implements AssetResolver
{
    @Override
    public TapestryFile resolveInWorkspace(String path)
    {
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        
        for (IProject project : projects)
        {
            try
            {
                if (EclipseUtils.isJavaProject(project))
                {
                    IContainer webapp = TapestryUtils.findWebapp(project);
                    
                    if (webapp != null)
                    {
                        IFile file = EclipseUtils.findFileCaseInsensitive(webapp, path);
                        
                        if (file != null)
                        {
                            return TapestryUtils.createTapestryContext(file).getInitialFile();
                        }
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
    public TapestryFile resolve(String path, TapestryFile relativeTo) throws UnresolvableReferenceException
    {
        IContainer webapp = TapestryUtils.findWebapp(relativeTo.getProject());
        
        if (webapp == null)
        {
            throw new UnresolvableReferenceException("Couldn't find context folder ('src/main/webapp')");
        }
        
        IResource resource = webapp.findMember(path);
        
        if (resource == null)
        {
            throw new UnresolvableReferenceException("File not found '"
                    + webapp.getProjectRelativePath().toPortableString() + "/" + path + "'");
        }
        
        if (!(resource instanceof IFile))
        {
            throw new UnresolvableReferenceException(
                    "'" + webapp.getProjectRelativePath().toPortableString() + "/" + path + "' is not a file");
        }
        
        IFile file = (IFile) resource;
        
        return new LocalFile(relativeTo.getContext(), file);
    }
}
