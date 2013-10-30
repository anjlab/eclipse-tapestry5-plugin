package com.anjlab.eclipse.tapestry5.views;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.anjlab.eclipse.tapestry5.Activator;
import com.anjlab.eclipse.tapestry5.EclipseUtils;
import com.anjlab.eclipse.tapestry5.TapestryContext;
import com.anjlab.eclipse.tapestry5.TapestryUtils;
import com.anjlab.eclipse.tapestry5.actions.NewFileWizardAction;
import com.anjlab.eclipse.tapestry5.actions.NewJavaClassWizardAction;

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
    private static final String DEFAULT_JAVA_SOURCE_FOLDER = "src/main/java";

    /**
     * The ID of the view as specified by the extension.
     */
    public static final String ID = "com.anjlab.eclipse.tapestry5.views.TapestryOutlineView";

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
        viewer.setContentProvider(new ViewContentProvider((IFile) null));
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
                
                List<IFile> changedFiles = EclipseUtils.getAllAffectedResources(
                        event.getDelta(), IFile.class, IResourceDelta.CHANGED);
                
                for (IFile changedFile : changedFiles)
                {
                    if (!TapestryUtils.isJavaFile(changedFile))
                    {
                        continue;
                    }
                    
                    if (getContentProvider().getContext().contains(changedFile))
                    {
                        //  Some @Imports may have changed
                        updateContext(changedFile);
                        
                        break;
                    }
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
        
        contributeToActionBars();
    }
    
    private void contributeToActionBars()
    {
        IActionBars bars = getViewSite().getActionBars();
        IToolBarManager localToolbar = bars.getToolBarManager();
        
        IAction createAction = new Action("Create...")
        {
            @Override
            public void run()
            {
                Menu menu = new Menu(viewer.getControl());
                
                TapestryContext tapestryContext = Activator.getDefault().getTapestryContext();
                
                if (tapestryContext != null && !tapestryContext.isEmpty())
                {
                    newJavaClassMenuItem(menu, tapestryContext.getProject(),
                            "Create " + tapestryContext.getName() + ".java...",
                            tapestryContext.getPackageName(),
                            tapestryContext.getName())
                        .setEnabled(tapestryContext.getJavaFile() == null);
                    
                    String tmlFileName = tapestryContext.getName() + ".tml";
                    newTextFileMenuItem(menu, tapestryContext, "Create " + tmlFileName + "...", tmlFileName)
                        .setEnabled(!tapestryContext.contains(tmlFileName));
                    
                    String jsFileName = tapestryContext.getName() + ".js";
                    newTextFileMenuItem(menu, tapestryContext, "Create " + jsFileName + "...", jsFileName)
                        .setEnabled(!tapestryContext.contains(jsFileName));
                    
                    String cssFileName = tapestryContext.getName() + ".css";
                    newTextFileMenuItem(menu, tapestryContext, "Create " + cssFileName + "...", cssFileName)
                        .setEnabled(!tapestryContext.contains(cssFileName));
                    
                    newTextFileMenuItem(menu, tapestryContext, "Create other...", "");
                }
                
                IProject contextProject = tapestryContext != null ? tapestryContext.getProject() : null;
                
                if (contextProject == null)
                {
                    IResource contextResource = TapestryUtils.getResourceForTapestryContext(getSite().getWorkbenchWindow());
                    
                    contextProject = contextResource != null ? contextResource.getProject() : null;
                }
                
                if (contextProject != null && TapestryUtils.getAppPackage(contextProject) != null)
                {
                    if (menu.getItemCount() > 0)
                    {
                        new MenuItem(menu, SWT.SEPARATOR);
                    }
                    
                    newJavaClassMenuItem(menu, contextProject,
                            "New Page Class...",
                            TapestryUtils.getPagesPackage(contextProject),
                            null);
                    
                    newJavaClassMenuItem(menu, contextProject,
                            "New Component Class...",
                            TapestryUtils.getComponentsPackage(contextProject),
                            null);
                    
                    newJavaClassMenuItem(menu, contextProject,
                            "New Mixin Class...",
                            TapestryUtils.getMixinsPackage(contextProject),
                            null);
                }
                
                if (menu.getItemCount() > 0)
                {
                    menu.setVisible(true);
                }
                else
                {
                    EclipseUtils.openInformation(getSite().getWorkbenchWindow(),
                            "Try selecting your Tapestry5 project in the Package/Project Explorer.");
                }
            }

            private IAction newTextFileMenuItem(Menu menu, TapestryContext tapestryContext, String title, String fileName)
            {
                NewFileWizardAction newFile = new NewFileWizardAction(tapestryContext.getProject(),
                        tapestryContext,
                        getSite().getShell(),
                        getSite().getWorkbenchWindow());
                
                newFile.setFileName(fileName);
                newFile.setFolder("src/main/resources/" + tapestryContext.getPackageName().replaceAll("\\.", "/"));
                
                newFile.setText(title);
                newFile.setImageDescriptor(PlatformUI.getWorkbench().getEditorRegistry()
                        .getImageDescriptor(fileName));
                
                return addActionToMenu(menu, newFile);
            }

            private IAction newJavaClassMenuItem(Menu menu, IProject project, String title, String packageName, String typeName)
            {
                NewJavaClassWizardAction newJavaClass = new NewJavaClassWizardAction(project);
                newJavaClass.setText(title);
                newJavaClass.setSourceFolder(DEFAULT_JAVA_SOURCE_FOLDER);
                newJavaClass.setPackageName(packageName);
                newJavaClass.setTypeName(typeName);
                
                return addActionToMenu(menu, newJavaClass);
            }
            
            private IAction addActionToMenu(Menu menu, IAction action)
            {
                new ActionContributionItem(action).fill(menu, -1);
                return action;
            }
        };
        
        createAction.setImageDescriptor(
                PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_ADD));
        
        localToolbar.add(createAction);
    }
    
    private void updateContext(IFile file)
    {
        ViewContentProvider provider = new ViewContentProvider(file);
        
        if (!provider.hasElements() && getContentProvider() != null
                && getContentProvider().getContext().contains(file))
        {
            //  In case if we clicked on @Import'ed asset (JS or CSS) file, then we can't obtain tapestry context for it
            //  because we don't have any naming conventions for these files, and also these files may be
            //  referenced from multiple components/pages, so they may belong to multiple contexts.
            //  Anyway if we can't find any elements for the context -- we simply show the previous one.
            
            provider = getContentProvider();
        }
        
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
    
    private ViewContentProvider getContentProvider()
    {
        return Activator.getDefault().getContentProvider();
    }
    
    private void setContentProvider(ViewContentProvider contentProvider)
    {
        Activator.getDefault().setContentProvider(contentProvider);
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