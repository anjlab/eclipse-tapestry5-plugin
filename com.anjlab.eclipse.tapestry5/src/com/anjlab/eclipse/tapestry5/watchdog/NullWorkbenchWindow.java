package com.anjlab.eclipse.tapestry5.watchdog;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.dynamichelpers.IExtensionTracker;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IPerspectiveListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.WorkbenchException;

@SuppressWarnings("rawtypes")
public class NullWorkbenchWindow implements IWorkbenchWindow
{
    @Override public boolean hasService(Class api) { return false; }

    @Override public Object getService(Class api) { return null; }

    @Override public void removePerspectiveListener(IPerspectiveListener listener) { }

    @Override public void removePageListener(IPageListener listener) { }

    @Override public void addPerspectiveListener(IPerspectiveListener listener) { }

    @Override public void addPageListener(IPageListener listener) { }

    @Override public void setActivePage(IWorkbenchPage page) { }

    @Override public void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable)
              throws InvocationTargetException, InterruptedException { }

    @Override public IWorkbenchPage openPage(String perspectiveId, IAdaptable input)
              throws WorkbenchException { return null; }

    @Override public IWorkbenchPage openPage(IAdaptable input) throws WorkbenchException { return null; }

    @Override public boolean isApplicationMenu(String menuId) { return false; }

    @Override public IWorkbench getWorkbench() { return null; }

    @Override public Shell getShell() { return null; }

    @Override public ISelectionService getSelectionService() { return null; }

    @Override public IPartService getPartService() { return null; }

    @Override public IWorkbenchPage[] getPages() { return null; }

    @Override public IExtensionTracker getExtensionTracker() { return null; }

    @Override public IWorkbenchPage getActivePage() { return null; }

    @Override public boolean close() { return false; }
}