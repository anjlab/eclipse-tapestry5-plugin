package com.anjlab.eclipse.tapestry5.views;

import java.util.ArrayList;

public class TreeParent extends TreeObject
{
    public static final class DataObject
    {
        private final String data;
        
        public DataObject(String data)
        {
            if (data == null)
            {
                throw new IllegalArgumentException("Nulls not allowed");
            }
            this.data = data;
        }
        
        @Override
        public boolean equals(Object obj)
        {
            return obj instanceof DataObject
                && data.equals(((DataObject) obj).data);
        }
        
        @Override
        public int hashCode()
        {
            return data.hashCode();
        }
    }
    
    private ArrayList<TreeObject> children;

    public TreeParent(String name, Object data)
    {
        super(name, data);
        children = new ArrayList<TreeObject>();
    }

    public void addChild(TreeObject child)
    {
        children.add(child);
        child.setParent(this);
    }

    public void removeChild(TreeObject child)
    {
        children.remove(child);
        child.setParent(null);
    }

    public int getChildCount()
    {
        return children.size();
    }
    
    public TreeObject[] getChildren()
    {
        return (TreeObject[]) children.toArray(new TreeObject[children.size()]);
    }

    public boolean hasChildren()
    {
        return children.size() > 0;
    }
}