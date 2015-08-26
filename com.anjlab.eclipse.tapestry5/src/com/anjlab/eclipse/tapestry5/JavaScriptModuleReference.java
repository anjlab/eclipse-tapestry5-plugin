package com.anjlab.eclipse.tapestry5;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ISourceRange;

import com.anjlab.eclipse.tapestry5.internal.TapestryBuiltinJavaScriptModuleConfiguration;

public class JavaScriptModuleReference extends AbstractFileReference
{

    public static final String MARKER_NAME = "ModuleName";

    private static final TapestryBuiltinJavaScriptModuleConfiguration
        builtinModules = new TapestryBuiltinJavaScriptModuleConfiguration();
    
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
        String path = builtinModules.getPath(moduleName);
        
        if (StringUtils.isEmpty(path))
        {
            path = "META-INF/modules/" + moduleName + ".js";
        }
        
        //  TODO First segment of moduleName holds the name of the library,
        //  also extension name may be different
        
        path = TapestryUtils.expandSymbols(path);
        
        return new Path(path);
    }

    @Override
    public String getName()
    {
        return reference;
    }

    @Override
    protected TapestryFile resolveFile() throws UnresolvableReferenceException
    {
        String path = getPath().toOSString();
        
        Asset asset = new Asset(path);
        
        AssetResolver assetResolver = TapestryUtils.createAssetResolver(asset.bindingPrefix);
        
        TapestryFile resolvedFile = assetResolver.resolve(asset.path, javaFile);
        
        return resolvedFile;
    }

}
