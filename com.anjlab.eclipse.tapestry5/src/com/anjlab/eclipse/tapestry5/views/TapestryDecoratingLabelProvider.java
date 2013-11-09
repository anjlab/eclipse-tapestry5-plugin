package com.anjlab.eclipse.tapestry5.views;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.ui.internal.navigator.NavigatorDecoratingLabelProvider;

@SuppressWarnings("restriction")
public class TapestryDecoratingLabelProvider extends NavigatorDecoratingLabelProvider
{
    public TapestryDecoratingLabelProvider(ILabelProvider commonLabelProvider)
    {
        super(commonLabelProvider);
    }
}
