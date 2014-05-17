package com.anjlab.eclipse.tapestry5;

import org.eclipse.ui.IWorkbenchWindow;

public class TapestryContextScope
{
    public final IWorkbenchWindow window;
    public final TapestryProject project;
    public final TapestryContext context;
    public final TapestryComponentSpecification specification;
    
    public TapestryContextScope(IWorkbenchWindow window,
                                TapestryProject project,
                                TapestryContext context,
                                TapestryComponentSpecification specification)
    {
        this.window = window;
        this.project = project;
        this.context = context;
        this.specification = specification;
    }
}