package com.anjlab.eclipse.tapestry5;


public class TapestrySymbol implements Cloneable, Openable
{

    private final TapestryService symbolProvider;
    private final String name;
    private final String value;
    private final DeclarationReference reference;
    private final boolean override;
    private boolean overridden;
    
    public TapestrySymbol(
            String name,
            String value,
            boolean override,
            DeclarationReference reference,
            TapestryService symbolProvider)
    {
        this.name = name;
        this.value = value;
        this.override = override;
        this.reference = reference;
        this.symbolProvider = symbolProvider;
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

    public TapestryService getSymbolProvider()
    {
        return symbolProvider;
    }

    public boolean isOverridden()
    {
        return overridden;
    }

    public void setOverridden(boolean overridden)
    {
        this.overridden = overridden;
    }
    
    @Override
    public TapestrySymbol clone()
    {
        return new TapestrySymbol(name, value, override, reference, symbolProvider);
    }
    
    @Override
    public void openInEditor()
    {
        getReference().openInEditor();
    }
}
