package com.anjlab.eclipse.tapestry5.internal.visitors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.MethodInvocation;

import com.anjlab.eclipse.tapestry5.Activator;
import com.anjlab.eclipse.tapestry5.DeclarationReference.ASTNodeReference;
import com.anjlab.eclipse.tapestry5.JavaScriptStack;
import com.anjlab.eclipse.tapestry5.ObjectCallback;
import com.anjlab.eclipse.tapestry5.TapestryModule;
import com.anjlab.eclipse.tapestry5.TapestryUtils;

public class JavaScriptStackCapturingVisitor extends TapestryServiceConfigurationCapturingVisitor
{
    private final ObjectCallback<JavaScriptStack, RuntimeException> javaScriptStackFound;
    
    public JavaScriptStackCapturingVisitor(IProgressMonitor monitor, TapestryModule tapestryModule,
            ObjectCallback<JavaScriptStack, RuntimeException> javaScriptStackFound)
    {
        super(monitor, tapestryModule);
        
        this.javaScriptStackFound = javaScriptStackFound;
        
        usesMappedConfiguration();
    }

    @Override
    protected void mappedConfigurationAddOverride(MethodInvocation node, Object key, Object value)
    {
        //  TODO Use InjectionDeclaration
        if (!(key instanceof String) || !(value instanceof IType))
        {
            return;
        }
        
        String stackName = (String) key;
        IType stackType = (IType) value;
        
        try
        {
            tryAddJavaScriptStack(node, stackName, stackType, stackType.getFullyQualifiedName());
            
            String[] interfaceNames = stackType.getSuperInterfaceNames();
            
            for (String interfaceName : interfaceNames)
            {
                tryAddJavaScriptStack(node, stackName, stackType, interfaceName);
            }
        }
        catch (JavaModelException e)
        {
            Activator.getDefault().logWarning(
                    "Unable to get super interfaces of " + stackType);
        }
    }
    
    private void tryAddJavaScriptStack(MethodInvocation node, String stackName, IType type, String interfaceName)
    {
        if (TapestryUtils.isTapestryJavaScriptStackInterface(interfaceName))
        {
            javaScriptStackFound.callback(new JavaScriptStack(
                    stackName,
                    type,
                    isOverride(node),
                    new ASTNodeReference(tapestryModule, tapestryModule.getModuleClass(), node)));
        }
    }
}