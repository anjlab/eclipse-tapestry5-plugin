package com.anjlab.eclipse.tapestry5.views;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;

import com.anjlab.eclipse.tapestry5.JavaScriptStack;
import com.anjlab.eclipse.tapestry5.LibraryMapping;
import com.anjlab.eclipse.tapestry5.TapestryModule;
import com.anjlab.eclipse.tapestry5.TapestryService;
import com.anjlab.eclipse.tapestry5.TapestrySymbol;

public class ViewLabelDecorator extends LabelProvider implements ILightweightLabelDecorator
{

    @Override
    public void decorate(Object element, IDecoration decoration)
    {
        if (element instanceof TreeObject)
        {
            Object data = ((TreeObject) element).getData();
            
            if (data instanceof TapestryModule)
            {
                decoration.addSuffix(" " + StringUtils.join(((TapestryModule) data).references(), ", "));
            }
            else if (data instanceof LibraryMapping)
            {
                decoration.addSuffix(" " + ((LibraryMapping) data).getRootPackage());
            }
            else if (data instanceof JavaScriptStack)
            {
                decoration.addSuffix(" " + ((JavaScriptStack) data).getType().getFullyQualifiedName());
            }
            else if (data instanceof TapestrySymbol)
            {
                TapestrySymbol symbol = (TapestrySymbol) data;
                
                decoration.addSuffix(
                        " from " + symbol.getSymbolProvider().getDefinition().getId()
                        + " in " + symbol.getReference().getTapestryModule().getName()
                        + ", =" + symbol.getValue());
            }
            else if (data instanceof TapestryService)
            {
                decoration.addSuffix(" " + ((TapestryService) data).getDefinition().getIntfClass());
            }
            else if (data instanceof Throwable)
            {
                decoration.addSuffix(" " + ((Throwable) data).getMessage());
            }
            else if (data instanceof String)
            {
                decoration.addSuffix(" " + data);
            }
            else if (element instanceof TreeParent)
            {
                int childCount = ((TreeParent) element).getChildCount();
                if (childCount > 0)
                {
                    decoration.addSuffix(" " + childCount);
                }
            }
        }
    }

}
