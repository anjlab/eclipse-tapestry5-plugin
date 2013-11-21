package com.anjlab.eclipse.tapestry5;


public interface AssetResolver
{

    TapestryFile resolve(String path, TapestryFile javaFile) throws AssetException;

}
