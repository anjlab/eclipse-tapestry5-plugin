package com.anjlab.eclipse.tapestry5;

import org.eclipse.jdt.core.ISourceRange;

public class Member
{

    private String name;
    private ISourceRange nameRange;
    private String javadoc;

    private TapestryComponentSpecification specification;
    
    public TapestryComponentSpecification getSpecification()
    {
        return specification;
    }

    public void setSpecification(TapestryComponentSpecification specification)
    {
        this.specification = specification;
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

    public ISourceRange getNameRange()
    {
        return nameRange;
    }

    public void setNameRange(ISourceRange nameRange)
    {
        this.nameRange = nameRange;
    }
}