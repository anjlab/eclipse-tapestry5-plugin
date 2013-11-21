package com.anjlab.eclipse.tapestry5;

import org.eclipse.ui.IWorkbenchWindow;

public interface ITapestryContextListener
{
    void projectChanged(IWorkbenchWindow window, TapestryProject newTapestryProject);
    
    void contextChanged(IWorkbenchWindow window, TapestryContext newContext);
    
    void selectionChanged(IWorkbenchWindow window, TapestryFile tapestryFile);
}
