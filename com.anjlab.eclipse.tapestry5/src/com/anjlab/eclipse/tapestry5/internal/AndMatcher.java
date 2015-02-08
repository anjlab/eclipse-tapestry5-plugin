package com.anjlab.eclipse.tapestry5.internal;

import java.util.ArrayList;
import java.util.List;

import com.anjlab.eclipse.tapestry5.TapestryService;
import com.anjlab.eclipse.tapestry5.TapestryService.Matcher;

public class AndMatcher implements Matcher
{
    private final List<Matcher> matchers;

    public AndMatcher()
    {
        matchers = new ArrayList<Matcher>();
    }

    public AndMatcher(List<Matcher> matchers)
    {
        this();
        this.matchers.addAll(matchers);
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
            if (!m.matches(service))
            {
                return false;
            }
        }

        return matchers.size() > 0;
    }

}
