package com.anjlab.eclipse.tapestry5.actions;

import java.io.InputStream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

import com.anjlab.eclipse.tapestry5.EclipseUtils;

public abstract class OpenFileInTargetFolderAction extends Action
{
    private final IWorkbenchWindow window;
    private final IFolder targetDir;
    private final String fileName;
    
    public OpenFileInTargetFolderAction(IWorkbenchWindow window, IFolder targetDir, String fileName)
    {
        super();
        this.window = window;
        this.targetDir = targetDir;
        this.fileName = fileName;
    }

    protected abstract InputStream getInitialContents();

    private void preCreateFolder(IFolder folder) throws CoreException
    {
        if (!folder.exists())
        {
            IContainer parent = folder.getParent();

            if (parent instanceof IFolder)
            {
                preCreateFolder((IFolder) parent);
            }

            folder.create(false, true, null);
        }
    }

    private void createFile(final IFolder targetDir, final String fileName)
    {
        try
        {
            preCreateFolder(targetDir);
        }
        catch (CoreException e)
        {
            throw new RuntimeException("Unable to pre-create folder for new file", e);
        }

        final WizardNewFileCreationPage fileCreationPage = new WizardNewFileCreationPage("", new TreeSelection())
        {
            @Override
            protected InputStream getInitialContents()
            {
                return OpenFileInTargetFolderAction.this.getInitialContents();
            }
        };

        WizardDialog dialog = new WizardDialog(window.getShell(), new Wizard()
        {
            @Override
            public boolean performFinish()
            {
                fileCreationPage.createNewFile();

                return true;
            }

            @Override
            public void addPages()
            {
                super.addPages();

                fileCreationPage.setFileName(fileName);

                fileCreationPage.setContainerFullPath(targetDir.getFullPath());

                addPage(fileCreationPage);
            }
        });

        dialog.create();
        try
        {
            dialog.getCurrentPage().getWizard().performFinish();
        }
        finally
        {
            dialog.close();
        }
    }

    @Override
    public void run()
    {
        IFile file = targetDir.getFile(fileName);

        if (!file.exists())
        {
            createFile(targetDir, fileName);
        }

        EclipseUtils.openFile(window, file);
    }

}