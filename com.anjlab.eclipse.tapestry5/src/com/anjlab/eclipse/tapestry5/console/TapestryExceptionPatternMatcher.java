package com.anjlab.eclipse.tapestry5.console;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.console.IPatternMatchListenerDelegate;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;

import com.anjlab.eclipse.tapestry5.Activator;
import com.anjlab.eclipse.tapestry5.Asset;
import com.anjlab.eclipse.tapestry5.AssetException;
import com.anjlab.eclipse.tapestry5.AssetResolver;
import com.anjlab.eclipse.tapestry5.EclipseUtils;
import com.anjlab.eclipse.tapestry5.SetEditorCaretPositionLineColumn;
import com.anjlab.eclipse.tapestry5.TapestryFile;
import com.anjlab.eclipse.tapestry5.TapestryUtils;

public class TapestryExceptionPatternMatcher implements IPatternMatchListenerDelegate
{
    private TextConsole console;
    
    @Override
    public void connect(TextConsole console)
    {
        this.console = console;
    }

    @Override
    public void disconnect()
    {
        this.console = null;
    }

    private static final Pattern ASSET_PATH_PATTERN = Pattern.compile("at ([^\\],]+)");
    private static final Pattern LINE_NUMBER_PATTERN = Pattern.compile(", line (\\d+)");
    private static final Pattern COLUMN_NUMBER_PATTERN = Pattern.compile(", column (\\d+)");

    @Override
    public void matchFound(final PatternMatchEvent event)
    {
        try
        {
            console.addHyperlink(new IHyperlink()
            {
                
                @Override
                public void linkExited()
                {
                    
                }
                
                @Override
                public void linkEntered()
                {
                    
                }
                
                @Override
                public void linkActivated()
                {
                    String reference;
                    try
                    {
                        reference = console.getDocument().get(event.getOffset(), event.getLength());
                    }
                    catch (BadLocationException e)
                    {
                        return;
                    }
                    
                    Matcher matcher = ASSET_PATH_PATTERN.matcher(reference);
                    
                    if (!matcher.find())
                    {
                        //  This may only happen if regexp in plugin.xml is out-of-sync with ASSET_PATH_PATTERN
                        Activator.getDefault().logError("Unable to extract asset path from " + reference);
                        return;
                    }
                    
                    String assetPath = matcher.group(1);
                    
                    Asset asset = new Asset(assetPath);
                    
                    AssetResolver assetResolver;
                    
                    try
                    {
                        assetResolver = TapestryUtils.createAssetResolver(asset.bindingPrefix);
                    }
                    catch (AssetException e)
                    {
                        Activator.getDefault().logError(e.getMessage(), e);
                        return;
                    }
                    
                    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                    
                    TapestryFile file = assetResolver.resolveInWorkspace(asset.path);
                    
                    if (file == null)
                    {
                        EclipseUtils.openInformation(window, "Source not found for '" + assetPath + "'");
                        return;
                    }
                    
                    matcher = LINE_NUMBER_PATTERN.matcher(reference);
                    
                    int line = -1;
                    int column = 0;
                    
                    if (matcher.find())
                    {
                        line = Integer.parseInt(matcher.group(1));
                    }
                    
                    matcher = COLUMN_NUMBER_PATTERN.matcher(reference);
                    
                    if (matcher.find())
                    {
                        column = Integer.parseInt(matcher.group(1)) - 1;
                    }
                    
                    EclipseUtils.openFile(window, file, new SetEditorCaretPositionLineColumn(line, column));
                }
            },
            event.getOffset() + "[at ".length(), event.getLength() - "[ at".length() - "]".length());
        }
        catch (BadLocationException e)
        {
            //  Do nothing
        }
    }

}
