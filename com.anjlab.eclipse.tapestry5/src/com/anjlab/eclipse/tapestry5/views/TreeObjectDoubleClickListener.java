package com.anjlab.eclipse.tapestry5.views;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IViewSite;

import com.anjlab.eclipse.tapestry5.EclipseUtils;
import com.anjlab.eclipse.tapestry5.JavaScriptStack;
import com.anjlab.eclipse.tapestry5.LibraryMapping;
import com.anjlab.eclipse.tapestry5.TapestryFile;
import com.anjlab.eclipse.tapestry5.TapestryModule;
import com.anjlab.eclipse.tapestry5.TapestryService;
import com.anjlab.eclipse.tapestry5.TapestryService.ServiceInstrumenter;
import com.anjlab.eclipse.tapestry5.TapestrySymbol;

public class TreeObjectDoubleClickListener implements IDoubleClickListener
{
    public void doubleClick(DoubleClickEvent event)
    {
        ISelection selection = event.getSelection();
        
        Object obj = ((IStructuredSelection) selection).getFirstElement();
        
        if (obj instanceof TreeObject)
        {
            Object data = ((TreeObject) obj).getData();
            
            if (data instanceof TapestryModule)
            {
                EclipseUtils.openDeclaration(
                        ((TapestryModule) data).getModuleClass(), null);
            }
            else if (data instanceof JavaScriptStack)
            {
                EclipseUtils.openDeclaration(
                        ((JavaScriptStack) data).getType(), null);
            }
            else if (data instanceof TapestrySymbol)
            {
                ((TapestrySymbol) data).getReference().openInEditor();
            }
            else if (data instanceof LibraryMapping)
            {
                ((LibraryMapping) data).getReference().openInEditor();
            }
            else if (data instanceof TapestryService)
            {
                ((TapestryService) data).getReference().openInEditor();
            }
            else if (data instanceof ServiceInstrumenter)
            {
                ((ServiceInstrumenter) data).getReference().openInEditor();
            }
            else if (data instanceof TapestryFile)
            {
                EclipseUtils.openFile(
                        ((IViewSite) event.getViewer().getInput()).getWorkbenchWindow(),
                        (TapestryFile) data);
            }
        }
    }
}