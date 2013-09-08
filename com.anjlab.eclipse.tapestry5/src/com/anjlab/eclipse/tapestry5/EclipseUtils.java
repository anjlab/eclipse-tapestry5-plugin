package com.anjlab.eclipse.tapestry5;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.ide.IDE;

public class EclipseUtils
{

    public static IFile getFileFromPage(IWorkbenchPage page)
    {
        if (page == null)
        {
            return null;
        }
        
        IEditorPart activeEditor = page.getActiveEditor();
        
        if (activeEditor == null)
        {
            return null;
        }
        
        IEditorInput editorInput = activeEditor.getEditorInput();
        
        if (editorInput instanceof IFileEditorInput)
        {
            IFileEditorInput fileEditorInput = (IFileEditorInput) editorInput;
            
            return fileEditorInput.getFile();
        }
        
        return null;
    }

    public static IFile getFileFromSelection(ISelection selection) throws JavaModelException
    {
        return selection instanceof IStructuredSelection
             ? getFileFromSelectionElement(((IStructuredSelection) selection).getFirstElement())
             : null;
    }

    private static IFile getFileFromSelectionElement(Object firstElement) throws JavaModelException
    {
        if (firstElement == null)
        {
            return null;
        }
        
        if (firstElement instanceof ICompilationUnit)
        {
            ICompilationUnit compilationUnit = ((ICompilationUnit) firstElement);
            
            return (IFile) compilationUnit.getCorrespondingResource().getAdapter(IFile.class);
        }
        
        if (firstElement instanceof ITreeSelection)
        {
            ITreeSelection treeSelection = (ITreeSelection) firstElement;
            
            return getFileFromSelectionElement(treeSelection.getFirstElement());
        }
        
        IFile file = (IFile) Platform.getAdapterManager().getAdapter(firstElement, IFile.class);
        
        if (file == null)
        {
            if (firstElement instanceof IAdaptable)
            {
                file = (IFile) ((IAdaptable) firstElement).getAdapter(IFile.class);
            }
        }
        
        return file;
    }

    public static void openFile(final IWorkbenchWindow window, final IFile file)
    {
        window.getShell().getDisplay().asyncExec(new Runnable()
        {
            public void run()
            {
                try
                {
                    IDE.openEditor(window.getActivePage(), file, true);
                }
                catch (Exception e)
                {
                    Activator.getDefault().logError("Unable to open editor", e);
                    
                    openError(window, "Unable to open editor: " + e.getLocalizedMessage());
                }
            }
        });
    }

    public static void openError(final IWorkbenchWindow window, String message)
    {
        MessageDialog.openError(
                window.getShell(),
                "Eclipse Integration for Tapestry5",
                message);
    }

    public static <T> List<T> getAllAffectedResources(IResourceDelta delta, Class<T> clazz)
    {
        return getAllAffectedResources(delta, clazz, 0xFFFFFFFF);
    }
    
    @SuppressWarnings("unchecked")
    public static <T> List<T> getAllAffectedResources(IResourceDelta delta, Class<T> clazz, int deltaKind)
    {
        List<T> files = new ArrayList<T>();
        
        for (IResourceDelta child : delta.getAffectedChildren())
        {
            IResource resource = child.getResource();
            
            if (resource != null && clazz.isAssignableFrom(resource.getClass()))
            {
                if ((child.getKind() & deltaKind) != 0)
                {
                    files.add((T) resource);
                }
            }
            else
            {
                files.addAll(getAllAffectedResources(child, clazz, deltaKind));
            }
        }
        return files;
    }

}
