package com.anjlab.eclipse.tapestry5.watchdog;

import org.eclipse.core.resources.IFile;

import com.anjlab.eclipse.tapestry5.watchdog.WebXmlReader.WebXml;

public interface IWebXmlListener
{
    void webXmlChanged(IFile webXmlFile, WebXml webXml);
}
