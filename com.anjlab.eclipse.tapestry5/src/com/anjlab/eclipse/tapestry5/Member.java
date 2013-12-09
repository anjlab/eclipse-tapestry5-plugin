package com.anjlab.eclipse.tapestry5;

public class Member
{

    public String name;
    private String javadoc;

    public Member()
    {
        super();
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getJavadoc()
    {
        return javadoc;
    }

    public void setJavadoc(String javadoc)
    {
        this.javadoc = javadoc;
    }

}