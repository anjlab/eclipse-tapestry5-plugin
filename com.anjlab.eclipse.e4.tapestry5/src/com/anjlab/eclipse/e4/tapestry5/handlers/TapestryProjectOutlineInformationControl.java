package com.anjlab.eclipse.e4.tapestry5.handlers;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.ui.IWorkbenchWindow;

import com.anjlab.eclipse.tapestry5.TapestryProject;
import com.anjlab.eclipse.tapestry5.views.project.TapestryProjectOutlineContentProvider;

public final class TapestryProjectOutlineInformationControl extends AbstractTapestryContextInformation
{
    public TapestryProjectOutlineInformationControl(
            final IWorkbenchWindow window,
            final TapestryProject tapestryProject)
    {
        super(window.getShell(), new ContentProviderCreator()
        {
            @Override
            public ITreeContentProvider createContentProvider()
            {
                return new TapestryProjectOutlineContentProvider(tapestryProject);
            }
        });
    }
}