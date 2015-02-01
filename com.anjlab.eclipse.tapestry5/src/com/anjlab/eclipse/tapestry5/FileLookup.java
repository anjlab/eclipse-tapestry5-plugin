package com.anjlab.eclipse.tapestry5;

import org.eclipse.jdt.core.JavaModelException;


public interface FileLookup
{
    TapestryFile findClasspathFileCaseInsensitive(String path);

    String findClasspathRelativePath(TapestryFile file) throws JavaModelException;
}
