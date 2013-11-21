package com.anjlab.eclipse.tapestry5.views.context;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.part.ViewPart;

import com.anjlab.eclipse.tapestry5.Activator;
import com.anjlab.eclipse.tapestry5.EclipseUtils;
import com.anjlab.eclipse.tapestry5.ITapestryContextListener;
import com.anjlab.eclipse.tapestry5.TapestryContext;
import com.anjlab.eclipse.tapestry5.TapestryFile;
import com.anjlab.eclipse.tapestry5.TapestryProject;
import com.anjlab.eclipse.tapestry5.views.TreeObject;
import com.anjlab.eclipse.tapestry5.views.ViewLabelProvider;

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

public class TapestryContextView extends ViewPart
{
    /**
     * The ID of the view as specified by the extension.
     */
    public static final String ID = "com.anjlab.eclipse.tapestry5.views.context.TapestryContextView";

    private TreeViewer viewer;
    private ITapestryContextListener tapestryContextListener;

    /**
     * This is a callback that will allow us to create the viewer and initialize it.
     */
    public void createPartControl(Composite parent)
    {
        viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        viewer.setContentProvider(new TapestryContextContentProvider(Activator.getDefault().getTapestryContext(getSite().getWorkbenchWindow())));
        viewer.setLabelProvider(new ViewLabelProvider());
        viewer.setInput(getViewSite());
        viewer.addDoubleClickListener(new IDoubleClickListener()
        {
            public void doubleClick(DoubleClickEvent event)
            {
                ISelection selection = viewer.getSelection();
                
                Object obj = ((IStructuredSelection) selection).getFirstElement();
                
                if (obj instanceof TreeObject)
                {
                    TapestryFile file = (TapestryFile) ((TreeObject) obj).getData();
                    
                    EclipseUtils.openFile(getViewSite().getWorkbenchWindow(), file);
                }
            }
        });
        
        tapestryContextListener = new ITapestryContextListener()
        {
            @Override
            public void contextChanged(IWorkbenchWindow window, final TapestryContext newContext)
            {
                window.getShell().getDisplay().syncExec(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        viewer.setContentProvider(new TapestryContextContentProvider(newContext));
                    }
                });
            }
            
            @Override
            public void selectionChanged(IWorkbenchWindow window, TapestryFile selectedFile)
            {
                viewer.setSelection(
                        new TreeSelection(
                            new TreePath(
                                new Object[] { new TreeObject(selectedFile.getName(), selectedFile) })));
            }
            
            @Override
            public void projectChanged(IWorkbenchWindow window, TapestryProject newTapestryProject) { }
        };
        
        Activator.getDefault().addTapestryContextListener(getViewSite().getWorkbenchWindow(), tapestryContextListener);
    }
    
    @Override
    public void dispose()
    {
        Activator.getDefault().removeTapestryContextListener(getViewSite().getWorkbenchWindow(), tapestryContextListener);
        
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