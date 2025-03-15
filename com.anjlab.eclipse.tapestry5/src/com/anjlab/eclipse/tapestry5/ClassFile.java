package com.anjlab.eclipse.tapestry5;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IOrdinaryClassFile;

public class ClassFile extends AbstractTapestryFile
{
    private TapestryContext context;
    private IOrdinaryClassFile classFile;

    public ClassFile(TapestryContext context, IOrdinaryClassFile classFile)
    {
        this.context = context;
        this.classFile = classFile;
    }

    public IOrdinaryClassFile getClassFile()
    {
        return classFile;
    }
    
    @Override
    public boolean isJavaFile()
    {
        return true;
    }

    @Override
    public IPath getPath()
    {
        String folder = classFile.getType().getPackageFragment().getElementName().replace(".", "/");
        return new Path((folder.isEmpty() ? "" : "/" + folder) + "/" + getName());
    }

    @Override
    public String getName()
    {
        return classFile.getElementName();
    }

    @Override
    public IProject getProject()
    {
        return classFile.getJavaProject().getProject();
    }

    @Override
    public TapestryContext getContext()
    {
        return context;
    }

    @Override
    public String toString()
    {
        return classFile.getType().getFullyQualifiedName();
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        
        if (!(obj instanceof ClassFile))
        {
            return false;
        }
        
        return this.classFile.equals(((ClassFile) obj).classFile);
    }

    @Override
    public boolean exists()
    {
        return classFile.exists();
    }
    
    @Override
    public String getClassName()
    {
        return classFile.getType().getFullyQualifiedName();
    }
}
