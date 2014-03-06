package com.anjlab.eclipse.tapestry5.watchdog;

import org.eclipse.ui.IWorkbenchWindow;

public interface Watchdog
{

    public abstract void stop();

    public abstract void start();

    public abstract void removeListener(IWorkbenchWindow window, Object listener);

    public abstract void addListener(IWorkbenchWindow window, Object listener);

}