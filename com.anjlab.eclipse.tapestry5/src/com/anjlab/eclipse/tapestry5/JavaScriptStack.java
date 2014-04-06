package com.anjlab.eclipse.tapestry5;

import org.eclipse.jdt.core.IType;

public class JavaScriptStack
{
    private final String name;
    private final IType declaration;
    private final boolean overrides;
    private boolean overridden;
    
    public JavaScriptStack(String stackName, IType declaration, boolean overrides)
    {
        this.name = stackName;
        this.declaration = declaration;
        this.overrides = overrides;
    }

    public boolean isOverridden()
    {
        return overridden;
    }
    public void setOverridden(boolean overridden)
    {
        this.overridden = overridden;
    }
    public String getName()
    {
        return name;
    }
    public IType getDeclaration()
    {
        return declaration;
    }
    public boolean isOverrides()
    {
        return overrides;
    }
}
