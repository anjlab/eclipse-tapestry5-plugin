package com.anjlab.eclipse.tapestry5.views.context;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.part.ViewPart;

import com.anjlab.eclipse.tapestry5.Activator;
import com.anjlab.eclipse.tapestry5.EclipseUtils;
import com.anjlab.eclipse.tapestry5.TapestryContext;
import com.anjlab.eclipse.tapestry5.TapestryUtils;
import com.anjlab.eclipse.tapestry5.views.NameSorter;
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
    private ISelectionListener selectionListener;
    private IResourceChangeListener postBuildListener;
    private IResourceChangeListener postChangeListener;

    /**
     * This is a callback that will allow us to create the viewer and initialize it.
     */
    public void createPartControl(Composite parent)
    {
        viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        viewer.setContentProvider(new TapestryContextContentProvider((IFile) null));
        viewer.setLabelProvider(new ViewLabelProvider());
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
                    IFile file = (IFile) ((TreeObject) obj).getData();
                    
                    EclipseUtils.openFile(getViewSite().getWorkbenchWindow(), file);
                }
            }
        });
        
        postChangeListener = new IResourceChangeListener()
        {
            @Override
            public void resourceChanged(IResourceChangeEvent event)
            {
                if (getContentProvider() == null)
                {
                    return;
                }
                
                TapestryContext context = null;
                
                for (IFile affectedFile : EclipseUtils.getAllAffectedResources(
                                            event.getDelta(), IFile.class))
                {
                    if (getContentProvider().getContext().contains(affectedFile))
                    {
                        if (!affectedFile.exists())
                        {
                            getContentProvider().getContext().remove(affectedFile);
                            
                            //  Context changed
                            context = getContentProvider().getContext();
                        }
                        else
                        {
                            context = TapestryUtils.createTapestryContext(affectedFile);
                            
                            if (!context.isEmpty())
                            {
                                break;
                            }
                        }
                    }
                }
                
                if (context != null)
                {
                    //  Some files changed in context
                    updateContentProvider(new TapestryContextContentProvider(context));
                }
            }
        };
        
        ResourcesPlugin.getWorkspace().addResourceChangeListener(postChangeListener, IResourceChangeEvent.POST_CHANGE);
        
        postBuildListener = new IResourceChangeListener()
        {
            @Override
            public void resourceChanged(IResourceChangeEvent event)
            {
                if (getContentProvider() != null)
                {
                    List<IProject> projects = EclipseUtils.getAllAffectedResources(event.getDelta(), IProject.class);
                    
                    for (IProject project : projects)
                    {
                        TapestryContext.deleteMarkers(project);
                    }
                    
                    getContentProvider().getContext().validate();
                }
            }
        };
        ResourcesPlugin.getWorkspace().addResourceChangeListener(postBuildListener, IResourceChangeEvent.POST_BUILD);
        
        selectionListener = new ISelectionListener()
        {
            private IFile activeFile;
            
            @Override
            public void selectionChanged(IWorkbenchPart part, ISelection selection)
            {
                IWorkbenchWindow window = part.getSite().getWorkbenchWindow();
                
                IWorkbenchPage activePage = window.getActivePage();
                
                IFile file = EclipseUtils.getFileFromPage(activePage);
                
                if (file != null)
                {
                    if (activeFile != file)
                    {
                        activeFile = file;
                        
                        updateContext(file);
                        
                        viewer.setSelection(
                                new TreeSelection(
                                    new TreePath(
                                        new Object[] { new TreeObject(file.getName(), file) })));
                    }
                }
                else
                {
                    // Keep outline view presenting information about previous activeFile
                }
            }
        };
        
        getSite().getPage().addSelectionListener(selectionListener);
    }
    
    private void updateContext(IFile fromFile)
    {
        TapestryContextContentProvider provider = new TapestryContextContentProvider(fromFile);
        
        if (!provider.hasElements() && getContentProvider() != null
                && getContentProvider().getContext().contains(fromFile))
        {
            //  In case if we clicked on @Import'ed asset (JS or CSS) file, then we can't obtain tapestry context for it
            //  because we don't have any naming conventions for these files, and also these files may be
            //  referenced from multiple components/pages, so they may belong to multiple contexts.
            //  Anyway if we can't find any elements for the context -- we simply show the previous one.
            
            provider = getContentProvider();
        }
        
        updateContentProvider(provider);
    }

    private void updateContentProvider(TapestryContextContentProvider provider)
    {
        setContentProvider(provider);
        
        getViewSite().getWorkbenchWindow().getShell().getDisplay().syncExec(new Runnable()
        {
            @Override
            public void run()
            {
                viewer.setContentProvider(getContentProvider());
            }
        });
    }
    
    private TapestryContextContentProvider getContentProvider()
    {
        return Activator.getDefault().getContextContentProvider();
    }
    
    private void setContentProvider(TapestryContextContentProvider contentProvider)
    {
        Activator.getDefault().setContextContentProvider(contentProvider);
    }
    
    @Override
    public void dispose()
    {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(postBuildListener);
        
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(postChangeListener);
        
        getSite().getPage().removeSelectionListener(selectionListener);
        
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