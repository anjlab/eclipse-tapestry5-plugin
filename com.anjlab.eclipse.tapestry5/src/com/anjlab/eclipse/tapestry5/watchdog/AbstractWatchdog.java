package com.anjlab.eclipse.tapestry5.watchdog;

public class AbstractWatchdog implements Watchdog
{
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
}