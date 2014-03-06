package com.anjlab.eclipse.tapestry5.watchdog;

import org.eclipse.ui.IWorkbenchWindow;

public class AbstractWatchdog implements Watchdog
{
    protected final WindowListeners listeners = new WindowListeners();
    
    private boolean started;

    @Override
    public synchronized void start()
    {
        if (started)
        {
            throw new IllegalStateException("Already started");
        }
        
        started = true;
    }

    @Override
    public synchronized void stop()
    {
        started = false;
    }
    
    @Override
    public void addListener(IWorkbenchWindow window, Object listener)
    {
        listeners.addListener(window, listener);
    }
    
    @Override
    public void removeListener(IWorkbenchWindow window, Object listener)
    {
        listeners.removeListener(window, listener);
    }
}