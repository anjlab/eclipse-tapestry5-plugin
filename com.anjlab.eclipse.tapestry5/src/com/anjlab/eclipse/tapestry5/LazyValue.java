package com.anjlab.eclipse.tapestry5;

public abstract class LazyValue<T>
{
    private boolean evaluated;
    private boolean error;
    private T value;

    public boolean isError()
    {
        return error;
    }

    public boolean isEvaluated()
    {
        return evaluated;
    }

    public T get()
    {
        if (!evaluated)
        {
            try
            {
                this.value = eval();
            }
            catch (Exception e)
            {
                this.error = true;

                Activator.getDefault().logError("Error evaluating value", e);
            }
            finally
            {
                evaluated = true;
            }
        }

        return this.value;
    }

    protected abstract T eval() throws Exception;
}
