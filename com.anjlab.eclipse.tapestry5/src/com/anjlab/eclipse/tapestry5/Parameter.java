package com.anjlab.eclipse.tapestry5;

public class Parameter extends Member
{
    private boolean required;
    private boolean allowNull = true;
    private boolean cache = true;
    private String value = "";
    private String defaultPrefix = "prop";
    private boolean principal;
    private boolean autoconnect;
    
    public boolean isRequired()
    {
        return required;
    }
    public void setRequired(boolean required)
    {
        this.required = required;
    }
    public boolean isAllowNull()
    {
        return allowNull;
    }
    public void setAllowNull(boolean allowNull)
    {
        this.allowNull = allowNull;
    }
    public boolean isCache()
    {
        return cache;
    }
    public void setCache(boolean cache)
    {
        this.cache = cache;
    }
    public String getValue()
    {
        return value;
    }
    public void setValue(String value)
    {
        this.value = value;
    }
    public String getDefaultPrefix()
    {
        return defaultPrefix;
    }
    public void setDefaultPrefix(String defaultPrefix)
    {
        this.defaultPrefix = defaultPrefix;
    }
    public boolean isPrincipal()
    {
        return principal;
    }
    public void setPrincipal(boolean principal)
    {
        this.principal = principal;
    }
    public boolean isAutoconnect()
    {
        return autoconnect;
    }
    public void setAutoconnect(boolean autoconnect)
    {
        this.autoconnect = autoconnect;
    }
}