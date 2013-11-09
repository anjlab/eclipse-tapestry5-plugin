package com.anjlab.eclipse.tapestry5;

import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.ui.IContributorResourceAdapter;

public class AssetPath implements IFile
{
    public static final String ASSET_PATH_MARKER_ATTRIBUTE = "AssetPath";
    
    private IFile javaFile;
    private String assetPath;
    private ISourceRange sourceRange;
    
    public AssetPath(IFile javaFile, ISourceRange sourceRange, String assetPath)
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
    public int getType()
    {
        return IFile.FILE;
    }
    
    @Override
    public int hashCode()
    {
        return javaFile.hashCode() + assetPath.hashCode();
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        
        if (obj instanceof AssetPath)
        {
            AssetPath other = (AssetPath) obj;
            
            return javaFile.equals(other.javaFile) && assetPath.equals(other.assetPath);
        }
        
        if (obj instanceof IFile)
        {
            try
            {
                return obj.equals(resolveFile(false));
            }
            catch (AssetException e)
            {
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
    
    public IFile getJavaFile()
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
    
    public IFile resolveFile(boolean updateMarker) throws AssetException
    {
        Asset asset = new Asset(assetPath);
        
        AssetResolver assetResolver = TapestryUtils.createAssetResolver(asset.bindingPrefix, asset.path);
        
        if (assetResolver == null)
        {
            throw new AssetException("Binding prefix '" + asset.bindingPrefix + "' not supported");
        }
        
        try
        {
            IFile resolvedFile = assetResolver.resolve(asset.path, javaFile);
            
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
        IMarker marker = findMarker();
        
        if (marker == null)
        {
            marker = javaFile.createMarker(IMarker.PROBLEM);
            
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
        IMarker[] markers = javaFile.findMarkers(IMarker.PROBLEM, false, IResource.DEPTH_ZERO);
        
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
    
    @SuppressWarnings("rawtypes")
    @Override
    public Object getAdapter(Class clazz)
    {
        if (clazz.equals(IContributorResourceAdapter.class))
        {
            return new IContributorResourceAdapter()
            {
                @Override
                public IResource getAdaptedResource(IAdaptable adaptable)
                {
                    return AssetPath.this;
                }
            };
        }
        return null;
    }
    
    @Override
    public void accept(IResourceVisitor arg0) throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void accept(IResourceProxyVisitor arg0, int arg1)
            throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void accept(IResourceProxyVisitor arg0, int arg1, int arg2)
            throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void accept(IResourceVisitor arg0, int arg1, boolean arg2)
            throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void accept(IResourceVisitor arg0, int arg1, int arg2)
            throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void clearHistory(IProgressMonitor arg0) throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void copy(IPath arg0, boolean arg1, IProgressMonitor arg2)
            throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void copy(IPath arg0, int arg1, IProgressMonitor arg2)
            throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void copy(IProjectDescription arg0, boolean arg1,
            IProgressMonitor arg2) throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void copy(IProjectDescription arg0, int arg1, IProgressMonitor arg2)
            throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public IMarker createMarker(String arg0) throws CoreException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IResourceProxy createProxy()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void delete(boolean arg0, IProgressMonitor arg1)
            throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete(int arg0, IProgressMonitor arg1) throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteMarkers(String arg0, boolean arg1, int arg2)
            throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean exists()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public IMarker findMarker(long arg0) throws CoreException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IMarker[] findMarkers(String arg0, boolean arg1, int arg2)
            throws CoreException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int findMaxProblemSeverity(String arg0, boolean arg1, int arg2)
            throws CoreException
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getFileExtension()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getLocalTimeStamp()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public IPath getLocation()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public URI getLocationURI()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IMarker getMarker(long arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getModificationStamp()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public IContainer getParent()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IPathVariableManager getPathVariableManager()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<QualifiedName, String> getPersistentProperties()
            throws CoreException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getPersistentProperty(QualifiedName arg0)
            throws CoreException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IProject getProject()
    {
        return null;
    }

    @Override
    public IPath getProjectRelativePath()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IPath getRawLocation()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public URI getRawLocationURI()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResourceAttributes getResourceAttributes()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<QualifiedName, Object> getSessionProperties()
            throws CoreException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getSessionProperty(QualifiedName arg0) throws CoreException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IWorkspace getWorkspace()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isAccessible()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isDerived()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isDerived(int arg0)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isHidden()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isHidden(int arg0)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isLinked()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isLinked(int arg0)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isLocal(int arg0)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isPhantom()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isSynchronized(int arg0)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isTeamPrivateMember()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isTeamPrivateMember(int arg0)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isVirtual()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void move(IPath arg0, boolean arg1, IProgressMonitor arg2)
            throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void move(IPath arg0, int arg1, IProgressMonitor arg2)
            throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void move(IProjectDescription arg0, int arg1, IProgressMonitor arg2)
            throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void move(IProjectDescription arg0, boolean arg1, boolean arg2,
            IProgressMonitor arg3) throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void refreshLocal(int arg0, IProgressMonitor arg1)
            throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void revertModificationStamp(long arg0) throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void setDerived(boolean arg0) throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void setDerived(boolean arg0, IProgressMonitor arg1)
            throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void setHidden(boolean arg0) throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void setLocal(boolean arg0, int arg1, IProgressMonitor arg2)
            throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public long setLocalTimeStamp(long arg0) throws CoreException
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setPersistentProperty(QualifiedName arg0, String arg1)
            throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void setReadOnly(boolean arg0)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void setResourceAttributes(ResourceAttributes arg0)
            throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void setSessionProperty(QualifiedName arg0, Object arg1)
            throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void setTeamPrivateMember(boolean arg0) throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void touch(IProgressMonitor arg0) throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean contains(ISchedulingRule arg0)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isConflicting(ISchedulingRule arg0)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void appendContents(InputStream arg0, int arg1, IProgressMonitor arg2)
            throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void appendContents(InputStream arg0, boolean arg1, boolean arg2,
            IProgressMonitor arg3) throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void create(InputStream arg0, boolean arg1, IProgressMonitor arg2)
            throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void create(InputStream arg0, int arg1, IProgressMonitor arg2)
            throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void createLink(IPath arg0, int arg1, IProgressMonitor arg2)
            throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void createLink(URI arg0, int arg1, IProgressMonitor arg2)
            throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete(boolean arg0, boolean arg1, IProgressMonitor arg2)
            throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public String getCharset() throws CoreException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getCharset(boolean arg0) throws CoreException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getCharsetFor(Reader arg0) throws CoreException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IContentDescription getContentDescription() throws CoreException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InputStream getContents() throws CoreException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InputStream getContents(boolean arg0) throws CoreException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getEncoding() throws CoreException
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public IPath getFullPath()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IFileState[] getHistory(IProgressMonitor arg0) throws CoreException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isReadOnly()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void move(IPath arg0, boolean arg1, boolean arg2,
            IProgressMonitor arg3) throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void setCharset(String arg0) throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void setCharset(String arg0, IProgressMonitor arg1)
            throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void setContents(InputStream arg0, int arg1, IProgressMonitor arg2)
            throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void setContents(IFileState arg0, int arg1, IProgressMonitor arg2)
            throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void setContents(InputStream arg0, boolean arg1, boolean arg2,
            IProgressMonitor arg3) throws CoreException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void setContents(IFileState arg0, boolean arg1, boolean arg2,
            IProgressMonitor arg3) throws CoreException
    {
        // TODO Auto-generated method stub

    }

}
