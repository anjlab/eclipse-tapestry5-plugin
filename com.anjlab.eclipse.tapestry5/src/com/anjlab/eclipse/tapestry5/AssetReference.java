package com.anjlab.eclipse.tapestry5;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ISourceRange;

public class AssetReference extends AbstractTapestryFile
{
    public static final String ASSET_PATH_MARKER_ATTRIBUTE = "AssetPath";
    
    private TapestryFile javaFile;
    private String assetPath;
    private ISourceRange sourceRange;
    
    public AssetReference(TapestryFile javaFile, ISourceRange sourceRange, String assetPath)
    {
        this.javaFile = javaFile;
        this.sourceRange = sourceRange;
        this.assetPath = assetPath.replace('\\', '/');
    }
    
    @Override
    public String toString()
    {
        return assetPath;
    }
    
    @Override
    public int hashCode()
    {
        return javaFile.hashCode() + assetPath.hashCode();
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        
        if (obj == null)
        {
            return false;
        }
        
        if (obj instanceof AssetReference)
        {
            AssetReference other = (AssetReference) obj;
            
            return javaFile.equals(other.javaFile) && assetPath.equals(other.assetPath);
        }
        
        if (obj instanceof TapestryFile)
        {
            try
            {
                return obj.equals(resolveFile(false));
            }
            catch (AssetException e)
            {
                //  Ignore
            }
        }
        
        return false;
    }
    
    public String getAssetPath()
    {
        return assetPath;
    }
    
    public ISourceRange getSourceRange()
    {
        return sourceRange;
    }
    
    public TapestryFile getJavaFile()
    {
        return javaFile;
    }
    
    private static class Asset
    {
        private final String bindingPrefix;
        private final String path;
        
        public Asset(String assetPath)
        {
            String path = assetPath;
            
            String bindingPrefix = "default";
            
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
    
    public TapestryFile resolveFile(boolean updateMarker) throws AssetException
    {
        Asset asset = new Asset(assetPath);
        
        AssetResolver assetResolver = TapestryUtils.createAssetResolver(asset.bindingPrefix, asset.path);
        
        if (assetResolver == null)
        {
            throw new AssetException("Binding prefix '" + asset.bindingPrefix + "' not supported");
        }
        
        try
        {
            TapestryFile resolvedFile = assetResolver.resolve(asset.path, javaFile);
            
            if (updateMarker)
            {
                try
                {
                    deleteMarker();
                }
                catch (CoreException e)
                {
                    Activator.getDefault().logError("Error deleting problem marker", e);
                }
            }
            
            return resolvedFile;
        }
        catch (AssetException e)
        {
            if (updateMarker)
            {
                try
                {
                    createMarker(e);
                }
                catch (CoreException e2)
                {
                    Activator.getDefault().logError("Error creating problem marker", e2);
                }
            }
            
            throw e;
        }
    }
    
    @Override
    public String getName()
    {
        Asset asset = new Asset(assetPath);
        
        int separatorIndex = asset.path.lastIndexOf('/');
        
        if (separatorIndex < 0)
        {
            return asset.path;
        }
        return asset.path.substring(separatorIndex + 1);
    }
    
    public void createMarker(Throwable t) throws CoreException
    {
        if (!(javaFile instanceof LocalFile))
        {
            return;
        }
        
        IMarker marker = findMarker();
        
        if (marker == null)
        {
            marker = ((LocalFile) javaFile).getFile().createMarker(IMarker.PROBLEM);
            
            marker.setAttribute(IMarker.MESSAGE, t.getLocalizedMessage());
            marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
            marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
            
            if (sourceRange != null)
            {
                marker.setAttribute(IMarker.CHAR_START, sourceRange.getOffset());
                marker.setAttribute(IMarker.CHAR_END, sourceRange.getOffset() + sourceRange.getLength());
            }
            
            marker.setAttribute(ASSET_PATH_MARKER_ATTRIBUTE, assetPath);
        }
    }

    public IMarker findMarker() throws CoreException
    {
        if (!(javaFile instanceof LocalFile))
        {
            return null;
        }
        
        IMarker[] markers = ((LocalFile) javaFile).getFile().findMarkers(IMarker.PROBLEM, false, IResource.DEPTH_ZERO);
        
        IMarker assetPathMarker = null;
        
        for (IMarker marker : markers)
        {
            Object markerPath = marker.getAttribute(ASSET_PATH_MARKER_ATTRIBUTE);
            
            if (markerPath != null && markerPath.equals(assetPath))
            {
                assetPathMarker = marker;
                break;
            }
        }
        return assetPathMarker;
    }

    public void deleteMarker() throws CoreException
    {
        IMarker marker = findMarker();
        
        if (marker != null)
        {
            marker.delete();
        }
    }

    @Override
    public IPath getPath()
    {
        return new Path(assetPath);
    }
    
    @Override
    public IProject getProject()
    {
        return javaFile.getProject();
    }

    @Override
    public TapestryContext getContext()
    {
        return getJavaFile().getContext();
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
