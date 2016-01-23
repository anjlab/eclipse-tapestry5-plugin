package com.anjlab.eclipse.tapestry5.actions;

import java.io.InputStream;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
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
import com.anjlab.eclipse.tapestry5.TapestryProject;
import com.anjlab.eclipse.tapestry5.TapestryUtils;
import com.anjlab.eclipse.tapestry5.templates.ProjectSettings;
import com.anjlab.eclipse.tapestry5.templates.TapestryTemplates;
import com.anjlab.eclipse.tapestry5.views.context.TapestryContextView;
import com.google.common.base.CaseFormat;

public class CreateActionViewDelegate implements IViewActionDelegate, IMenuCreator
{
    private static final String DEFAULT_RESOURCES_SOURCE_FOLDER = "src/main/resources/";

    private static final String DEFAULT_JAVA_SOURCE_FOLDER = "src/main/java";

    private IWorkbenchWindow window;
    private IViewPart view;

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
        newFile.setFolder(DEFAULT_RESOURCES_SOURCE_FOLDER
                + tapestryContext.getPackageName().replaceAll("\\.", "/"));

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

    private IAction newEditProjectSettingsAction(Menu menu, TapestryProject tapestryProject, ProjectSettings settings)
    {
        OpenFileInTargetFolderAction editSettings =
                new EditProjectSettingsAction(window, tapestryProject);

        editSettings.setText("Project Settings...");

        return addActionToMenu(menu, editSettings);
    }

    private void newEditDefaultTemplatesAction(Menu menu, TapestryProject project, ProjectSettings settings)
    {
        MenuItem editCascade = new MenuItem(menu, SWT.CASCADE);
        editCascade.setText("Edit Templates");

        Menu editMenu = new Menu(menu);
        editCascade.setMenu(editMenu);

        TapestryTemplates tapestryTemplates = TapestryTemplates.get(project);

        IFolder templatesDir = project.getProject()
                .getFolder(TapestryUtils.SRC_MAIN_ECLIPSE_TAPESTRY5);

        addTemplateToMenu(editMenu, tapestryTemplates, templatesDir, "page.tml");
        addTemplateToMenu(editMenu, tapestryTemplates, templatesDir, "page.js");
        addTemplateToMenu(editMenu, tapestryTemplates, templatesDir, "page.css");
        addTemplateToMenu(editMenu, tapestryTemplates, templatesDir, "page.properties");

        new MenuItem(editMenu, SWT.SEPARATOR);

        addTemplateToMenu(editMenu, tapestryTemplates, templatesDir, "component.tml");
        addTemplateToMenu(editMenu, tapestryTemplates, templatesDir, "component.js");
        addTemplateToMenu(editMenu, tapestryTemplates, templatesDir, "component.css");
        addTemplateToMenu(editMenu, tapestryTemplates, templatesDir, "component.properties");

        new MenuItem(editMenu, SWT.SEPARATOR);

        addTemplateToMenu(editMenu, tapestryTemplates, templatesDir, "mixin.js");
        addTemplateToMenu(editMenu, tapestryTemplates, templatesDir, "mixin.css");
        addTemplateToMenu(editMenu, tapestryTemplates, templatesDir, "mixin.properties");
    }

    private void addTemplateToMenu(
            Menu editMenu,
            TapestryTemplates tapestryTemplates,
            IFolder templatesDir,
            String fileName)
    {
        addActionToMenu(
                editMenu,
                new OpenFileInTargetFolderAction(
                        window,
                        templatesDir,
                        fileName)
                {
                    @Override
                    protected InputStream getInitialContents()
                    {
                        Path path = new Path(fileName);
                        
                        return tapestryTemplates.openTemplate(
                                null,
                                path.removeFileExtension().lastSegment(),
                                path.getFileExtension());
                    }
                }).setText(fileName);
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

        ProjectSettings settings;
        TapestryModule module;

        if (tapestryContext != null && !tapestryContext.isEmpty())
        {
            module = TapestryUtils.getTapestryModule(window, tapestryContext.getProject());

            settings = resolveProjectSettings(module);

            fillMenuForTapestryContext(menu, tapestryContext, settings);
        }
        else
        {
            IProject project = EclipseUtils.getProjectFromSelection(
                    EclipseUtils.getProjectExplorerSelection(window));

            module = TapestryUtils.getTapestryModule(window, project);

            settings = resolveProjectSettings(module);
        }

        fillMenuForTapestryModule(menu, module, settings);

        return menu;
    }

    private ProjectSettings resolveProjectSettings(TapestryModule module)
    {
        return TapestryUtils.readProjectSettings(module != null ? module.getProject() : null);
    }

    protected void fillMenuForTapestryContext(
            Menu menu, TapestryContext tapestryContext, ProjectSettings settings)
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

            // Use ProjectSettings#fileNamingConvention for suggested names
            // (except *.java, *.tml, and *.properties)

            for (String extension : extensions)
            {
                String fileNameWithoutExtension = tapestryContext.getName();
                String fileName = fileNameWithoutExtension + extension;
                switch (extension)
                {
                case ".tml":
                case ".properties":
                    // Case for these should be the same as context name: UpperCamel
                    break;
                default:
                    for (Entry<String, String> entry : settings.getFileNamingConventions().entrySet())
                    {
                        String regex = TapestryUtils.createRegexpFromGlob(entry.getKey());
                        if (Pattern.matches(regex, fileName))
                        {
                            CaseFormat format = toCaseFormat(entry.getValue());
                            fileNameWithoutExtension =
                                    CaseFormat.UPPER_CAMEL.to(
                                            format, fileNameWithoutExtension);
                            break;
                        }
                    }
                    break;
                }
                fileName = fileNameWithoutExtension + extension;
                newTextFileMenuItem(menu, tapestryContext, "Create " + fileName + "...", fileName)
                    .setEnabled(!tapestryContext.contains(fileName));
            }

            newTextFileMenuItem(menu, tapestryContext, "Create other...",
                    CaseFormat.UPPER_CAMEL.to(
                            toCaseFormat(settings.getFileNamingConventions().get("*.*")),
                            tapestryContext.getName()));
        }
    }

    private static CaseFormat toCaseFormat(String convention)
    {
        switch (convention)
        {
        case "UpperCamel":
            return CaseFormat.UPPER_CAMEL;
        case "lowerCamel":
            return CaseFormat.LOWER_CAMEL;
        case "lower_underscode":
            return CaseFormat.LOWER_UNDERSCORE;
        case "lower-hyphen":
            return CaseFormat.LOWER_HYPHEN;
        default:
            return CaseFormat.UPPER_CAMEL;
        }
    }

    protected void fillMenuForTapestryModule(Menu menu, TapestryModule module, ProjectSettings settings)
    {
        IProject project = module != null ? module.getEclipseProject() : null;

        if (module == null
                || module.isReadOnly()
                //  TODO Support creating classes for any non-ReadOnly TapestryModules,
                //  not only for the AppModule
                || !TapestryUtils.isTapestryProject(project))
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

        new MenuItem(menu, SWT.SEPARATOR);

        newEditDefaultTemplatesAction(menu, module.getProject(), settings);

        newEditProjectSettingsAction(menu, module.getProject(), settings);
    }

    @Override
    public Menu getMenu(Menu parent)
    {
        return null;
    }

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
