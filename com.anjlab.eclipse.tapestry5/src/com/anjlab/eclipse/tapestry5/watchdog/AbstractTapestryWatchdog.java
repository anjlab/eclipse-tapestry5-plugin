package com.anjlab.eclipse.tapestry5.watchdog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.ui.IWorkbenchWindow;

import com.anjlab.eclipse.tapestry5.ITapestryContextListener;

public class AbstractTapestryWatchdog extends AbstractWatchdog
{
    protected final Map<IWorkbenchWindow, List<ITapestryContextListener>> tapestryContextListeners;
    
    protected final IWorkbenchWindow NULL_WINDOW = new NullWorkbenchWindow();
    
    public AbstractTapestryWatchdog()
    {
        tapestryContextListeners = new ConcurrentHashMap<IWorkbenchWindow, List<ITapestryContextListener>>();
    }

    public synchronized void addTapestryContextListener(IWorkbenchWindow window,
            ITapestryContextListener listener)
    {
        if (window == null)
        {
            window = NULL_WINDOW;
        }
        List<ITapestryContextListener> listeners = tapestryContextListeners.get(window);
        if (listeners == null)
        {
            listeners = new ArrayList<ITapestryContextListener>();
            tapestryContextListeners.put(window, listeners);
        }
        listeners.add(listener);
    }

    public synchronized void removeTapestryContextListener(IWorkbenchWindow window,
            ITapestryContextListener listener)
    {
        if (window == null)
        {
            window = NULL_WINDOW;
        }
        List<ITapestryContextListener> listeners = tapestryContextListeners.get(window);
        if (listeners != null)
        {
            listeners.remove(listener);
        }
    }

}