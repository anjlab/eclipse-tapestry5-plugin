package com.anjlab.eclipse.tapestry5;

public interface ObjectCallback<T, E extends Throwable>
{
    void callback(T obj) throws E;
}