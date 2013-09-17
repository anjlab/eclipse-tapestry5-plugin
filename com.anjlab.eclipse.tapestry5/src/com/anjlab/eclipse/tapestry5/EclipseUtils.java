package com.anjlab.eclipse.tapestry5;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;
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
                    IFile localFile = file;
                    
                    if (localFile instanceof AssetPath)
                    {
                        AssetPath assetPath = (AssetPath) localFile;
                        try
                        {
                            localFile = assetPath.resolveFile(false);
                        }
                        catch (AssetException e)
                        {
                            EclipseUtils.openError(window,
                                    "Unable to resolve asset '" + assetPath.getAssetPath() + "': "
                                            + e.getLocalizedMessage());
                            
                            return;
                        }
                    }
                    
                    IDE.openEditor(window.getActivePage(), localFile, true);
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

    public static boolean isSourceFolder(IContainer container) throws JavaModelException
    {
        return EclipseUtils.isSourceFolder((IJavaElement) container.getAdapter(IJavaElement.class));
    }

    public static boolean isSourceFolder(IJavaElement javaElement) throws JavaModelException
    {
        return javaElement != null
            && (javaElement instanceof IPackageFragmentRoot)
            && (((IPackageFragmentRoot) javaElement).getKind() == IPackageFragmentRoot.K_SOURCE);
    }

    public static IFile findFileCaseInsensitive(IContainer container, String componentPath)
    {
        String[] parts = (componentPath.startsWith("/") ? componentPath.substring(1) : componentPath).split("/");
        for (int i = 0; i < parts.length; i++)
        {
            String part = parts[i];
            try
            {
                boolean found = false;
                
                for (IResource member : container.members())
                {
                    if (part.equalsIgnoreCase(member.getName()))
                    {
                        if (member instanceof IFile && i == parts.length - 1)
                        {
                            return (IFile) member;
                        }
                        
                        if (!(member instanceof IContainer))
                        {
                            return null;
                        }
                        
                        container = (IContainer) member;
                        
                        found = true;
                        
                        break;
                    }
                }
                
                if (!found)
                {
                    return null;
                }
            }
            catch (CoreException e)
            {
            }
        }
        return null;
    }

    public static IFile getFileForTapestryContext(IWorkbenchWindow window)
    {
        IFile file = null;
        
        try
        {
            file = getFileFromSelection(window.getSelectionService().getSelection());
        }
        catch (JavaModelException e)
        {
            //  Ignore
        }
        
        if (file == null)
        {
            file = getFileFromPage(window.getActivePage());
        }
        
        return file;
    }

}
