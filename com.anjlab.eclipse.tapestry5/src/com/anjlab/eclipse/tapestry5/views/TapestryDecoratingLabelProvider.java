package com.anjlab.eclipse.tapestry5.views;

import org.eclipse.jface.viewers.DecoratingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DecorationContext;
import org.eclipse.ui.PlatformUI;

public class TapestryDecoratingLabelProvider extends DecoratingStyledCellLabelProvider
{
    public TapestryDecoratingLabelProvider()
    {
        super(new LabelProvider(),
                PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator(),
                DecorationContext.DEFAULT_CONTEXT);
    }
}
