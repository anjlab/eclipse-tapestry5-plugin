package com.anjlab.eclipse.tapestry5.templates;

import java.io.InputStream;

import com.anjlab.eclipse.tapestry5.TapestryProject;

public class TapestryTemplates
{
    private static final ProjectConventions defaultConventions = new ProjectConventions();
    
    private static class ProjectConventions
    {
        public String buildTemplateFileName(String fileName, String fileExtension)
        {
            return fileName + "." + fileExtension;
        }
    };
    
    private final ProjectConventions projectConventions;
    
    private TapestryTemplates(ProjectConventions conventions)
    {
        this.projectConventions = conventions;
    }
    
    public static TapestryTemplates get(final TapestryProject project)
    {
        if (project == null)
        {
            return new TapestryTemplates(defaultConventions);
        }
        
        return new TapestryTemplates(new ProjectConventions()
        {
            @Override
            public String buildTemplateFileName(String fileName, String fileExtension)
            {
                return fileName + "-" + project.getTapestryVersionMajorMinor() + "." + fileExtension;
            }
        });
    }

    public InputStream openTemplate(String fileName, String fileExtension)
    {
        InputStream stream = getClass().getResourceAsStream(
                projectConventions.buildTemplateFileName(fileName, fileExtension));
        
        if (stream == null && projectConventions != defaultConventions)
        {
            stream = getClass().getResourceAsStream(
                    defaultConventions.buildTemplateFileName(fileName, fileExtension));
        }
        
        return stream;
    }
}
