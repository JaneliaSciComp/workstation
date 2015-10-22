package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;

/**
 * Action to copy the contents of the status label to the clipboard.
 */
public class MicronsToClipboardAction extends AbstractAction {

    private JLabel statusLabel;

    public MicronsToClipboardAction(JLabel statusLabel) {
        this.statusLabel = statusLabel;
        putValue(Action.NAME, "Copy Micron Coords to Clipboard");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String content = statusLabel.getText();
        StringSelection selection = new StringSelection(content);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
    }
}
