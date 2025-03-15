package com.anjlab.eclipse.e4.tapestry5.handlers;

import org.eclipse.jdt.ui.actions.CustomFiltersActionGroup;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlExtension;
import org.eclipse.jface.text.IInformationControlExtension2;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchCommandConstants;

import com.anjlab.eclipse.tapestry5.Openable;
import com.anjlab.eclipse.tapestry5.views.NameSorter;
import com.anjlab.eclipse.tapestry5.views.TapestryDecoratingLabelProvider;
import com.anjlab.eclipse.tapestry5.views.TreeObject;
import com.anjlab.eclipse.tapestry5.views.TreeObjectDoubleClickListener;
import com.anjlab.eclipse.tapestry5.views.TreeParent;

/**
 * Inspired by org.eclipse.jdt.internal.ui.text.JavaOutlineInformationControl
 * 
 * @author dmitrygusev
 *
 */
public abstract class AbstractTapestryContextInformation extends PopupDialog
        implements IInformationControl, IInformationControlExtension,
        IInformationControlExtension2, DisposeListener
{

    protected interface ContentProviderCreator
    {
        ITreeContentProvider createContentProvider();
    }

    /**
     * The NamePatternFilter selects the elements which match the given string
     * patterns.
     *
     * @since 2.0
     */
    protected class NamePatternFilter extends ViewerFilter
    {

        /*
         * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.
         * viewers.Viewer, java.lang.Object, java.lang.Object)
         */
        @Override
        public boolean select(Viewer viewer, Object parentElement, Object element)
        {
            PatternMatcher matcher = getMatcher();

            if (matcher == null || !(viewer instanceof TreeViewer))
            {
                return true;
            }

            TreeViewer treeViewer = (TreeViewer) viewer;

            String matchName = getText(element, treeViewer);
            matchName = TextProcessor.deprocess(matchName);

            if (matchName != null
                    && matcher.matches(matchName)
                    && (isOpenable(element)
                            || !(element instanceof TreeParent)))
            {
                return true;
            }

            return hasUnfilteredChild(treeViewer, element);
        }

        private boolean isOpenable(Object element)
        {
            return ((TreeObject) element).getData() instanceof Openable;
        }

        private boolean hasUnfilteredChild(TreeViewer viewer, Object element)
        {
            if (element instanceof TreeParent)
            {
                Object[] children = ((ITreeContentProvider) viewer.getContentProvider())
                        .getChildren(element);

                for (int i = 0; i < children.length; i++)
                {
                    if (select(viewer, element, children[i]))
                    {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /** The control's text widget */
    private Text fFilterText;
    /** The control's tree widget */
    private TreeViewer fTreeViewer;
    /** The current string matcher */
    private PatternMatcher fPatternMatcher;
    /**
     * Fields that support the dialog menu
     * 
     * @since 3.0
     * @since 3.2 - now appended to framework menu
     */
    private Composite fViewMenuButtonComposite;
    private CustomFiltersActionGroup fCustomFiltersActionGroup;
    private IAction fShowViewMenuAction;
    private IDoubleClickListener gotoListener = new TreeObjectDoubleClickListener();
    private ContentProviderCreator contentProviderCreator;

    public AbstractTapestryContextInformation(Shell parent, ContentProviderCreator contentProviderCreator)
    {
        super(parent, SWT.RESIZE, true, true, false, true, true, null, null);

        this.contentProviderCreator = contentProviderCreator;

        // Title and status text must be set to get the title label created,
        // so force empty values here.
        if (hasHeader())
            setTitleText("");
        setInfoText("");

        // Create all controls early to preserve the life cycle of the original
        // implementation.
        create();

        // Status field text can only be computed after widgets are created.
        setInfoText(getStatusFieldText());
    }

    protected String getStatusFieldText()
    {
        return "";
    }

    protected String getText(Object element, TreeViewer treeViewer)
    {
        return ((org.eclipse.jface.viewers.DecoratingStyledCellLabelProvider) treeViewer
                .getLabelProvider()).getStyledStringProvider()
                        .getStyledText(element).toString();
    }

    /**
     * Fills the view menu. Clients can extend or override.
     *
     * @param viewMenu
     *            the menu manager that manages the menu
     * @since 3.0
     */
    protected void fillViewMenu(IMenuManager viewMenu)
    {
        fCustomFiltersActionGroup.fillViewMenu(viewMenu);
    }

    @Override
    protected void fillDialogMenu(IMenuManager dialogMenu)
    {
        super.fillDialogMenu(dialogMenu);
        fillViewMenu(dialogMenu);
    }

    protected PatternMatcher getMatcher()
    {
        return fPatternMatcher;
    }

    @Override
    protected Control createTitleMenuArea(Composite parent)
    {
        fViewMenuButtonComposite = (Composite) super.createTitleMenuArea(
                parent);
    
        // If there is a header, then the filter text must be created
        // underneath the title and menu area.
    
        if (hasHeader())
        {
            fFilterText = createFilterText(parent);
        }
    
        // Create show view menu action
        fShowViewMenuAction = new Action("showViewMenu") //$NON-NLS-1$
        {
            /*
             * @see org.eclipse.jface.action.Action#run()
             */
            @Override
            public void run()
            {
                showDialogMenu();
            }
        };
    
        fShowViewMenuAction.setEnabled(true);
        fShowViewMenuAction.setActionDefinitionId(IWorkbenchCommandConstants.WINDOW_SHOW_VIEW_MENU);
    
        return fViewMenuButtonComposite;
    }

    /**
     * Returns <code>true</code> if the control has a header, <code>false</code>
     * otherwise.
     * <p>
     * The default is to return <code>false</code>.
     * </p>
     *
     * @return <code>true</code> if the control has a header
     */
    protected boolean hasHeader()
    {
        // default is to have no header
        return false;
    }

    @Override
    protected Control createTitleControl(Composite parent)
    {
        if (hasHeader())
        {
            return super.createTitleControl(parent);
        }
        fFilterText = createFilterText(parent);
        return fFilterText;
    }

    @Override
    protected void setTabOrder(Composite composite)
    {
        if (hasHeader())
        {
            composite.setTabList(
                    new Control[] { fFilterText, fTreeViewer.getTree() });
        } else
        {
            fViewMenuButtonComposite.setTabList(new Control[] { fFilterText });
            composite.setTabList(new Control[] { fViewMenuButtonComposite,
                    fTreeViewer.getTree() });
        }
    }

    protected Text createFilterText(Composite parent)
    {
        fFilterText = new Text(parent, SWT.NONE);
        Dialog.applyDialogFont(fFilterText);
    
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.horizontalAlignment = GridData.FILL;
        data.verticalAlignment = GridData.CENTER;
        fFilterText.setLayoutData(data);
    
        fFilterText.addKeyListener(new KeyListener()
        {
            public void keyPressed(KeyEvent e)
            {
                if (e.keyCode == 0x0D || e.keyCode == SWT.KEYPAD_CR)
                {
                    //  Enter key
                    gotoSelectedElement();
                }
                else if (e.keyCode == SWT.ARROW_DOWN)
                {
                    fTreeViewer.getTree().setFocus();
                }
                else if (e.keyCode == SWT.ARROW_UP)
                {
                    fTreeViewer.getTree().setFocus();
                }
                else if (e.character == 0x1B)
                {
                    // ESC
                    dispose();
                }
            }
    
            public void keyReleased(KeyEvent e)
            {
                // do nothing
            }
        });
    
        return fFilterText;
    }

    private void gotoSelectedElement()
    {
        ISelection selection = fTreeViewer.getSelection();
        if (selection != null && !selection.isEmpty())
        {
            DoubleClickEvent event = new DoubleClickEvent(fTreeViewer, selection);
            dispose();
            gotoListener.doubleClick(event);
        }
    }

    private void installFilter()
    {
        fFilterText.setText(""); //$NON-NLS-1$
    
        fFilterText.addModifyListener(new ModifyListener()
        {
            public void modifyText(ModifyEvent e)
            {
                String text = ((Text) e.widget).getText();
                setMatcherString(text, true);
            }
        });
    }

    /**
     * Sets the patterns to filter out for the receiver.
     *
     * @param pattern
     *            the pattern
     * @param update
     *            <code>true</code> if the viewer should be updated
     * 
     */
    protected void setMatcherString(String pattern, boolean update)
    {
        if (pattern.length() == 0)
        {
            fPatternMatcher = null;
        }
        else
        {
            fPatternMatcher = new SimplePatternMatcher(pattern);
        }
    
        if (update)
        {
            stringMatcherUpdated();
        }
    }

    /**
     * Selects the first element in the tree which matches the current filter
     * pattern.
     */
    protected void selectFirstMatch()
    {
        Object selectedElement = fTreeViewer.testFindItem(null);
        TreeItem element;
        final Tree tree = fTreeViewer.getTree();
        if (selectedElement instanceof TreeItem)
        {
            element = findElement(new TreeItem[] { (TreeItem) selectedElement });
        }
        else
        {
            element = findElement(tree.getItems());
        }
    
        if (element != null)
        {
            tree.setSelection(element);
            tree.showItem(element);
        }
        else
        {
            fTreeViewer.setSelection(StructuredSelection.EMPTY);
        }
    }

    private TreeItem findElement(TreeItem[] items)
    {
        return findElement(items, null, true);
    }

    private TreeItem findElement(TreeItem[] items, TreeItem[] toBeSkipped, boolean allowToGoUp)
    {
        if (fPatternMatcher == null)
        {
            return items.length > 0 ? items[0] : null;
        }
    
        // First search at same level
        for (int i = 0; i < items.length; i++)
        {
            final TreeItem item = items[i];
            Object element = item.getData();
            if (element != null)
            {
                String label = getText(element, fTreeViewer);
                if (fPatternMatcher.matches(label))
                {
                    return item;
                }
            }
        }
    
        // Go one level down for each item
        for (int i = 0; i < items.length; i++)
        {
            final TreeItem item = items[i];
            TreeItem foundItem = findElement(
                    selectItems(item.getItems(), toBeSkipped), null, false);
            if (foundItem != null)
            {
                return foundItem;
            }
        }
    
        if (!allowToGoUp || items.length == 0)
        {
            return null;
        }
    
        // Go one level up (parent is the same for all items)
        TreeItem parentItem = items[0].getParentItem();
        if (parentItem != null)
        {
            return findElement(new TreeItem[] { parentItem }, items, true);
        }
    
        // Check root elements
        return findElement(selectItems(items[0].getParent().getItems(), items), null, false);
    }

    private boolean canSkip(TreeItem item, TreeItem[] toBeSkipped)
    {
        if (toBeSkipped == null)
        {
            return false;
        }
    
        for (int i = 0; i < toBeSkipped.length; i++)
        {
            if (toBeSkipped[i] == item)
            {
                return true;
            }
        }
        return false;
    }

    private TreeItem[] selectItems(TreeItem[] items, TreeItem[] toBeSkipped)
    {
        if (toBeSkipped == null || toBeSkipped.length == 0)
        {
            return items;
        }
    
        int j = 0;
        for (int i = 0; i < items.length; i++)
        {
            TreeItem item = items[i];
            if (!canSkip(item, toBeSkipped))
            {
                items[j++] = item;
            }
        }
        if (j == items.length)
        {
            return items;
        }
    
        TreeItem[] result = new TreeItem[j];
        System.arraycopy(items, 0, result, 0, j);
        return result;
    }

    /**
     * The string matcher has been modified. The default implementation
     * refreshes the view and selects the first matched element
     */
    protected void stringMatcherUpdated()
    {
        // refresh viewer to re-filter
        fTreeViewer.getControl().setRedraw(false);
        fTreeViewer.refresh();
        fTreeViewer.expandAll();
        selectFirstMatch();
        fTreeViewer.getControl().setRedraw(true);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        fTreeViewer = createTreeViewer(parent);
    
        fCustomFiltersActionGroup = new CustomFiltersActionGroup(getId(), fTreeViewer);
    
        final Tree tree = fTreeViewer.getTree();
        tree.addKeyListener(new KeyListener()
        {
            public void keyPressed(KeyEvent e)
            {
                if (e.character == 0x1B)
                {
                    // ESC
                    dispose();
                }
            }
    
            public void keyReleased(KeyEvent e)
            {
                // do nothing
            }
        });
    
        tree.addSelectionListener(new SelectionListener()
        {
            public void widgetSelected(SelectionEvent e)
            {
                // do nothing
            }
    
            public void widgetDefaultSelected(SelectionEvent e)
            {
                gotoSelectedElement();
            }
        });
    
        tree.addMouseMoveListener(new MouseMoveListener()
        {
            TreeItem fLastItem = null;
    
            public void mouseMove(MouseEvent e)
            {
                if (tree.equals(e.getSource()))
                {
                    Object o = tree.getItem(new Point(e.x, e.y));
                    if (fLastItem == null ^ o == null)
                    {
                        tree.setCursor(o == null
                                ? null
                                : tree.getDisplay()
                                        .getSystemCursor(SWT.CURSOR_HAND));
                    }
                    if (o instanceof TreeItem)
                    {
                        Rectangle clientArea = tree.getClientArea();
                        if (!o.equals(fLastItem))
                        {
                            fLastItem = (TreeItem) o;
                            tree.setSelection(new TreeItem[] { fLastItem });
                        }
                        else if (e.y - clientArea.y < tree.getItemHeight() / 4)
                        {
                            // Scroll up
                            Point p = tree.toDisplay(e.x, e.y);
                            Item item = fTreeViewer.scrollUp(p.x, p.y);
                            if (item instanceof TreeItem)
                            {
                                fLastItem = (TreeItem) item;
                                tree.setSelection(new TreeItem[] { fLastItem });
                            }
                        }
                        else if (clientArea.y + clientArea.height - e.y < tree.getItemHeight() / 4)
                        {
                            // Scroll down
                            Point p = tree.toDisplay(e.x, e.y);
                            Item item = fTreeViewer.scrollDown(p.x, p.y);
                            if (item instanceof TreeItem)
                            {
                                fLastItem = (TreeItem) item;
                                tree.setSelection(new TreeItem[] { fLastItem });
                            }
                        }
                    }
                    else if (o == null)
                    {
                        fLastItem = null;
                    }
                }
            }
        });
    
        tree.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseUp(MouseEvent e)
            {
    
                if (tree.getSelectionCount() < 1)
                    return;
    
                if (e.button != 1)
                    return;
    
                if (tree.equals(e.getSource()))
                {
                    Object o = tree.getItem(new Point(e.x, e.y));
                    TreeItem selection = tree.getSelection()[0];
                    if (selection.equals(o))
                    {
                        gotoSelectedElement();
                    }
                }
            }
        });
    
        installFilter();
    
        addDisposeListener(this);
        return fTreeViewer.getControl();
    }

    private String getId()
    {
        return TapestryContextInformationControl.class.getName();
    }

    public void addDisposeListener(DisposeListener listener)
    {
        getShell().addDisposeListener(listener);
    }

    protected TreeViewer createTreeViewer(Composite parent)
    {
        int style = SWT.V_SCROLL | SWT.H_SCROLL;
    
        TreeViewer treeViewer = new TreeViewer(parent,
                SWT.SINGLE | (style & ~SWT.MULTI));
    
        // Hard-coded filters
        treeViewer.addFilter(new NamePatternFilter());
    
        treeViewer.setContentProvider(getContentProvider());
        treeViewer.setLabelProvider(new TapestryDecoratingLabelProvider());
        treeViewer.setComparator(new NameSorter());
        treeViewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);
        treeViewer.setInput(this);
    
        Tree tree = treeViewer.getTree();
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = tree.getItemHeight() * 12;
        tree.setLayoutData(gd);
    
        return treeViewer;
    }

    /**
     * {@inheritDoc}
     * 
     * @param event
     *            can be null
     *            <p>
     *            Subclasses may extend.
     *            </p>
     */
    public void widgetDisposed(DisposeEvent event)
    {
        fTreeViewer = null;
        fFilterText = null;
    }

    @Override
    public void setInput(Object input)
    {
        if (input != null)
        {
            fTreeViewer.setSelection(
                    new StructuredSelection(new TreeObject("", input)));
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setInformation(String information)
    {
        // this method is ignored, see IInformationControlExtension2
    }

    /**
     * {@inheritDoc}
     */
    public void setVisible(boolean visible)
    {
        if (visible)
        {
            open();
        }
        else
        {
            saveDialogBounds(getShell());
            getShell().setVisible(false);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasContents()
    {
        return fTreeViewer != null && fTreeViewer.getInput() != null;
    }

    /**
     * {@inheritDoc}
     */
    public void setSizeConstraints(int maxWidth, int maxHeight)
    {
        // ignore
    }

    /**
     * {@inheritDoc}
     */
    public Point computeSizeHint()
    {
        // return the shell's size - note that it already has the persisted size
        // if persisting
        // is enabled.
        return getShell().getSize();
    }

    /**
     * {@inheritDoc}
     */
    public void setLocation(Point location)
    {
        /*
         * If the location is persisted, it gets managed by PopupDialog - fine.
         * Otherwise, the location is computed in Window#getInitialLocation,
         * which will center it in the parent shell / main monitor, which is
         * wrong for two reasons: - we want to center over the editor / subject
         * control, not the parent shell - the center is computed via the
         * initalSize, which may be also wrong since the size may have been
         * updated since via min/max sizing of
         * AbstractInformationControlManager. In that case, override the
         * location with the one computed by the manager. Note that the call to
         * constrainShellSize in PopupDialog.open will still ensure that the
         * shell is entirely visible.
         */
        if (!getPersistLocation() || getDialogSettings() == null)
        {
            getShell().setLocation(location);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setSize(int width, int height)
    {
        getShell().setSize(width, height);
    }

    /**
     * {@inheritDoc}
     */
    public void removeDisposeListener(DisposeListener listener)
    {
        getShell().removeDisposeListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    public void setForegroundColor(Color foreground)
    {
        applyForegroundColor(foreground, getContents());
    }

    /**
     * {@inheritDoc}
     */
    public void setBackgroundColor(Color background)
    {
        applyBackgroundColor(background, getContents());
    }

    /**
     * {@inheritDoc}
     */
    public boolean isFocusControl()
    {
        return getShell().getDisplay().getActiveShell() == getShell();
    }

    /**
     * {@inheritDoc}
     */
    public void setFocus()
    {
        getShell().forceFocus();
        fFilterText.setFocus();
    }

    /**
     * {@inheritDoc}
     */
    public void addFocusListener(FocusListener listener)
    {
        getShell().addFocusListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    public void removeFocusListener(FocusListener listener)
    {
        getShell().removeFocusListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    public final void dispose()
    {
        close();
    }

    private ITreeContentProvider getContentProvider()
    {
        return contentProviderCreator.createContentProvider();
    }

}