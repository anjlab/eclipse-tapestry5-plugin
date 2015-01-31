package com.anjlab.eclipse.tapestry5.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.eclipse.jdt.core.dom.ASTNode;

public class DeclarationCapturingScope
{
    public static class Declaration
    {
        public final String name;
        public final String className;
        public final ASTNode node;

        public Declaration(String name, String className, ASTNode node)
        {
            this.name = name;
            this.className = className;
            this.node = node;
        }
    }

    private Stack<Map<String, Declaration>> scopes = new Stack<Map<String, Declaration>>();

    public DeclarationCapturingScope()
    {
        enterScope();
    }

    public void enterScope()
    {
        scopes.push(new HashMap<String, Declaration>());
    }

    public void exitScope()
    {
        scopes.pop();
    }

    public void add(Declaration declaration)
    {
        scopes.peek().put(declaration.name, declaration);
    }

    public Declaration findClosest(String name)
    {
        for (int i = scopes.size() - 1; i >= 0; i--)
        {
            Map<String, Declaration> scope = scopes.get(i);
            Declaration declaration = scope.get(name);
            if (declaration != null)
            {
                return declaration;
            }
        }
        return null;
    }
}
