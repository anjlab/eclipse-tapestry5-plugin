package com.anjlab.eclipse.tapestry5.internal;

import com.anjlab.eclipse.tapestry5.TapestryService;
import com.anjlab.eclipse.tapestry5.TapestryService.Matcher;

public class IdentityMatcher implements Matcher
{
    private final boolean matches;
    
    public IdentityMatcher(boolean matches)
    {
        this.matches = matches;
    }

    @Override
    public boolean matches(TapestryService service)
    {
        return matches;
    }
}
