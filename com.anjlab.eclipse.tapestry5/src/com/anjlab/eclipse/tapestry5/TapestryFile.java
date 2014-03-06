package com.anjlab.eclipse.tapestry5;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

public interface TapestryFile
{
    boolean isTemplateFile();
    
    boolean isJavaFile();
    
    IPath getPath();
    
    String getName();
    
    String getFileExtension();
    
    IProject getProject();
    
    boolean isPropertiesFile();
    
    boolean isJavaScriptFile();
    
    boolean isStyleSheetFile();
    
    TapestryContext getContext();

    boolean exists();
}
