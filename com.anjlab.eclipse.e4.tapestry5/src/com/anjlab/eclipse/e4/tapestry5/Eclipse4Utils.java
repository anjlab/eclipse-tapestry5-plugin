package com.anjlab.eclipse.e4.tapestry5;

import org.eclipse.jdt.ui.IPackagesViewPart;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.navigator.CommonNavigator;

public class Eclipse4Utils
{
    /**
     * IWorkbenchPart activePart = window.getPartService().getActivePart();
     *  
     *  Tree tree = Eclipse4Utils.getTreeFromPart(activePart);
     *  
     *  if (tree != null)
     *  {
     *      //  We're in navigator tree.
     *      //  Try determining selected items from this tree to position editor list right near it.
     *
     * @param activePart
     * @return
     */
    public static Tree getTreeFromPart(IWorkbenchPart activePart)
    {
        Tree tree = null;
        if (activePart instanceof CommonNavigator)
        {
            tree = ((CommonNavigator) activePart).getCommonViewer().getTree();
        }
        else if (activePart instanceof IPackagesViewPart)
        {
            TreeViewer treeViewer = ((IPackagesViewPart) activePart).getTreeViewer();
            tree = treeViewer.getTree();
        }
        return tree;
    }

}
