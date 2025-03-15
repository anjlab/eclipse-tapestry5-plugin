package com.anjlab.eclipse.tapestry5;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJarEntryResource;
import org.eclipse.jdt.core.IOrdinaryClassFile;

public class TapestryContextFactory
{

    public TapestryContext createTapestryContext(IFile file)
    {
        try
        {
            return new LocalTapestryContext(file);
        }
        catch (RuntimeException e)
        {
            throw new RuntimeException("Error creating context from " + file, e);
        }
    }

    public TapestryContext createTapestryContext(IJarEntryResource jarEntry)
    {
        try
        {
            return new JarTapestryContext(jarEntry);
        }
        catch (RuntimeException e)
        {
            throw new RuntimeException("Error creating context from " + jarEntry, e);
        }
    }

    public TapestryContext createTapestryContext(IOrdinaryClassFile classFile)
    {
        try
        {
            return new JarTapestryContext(classFile);
        }
        catch (RuntimeException e)
        {
            throw new RuntimeException("Error creating context from " + classFile, e);
        }
    }

}
