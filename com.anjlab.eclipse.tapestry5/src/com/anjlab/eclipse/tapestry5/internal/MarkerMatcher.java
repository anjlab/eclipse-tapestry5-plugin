package com.anjlab.eclipse.tapestry5.internal;

import static com.anjlab.eclipse.tapestry5.TapestryUtils.getSimpleName;

import org.apache.commons.lang3.StringUtils;

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
            if (StringUtils.equals(this.marker, marker)
                    || StringUtils.equals(getSimpleName(this.marker), getSimpleName(marker)))
            {
                return true;
            }
        }
        return false;
    }

}
