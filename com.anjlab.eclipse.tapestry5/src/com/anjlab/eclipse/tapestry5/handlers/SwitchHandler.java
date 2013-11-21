package com.anjlab.eclipse.tapestry5.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com.anjlab.eclipse.tapestry5.EclipseUtils;
import com.anjlab.eclipse.tapestry5.TapestryContext;
import com.anjlab.eclipse.tapestry5.TapestryFile;
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
            //  Create new context from window (this includes search in Package Explorer)
            //  Activator#getTapestryContext() only holds context for ActivePage,
            //  so we can't use it here
            TapestryContext tapestryContext = TapestryUtils.createTapestryContext(window);
            
            if (!tapestryContext.isEmpty())
            {
                TapestryFile currentFile = tapestryContext.getInitialFile();
                
                TapestryFile switchTarget = null;
                
                if (currentFile.isJavaFile())
                {
                    switchTarget = tapestryContext.getTemplateFile();
                }
                else if (currentFile.isTemplateFile())
                {
                    switchTarget = tapestryContext.getJavaFile();
                }
                else
                {
                    //  Switch to Java file by default
                    switchTarget = tapestryContext.getJavaFile();
                }
                
                if (switchTarget == null)
                {
                    throw new ExecutionException("Complement file not found for "
                                + currentFile.getPath().toPortableString());
                }
                
                EclipseUtils.openFile(window, switchTarget);
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
