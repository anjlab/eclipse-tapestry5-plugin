package com.anjlab.eclipse.tapestry5;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

public class LocalFile extends AbstractTapestryFile
{
    private IFile file;
    private TapestryContext context;
    
    public LocalFile(TapestryContext context, IFile file)
    {
        assert context != null;
        assert file != null;
        
        this.context = context;
        this.file = file;
    }

    public IFile getFile()
    {
        return file;
    }
    
    @Override
    public IPath getPath()
    {
        return file.getProjectRelativePath();
    }

    @Override
    public String getName()
    {
        return file.getName();
    }

    @Override
    public IProject getProject()
    {
        return file.getProject();
    }

    @Override
    public TapestryContext getContext()
    {
        return context;
    }

    @Override
    public String getFileExtension()
    {
        return file.getFileExtension();
    }
    
    @Override
    public String toString()
    {
        return file.toString();
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        
        if (!(obj instanceof LocalFile))
        {
            return false;
        }
        
        return this.file.equals(((LocalFile) obj).file);
    }
    
    @Override
    public boolean exists()
    {
        return file.exists();
    }
    
    @Override
    public String getClassName()
    {
        return EclipseUtils.getClassName(file);
    }
}
