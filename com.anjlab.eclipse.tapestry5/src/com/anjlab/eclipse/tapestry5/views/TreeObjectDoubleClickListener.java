package com.anjlab.eclipse.tapestry5.views;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.anjlab.eclipse.tapestry5.Openable;

public class TreeObjectDoubleClickListener implements IDoubleClickListener
{
    public void doubleClick(DoubleClickEvent event)
    {
        ISelection selection = event.getSelection();
        
        Object obj = ((IStructuredSelection) selection).getFirstElement();
        
        if (obj instanceof TreeObject)
        {
            Object data = ((TreeObject) obj).getData();
            
            if (data instanceof Openable)
            {
                ((Openable) data).openInEditor();
            }
        }
    }
}