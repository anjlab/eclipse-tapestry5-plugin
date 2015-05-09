package com.anjlab.eclipse.tapestry5.views;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;

public class SimpleSelectionProvider implements ISelectionProvider
{
    private final ListenerList listeners = new ListenerList();
    private ISelection selection;
    
    @Override
    public void setSelection(ISelection selection)
    {
        this.selection = selection;
        
        for (Object l : listeners.getListeners())
        {
            ((ISelectionChangedListener) l).selectionChanged(new SelectionChangedEvent(this, selection));
        }
    }

    @Override
    public void removeSelectionChangedListener(ISelectionChangedListener listener)
    {
        listeners.remove(listener);
    }

    @Override
    public ISelection getSelection()
    {
        return selection;
    }

    @Override
    public void addSelectionChangedListener(ISelectionChangedListener listener)
    {
        listeners.add(listener);
    }
}