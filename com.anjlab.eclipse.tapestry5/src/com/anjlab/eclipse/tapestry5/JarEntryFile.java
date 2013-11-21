package com.anjlab.eclipse.tapestry5;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJarEntryResource;

public class JarEntryFile extends AbstractTapestryFile
{
    private TapestryContext context;
    private IJarEntryResource jarEntry;

    public JarEntryFile(JarTapestryContext context, IJarEntryResource jarEntry)
    {
        this.context = context;
        this.jarEntry = jarEntry;
    }

    public IJarEntryResource getJarEntry()
    {
        return jarEntry;
    }
    
    @Override
    public IPath getPath()
    {
        return jarEntry.getFullPath();
    }

    @Override
    public String getName()
    {
        return jarEntry.getName();
    }

    @Override
    public IProject getProject()
    {
        return jarEntry.getPackageFragmentRoot().getJavaProject().getProject();
    }

    @Override
    public TapestryContext getContext()
    {
        return context;
    }

    @Override
    public String toString()
    {
        return getPath().toPortableString();
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        
        if (!(obj instanceof JarEntryFile))
        {
            return false;
        }
        
        return this.jarEntry.equals(((JarEntryFile) obj).jarEntry);
    }
}
