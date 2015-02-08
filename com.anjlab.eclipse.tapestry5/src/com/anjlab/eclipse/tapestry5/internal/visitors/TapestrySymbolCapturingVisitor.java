package com.anjlab.eclipse.tapestry5.internal.visitors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.dom.MethodInvocation;

import com.anjlab.eclipse.tapestry5.DeclarationReference.ASTNodeReference;
import com.anjlab.eclipse.tapestry5.ObjectCallback;
import com.anjlab.eclipse.tapestry5.TapestryModule;
import com.anjlab.eclipse.tapestry5.TapestrySymbol;

public class TapestrySymbolCapturingVisitor extends TapestryServiceConfigurationCapturingVisitor
{
    private final ObjectCallback<TapestrySymbol, RuntimeException> symbolFound;
    
    public TapestrySymbolCapturingVisitor(IProgressMonitor monitor, TapestryModule tapestryModule,
            ObjectCallback<TapestrySymbol, RuntimeException> symbolFound)
    {
        super(monitor, tapestryModule);
        
        this.symbolFound = symbolFound;
        
        usesMappedConfiguration();
    }

    @Override
    protected void mappedConfigurationAddOverride(MethodInvocation node, Object key, Object value)
    {
        //  TODO Use InjectedDeclaration
        if (!(key instanceof String) || !(value instanceof String))
        {
            return;
        }
        
        symbolFound.callback(
                new TapestrySymbol(
                        (String) key,
                        (String) value,
                        isOverride(node),
                        new ASTNodeReference(tapestryModule, tapestryModule.getModuleClass(), node)));
    }
}
