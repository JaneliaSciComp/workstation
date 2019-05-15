package org.janelia.workstation.common.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;
import org.janelia.workstation.core.api.ConnectionMgr;


/**
 * A dialog for connecting to a Workstation server (i.e. API Gateway).
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ConnectDialog extends ModalDialog {

    private final JTextField connectionStringField;

    public ConnectDialog() {

        setTitle("Connect to Workstation Server");

        JPanel mainPanel = new JPanel(new MigLayout("wrap 2"));

        connectionStringField = new JTextField(20);
        
        mainPanel.add(new JLabel("Connection URL"));
        mainPanel.add(connectionStringField);
        
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
        
        JButton okButton = new JButton("Connect");
        okButton.setToolTipText("Connect to the specified server");
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
                connectionStringField.selectAll();
            }
        });
    }

    public void showDialog() {
        String connectionString = ConnectionMgr.getConnectionMgr().getConnectionString();
        connectionStringField.setText(connectionString);
        packAndShow();
    }

    private void saveAndClose() {

        String connectionString = connectionStringField.getText().trim();

        // Test to make sure its a valid URL
        try {
            new URL(connectionString);
        }
        catch (MalformedURLException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Invalid URL specified",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        ConnectionMgr.getConnectionMgr().setConnectionString(connectionString);
        setVisible(false);
    }
}
