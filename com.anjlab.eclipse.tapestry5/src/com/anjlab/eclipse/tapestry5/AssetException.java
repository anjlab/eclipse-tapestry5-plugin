package com.anjlab.eclipse.tapestry5;

public class AssetException extends Exception
{
    private static final long serialVersionUID = 1L;

    public AssetException(String message)
    {
        super(message);
    }
    
    public AssetException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
