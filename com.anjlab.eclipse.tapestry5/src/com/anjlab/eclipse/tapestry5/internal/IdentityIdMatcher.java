package com.anjlab.eclipse.tapestry5.internal;

import com.anjlab.eclipse.tapestry5.TapestryService;
import com.anjlab.eclipse.tapestry5.TapestryService.Matcher;

public class IdentityIdMatcher implements Matcher
{
    private final String serviceId;
    
    public IdentityIdMatcher(String serviceId)
    {
        this.serviceId = serviceId;
    }

    @Override
    public boolean matches(TapestryService service)
    {
        return serviceId.equalsIgnoreCase(service.getDefinition().getId());
    }

}
