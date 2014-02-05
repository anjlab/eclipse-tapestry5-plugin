package com.anjlab.eclipse.tapestry5;

public class Asset
{
    public final String bindingPrefix;
    public final String path;
    
    public Asset(String assetPath)
    {
        String path = assetPath;
        
        String bindingPrefix = "classpath";
        
        int colonIndex = assetPath.indexOf(":");
        if (colonIndex > 0)
        {
            bindingPrefix = assetPath.substring(0, colonIndex);
            
            path = assetPath.substring(colonIndex + 1);
        }
        
        this.path = path;
        this.bindingPrefix = bindingPrefix;
    }
}