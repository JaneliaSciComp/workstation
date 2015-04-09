package org.janelia.it.workstation.gui.dialogs;

import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Base class for dialogs holds common functionality for dialog boxes.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ModalDialog extends JDialog {

    public ModalDialog() {

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
        setLocationRelativeTo(SessionMgr.getMainFrame());
        setVisible(true);
    }

}
