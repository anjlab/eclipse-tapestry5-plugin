package com.anjlab.eclipse.tapestry5.actions;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.actions.OpenNewClassWizardAction;
import org.eclipse.jdt.ui.wizards.NewClassWizardPage;
import org.eclipse.jface.action.Action;

public class NewJavaClassWizardAction extends Action
{
    private IProject project;
    
    private String sourceFolder;
    private String packageName;
    private String typeName;
    
    public NewJavaClassWizardAction(IProject project)
    {
        this.project = project;
        setImageDescriptor(JavaUI.getSharedImages().getImageDescriptor(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_CLASS));
    }
    
    public void run()
    {
        NewClassWizardPage wizardPage = new NewClassWizardPage();
        
        IJavaProject javaProject = JavaCore.create(project);
        
        IFolder folder = project.getFolder(getSourceFolder());
        IPackageFragmentRoot sourceFolder = javaProject.getPackageFragmentRoot(folder);
        wizardPage.setPackageFragmentRoot(sourceFolder, true);
        
        IPackageFragment pkg = sourceFolder.getPackageFragment(getPackageName());
        wizardPage.setPackageFragment(pkg, true);
        
        wizardPage.setTypeName(getTypeName() == null ? "" : getTypeName(), true);
        
        OpenNewClassWizardAction action = new OpenNewClassWizardAction();
        action.setConfiguredWizardPage(wizardPage);
        action.run();
    }
    
    public String getSourceFolder()
    {
        return sourceFolder;
    }
    
    public void setSourceFolder(String sourceFolder)
    {
        this.sourceFolder = sourceFolder;
    }
    
    public String getPackageName()
    {
        return packageName;
    }
    
    public void setPackageName(String packageName)
    {
        this.packageName = packageName;
    }
    
    public String getTypeName()
    {
        return typeName;
    }
    
    public void setTypeName(String typeName)
    {
        this.typeName = typeName;
    }
}