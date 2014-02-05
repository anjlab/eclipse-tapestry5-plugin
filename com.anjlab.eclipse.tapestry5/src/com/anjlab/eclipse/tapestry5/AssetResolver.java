package com.anjlab.eclipse.tapestry5;



public interface AssetResolver
{

    TapestryFile resolve(String path, TapestryFile relativeTo) throws AssetException;

    TapestryFile resolveInWorkspace(String path);

}
