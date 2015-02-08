package com.anjlab.eclipse.tapestry5.internal;

import com.anjlab.eclipse.tapestry5.TapestryModule;
import com.anjlab.eclipse.tapestry5.TapestryService;
import com.anjlab.eclipse.tapestry5.TapestryService.Matcher;

public class TapestryModuleMatcher implements Matcher
{
    private final TapestryModule tapestryModule;

    public TapestryModuleMatcher(TapestryModule tapestryModule)
    {
        this.tapestryModule = tapestryModule;
    }

    @Override
    public boolean matches(TapestryService service)
    {
        return tapestryModule.equals(service.getTapestryModule());
    }

}
