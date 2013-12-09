package com.anjlab.eclipse.tapestry5;

public class Property extends Member
{
    private boolean read;
    private boolean write;
    
    public boolean isRead()
    {
        return read;
    }
    public void setRead(boolean read)
    {
        this.read = read;
    }
    public boolean isWrite()
    {
        return write;
    }
    public void setWrite(boolean write)
    {
        this.write = write;
    }
}