package com.anjlab.eclipse.tapestry5.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com.anjlab.eclipse.tapestry5.EclipseUtils;
import com.anjlab.eclipse.tapestry5.TapestryUtils;

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
        
        try
        {
            IFile file = EclipseUtils.getFileForTapestryContext(window);
            
            if (file != null)
            {
                if (!TapestryUtils.isTemplateFile(file) && !TapestryUtils.isJavaFile(file))
                {
                    throw new ExecutionException("This feature only works for *.java and *.tml files");
                }
                
                IFile complementFile = TapestryUtils.findComplementFile(file);
                
                if (complementFile == null)
                {
                    throw new ExecutionException("Complement file not found for "
                                + file.getProjectRelativePath().toPortableString());
                }
                
                EclipseUtils.openFile(window, complementFile);
            }
        }
        catch (ExecutionException e)
        {
            MessageDialog.openError(
                    window.getShell(),
                    "Eclipse Integration for Tapestry5",
                    e.getLocalizedMessage());
        }
        return null;
    }

}
