package com.anjlab.eclipse.tapestry5;

public class LibraryMapping implements Openable
{
    private String pathPrefix;
    private String rootPackage;
    private DeclarationReference reference;
    
    public LibraryMapping(String pathPrefix, String rootPackage, DeclarationReference reference)
    {
        this.pathPrefix = pathPrefix;
        this.rootPackage = rootPackage;
        this.reference = reference;
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
    public DeclarationReference getReference()
    {
        return reference;
    }
    
    @Override
    public void openInEditor()
    {
        getReference().openInEditor();
    }
}
