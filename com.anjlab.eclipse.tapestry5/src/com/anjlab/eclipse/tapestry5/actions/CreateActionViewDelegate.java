package com.anjlab.eclipse.tapestry5.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.anjlab.eclipse.tapestry5.Activator;
import com.anjlab.eclipse.tapestry5.EclipseUtils;
import com.anjlab.eclipse.tapestry5.TapestryContext;
import com.anjlab.eclipse.tapestry5.TapestryUtils;

public class CreateActionViewDelegate implements IViewActionDelegate, IMenuCreator
{
    private static final String DEFAULT_JAVA_SOURCE_FOLDER = "src/main/java";

    @Override
    public void run(IAction action)
    {
        Menu menu = getMenu(window.getShell());
        
        if (menu.getItemCount() > 0)
        {
            menu.setVisible(true);
        }
        else
        {
            EclipseUtils.openInformation(window,
                    "Try selecting your Tapestry5 project in the Package Explorer.");
        }
    }

    private IAction newTextFileMenuItem(Menu menu, TapestryContext tapestryContext, String title, String fileName)
    {
        NewFileWizardAction newFile = new NewFileWizardAction(tapestryContext.getProject(),
                tapestryContext,
                window.getShell(),
                window);
        
        newFile.setFileName(fileName);
        newFile.setFolder("src/main/resources/" + tapestryContext.getPackageName().replaceAll("\\.", "/"));
        
        newFile.setText(title);
        newFile.setImageDescriptor(PlatformUI.getWorkbench().getEditorRegistry()
                .getImageDescriptor(fileName));
        
        return addActionToMenu(menu, newFile);
    }

    private IAction newJavaClassMenuItem(Menu menu, IProject project, String title, String packageName, String typeName)
    {
        NewJavaClassWizardAction newJavaClass = new NewJavaClassWizardAction(project);
        newJavaClass.setText(title);
        newJavaClass.setSourceFolder(DEFAULT_JAVA_SOURCE_FOLDER);
        newJavaClass.setPackageName(packageName);
        newJavaClass.setTypeName(typeName);
        
        return addActionToMenu(menu, newJavaClass);
    }
    
    private IAction addActionToMenu(Menu menu, IAction action)
    {
        new ActionContributionItem(action).fill(menu, -1);
        return action;
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection)
    {
        
    }

    @Override
    public void dispose()
    {
        
    }

    @Override
    public Menu getMenu(Control parent)
    {
        Menu menu = new Menu(parent);
        
        TapestryContext tapestryContext = Activator.getDefault().getTapestryContext();
        
        if (tapestryContext != null && !tapestryContext.isEmpty() && tapestryContext.getName() != null)
        {
            newJavaClassMenuItem(menu, tapestryContext.getProject(),
                    "Create " + tapestryContext.getName() + ".java...",
                    tapestryContext.getPackageName(),
                    tapestryContext.getName())
                .setEnabled(tapestryContext.getJavaFile() == null);
            
            String tmlFileName = tapestryContext.getName() + ".tml";
            newTextFileMenuItem(menu, tapestryContext, "Create " + tmlFileName + "...", tmlFileName)
                .setEnabled(!tapestryContext.contains(tmlFileName));
            
            String jsFileName = tapestryContext.getName() + ".js";
            newTextFileMenuItem(menu, tapestryContext, "Create " + jsFileName + "...", jsFileName)
                .setEnabled(!tapestryContext.contains(jsFileName));
            
            String cssFileName = tapestryContext.getName() + ".css";
            newTextFileMenuItem(menu, tapestryContext, "Create " + cssFileName + "...", cssFileName)
                .setEnabled(!tapestryContext.contains(cssFileName));
            
            newTextFileMenuItem(menu, tapestryContext, "Create other...", "");
        }
        
        IProject contextProject = tapestryContext != null ? tapestryContext.getProject() : null;
        
        if (contextProject == null)
        {
            IResource contextResource = TapestryUtils.getResourceForTapestryContext(window);
            
            contextProject = contextResource != null ? contextResource.getProject() : null;
        }
        
        if (contextProject != null && TapestryUtils.getAppPackage(contextProject) != null)
        {
            if (menu.getItemCount() > 0)
            {
                new MenuItem(menu, SWT.SEPARATOR);
            }
            
            newJavaClassMenuItem(menu, contextProject,
                    "New Page Class...",
                    TapestryUtils.getPagesPackage(contextProject),
                    null);
            
            newJavaClassMenuItem(menu, contextProject,
                    "New Component Class...",
                    TapestryUtils.getComponentsPackage(contextProject),
                    null);
            
            newJavaClassMenuItem(menu, contextProject,
                    "New Mixin Class...",
                    TapestryUtils.getMixinsPackage(contextProject),
                    null);
        }
        
        return menu;
    }

    @Override
    public Menu getMenu(Menu parent)
    {
        return null;
    }

    private IWorkbenchWindow window;
    
    @Override
    public void init(IViewPart view)
    {
        this.window = view.getSite().getWorkbenchWindow();
    }

    public void init(IWorkbenchWindow window)
    {
        this.window = window;
    }

}
