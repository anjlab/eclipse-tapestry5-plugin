package com.anjlab.eclipse.tapestry5;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ISourceRange;

public class JavaScriptModuleReference extends AbstractFileReference
{

    private static final String MARKER_NAME = "ModuleName";

    private final String moduleName;
    private final String functionName;
    
    public JavaScriptModuleReference(TapestryFile javaFile, ISourceRange sourceRange, String reference)
    {
        super(javaFile, sourceRange, reference, MARKER_NAME);
        
        int colonIndex = reference.lastIndexOf(':');
        
        if (colonIndex < 0)
        {
            moduleName = reference;
            functionName = null;
        }
        else
        {
            moduleName = reference.substring(0, colonIndex);
            functionName = reference.substring(colonIndex + 1);
        }
    }

    public String getModuleName()
    {
        return moduleName;
    }

    public String getFunctionName()
    {
        return functionName;
    }

    @Override
    public IPath getPath()
    {
        return new Path("META-INF/modules/" + moduleName + ".js");
    }

    @Override
    public String getName()
    {
        return reference;
    }

    @Override
    protected TapestryFile resolveFile() throws UnresolvableReferenceException
    {
        String path = getPath().toPortableString();
        
        TapestryFile tapestryFile =
                javaFile
                    .getContext()
                    .createLookup()
                    .findClasspathFileCaseInsensitive(path);
        
        if (tapestryFile == null)
        {
            throw new UnresolvableReferenceException(
                    "Couldn't resolve classpath asset from path '" + path + "'");
        }
        
        return tapestryFile;
    }

}
