package org.janelia.it.workstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.*;

import net.miginfocom.swing.MigLayout;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;

import org.janelia.it.workstation.gui.dialogs.ModalDialog;
/**
 * A dialog for entering a username to run as, for administrator use only. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RunAsUserDialog extends ModalDialog {

    private final JPanel mainPanel;
    private final JLabel usernameLabel;
    private final JTextField usernameField;

    public RunAsUserDialog() {

        setTitle("Run As User");
        
        mainPanel = new JPanel(new MigLayout("wrap 2"));
        
        usernameLabel = new JLabel("User Name");
        usernameField = new JTextField(20);
        
        mainPanel.add(usernameLabel);
        mainPanel.add(usernameField);
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Close without saving changes");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        AbstractAction okAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveAndClose();
            }
        };
        
        JButton okButton = new JButton("OK");
        okButton.setToolTipText("Close and save changes");
        okButton.addActionListener(okAction);
                
        getRootPane().setDefaultButton(okButton);
                
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(okButton);

        add(mainPanel, BorderLayout.CENTER);
        add(buttonPane, BorderLayout.SOUTH);
        
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                usernameField.selectAll();
            }
        });
    }

    public void showDialog() {
       String runAsUser = (String) getModelProperty(AccessManager.RUN_AS_USER, "");
       // usernameField.setText(runAsUser);
        packAndShow();
    }

    private void saveAndClose() {

        String runAsUser = usernameField.getText().trim();
        
        final SessionMgr sessionMgr = SessionMgr.getSessionMgr();
        sessionMgr.setModelProperty(AccessManager.RUN_AS_USER, runAsUser);
        
        boolean runAsSuccess = AccessManager.getAccessManager().setRunAsUser(runAsUser);
        
        if (false) {
            Object[] options = { "Fix username", "Cancel" };
            final int answer = JOptionPane.showOptionDialog(null, 
                    "User does not exist.", "Username Invalid",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (answer != 0) {
                sessionMgr.setModelProperty(AccessManager.RUN_AS_USER, "");
                setVisible(false);
            }
        }
        else {
            setVisible(false);
        }
    }
    
    private Object getModelProperty(String key, Object defaultValue) {
        final SessionMgr sessionMgr = SessionMgr.getSessionMgr();
        Object value = sessionMgr.getModelProperty(key);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }
}
