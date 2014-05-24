package com.anjlab.eclipse.tapestry5.actions;

import org.eclipse.core.resources.IProject;
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

import com.anjlab.eclipse.tapestry5.EclipseUtils;
import com.anjlab.eclipse.tapestry5.TapestryContext;
import com.anjlab.eclipse.tapestry5.TapestryModule;
import com.anjlab.eclipse.tapestry5.TapestryUtils;
import com.anjlab.eclipse.tapestry5.views.context.TapestryContextView;

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
        
        TapestryContext tapestryContext = view instanceof TapestryContextView
                ? ((TapestryContextView) view).getTapestryContext()
                : TapestryUtils.createTapestryContext(window); // From Main ToolBar
        
        TapestryModule module;
        
        if (tapestryContext != null && !tapestryContext.isEmpty())
        {
            fillMenuForTapestryContext(menu, tapestryContext);
            
            module = TapestryUtils.getTapestryModule(window, tapestryContext.getProject());
        }
        else
        {
            IProject project = EclipseUtils.getProjectFromSelection(
                    EclipseUtils.getProjectExplorerSelection(window));
            
            module = TapestryUtils.getTapestryModule(window, project);
        }
        
        fillMenuForTapestryModule(menu, module);
        
        return menu;
    }

    protected void fillMenuForTapestryContext(Menu menu, TapestryContext tapestryContext)
    {
        if (!tapestryContext.isEmpty() && tapestryContext.getName() != null)
        {
            if (tapestryContext.isReadOnly())
            {
                newTextFileMenuItem(menu, tapestryContext, "This Context is Read-Only", "")
                    .setEnabled(false);
                
                return;
            }
            
            String packageName = tapestryContext.getPackageName();
            newJavaClassMenuItem(menu, tapestryContext.getProject(),
                    "Create " + tapestryContext.getName() + ".java...",
                    packageName != null ? packageName : TapestryUtils.getPagesPackage(tapestryContext.getProject()),
                    tapestryContext.getName())
                .setEnabled(tapestryContext.getJavaFile() == null);
            
            String[] extensions = new String[] { ".tml", ".properties", ".js", ".css" };
            
            for (String extension : extensions)
            {
                String fileName = tapestryContext.getName() + extension;
                newTextFileMenuItem(menu, tapestryContext, "Create " + fileName + "...", fileName)
                    .setEnabled(!tapestryContext.contains(fileName));
            }
            
            newTextFileMenuItem(menu, tapestryContext, "Create other...", tapestryContext.getName());
        }
    }

    protected void fillMenuForTapestryModule(Menu menu, TapestryModule module)
    {
        IProject project = module != null ? module.getEclipseProject() : null;
        
        if (module == null
                || module.isReadOnly()
                //  TODO Support creating classes for any non-ReadOnly TapestryModules,
                //  not only for the AppModule
                || !TapestryUtils.isTapestryAppProject(project))
        {
            return;
        }
        
        if (menu.getItemCount() > 0)
        {
            new MenuItem(menu, SWT.SEPARATOR);
        }
        
        newJavaClassMenuItem(menu, project,
                "New Page Class...",
                //  TODO Get package for TapestryModule
                //  Check if TapestryModule.isAppModule() then resolve package via IProject,
                //  otherwise via LibraryMappings
                //  TODO Support LibraryMappings for workspace projects -- they usually don't provide MANIFEST.MF
                //  maybe try searching manifests in build folders of the projects?
                TapestryUtils.getPagesPackage(project),
                null);
        
        newJavaClassMenuItem(menu, project,
                "New Component Class...",
                //  TODO Get package for TapestryModule
                TapestryUtils.getComponentsPackage(project),
                null);
        
        newJavaClassMenuItem(menu, project,
                "New Mixin Class...",
                //  TODO Get package for TapestryModule
                TapestryUtils.getMixinsPackage(project),
                null);
    }

    @Override
    public Menu getMenu(Menu parent)
    {
        return null;
    }

    private IWorkbenchWindow window;
    private IViewPart view;
    
    @Override
    public void init(IViewPart view)
    {
        this.view = view;
        this.window = view.getSite().getWorkbenchWindow();
    }

    public void init(IWorkbenchWindow window)
    {
        this.window = window;
    }

}
