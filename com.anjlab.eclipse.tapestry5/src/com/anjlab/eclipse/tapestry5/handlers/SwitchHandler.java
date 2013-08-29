package com.anjlab.eclipse.tapestry5.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
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
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE;

import com.anjlab.eclipse.tapestry5.Activator;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class SwitchHandler extends AbstractHandler
{
    /**
     * The constructor.
     */
    public SwitchHandler()
    {
    }

    /**
     * the command has been executed, so extract extract the needed information
     * from the application context.
     */
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
        
        ISelection selection = window.getSelectionService().getSelection();
        
        if (selection instanceof IStructuredSelection)
        {
            Object firstElement = ((IStructuredSelection) selection).getFirstElement();
            
            if (firstElement != null)
            {
                try
                {
                    IFile file = getFileFromSelection(selection);
                    
                    if (file != null)
                    {
                        openComplementFile(window, file);
                    }
                }
                catch (JavaModelException e)
                {
                    throw new ExecutionException("Unable to get file from selection", e);
                }
            }
        }
        
        IWorkbenchPage activePage = window.getActivePage();
        
        if (activePage == null)
        {
            return null;
        }
        
        IEditorPart activeEditor = activePage.getActiveEditor();
        
        if (activeEditor == null)
        {
            return null;
        }
        
        IEditorInput editorInput = activeEditor.getEditorInput();
        
        if (editorInput instanceof IFileEditorInput)
        {
            IFileEditorInput fileEditorInput = (IFileEditorInput) editorInput;
            
            openComplementFile(window, fileEditorInput.getFile());
        }
        
        return null;
    }

    private IFile getFileFromSelection(Object firstElement) throws JavaModelException
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
            
            return getFileFromSelection(treeSelection.getFirstElement());
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

    private void openComplementFile(final IWorkbenchWindow window, final IFile file)
    {
        if (file == null)
        {
            return;
        }
        
        window.getShell().getDisplay().asyncExec(new Runnable()
        {
            public void run()
            {
                IFile complementFile = null;
                
                try
                {
                    if (getComplementFileExtension(file.getFileExtension()) == null)
                    {
                        throw new PartInitException("This feature only works for *.java and *.tml files");
                    }
                    
                    complementFile = findComplementFile(file);
                    
                    if (complementFile == null)
                    {
                        throw new PartInitException("Complement file not found for "
                                    + file.getProjectRelativePath().toPortableString());
                    }
                    
                    IDE.openEditor(window.getActivePage(), complementFile, true);
                }
                catch (Exception e)
                {
                    Activator.getDefault().logError("Unable to open editor", e);
                    
                    MessageDialog.openError(
                            window.getShell(),
                            "Eclipse Integration for Tapestry5",
                            "Unable to open editor: " + e.getLocalizedMessage());
                }
            }
            
            private IFile findComplementFile(IFile file) throws CoreException
            {
                //  Try searching in the same folder first
                
                IResource resource = file.getParent().findMember(getComplementFileName(file.getName(), file.getFileExtension()));
                
                if (resource instanceof IFile)
                {
                    return (IFile) resource;
                }
                
                //  Look in the source folders
                
                if (!file.getProject().hasNature(JavaCore.NATURE_ID))
                {
                    Activator.getDefault().logError("Project '" + file.getProject() + "' does not a Java nature");
                    
                    return null;
                }
                
                IJavaProject project = JavaCore.create(file.getProject());
                
                IContainer adaptedProject = (IContainer) project.getCorrespondingResource().getAdapter(IContainer.class);
                
                IResource adaptedFile = adaptedProject.findMember(file.getProjectRelativePath());
                
                IContainer container = adaptedFile.getParent();
                
                while (container != null && !isSourceFolder(container))
                {
                    container = container.getParent();
                }
                
                if (container == null)
                {
                    Activator.getDefault().logWarning("Unable to find source folder for file: " + file.getFullPath());
                    
                    return null;
                }
                
                //  Get the file name relative to source folder
                String fileName = file.getProjectRelativePath().toPortableString().substring(
                        container.getProjectRelativePath().toPortableString().length());
                
                String complementFileName = getComplementFileName(fileName, file.getFileExtension());
                
                for (IPackageFragmentRoot root : project.getAllPackageFragmentRoots())
                {
                    if (!isSourceFolder(root))
                    {
                        continue;
                    }
                    
                    IContainer resourceContainer = (IContainer) root.getCorrespondingResource().getAdapter(IContainer.class);
                    
                    resource = resourceContainer.findMember(complementFileName);
                    
                    if (resource instanceof IFile)
                    {
                        return (IFile) resource;
                    }
                }
                
                return null;
            }

            private boolean isSourceFolder(IContainer container) throws JavaModelException
            {
                return isSourceFolder((IJavaElement) container.getAdapter(IJavaElement.class));
            }

            private boolean isSourceFolder(IJavaElement javaElement) throws JavaModelException
            {
                return javaElement != null && (javaElement instanceof IPackageFragmentRoot);
            }

            private String getComplementFileName(String fileName, String originalExtension)
            {
                return fileName.substring(0, fileName.lastIndexOf(originalExtension))
                        + getComplementFileExtension(originalExtension);
            }

            private String getComplementFileExtension(String extension)
            {
                return "tml".equals(extension) ? "java" : "java".equals(extension) ? "tml" : null;
            }
        });
    }
}
