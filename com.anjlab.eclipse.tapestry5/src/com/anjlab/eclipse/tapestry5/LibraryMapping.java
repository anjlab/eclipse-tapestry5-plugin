package com.anjlab.eclipse.tapestry5;

public class LibraryMapping
{
    private String pathPrefix;
    private String rootPackage;
    
    public LibraryMapping(String pathPrefix, String rootPackage)
    {
        this.pathPrefix = pathPrefix;
        this.rootPackage = rootPackage;
    }
    
    public String getPathPrefix()
    {
        return pathPrefix;
    }
    public void setPathPrefix(String pathPrefix)
    {
        this.pathPrefix = pathPrefix;
    }
    public String getRootPackage()
    {
        return rootPackage;
    }
    public void setRootPackage(String rootPackage)
    {
        this.rootPackage = rootPackage;
    }
}
