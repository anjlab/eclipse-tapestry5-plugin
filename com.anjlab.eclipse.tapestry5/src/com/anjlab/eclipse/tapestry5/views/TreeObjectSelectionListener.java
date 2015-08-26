package com.anjlab.eclipse.tapestry5.views;

import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;

import com.anjlab.eclipse.tapestry5.DeclarationReference;
import com.anjlab.eclipse.tapestry5.DeclarationReference.ASTNodeReference;
import com.anjlab.eclipse.tapestry5.EclipseUtils;
import com.anjlab.eclipse.tapestry5.JavaScriptStack;
import com.anjlab.eclipse.tapestry5.LibraryMapping;
import com.anjlab.eclipse.tapestry5.SetEditorCaretPositionOffsetLength;
import com.anjlab.eclipse.tapestry5.TapestryModule;
import com.anjlab.eclipse.tapestry5.TapestryModuleReference;
import com.anjlab.eclipse.tapestry5.TapestryService;
import com.anjlab.eclipse.tapestry5.TapestryService.ServiceInstrumenter;
import com.anjlab.eclipse.tapestry5.TapestrySymbol;

@SuppressWarnings("restriction")
public class TreeObjectSelectionListener implements ISelectionChangedListener
{
    private final IWorkbenchWindow window;
    private final ISelectionProvider selectionProvider;
    private final TreeViewer viewer;
    
    public TreeObjectSelectionListener(
            IWorkbenchWindow window, ISelectionProvider selectionProvider, TreeViewer viewer)
    {
        this.window = window;
        this.selectionProvider = selectionProvider;
        this.viewer = viewer;
    }

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
            
            IProject project = ((IProjectProvider) viewer.getContentProvider()).getProject();
            
            if (project == null)
            {
                return;
            }
            
            IType element = EclipseUtils.findTypeDeclaration(
                    project,
                    service.getDefinition().getIntfClass());
            
            if (element != null)
            {
                selectionProvider.setSelection(new StructuredSelection(element));
                
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
                selectionProvider.setSelection(new StructuredSelection(element));
                
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
                selectionProvider.setSelection(new StructuredSelection(element));
                
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
                selectionProvider.setSelection(new StructuredSelection(element));
                
                updateSelectionInActiveEditor(stack.getReference());
                
                return;
            }
        }
        
        if (selectedObject instanceof TapestrySymbol)
        {
            TapestrySymbol symbol = (TapestrySymbol) selectedObject;
            
            DeclarationReference element = symbol.getReference();
            
            if (element != null)
            {
                selectionProvider.setSelection(new StructuredSelection(element));
                
                updateSelectionInActiveEditor(element);
                
                return;
            }
        }
        
        if (selectedObject instanceof TapestryModule)
        {
            TapestryModule module = (TapestryModule) selectedObject;
            
            IJavaElement element = module.getModuleClass();
            
            if (element != null)
            {
                selectionProvider.setSelection(new StructuredSelection(element));
                
                for (TapestryModuleReference reference : module.references())
                {
                    updateSelectionInActiveEditor(reference.getReference());
                }
                
                return;
            }
        }
        
        selectionProvider.setSelection(new ISelection()
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
        IWorkbenchPage activePage = window.getActivePage();
        
        if (activePage != null)
        {
            IEditorPart activeEditor = activePage.getActiveEditor();
            
            if (activeEditor != null)
            {
                IEditorInput input = activeEditor.getEditorInput();
                
                if (input instanceof IClassFileEditorInput)
                {
                    IClassFile classFile = ((IClassFileEditorInput) input).getClassFile();
                    
                    if (ObjectUtils.equals(classFile.getType(), EclipseUtils.findParentType(reference.getElement())))
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
}