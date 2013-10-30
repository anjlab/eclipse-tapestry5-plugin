package com.anjlab.eclipse.tapestry5.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com.anjlab.eclipse.tapestry5.actions.CreateActionViewDelegate;

public class CreateHandler extends AbstractHandler
{

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        CreateActionViewDelegate delegate = new CreateActionViewDelegate();
        
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
        
        delegate.init(window);
        
        delegate.run(null);
        
        return null;
    }

}
