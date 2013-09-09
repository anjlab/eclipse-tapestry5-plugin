package com.anjlab.eclipse.tapestry5;

import org.eclipse.core.resources.IFile;

public interface AssetResolver
{

    IFile resolve(String path, IFile relativeTo) throws AssetException;

}
