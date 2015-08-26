package com.anjlab.eclipse.tapestry5.internal;

import static com.anjlab.eclipse.tapestry5.TapestryUtils.getSimpleName;

import org.apache.commons.lang3.StringUtils;

import com.anjlab.eclipse.tapestry5.TapestryService;
import com.anjlab.eclipse.tapestry5.TapestryService.Matcher;

public class ServiceImplMatcher implements Matcher
{
    private final String className;
    
    public ServiceImplMatcher(String className)
    {
        this.className = className;
    }

    @Override
    public boolean matches(TapestryService service)
    {
        if (StringUtils.isEmpty(service.getDefinition().getImplClass()))
        {
            return false;
        }
        
        return StringUtils.equals(className, service.getDefinition().getImplClass())
                || StringUtils.equals(getSimpleName(this.className),
                        getSimpleName(service.getDefinition().getImplClass()));
    }

}
