package org.janelia.it.workstation.shared.util.text_component;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


/**
 * Title:        Listener for Text Component Right Clicks.
 * Description:  Mouse Listener to present popup of text data transfer items.
 * NOTE: uses techniques echoed in borland classes included with JBuilder.
 */
public class DataTransferMouseListener extends MouseAdapter {
    // Establish actions for menu, and their labels
    private static Action CUT_ACTION;
    private static String CUT_ACTION_NAME = "Cut";
    private static Action COPY_ACTION;
    private static String COPY_ACTION_NAME = "Copy";
    private static Action PASTE_ACTION;
    private static String PASTE_ACTION_NAME = "Paste";
    private static Action CLEARALL_ACTION;
    private static Action SELECTALL_ACTION;

    static {
        CUT_ACTION = new DefaultEditorKit.CutAction();


        // CUT_ACTION.putValue(Action.SMALL_ICON, new ImageIcon(DataTransferMouseListener.class.getResource("image/cut.gif")));
        COPY_ACTION = new DefaultEditorKit.CopyAction();


        // COPY_ACTION.putValue(Action.SMALL_ICON, new ImageIcon(DataTransferMouseListener.class.getResource("image/copy.gif")));
        PASTE_ACTION = new DefaultEditorKit.PasteAction();


        // PASTE_ACTION.putValue(Action.SMALL_ICON, new ImageIcon(DataTransferMouseListener.class.getResource("image/paste.gif")));
        CLEARALL_ACTION = new ClearAllAction();


        // CLEARALL_ACTION.putValue(Action.SMALL_ICON, new ImageIcon(CLEARALL_ACTION.getClass().getResource("image/clearAll.gif")));
        SELECTALL_ACTION = new SelectAllAction();
    } // End initializer static block

    /**
     * Constructor sets itself as a listener on the widget.
     */
    public DataTransferMouseListener(JTextComponent lTextComponent) {
        if (lTextComponent == null) {
            throw new IllegalArgumentException("Restriction violated in constructing " + this.getClass().getName() + ": null text component");
        }

        lTextComponent.addMouseListener(this);
    } // End constructor

    //------------------------------------------IMPLEMENTATION OF MouseListener

    /**
     * Override of mouseReleased.  Here: to popup a menu, and nothing else.
     */
    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger() && (e.getSource() instanceof JTextComponent)) {
            JTextComponent component = (JTextComponent) e.getSource();
            displayMenuAt(e.getPoint(), component);
        } // Will present the menu.
    } // End method: mouseReleased

    /**
     * Override of mousePressed.  Here: to popup a menu, and nothing else.
     */
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger() && (e.getSource() instanceof JTextComponent)) {
            JTextComponent lComponent = (JTextComponent) e.getSource();
            displayMenuAt(e.getPoint(), lComponent);
        } // Will present the menu.
    } // End method: mousePressed

    //------------------------------------------HELPER METHODS

    /**
     * Shows the popup menu where user specified with click.
     */
    private void displayMenuAt(Point lPoint, JTextComponent lSource) {
        updateActions(lSource);
        lSource.grabFocus();
        createMenu(lSource).show(lSource, lPoint.x, lPoint.y);
    } // End method: displayMenuAt

    /**
     * Sets enablement of actions on menu according to state of widget.
     */
    private void updateActions(JTextComponent lSource) {
        CLEARALL_ACTION.setEnabled(lSource.isEditable());
        CUT_ACTION.setEnabled(lSource.isEditable());
        PASTE_ACTION.setEnabled(lSource.isEditable());
    } // End method: enableActions

    /**
     * Makes a menu, adding only meaningful items.
     */
    private JPopupMenu createMenu(JTextComponent lSource) {
        JPopupMenu lPopupMenu = new JPopupMenu();

        JMenuItem lMenuItem;

        // Cache a flag for text to be selected, cleared, etc.
        boolean lTextInBox = false;

        if ((lSource.getText() != null) && (lSource.getText().length() > 0)) {
            lTextInBox = true;
        }

        // Cache a flag for selected text in text component.
        boolean lTextIsSelected = false;

        if ((lSource.getSelectedText() != null) && (lSource.getSelectedText().length() > 0)) {
            lTextIsSelected = true;
        }

        // Cache a flag for whether anything exists in the clipboard.
        boolean lDataInClipboard = false;
        Object lClipboardContents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this);

        if (lClipboardContents != null) {
            lDataInClipboard = true;
        }

        if (lSource.isEditable()) {
            lMenuItem = lPopupMenu.add(CUT_ACTION);
            lMenuItem.setIcon(null);
            lMenuItem.setText(CUT_ACTION_NAME);

            if (!lTextIsSelected) {
                lMenuItem.setEnabled(false);
            }
        } // Editable by cut

        lMenuItem = lPopupMenu.add(COPY_ACTION);
        lMenuItem.setIcon(null);
        lMenuItem.setText(COPY_ACTION_NAME);

        if (!lTextIsSelected) {
            lMenuItem.setEnabled(false);
        }

        if (lSource.isEditable()) {
            lMenuItem = lPopupMenu.add(PASTE_ACTION);
            lMenuItem.setIcon(null);
            lMenuItem.setText(PASTE_ACTION_NAME);

            if (!lDataInClipboard) {
                lMenuItem.setEnabled(false);
            }
        } // Editable by paste

        lPopupMenu.addSeparator();

        if (lSource.isEditable()) {
            lMenuItem = lPopupMenu.add(CLEARALL_ACTION);
            lMenuItem.setIcon(null);

            if (!lTextInBox) {
                lMenuItem.setEnabled(false);
            }
        } // Editable by Clear All

        lMenuItem = lPopupMenu.add(SELECTALL_ACTION);

        if (!lTextInBox) {
            lMenuItem.setEnabled(false);
        }

        return lPopupMenu;
    } // End method: createMenu

    /**
     * Actions to clear text or select all text in the text component.
     */
    public static class ClearAllAction extends TextAction {
        public ClearAllAction() {
            super("Clear All");
            setEnabled(false);
        } // End constructor

        public void actionPerformed(ActionEvent e) {
            JTextComponent lTarget = getTextComponent(e);

            if ((lTarget != null) && lTarget.isEditable()) {
                try {
                    lTarget.getDocument().remove(0, lTarget.getDocument().getLength());
                } // End try block
                catch (BadLocationException ex) {
                    // if this happens, then something is wrong internally with
                    // the document, and we're in REAL SERIOUS trouble.
                    throw new IllegalStateException(ex.getMessage());
                } // End catch block
            } // Target usable.
        } // End method: actionPerformed
    } // End class: ClearAllAction

    public static class SelectAllAction extends TextAction {
        public SelectAllAction() {
            super("Select All");
        } // End constructor

        public void actionPerformed(ActionEvent e) {
            JTextComponent lTarget = getTextComponent(e);

            if (lTarget != null) {
                lTarget.setCaretPosition(0);
                lTarget.moveCaretPosition(lTarget.getDocument().getLength());
            } // Target usable
        } // End method: actionPerformed
    } // End class: SelectAllAction
} // End class: DataTransferMouseListener
