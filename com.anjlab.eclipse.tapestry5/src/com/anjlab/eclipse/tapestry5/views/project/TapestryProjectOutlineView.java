package com.anjlab.eclipse.tapestry5.views.project;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.part.ViewPart;

import com.anjlab.eclipse.tapestry5.Activator;
import com.anjlab.eclipse.tapestry5.EclipseUtils;
import com.anjlab.eclipse.tapestry5.JavaScriptStack;
import com.anjlab.eclipse.tapestry5.LibraryMapping;
import com.anjlab.eclipse.tapestry5.TapestryContext;
import com.anjlab.eclipse.tapestry5.TapestryFile;
import com.anjlab.eclipse.tapestry5.TapestryModule;
import com.anjlab.eclipse.tapestry5.TapestryProject;
import com.anjlab.eclipse.tapestry5.TapestryService;
import com.anjlab.eclipse.tapestry5.views.NameSorter;
import com.anjlab.eclipse.tapestry5.views.TapestryDecoratingLabelProvider;
import com.anjlab.eclipse.tapestry5.views.TreeObject;
import com.anjlab.eclipse.tapestry5.views.ViewLabelProvider;
import com.anjlab.eclipse.tapestry5.watchdog.ITapestryContextListener;

/**
 * This sample class demonstrates how to plug-in a new workbench view. The view
 * shows data obtained from the model. The sample creates a dummy model on the
 * fly, but a real implementation would connect to the model available either in
 * this or another plug-in (e.g. the workspace). The view is connected to the
 * model using a content provider.
 * <p>
 * The view uses a label provider to define how model objects should be
 * presented in the view. Each view can present the same model objects using
 * different labels and icons, if needed. Alternatively, a single label provider
 * can be shared between views in order to ensure that objects of the same type
 * are presented in the same way everywhere.
 * <p>
 */

public class TapestryProjectOutlineView extends ViewPart
{
    /**
     * The ID of the view as specified by the extension.
     */
    public static final String ID = "com.anjlab.eclipse.tapestry5.views.TapestryProjectOutlineView";

    private TreeViewer viewer;
    private ITapestryContextListener tapestryContextListener;

    /**
     * This is a callback that will allow us to create the viewer and initialize it.
     */
    public void createPartControl(Composite parent)
    {
        viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        viewer.setContentProvider(new TapestryProjectOutlineContentProvider(Activator.getDefault().getTapestryProject(getSite().getWorkbenchWindow())));
        viewer.setLabelProvider(new TapestryDecoratingLabelProvider(new ViewLabelProvider()));
        viewer.setSorter(new NameSorter());
        viewer.setInput(getViewSite());
        viewer.addDoubleClickListener(new IDoubleClickListener()
        {
            public void doubleClick(DoubleClickEvent event)
            {
                ISelection selection = viewer.getSelection();
                
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
                    else if (data instanceof LibraryMapping)
                    {
                        ((LibraryMapping) data).getReference().openInEditor();
                    }
                    else if (data instanceof TapestryService)
                    {
                        ((TapestryService) data).getReference().openInEditor();
                    }
                }
            }
        });
        
        tapestryContextListener = new ITapestryContextListener()
        {
            @Override
            public void projectChanged(IWorkbenchWindow window, final TapestryProject newTapestryProject)
            {
                if (!getSite().getWorkbenchWindow().equals(window))
                {
                    return;
                }
                
                //  TODO Check if this is the same project with updates or simply a new project was selected
                
                window.getShell().getDisplay().syncExec(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        TreePath[] expandedTreePaths = viewer.getExpandedTreePaths();
                        
                        viewer.setContentProvider(new TapestryProjectOutlineContentProvider(newTapestryProject));
                        
                        viewer.setExpandedTreePaths(expandedTreePaths);
                    }
                });
            }
            
            @Override
            public void contextChanged(IWorkbenchWindow window, final TapestryContext newContext) { }
            
            @Override
            public void selectionChanged(IWorkbenchWindow window, TapestryFile selectedFile) { }
        };
        
        Activator.getDefault().addTapestryProjectListener(getViewSite().getWorkbenchWindow(), tapestryContextListener);
    }
    
    @Override
    public void dispose()
    {
        Activator.getDefault().removeTapestryProjectListener(getViewSite().getWorkbenchWindow(), tapestryContextListener);
        
        super.dispose();
    }
    
    /**
     * Passing the focus request to the viewer's control.
     */
    public void setFocus()
    {
        viewer.getControl().setFocus();
    }

}