package com.anjlab.eclipse.tapestry5;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ISourceRange;

public class AssetReference extends AbstractFileReference
{
    public static final String MARKER_NAME = "AssetPath";

    public AssetReference(TapestryFile javaFile, ISourceRange sourceRange, String assetPath)
    {
        super(javaFile, sourceRange, assetPath.replace('\\', '/'), MARKER_NAME);
    }
    
    protected TapestryFile resolveFile() throws UnresolvableReferenceException
    {
        Asset asset = new Asset(reference);
        
        AssetResolver assetResolver = TapestryUtils.createAssetResolver(asset.bindingPrefix);
        
        TapestryFile resolvedFile = assetResolver.resolve(asset.path, javaFile);
        
        return resolvedFile;
    }
    
    @Override
    public String getName()
    {
        Asset asset = new Asset(reference);
        
        int separatorIndex = asset.path.lastIndexOf('/');
        
        if (separatorIndex < 0)
        {
            return asset.path;
        }
        return asset.path.substring(separatorIndex + 1);
    }
    
    @Override
    public IPath getPath()
    {
        return new Path(reference);
    }
    
    @Override
    public boolean isJavaFile()
    {
        return false;
    }
    
    @Override
    public boolean isPropertiesFile()
    {
        return false;
    }
    
    @Override
    public boolean isTemplateFile()
    {
        return false;
    }
}
