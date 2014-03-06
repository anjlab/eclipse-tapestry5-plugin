package com.anjlab.eclipse.tapestry5.watchdog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.ui.IWorkbenchWindow;

public class WindowListeners
{
    private final Map<IWorkbenchWindow, List<Object>> hub;
    
    public WindowListeners()
    {
        hub = new ConcurrentHashMap<IWorkbenchWindow, List<Object>>();
    }

    public synchronized void addListener(IWorkbenchWindow window, Object listener)
    {
        if (window == null)
        {
            window = NullWorkbenchWindow.INSTANCE;
        }
        List<Object> listeners = hub.get(window);
        if (listeners == null)
        {
            listeners = new ArrayList<Object>();
            hub.put(window, listeners);
        }
        listeners.add(listener);
    }

    public synchronized void removeListener(IWorkbenchWindow window, Object listener)
    {
        if (window == null)
        {
            window = NullWorkbenchWindow.INSTANCE;
        }
        List<Object> listeners = hub.get(window);
        if (listeners != null)
        {
            hub.remove(listener);
        }
    }

    public <T> List<T> find(Class<T> listenerClass, IWorkbenchWindow window, boolean includeNullWindow)
    {
        List<T> result = new ArrayList<T>();
        if (window != null)
        {
            findListeners(listenerClass, window, result);
        }
        if (includeNullWindow || window == null)
        {
            findListeners(listenerClass, NullWorkbenchWindow.INSTANCE, result);
        }
        return result;
    }

    private <T> void findListeners(Class<T> listenerClass, IWorkbenchWindow window, List<T> result)
    {
        findListeners(listenerClass, result, hub.get(window));
    }

    @SuppressWarnings("unchecked")
    private <T> void findListeners(Class<T> listenerClass, List<T> result, List<Object> list)
    {
        if (list != null)
        {
            for (Object listener : list)
            {
                if (listenerClass.isAssignableFrom(listener.getClass()))
                {
                    result.add((T) listener);
                }
            }
        }
    }

}