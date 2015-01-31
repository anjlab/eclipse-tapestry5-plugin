package com.anjlab.eclipse.tapestry5;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;

public class LocalFileLookup implements FileLookup
{
    private final IType relativeTo;

    public LocalFileLookup(IType relativeTo)
    {
        this.relativeTo = relativeTo;
    }

    @Override
    public TapestryFile findClasspathFileCaseInsensitive(String path)
    {
        if (relativeTo == null)
        {
            return null;
        }
        
        IJavaProject javaProject = relativeTo.getJavaProject();
        
        return TapestryUtils.findFileInSourceFolders(javaProject, path);
    }

}
