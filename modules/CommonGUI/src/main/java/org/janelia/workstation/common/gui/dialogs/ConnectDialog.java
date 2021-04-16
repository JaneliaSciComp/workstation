package org.janelia.workstation.common.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;

import javax.swing.*;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.StringUtils;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.common.gui.support.SmartTextField;
import org.janelia.workstation.core.api.ConnectionMgr;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.lifecycle.ConnectionEvent;
import org.janelia.workstation.core.events.lifecycle.LocalProjectSelected;
import org.janelia.workstation.core.model.ConnectionResult;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A dialog for connecting to a Workstation server (i.e. API Gateway).
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ConnectDialog extends ModalDialog {

    private static final Logger log = LoggerFactory.getLogger(ConnectDialog.class);
    private static final String OK_BUTTON_TEXT = "Connect";
    private static final String HINT_TEXT = "<html>Which server would you like to connect to? This is usually the " +
            "same as the website where you downloaded the Workstation installer.</html>";
    private static final String CONNECTION_STRING_PREF = "connectionString";
    private static final String CONNECTION_STRING_LOCAL = "local";

    private final SmartTextField connectionStringField;
    private boolean connectLocally = false;
    private File projectDirectory;
    private final JCheckBox workstationLite;
    private final JPanel projectDirPanel = new JPanel();
    private final JFileChooser projectDir = new JFileChooser();
    private final JLabel projectDirLabel = new JLabel("Not Selected");
    private final JLabel errorLabel;
    private final JButton okButton;

    public ConnectDialog() {

        setTitle("Connect to Workstation Server");

        JPanel mainPanel = new JPanel(new MigLayout(
                "gap 5, fill, wrap 2",  // Layout constraints
                "[grow 0]5[grow 10]",  // Column constraints
                "[grow 0]10[grow 0]10[grow 75, fill]")); // Row constraints

        this.connectionStringField = new SmartTextField("CONNECT_HISTORY");

        errorLabel = new JLabel();
        setNoError();
        add(mainPanel, BorderLayout.CENTER);

        workstationLite = new JCheckBox("Use Workstation Locally");
        projectDirPanel.setLayout(new BoxLayout(projectDirPanel, BoxLayout.LINE_AXIS));
        projectDirPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        projectDirPanel.add(Box.createHorizontalGlue());

        projectDir.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        JButton projectDirButton = new JButton("Choose Project Dir");
        projectDirButton.addActionListener((e)-> {
            int saveDialogResult = projectDir.showSaveDialog(this);
            if (saveDialogResult != JFileChooser.APPROVE_OPTION)
                return;

            projectDirectory = projectDir.getSelectedFile();
            projectDirLabel.setText(projectDirectory.getPath());
        });
        projectDirPanel.add(projectDirLabel);
        projectDirPanel.add(projectDirButton);

        workstationLite.addActionListener((e) -> {
            if (workstationLite.isSelected()) {
                setLocal(true);
            } else {
                setLocal(false);
            }
        });

        mainPanel.add(new JLabel(HINT_TEXT), "span 2, al center top, width 100%");
        mainPanel.add(workstationLite,"span 2, al center top, width 100%");
        mainPanel.add(new JLabel("Connection URL"));
        mainPanel.add(connectionStringField, "width 300");
        mainPanel.add(new JLabel("Project Directory:"));
        mainPanel.add(projectDirPanel);
        mainPanel.add(errorLabel, "span 2, al center top, width 100%");

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Close without saving changes");
        cancelButton.addActionListener(e -> setVisible(false));

        okButton = new JButton(OK_BUTTON_TEXT);
        okButton.setToolTipText("Connect to the specified server");
        okButton.addActionListener(e -> saveAndClose());

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
                if (StringUtils.isEmpty(connectionStringField.getText())) {
                    connectionStringField.requestFocus();
                }
                else {
                    okButton.requestFocus();
                }
            }
        });
    }

    public void showDialog() {
        showDialog(null);
    }

    public void showDialog(ConnectionResult result) {

        if (isVisible()) {
            // The singleton dialog is already showing, just bring it to the front
            log.info("Connection dialog already visible");
            toFront();
            repaint();
            return;
        }

        if (result == null) {
            log.info("Showing connect dialog");
        }
        else {
            log.info("Showing connect dialog with errorText={}", result.getErrorText());
        }

        if (result == null || result.getErrorText()==null) {
            setNoError();
        }
        else {
            setError(result.getErrorText());
        }

        okButton.setIcon(null);
        okButton.setText(OK_BUTTON_TEXT);

        String connectionString = ConnectionMgr.getConnectionMgr().getConnectionString();
        if (connectionString!=null && connectionString.equals(CONNECTION_STRING_LOCAL)) {
            setLocal(true);
        } else {
            setLocal(false);
        }
        packAndShow();
    }

    private void setLocal(boolean local) {
        connectLocally = local;
        connectionStringField.setVisible(!local);
        projectDirPanel.setVisible(local);
    }

    private void saveAndClose() {

        okButton.setIcon(Icons.getLoadingIcon());
        okButton.setText(null);
        setNoError();

        if (connectLocally) {
            FrameworkAccess.setLocalPreferenceValue(ConnectionMgr.class,
                    CONNECTION_STRING_PREF, "local");

            Events.getInstance().postOnEventBus(new LocalProjectSelected());

        } else {
            String connectionString = connectionStringField.getText().trim();

            SimpleWorker worker = new SimpleWorker() {

                private ConnectionResult connectionResult;

                @Override
                protected void doStuff() throws Exception {
                    connectionResult = ConnectionMgr.getConnectionMgr().connect(connectionString);
                }

                @Override
                protected void hadSuccess() {
                    okButton.setIcon(null);
                    okButton.setText(OK_BUTTON_TEXT);
                    if (connectionResult.getErrorText() == null) {
                        connectionStringField.addCurrentTextToHistory();
                        setVisible(false);
                    } else {
                        setError(connectionResult.getErrorText());
                    }
                }

                @Override
                protected void hadError(Throwable e) {
                    log.error("Unknown connection error", e);
                    okButton.setIcon(null);
                    okButton.setText(OK_BUTTON_TEXT);
                    setError("Unknown connection error");
                }
            };

            worker.execute();
        }
    }

    private void setNoError() {
        errorLabel.setText("");
        errorLabel.setIcon(null);
    }

    private void setError(String errorText) {
        errorLabel.setText("<html>"+errorText+"</html>");
        errorLabel.setIcon(UIManager.getIcon("OptionPane.errorIcon"));
        pack();
    }
}
