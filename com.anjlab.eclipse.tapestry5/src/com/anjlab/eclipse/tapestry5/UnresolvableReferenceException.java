package com.anjlab.eclipse.tapestry5;

public class UnresolvableReferenceException extends Exception
{
    private static final long serialVersionUID = 1L;

    public UnresolvableReferenceException(String message)
    {
        super(message);
    }
    
    public UnresolvableReferenceException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
