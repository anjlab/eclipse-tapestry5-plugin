package com.anjlab.eclipse.tapestry5;

public class Component extends Member
{
    private String id = "";
    private String type = "";
    private boolean javaType = false;
    private String[] parameters = { };
    private boolean inheritInformalParameters = false;
    private String publishParameters = "";
    
    public String getId()
    {
        return id;
    }
    public void setId(String id)
    {
        this.id = id;
    }
    public String getType()
    {
        return type;
    }
    public void setType(String type)
    {
        this.type = type;
    }
    public String[] getParameters()
    {
        return parameters;
    }
    public void setParameters(String[] parameters)
    {
        this.parameters = parameters;
    }
    public boolean isInheritInformalParameters()
    {
        return inheritInformalParameters;
    }
    public void setInheritInformalParameters(boolean inheritInformalParameters)
    {
        this.inheritInformalParameters = inheritInformalParameters;
    }
    public String getPublishParameters()
    {
        return publishParameters;
    }
    public void setPublishParameters(String publishParameters)
    {
        this.publishParameters = publishParameters;
    }
    public boolean isJavaType()
    {
        return javaType;
    }
    public void setJavaType(boolean javaType)
    {
        this.javaType = javaType;
    }
}
