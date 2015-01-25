package com.anjlab.eclipse.tapestry5.watchdog;

import org.eclipse.core.resources.IFile;

public interface IEclipseClasspathListener
{
    void classpathChanged(IFile classpath);
}
