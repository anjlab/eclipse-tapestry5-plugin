package com.anjlab.eclipse.tapestry5.hyperlink;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import com.anjlab.eclipse.tapestry5.EclipseUtils;
import com.anjlab.eclipse.tapestry5.Member;
import com.anjlab.eclipse.tapestry5.TapestryFile;
import com.anjlab.eclipse.tapestry5.TextEditorCallback;
import com.anjlab.eclipse.tapestry5.hyperlink.XmlFragment.XmlAtomicFragment;

public class MemberHyperlink implements IHyperlink
{
    private XmlAtomicFragment atomicFragment;
    private Member member;
    private TapestryFile tapestryFile;
    
    public MemberHyperlink(XmlAtomicFragment atomicFragment, TapestryFile tapestryFile, Member member)
    {
        this.atomicFragment = atomicFragment;
        this.member = member;
        this.tapestryFile = tapestryFile;
    }
    
    @Override
    public IRegion getHyperlinkRegion()
    {
        return atomicFragment.region;
    }

    @Override
    public String getTypeLabel()
    {
        return member.getName();
    }

    @Override
    public String getHyperlinkText()
    {
        return member.getName();
    }

    @Override
    public void open()
    {
        EclipseUtils.openFile(PlatformUI.getWorkbench().getActiveWorkbenchWindow(), tapestryFile, new TextEditorCallback()
        {
            @Override
            public void editorOpened(ITextEditor textEditor)
            {
                if (member.getNameRange() != null)
                {
                    textEditor.selectAndReveal(
                            member.getNameRange().getOffset(),
                            member.getNameRange().getLength());
                }
            }
        });
    }
}
