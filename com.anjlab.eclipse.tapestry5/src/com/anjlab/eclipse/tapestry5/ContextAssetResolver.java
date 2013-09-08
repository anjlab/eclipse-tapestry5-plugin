package com.anjlab.eclipse.tapestry5;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;

public class ContextAssetResolver implements AssetResolver
{
    @Override
    public IFile resolve(String path, IFile relativeTo) throws AssetException
    {
        IContainer webapp = TapestryUtils.findWebapp(relativeTo.getProject());
        
        if (webapp != null)
        {
            IFile file = (IFile) webapp.findMember(path);
            
            if (file == null)
            {
                throw new AssetException("File not found '"
                        + webapp.getProjectRelativePath().toPortableString() + "/" + path + "'");
            }
            
            return file;
        }
        
        throw new AssetException("Couldn't find context folder ('src/main/webapp')");
    }
}
