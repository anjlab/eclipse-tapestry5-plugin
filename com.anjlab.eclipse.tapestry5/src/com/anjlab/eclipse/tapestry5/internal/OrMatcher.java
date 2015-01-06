package com.anjlab.eclipse.tapestry5.internal;

import java.util.ArrayList;
import java.util.List;

import com.anjlab.eclipse.tapestry5.TapestryService;
import com.anjlab.eclipse.tapestry5.TapestryService.Matcher;

public final class OrMatcher implements Matcher
{
    private final List<Matcher> matchers;

    public OrMatcher()
    {
        matchers = new ArrayList<Matcher>();
    }

    public void add(Matcher matcher)
    {
        matchers.add(matcher);
    }

    @Override
    public boolean matches(TapestryService service)
    {
        for (Matcher m : matchers)
        {
            if (m.matches(service))
            {
                return true;
            }
        }

        return false;
    }

}
