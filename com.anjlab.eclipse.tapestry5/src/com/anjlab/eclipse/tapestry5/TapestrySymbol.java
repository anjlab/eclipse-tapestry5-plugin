package com.anjlab.eclipse.tapestry5;


public class TapestrySymbol
{

    private final String name;
    private final String value;
    private final DeclarationReference reference;
    private final boolean override;
    private boolean overridden;
    
    public TapestrySymbol(
            String name,
            String value,
            boolean override,
            DeclarationReference reference)
    {
        this.name = name;
        this.value = value;
        this.override = override;
        this.reference = reference;
    }

    public String getName()
    {
        return name;
    }
    
    public String getValue()
    {
        return value;
    }
    
    public boolean isOverride()
    {
        return override;
    }
    
    public DeclarationReference getReference()
    {
        return reference;
    }

    public boolean isOverridden()
    {
        return overridden;
    }

    public void setOverridden(boolean overridden)
    {
        this.overridden = overridden;
    }
}
