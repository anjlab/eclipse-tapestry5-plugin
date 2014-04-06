package com.anjlab.eclipse.tapestry5;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

public class JavaScriptStackReference extends AbstractFileReference
{
    public static final String MARKER_NAME = "StackName";
    
    public JavaScriptStackReference(TapestryFile javaFile, String stackName, ISourceRange sourceRange)
    {
        super(javaFile, sourceRange, stackName, MARKER_NAME);
    }
    
    @Override
    protected TapestryFile resolveFile() throws UnresolvableReferenceException
    {
        JavaScriptStack stack = resolveStack();
        
        if (stack == null)
        {
            throw new UnresolvableReferenceException("Stack '"
                    + reference + "' not found in current tapestry project");
        }
        
        return resolveFile(getContext(), stack);
    }
    
    private TapestryFile resolveFile(TapestryContext context, JavaScriptStack stack)
            throws UnresolvableReferenceException
    {
        IType type = stack.getDeclaration();
        
        if (type.getClassFile() != null)
        {
            return new ClassFile(context, type.getClassFile());
        }
        
        try
        {
            if (type.getUnderlyingResource() != null)
            {
                IFile file = (IFile) type.getUnderlyingResource().getAdapter(IFile.class);
                LocalFile declaringFile = new LocalFile(context, file);
                return declaringFile;
            }
            
            throw new UnresolvableReferenceException("Unable to get underlying resource for " + type);
        }
        catch (JavaModelException e)
        {
            throw new UnresolvableReferenceException("Unable to get underlying resource for " + type, e);
        }
    }
    
    @Override
    public IPath getPath()
    {
        return javaFile.getPath();
    }

    @Override
    public String getName()
    {
        return reference;
    }

    @Override
    public IProject getProject()
    {
        return javaFile.getProject();
    }

    @Override
    public TapestryContext getContext()
    {
        return javaFile.getContext();
    }

    @Override
    public boolean exists()
    {
        JavaScriptStack stack = resolveStack();
        
        return stack == null
             ? false
             : stack.getDeclaration().exists();
    }

    private JavaScriptStack resolveStack()
    {
        return TapestryUtils.findStack(getProject(), reference);
    }

}
