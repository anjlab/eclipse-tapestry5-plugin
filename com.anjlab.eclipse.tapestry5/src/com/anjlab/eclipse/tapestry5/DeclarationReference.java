package com.anjlab.eclipse.tapestry5;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.ASTNode;

public interface DeclarationReference
{
    public static class ASTNodeReference implements DeclarationReference
    {
        private final IJavaElement element;
        private final ASTNode node;
        
        public ASTNodeReference(IJavaElement element, ASTNode node)
        {
            this.element = element;
            this.node = node;
        }
        
        @Override
        public IJavaElement getElement()
        {
            return element;
        }
        
        public ASTNode getNode()
        {
            return node;
        }
        
        @Override
        public void openInEditor()
        {
            EclipseUtils.openDeclaration(element,
                    new SetEditorCaretPositionOffsetLength(
                            node.getStartPosition(), node.getLength()));
        }
    }

    public static class JavaElementReference implements DeclarationReference
    {
        private final IJavaElement element;
    
        public JavaElementReference(IJavaElement element)
        {
            this.element = element;
        }
        
        @Override
        public IJavaElement getElement()
        {
            return element;
        }
        
        @Override
        public void openInEditor()
        {
            EclipseUtils.openDeclaration(element, null);
        }
    }

    public static class NonJavaReference implements DeclarationReference
    {
        @Override
        public IJavaElement getElement()
        {
            return null;
        }

        @Override
        public void openInEditor()
        {
            //  TODO In some cases we do know the file name and location,
            //  so we should implement this method for these cases
        }
    }
    
    IJavaElement getElement();

    void openInEditor();
}