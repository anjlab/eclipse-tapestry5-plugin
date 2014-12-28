package com.anjlab.eclipse.tapestry5;

import org.eclipse.jdt.core.IMethod;

public class TapestryService
{
    public static interface ServiceDeclaration
    {
        void openInEditor();
    }
    
    public static class BuilderMethodServiceDeclaration implements ServiceDeclaration
    {
        private final IMethod method;

        public BuilderMethodServiceDeclaration(IMethod method)
        {
            this.method = method;
        }
        
        @Override
        public void openInEditor()
        {
            EclipseUtils.openDeclaration(method);
        }
    }

    private TapestryModule tapestryModule;
    private String className;
    private String serviceId;
    private ServiceDeclaration declaration;
    
    public TapestryService(TapestryModule tapestryModule, String className, String serviceId, ServiceDeclaration declaration)
    {
        this.tapestryModule = tapestryModule;
        this.className = className;
        this.serviceId = serviceId;
        this.declaration = declaration;
    }
    
    public TapestryModule getTapestryModule()
    {
        return tapestryModule;
    }
    
    public String getClassName()
    {
        return className;
    }
    
    public String getServiceId()
    {
        return serviceId;
    }
    
    public ServiceDeclaration getDeclaration()
    {
        return declaration;
    }
}
