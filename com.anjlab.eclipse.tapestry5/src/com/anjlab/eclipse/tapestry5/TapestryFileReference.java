package com.anjlab.eclipse.tapestry5;

public interface TapestryFileReference
{

    public abstract String getReference();

    public abstract TapestryFile resolveFile(boolean updateMarker) throws UnresolvableReferenceException;

}