package com.anjlab.eclipse.tapestry5;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ISourceRange;

public abstract class AbstractFileReference extends AbstractTapestryFile implements TapestryFileReference
{
    protected final TapestryFile javaFile;
    protected final String reference;
    protected final ISourceRange sourceRange;
    protected final String markerName;
    
    public AbstractFileReference(TapestryFile javaFile, ISourceRange sourceRange,
                                 String reference,
                                 String markerName)
    {
        this.javaFile = javaFile;
        this.reference = reference;
        this.sourceRange = sourceRange;
        this.markerName = markerName;
    }
    
    @Override
    public String getReference()
    {
        return reference;
    }
    
    protected abstract TapestryFile resolveFile() throws UnresolvableReferenceException;
    
    @Override
    public TapestryFile resolveFile(boolean updateMarker) throws UnresolvableReferenceException
    {
        try
        {
            TapestryFile resolvedFile = resolveFile();
            
            if (updateMarker)
            {
                try
                {
                    deleteMarker(markerName, reference);
                }
                catch (CoreException e)
                {
                    Activator.getDefault().logError("Error deleting problem marker", e);
                }
            }
            
            return resolvedFile;
        }
        catch (UnresolvableReferenceException e)
        {
            if (updateMarker)
            {
                try
                {
                    createMarker(e, markerName, reference);
                }
                catch (CoreException e2)
                {
                    Activator.getDefault().logError("Error creating problem marker", e2);
                }
            }
            
            throw e;
        }
    }

    private void createMarker(Throwable t, String attributeName, String attributeValue) throws CoreException
    {
        if (!(javaFile instanceof LocalFile))
        {
            return;
        }
        
        IMarker marker = findMarker(attributeName, attributeValue);
        
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
            
            marker.setAttribute(attributeName, attributeValue);
        }
    }

    private IMarker findMarker(String attributeName, String attributeValue) throws CoreException
    {
        if (!(javaFile instanceof LocalFile))
        {
            return null;
        }
        
        IMarker[] markers = ((LocalFile) javaFile).getFile().findMarkers(IMarker.PROBLEM, false, IResource.DEPTH_ZERO);
        
        for (IMarker marker : markers)
        {
            Object attribute = marker.getAttribute(attributeName);
            
            if (attribute != null && attribute.equals(attributeValue))
            {
                return marker;
            }
        }
        
        return null;
    }

    private void deleteMarker(String attributeName, String attributeValue) throws CoreException
    {
        IMarker marker = findMarker(attributeName, attributeValue);
        
        if (marker != null)
        {
            marker.delete();
        }
    }

    public TapestryFile getJavaFile()
    {
        return javaFile;
    }
    
    @Override
    public boolean exists()
    {
        try
        {
            return resolveFile().exists();
        }
        catch (UnresolvableReferenceException e)
        {
            return false;
        }
    }
    

    @Override
    public String toString()
    {
        return reference;
    }
    
    @Override
    public int hashCode()
    {
        return javaFile.hashCode() + reference.hashCode();
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        
        if (obj instanceof AbstractFileReference)
        {
            AbstractFileReference other = (AbstractFileReference) obj;
            
            return javaFile.equals(other.javaFile) && reference.equals(other.reference);
        }
        
        if (obj instanceof TapestryFile)
        {
            try
            {
                return obj.equals(resolveFile());
            }
            catch (UnresolvableReferenceException e)
            {
                //  Ignore
            }
        }
        
        return false;
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
    public String getClassName()
    {
        return null;
    }
}