package com.anjlab.eclipse.tapestry5.views;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IContributorResourceAdapter;
/*
 * The content provider class is responsible for providing objects to the
 * view. It can wrap existing objects in adapters or simply return objects
 * as-is. These objects may be sensitive to the current input of the view,
 * or ignore it and always show the same content (like Task List, for
 * example).
 */

public class TreeObject implements IAdaptable
{
    private String name;
    private Object data;
    private TreeParent parent;

    public TreeObject(String name, Object data)
    {
        if (name == null || data == null)
        {
            throw new NullPointerException("Nulls not allowed");
        }
        
        this.name = name;
        this.data = data;
    }

    public String getName()
    {
        return name;
    }
    
    public Object getData()
    {
        return data;
    }
    
    public void setParent(TreeParent parent)
    {
        this.parent = parent;
    }

    public TreeParent getParent()
    {
        return parent;
    }

    public String toString()
    {
        return getName();
    }

    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> key)
    {
        if (key.equals(IContributorResourceAdapter.class))
        {
            return (T) new IContributorResourceAdapter()
            {
                @Override
                public IResource getAdaptedResource(IAdaptable adaptable)
                {
                    // TODO Auto-generated method stub
                    return (IResource) data;
                }
            };
        }
        return null;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        
        if (!(obj instanceof TreeObject))
        {
            return false;
        }
        
        TreeObject other = (TreeObject) obj;
        
        return data.equals(other.data);
    }

    @Override
    public int hashCode()
    {
        return data.hashCode() + name.hashCode();
    }
}