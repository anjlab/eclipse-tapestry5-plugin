package com.anjlab.eclipse.tapestry5.views.project;

import org.apache.commons.lang.ObjectUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.part.ViewPart;

import com.anjlab.eclipse.tapestry5.Activator;
import com.anjlab.eclipse.tapestry5.DeclarationReference;
import com.anjlab.eclipse.tapestry5.DeclarationReference.ASTNodeReference;
import com.anjlab.eclipse.tapestry5.EclipseUtils;
import com.anjlab.eclipse.tapestry5.JavaScriptStack;
import com.anjlab.eclipse.tapestry5.LibraryMapping;
import com.anjlab.eclipse.tapestry5.SetEditorCaretPositionOffsetLength;
import com.anjlab.eclipse.tapestry5.TapestryContext;
import com.anjlab.eclipse.tapestry5.TapestryFile;
import com.anjlab.eclipse.tapestry5.TapestryModule;
import com.anjlab.eclipse.tapestry5.TapestryProject;
import com.anjlab.eclipse.tapestry5.TapestryService;
import com.anjlab.eclipse.tapestry5.TapestryService.ServiceInstrumenter;
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

@SuppressWarnings("restriction")
public class TapestryProjectOutlineView extends ViewPart
{
    private final class SimpleSelectionProvider implements
            ISelectionProvider
    {
        private final ListenerList listeners = new ListenerList();
        private ISelection selection;
        
        @Override
        public void setSelection(ISelection selection)
        {
            this.selection = selection;
            
            for (Object l : listeners.getListeners())
            {
                ((ISelectionChangedListener) l).selectionChanged(new SelectionChangedEvent(this, selection));
            }
        }

        @Override
        public void removeSelectionChangedListener(ISelectionChangedListener listener)
        {
            listeners.remove(listener);
        }

        @Override
        public ISelection getSelection()
        {
            return selection;
        }

        @Override
        public void addSelectionChangedListener(ISelectionChangedListener listener)
        {
            listeners.add(listener);
        }
    }

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
        getSite().setSelectionProvider(new SimpleSelectionProvider());

        viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        viewer.setContentProvider(new TapestryProjectOutlineContentProvider(Activator.getDefault().getTapestryProject(getSite().getWorkbenchWindow())));
        viewer.setLabelProvider(new TapestryDecoratingLabelProvider(new ViewLabelProvider()));
        viewer.setSorter(new NameSorter());
        viewer.setInput(getViewSite());
        viewer.addSelectionChangedListener(new ISelectionChangedListener()
        {
            @Override
            public void selectionChanged(SelectionChangedEvent event)
            {
                Object selectedObject = getSelectedTapestryObject(event);
                
                //  Expose selected object via selection provider for other Eclipse components,
                //  for example, for JavaDoc view.
                //  
                //  JavaDoc view understands IJavaElement, this is what we can publish.
                //  TODO We also sometimes may have ASTNode instead of IJavaElement which it doesn't understand,
                //  TBD what to do with them: these may be LibraryMappings and services bound via binder.bind().
                
                if (selectedObject instanceof TapestryService)
                {
                    TapestryService service = (TapestryService) selectedObject;
                    
                    TapestryProjectOutlineContentProvider contentProvider =
                            (TapestryProjectOutlineContentProvider) viewer.getContentProvider();
                    
                    IType element = EclipseUtils.findTypeDeclaration(
                            contentProvider.getProject().getProject(),
                            service.getDefinition().getIntfClass());
                    
                    if (element != null)
                    {
                        getSite().getSelectionProvider().setSelection(new StructuredSelection(element));
                        
                        updateSelectionInActiveEditor(service.getReference());
                        
                        return;
                    }
                }
                
                if (selectedObject instanceof ServiceInstrumenter)
                {
                    ServiceInstrumenter instrumenter = (ServiceInstrumenter) selectedObject;
                    
                    IJavaElement element = instrumenter.getReference().getElement();
                    
                    if (element != null)
                    {
                        getSite().getSelectionProvider().setSelection(new StructuredSelection(element));
                        
                        updateSelectionInActiveEditor(instrumenter.getReference());
                        
                        return;
                    }
                }
                
                if (selectedObject instanceof LibraryMapping)
                {
                    LibraryMapping mapping = (LibraryMapping) selectedObject;
                    
                    DeclarationReference element = mapping.getReference();
                    
                    if (element != null)
                    {
                        getSite().getSelectionProvider().setSelection(new StructuredSelection(element));
                        
                        updateSelectionInActiveEditor(element);
                        
                        return;
                    }
                }
                
                if (selectedObject instanceof JavaScriptStack)
                {
                    JavaScriptStack stack = (JavaScriptStack) selectedObject;
                    
                    IJavaElement element = stack.getType();
                    
                    if (element != null)
                    {
                        getSite().getSelectionProvider().setSelection(new StructuredSelection(element));
                        
                        updateSelectionInActiveEditor(stack.getReference());
                        
                        return;
                    }
                }
                
                if (selectedObject instanceof TapestryModule)
                {
                    TapestryModule module = (TapestryModule) selectedObject;
                    
                    IJavaElement element = module.getModuleClass();
                    
                    if (element != null)
                    {
                        getSite().getSelectionProvider().setSelection(new StructuredSelection(element));
                        
                        updateSelectionInActiveEditor(module.getReference().getReference());
                        
                        return;
                    }
                }
                
                getSite().getSelectionProvider().setSelection(new ISelection()
                {
                    @Override
                    public boolean isEmpty()
                    {
                        return true;
                    }
                });
            }

            private void updateSelectionInActiveEditor(DeclarationReference reference)
            {
                if (reference instanceof ASTNodeReference)
                {
                    ASTNodeReference astReference = (ASTNodeReference) reference;
                    
                    updateSelectionInActiveEditor(
                            astReference,
                            astReference.getNode().getStartPosition(),
                            astReference.getNode().getLength());
                }
                else
                {
                    if (reference.getElement() instanceof ISourceReference)
                    {
                        ISourceReference sourceReference = (ISourceReference) reference.getElement();
                        
                        if (sourceReference.exists())
                        {
                            try
                            {
                                updateSelectionInActiveEditor(
                                        reference,
                                        sourceReference.getSourceRange().getOffset(),
                                        sourceReference.getSourceRange().getLength());
                            }
                            catch (JavaModelException e)
                            {
                                //  Ignore
                            }
                        }
                    }
                }
            }

            private void updateSelectionInActiveEditor(DeclarationReference reference, int offset, int length)
            {
                IWorkbenchPage activePage = getSite().getWorkbenchWindow().getActivePage();
                
                if (activePage != null)
                {
                    IEditorPart activeEditor = activePage.getActiveEditor();
                    
                    if (activeEditor != null)
                    {
                        IEditorInput input = activeEditor.getEditorInput();
                        
                        if (input instanceof IClassFileEditorInput)
                        {
                            IClassFile classFile = ((IClassFileEditorInput) input).getClassFile();
                            
                            if (ObjectUtils.equals(classFile.getType(), findParentType(reference.getElement())))
                            {
                                new SetEditorCaretPositionOffsetLength(offset, length).editorOpened(activeEditor);
                            }
                        }
                        else if (input instanceof IFileEditorInput)
                        {
                            IFile file = ((IFileEditorInput) input).getFile();
                            
                            if (ObjectUtils.equals(file, reference.getElement().getResource()))
                            {
                                new SetEditorCaretPositionOffsetLength(offset, length).editorOpened(activeEditor);
                            }
                        }
                    }
                }
            }

            private Object findParentType(IJavaElement element)
            {
                while (!(element instanceof IType) && element != null)
                {
                    element = element.getParent();
                }
                return element;
            }

            private Object getSelectedTapestryObject(SelectionChangedEvent event)
            {
                ISelection selection = event.getSelection();
                
                if (selection instanceof TreeSelection)
                {
                    TreeSelection treeSelection = (TreeSelection) selection;
                    
                    if (treeSelection.size() == 1)
                    {
                        if (treeSelection.getFirstElement() instanceof TreeObject)
                        {
                            TreeObject treeObject = (TreeObject) treeSelection.getFirstElement();
                            
                            return treeObject.getData();
                        }
                    }
                }
                
                return null;
            }
        });
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
                    else if (data instanceof ServiceInstrumenter)
                    {
                        ((ServiceInstrumenter) data).getReference().openInEditor();
                    }
                }
            }
        });
        
        tapestryContextListener = new ITapestryContextListener()
        {
            private boolean expanded;
            
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
                        TapestryProjectOutlineContentProvider contentProvider = new TapestryProjectOutlineContentProvider(newTapestryProject);
                        
                        viewer.setContentProvider(contentProvider);
                        
                        if (!expanded)
                        {
                            //  Expand modules only once
                            viewer.setExpandedElements(new Object[] { contentProvider.getModulesRoot() });
                            expanded = true;
                        }
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