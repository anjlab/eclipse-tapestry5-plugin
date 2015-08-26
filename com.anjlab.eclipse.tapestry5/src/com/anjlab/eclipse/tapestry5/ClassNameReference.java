package com.anjlab.eclipse.tapestry5;

import org.eclipse.core.resources.IProject;

public class ClassNameReference implements Openable
{
    private final IProject project;
    private final String className;
    
    public ClassNameReference(IProject project, String className)
    {
        this.project = project;
        this.className = className;
    }
    
    public String getClassName()
    {
        return className;
    }
    
    public IProject getProject()
    {
        return project;
    }
    
    @Override
    public void openInEditor()
    {
        EclipseUtils.openDeclaration(
                EclipseUtils.findTypeDeclaration(
                        project,
                        className),
                null);
    }
}
