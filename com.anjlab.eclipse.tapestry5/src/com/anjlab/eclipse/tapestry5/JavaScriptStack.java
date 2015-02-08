package com.anjlab.eclipse.tapestry5;

import org.eclipse.jdt.core.IType;


public class JavaScriptStack
{
    private final String name;
    private final DeclarationReference reference;
    private IType type;
    private final boolean override;
    private boolean overridden;
    
    public JavaScriptStack(String stackName, IType type, boolean override, DeclarationReference reference)
    {
        this.name = stackName;
        this.type = type;
        this.override = override;
        this.reference = reference;
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
    public IType getType()
    {
        return type;
    }
    public DeclarationReference getReference()
    {
        return reference;
    }
    public boolean isOverride()
    {
        return override;
    }
}
