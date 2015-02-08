package com.anjlab.eclipse.tapestry5;

import org.apache.commons.lang.StringUtils;

import com.anjlab.eclipse.tapestry5.DeclarationReference.NonJavaReference;

public abstract class TapestryModuleReference
{
    private final DeclarationReference reference;
    private final boolean conditional;
    
    public TapestryModuleReference(DeclarationReference reference, boolean conditional)
    {
        if (reference == null)
        {
            throw new NullPointerException("reference == null");
        }
        this.reference = reference;
        this.conditional = conditional;
    }

    public boolean isConditional()
    {
        return conditional;
    }

    public DeclarationReference getReference()
    {
        return reference;
    }

    public abstract String getLabel();

    @Override
    public String toString()
    {
        return getLabel();
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        
        if (!(obj instanceof TapestryModuleReference))
        {
            return false;
        }
        
        TapestryModuleReference other = (TapestryModuleReference) obj;
        
        if (reference instanceof NonJavaReference
                && other.reference instanceof NonJavaReference)
        {
            //  Non-Java references can only be compared by label
            return StringUtils.equals(getLabel(), other.getLabel());
        }
        
        return reference.equals(other.reference);
    }
}