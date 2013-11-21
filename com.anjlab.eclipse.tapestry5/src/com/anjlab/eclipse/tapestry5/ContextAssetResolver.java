package com.anjlab.eclipse.tapestry5;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

public class ContextAssetResolver implements AssetResolver
{
    @Override
    public TapestryFile resolve(String path, TapestryFile relativeTo) throws AssetException
    {
        IContainer webapp = TapestryUtils.findWebapp(relativeTo.getProject());
        
        if (webapp == null)
        {
            throw new AssetException("Couldn't find context folder ('src/main/webapp')");
        }
        
        IResource resource = webapp.findMember(path);
        
        if (resource == null)
        {
            throw new AssetException("File not found '"
                    + webapp.getProjectRelativePath().toPortableString() + "/" + path + "'");
        }
        
        if (!(resource instanceof IFile))
        {
            throw new AssetException(
                    "'" + webapp.getProjectRelativePath().toPortableString() + "/" + path + "' is not a file");
        }
        
        IFile file = (IFile) resource;
        
        return new LocalFile(relativeTo.getContext(), file);
    }
}
