package com.anjlab.eclipse.tapestry5.views.context;

import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.part.ViewPart;

import com.anjlab.eclipse.tapestry5.Activator;
import com.anjlab.eclipse.tapestry5.EclipseUtils;
import com.anjlab.eclipse.tapestry5.TapestryContext;
import com.anjlab.eclipse.tapestry5.TapestryFile;
import com.anjlab.eclipse.tapestry5.TapestryProject;
import com.anjlab.eclipse.tapestry5.views.NameSorter;
import com.anjlab.eclipse.tapestry5.views.SimpleSelectionProvider;
import com.anjlab.eclipse.tapestry5.views.TapestryDecoratingLabelProvider;
import com.anjlab.eclipse.tapestry5.views.TreeObject;
import com.anjlab.eclipse.tapestry5.views.TreeObjectDoubleClickListener;
import com.anjlab.eclipse.tapestry5.views.TreeObjectSelectionListener;
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
        final ISelectionProvider selectionProvider = new SimpleSelectionProvider();
        
        getSite().setSelectionProvider(selectionProvider);

        viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        setContentProvider(
                new TapestryContextContentProvider(
                        getSite().getWorkbenchWindow(),
                        Activator.getDefault().getTapestryContext(
                                getSite().getWorkbenchWindow())));
        viewer.setLabelProvider(new TapestryDecoratingLabelProvider(new ViewLabelProvider()));
        viewer.setSorter(new NameSorter());
        viewer.setInput(getViewSite());
        viewer.addSelectionChangedListener(
                new TreeObjectSelectionListener(
                        getSite().getWorkbenchWindow(),
                        selectionProvider,
                        viewer));
        viewer.addDoubleClickListener(new TreeObjectDoubleClickListener());
        
        tapestryContextListener = new ITapestryContextListener()
        {
            @Override
            public void contextChanged(final IWorkbenchWindow window, final TapestryContext newContext)
            {
                if (!getSite().getWorkbenchWindow().equals(window))
                {
                    return;
                }
                
                EclipseUtils.syncExec(window.getShell(), new Runnable()
                {
                    @Override
                    public void run()
                    {
                        setContentProvider(new TapestryContextContentProvider(window, newContext));
                    }
                });
            }
            
            @Override
            public void selectionChanged(IWorkbenchWindow window, TapestryFile selectedFile)
            {
                if (!getSite().getWorkbenchWindow().equals(window))
                {
                    return;
                }
                
                viewer.setSelection(
                        new TreeSelection(
                            new TreePath(
                                new Object[] { new TreeObject(selectedFile.getName(), selectedFile) })));
            }
            
            @Override
            public void projectChanged(IWorkbenchWindow window, TapestryProject newTapestryProject)
            {
                IContentProvider provider = viewer.getContentProvider();
                
                if (provider instanceof TapestryContextContentProvider)
                {
                    TapestryContextContentProvider contextProvider = (TapestryContextContentProvider) provider;
                    
                    TapestryContext context = contextProvider.getContext();
                    
                    if (context != null)
                    {
                        TapestryContext.deleteMarkers(context.getProject());
                        
                        context.validate();
                        
                        setContentProvider(new TapestryContextContentProvider(window, context));
                    }
                }
            }
        };
        
        Activator.getDefault().addTapestryContextListener(getViewSite().getWorkbenchWindow(), tapestryContextListener);
        Activator.getDefault().addTapestryProjectListener(getViewSite().getWorkbenchWindow(), tapestryContextListener);
    }

    private void setContentProvider(TapestryContextContentProvider contentProvider)
    {
        viewer.setContentProvider(contentProvider);
        viewer.expandToLevel(2);
    }
    
    public TapestryContext getTapestryContext()
    {
        return ((TapestryContextContentProvider) viewer.getContentProvider()).getContext();
    }
    
    @Override
    public void dispose()
    {
        Activator.getDefault().removeTapestryProjectListener(getViewSite().getWorkbenchWindow(), tapestryContextListener);
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