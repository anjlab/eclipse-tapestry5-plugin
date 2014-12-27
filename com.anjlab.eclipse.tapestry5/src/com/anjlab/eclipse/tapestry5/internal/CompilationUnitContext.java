package com.anjlab.eclipse.tapestry5.internal;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.ui.services.IDisposable;

import com.anjlab.eclipse.tapestry5.EclipseUtils;

public class CompilationUnitContext implements IDisposable
{
    public static abstract class CompilationUnitLifecycle
    {
        public abstract ICompilationUnit createCompilationUnit();
        
        public void dispose(ICompilationUnit compilationUnit)
        {
            //  Default implementation does nothing
        }
    }
    
    public static interface ContextRunnable
    {
        void run (CompilationUnitContext context) throws JavaModelException;
    }
    
    private final CompilationUnitLifecycle lifecycle;
    private ICompilationUnit compilationUnit;
    private boolean cuCreated;
    private AST ast;
    private boolean astParsed;
    
    public CompilationUnitContext(CompilationUnitLifecycle lifecycle)
    {
        this.lifecycle = lifecycle;
    }
    
    public ICompilationUnit getCompilationUnit()
    {
        if (!cuCreated)
        {
            cuCreated = true;
            
            compilationUnit = lifecycle.createCompilationUnit();
        }
        
        return compilationUnit;
    }
    
    public AST getAST()
    {
        if (!astParsed)
        {
            astParsed = true;
            try
            {
                ast = EclipseUtils.parse(compilationUnit).getAST();
            }
            catch (Exception e)
            {
                //  Ignore
            }
        }
        
        return ast;
    }
    
    @Override
    public final void dispose()
    {
        if (compilationUnit != null)
        {
            lifecycle.dispose(compilationUnit);
        }
    }
}