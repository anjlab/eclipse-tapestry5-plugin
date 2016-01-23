package com.anjlab.eclipse.tapestry5.actions;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.ui.IWorkbenchWindow;

import com.anjlab.eclipse.tapestry5.TapestryProject;
import com.anjlab.eclipse.tapestry5.TapestryUtils;
import com.anjlab.eclipse.tapestry5.templates.ProjectSettings;

public class EditProjectSettingsAction extends OpenFileInTargetFolderAction
{
    private TapestryProject tapestryProject;

    public EditProjectSettingsAction(IWorkbenchWindow window, TapestryProject tapestryProject)
    {
        super(window,
                tapestryProject.getProject()
                    .getFolder(TapestryUtils.SRC_MAIN_ECLIPSE_TAPESTRY5),
                ProjectSettings.CONFIG_FILE_NAME);

        this.tapestryProject = tapestryProject;
    }

    @Override
    protected InputStream getInitialContents()
    {
        try
        {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            ProjectSettings.getDefaultContents(tapestryProject),
                            "UTF-8"));

            // Copy configuration settings without exposing
            // implementation details

            try
            {
                StringBuilder contents = new StringBuilder();

                boolean skip = false;
                String line;
                while ((line = reader.readLine()) != null)
                {
                    if (line.contains("Implementation details begin"))
                    {
                        skip = true;
                    }
                    else if (line.contains("Implementation details end"))
                    {
                        skip = false;
                    }
                    else
                    {
                        if (skip)
                        {
                            continue;
                        }

                        if (contents.length() > 0)
                        {
                            contents.append('\n');
                        }

                        contents.append(line);
                    }
                }

                return new ByteArrayInputStream(contents.toString().getBytes());
            }
            finally
            {
                reader.close();
            }
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Error reading default settings", e);
        }
    }

}
