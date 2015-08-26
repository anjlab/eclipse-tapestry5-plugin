package com.anjlab.eclipse.e4.tapestry5.handlers;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.ui.IWorkbenchWindow;

import com.anjlab.eclipse.tapestry5.TapestryContext;
import com.anjlab.eclipse.tapestry5.views.context.TapestryContextContentProvider;

public final class TapestryContextInformationControl extends AbstractTapestryContextInformation
{
    public TapestryContextInformationControl(
            final IWorkbenchWindow window,
            final TapestryContext tapestryContext)
    {
        super(window.getShell(), new ContentProviderCreator()
        {
            @Override
            public ITreeContentProvider createContentProvider()
            {
                return new TapestryContextContentProvider(window, tapestryContext);
            }
        });
    }
}