package com.anjlab.eclipse.tapestry5.watchdog;

import org.eclipse.ui.IPageListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class WindowSelectionListener implements IWindowListener
{
    private ISelectionListener selectionListener;
    
    public WindowSelectionListener(ISelectionListener selectionListener)
    {
        this.selectionListener = selectionListener;
    }
    
    private IPageListener pageListener = new IPageListener()
    {
        @Override
        public void pageOpened(IWorkbenchPage page)
        {
            //  Prevent double-add
            page.removeSelectionListener(selectionListener);
            
            page.addSelectionListener(selectionListener);
        }
        
        @Override
        public void pageClosed(IWorkbenchPage page)
        {
            page.removeSelectionListener(selectionListener);
        }
        
        @Override
        public void pageActivated(IWorkbenchPage page)
        {
        }
    };

    @Override
    public void windowOpened(IWorkbenchWindow window)
    {
        //  Prevent double-add
        window.removePageListener(pageListener);
        
        window.addPageListener(pageListener);
        
        //  windowOpened might be fired manually and the page was already open at this moment,
        //  so trigger pageOpened event manually now to setup handlers
        pageListener.pageOpened(window.getActivePage());
    }

    @Override
    public void windowDeactivated(IWorkbenchWindow window)
    {
    }

    @Override
    public void windowClosed(IWorkbenchWindow window)
    {
        window.removePageListener(pageListener);
    }

    @Override
    public void windowActivated(IWorkbenchWindow window)
    {
    }
    
    protected void dispose()
    {
        IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
        for (IWorkbenchWindow window : windows)
        {
            IWorkbenchPage[] pages = window.getPages();
            for (IWorkbenchPage page : pages)
            {
                pageListener.pageClosed(page);
            }
            windowClosed(window);
        }
    }

    public WindowSelectionListener addListener()
    {
        //  Prevent double-add
        PlatformUI.getWorkbench().removeWindowListener(this);
        
        PlatformUI.getWorkbench().addWindowListener(this);
        
        IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        
        if (activeWindow != null)
        {
            //  This plug-in activated after the window opened, so should notify manually here
            windowOpened(activeWindow);
        }
        
        return this;
    }

    public void removeListener()
    {
        dispose();
        
        PlatformUI.getWorkbench().removeWindowListener(this);
    }
}