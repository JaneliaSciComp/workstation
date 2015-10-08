package org.janelia.it.workstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;

import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;

/**
 * Base class for dialogs holds common functionality for dialog boxes.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ModalDialog extends JDialog {

    public ModalDialog() {
        super(SessionMgr.getMainFrame());
        init();
    }
    
    public ModalDialog(Dialog parent) {
        super(parent);
        init();
    }
    
    public ModalDialog(Window parent) {
        super(parent);
        init();
    }
    
    public ModalDialog(Frame parent) {
        super(parent);
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
    }
}
