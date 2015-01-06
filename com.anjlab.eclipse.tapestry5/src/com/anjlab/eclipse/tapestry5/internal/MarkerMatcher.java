package com.anjlab.eclipse.tapestry5.internal;

import com.anjlab.eclipse.tapestry5.TapestryService;
import com.anjlab.eclipse.tapestry5.TapestryService.Matcher;

public class MarkerMatcher implements Matcher
{
    private final String marker;

    public MarkerMatcher(String marker)
    {
        this.marker = marker;
    }

    @Override
    public boolean matches(TapestryService service)
    {
        for (String marker : service.getDefinition().getMarkers())
        {
            if (this.marker.equals(marker))
            {
                return true;
            }
        }
        return false;
    }

}
