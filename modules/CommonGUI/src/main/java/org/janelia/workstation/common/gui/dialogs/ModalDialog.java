package org.janelia.workstation.common.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.common.gui.util.UIUtils;

/**
 * Base class for dialogs holds common functionality for dialog boxes.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ModalDialog extends JDialog {

    public ModalDialog() {
        super(FrameworkAccess.getMainFrame());
        init();
    }
    
    public ModalDialog(Dialog parent) {
        super(parent == null ? FrameworkAccess.getMainFrame() : parent);
        init();
    }
    
    public ModalDialog(Window parent) {
        super(parent == null ? FrameworkAccess.getMainFrame() : parent);
        init();
    }
    
    public ModalDialog(Frame parent) {
        super(parent == null ? FrameworkAccess.getMainFrame() : parent);
        init();
    }

    public ModalDialog(JPanel panel) {
        // TODO: this only works with JPanels within a Dialog
        super(UIUtils.getAncestorWithType(panel, Dialog.class));
        init();
    }
    
    private void init() {
        
        setModalityType(ModalityType.APPLICATION_MODAL);
        getContentPane().setLayout(new BorderLayout());

        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                setVisible(false);
            }
        });
    }
    
    protected void packAndShow() {
        SwingUtilities.updateComponentTreeUI(this);
        pack();
        setLocationRelativeTo(getParent());
        setVisible(true);
        // Avoid leaking memory from JNI references
        dispose();
    }
}
