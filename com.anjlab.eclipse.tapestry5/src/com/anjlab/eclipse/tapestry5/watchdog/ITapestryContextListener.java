package com.anjlab.eclipse.tapestry5.watchdog;

import org.eclipse.ui.IWorkbenchWindow;

import com.anjlab.eclipse.tapestry5.TapestryContext;
import com.anjlab.eclipse.tapestry5.TapestryFile;
import com.anjlab.eclipse.tapestry5.TapestryProject;

public interface ITapestryContextListener
{
    void projectChanged(IWorkbenchWindow window, TapestryProject newTapestryProject);
    
    void contextChanged(IWorkbenchWindow window, TapestryContext newContext);
    
    void selectionChanged(IWorkbenchWindow window, TapestryFile tapestryFile);
}
