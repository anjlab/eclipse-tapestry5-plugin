package com.anjlab.eclipse.tapestry5;

public class ClasspathAssetResolver2 extends ClasspathAssetResolver
{
    @Override
    public TapestryFile resolve(String path, TapestryFile relativeTo)
            throws UnresolvableReferenceException
    {
        try
        {
            return super.resolve(path, relativeTo);
        }
        catch (UnresolvableReferenceException e)
        {
            return super.resolve("META-INF/assets/" + path, relativeTo);
        }
    }
    
    @Override
    public TapestryFile resolveInWorkspace(String path)
    {
        TapestryFile file = super.resolveInWorkspace(path);
        
        if (file == null)
        {
            file = super.resolveInWorkspace("META-INF/assets/" + path);
        }
        
        return file;
    }
}
