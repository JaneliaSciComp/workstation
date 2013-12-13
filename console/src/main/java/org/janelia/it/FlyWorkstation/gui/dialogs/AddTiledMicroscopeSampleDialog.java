package org.janelia.it.FlyWorkstation.gui.dialogs;

import org.janelia.it.FlyWorkstation.shared.util.ConsoleProperties;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.jdesktop.swingx.VerticalLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;

/**
 * Created with IntelliJ IDEA.
 * User: saffordt
 * Date: 3/19/13
 * Time: 3:13 PM
 */
public class AddTiledMicroscopeSampleDialog extends ModalDialog {

    private static final String TOP_LEVEL_FOLDER_NAME = "Alignment Boards";
    private static final String TOOLTIP_INPUT_FILE      = "Root directory of the sample";
    private final JTextField pathTextField;
    private JFileChooser fileChooser;

    public AddTiledMicroscopeSampleDialog() {
        setTitle("Add Tiled Microscope Sample");

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new VerticalLayout(5));

        JPanel attrPanel = new JPanel();
        attrPanel.setLayout(new GridBagLayout());
        attrPanel.setBorder(BorderFactory.createTitledBorder("Basic Options"));

        GridBagConstraints c = new GridBagConstraints();
        c.ipadx = 5;
        c.gridx = 0; c.gridy = 0;
        String pathText = ConsoleProperties.getString("remote.defaultLinuxPath");
        pathTextField = new JTextField(40);
        pathTextField.setText(pathText);
        setSize(400, 400);
        File pathTest = new File(pathText);
        c.gridx = 0; c.gridy = 1;
        JLabel pathLabel = new JLabel("Sample Directory:");
        pathLabel.setToolTipText(TOOLTIP_INPUT_FILE);
        attrPanel.add(pathLabel, c);
        c.gridx = 1;
        attrPanel.add(pathTextField, c);
        if (pathTest.exists() && pathTest.canRead()) {
            fileChooser  = new JFileChooser(pathTest);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

            JButton _filePathButton = null;
            try {
                _filePathButton = new JButton(Utils.getClasspathImage("magnifier.png"));
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            if (_filePathButton != null) {
                _filePathButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        int returnVal = fileChooser.showOpenDialog(AddTiledMicroscopeSampleDialog.this);
                        if (returnVal == JFileChooser.APPROVE_OPTION) {
                            pathTextField.setText(fileChooser.getSelectedFile().getAbsolutePath());
                        }
                    }
                });
            }
            c.gridx = 2;
            attrPanel.add(_filePathButton, c);
        }

        mainPanel.add(attrPanel);
        add(mainPanel, BorderLayout.CENTER);

        JButton addButton = new JButton("Add");
        addButton.setToolTipText("Run the mask search");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addSample();
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Cancel and close this dialog");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(addButton);
        buttonPane.add(cancelButton);

        add(buttonPane, BorderLayout.SOUTH);
    }

    public void addSample() {
        final String inputDirPath = pathTextField.getText().trim();
        SimpleWorker executeWorker = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
//                startMaskSearch(inputDirPath, topLevelFolderName, matrixValue, queryChannel, maxHits, skipZeroes);
            }

            @Override
            protected void hadSuccess() {
                Utils.setDefaultCursor(AddTiledMicroscopeSampleDialog.this);
//                Browser browser = SessionMgr.getSessionMgr().getActiveBrowser();
//                browser.setPerspective(Perspective.TaskMonitoring);
                setVisible(false);
            }

            @Override
            protected void hadError(Throwable error) {
                error.printStackTrace();
                Utils.setDefaultCursor(AddTiledMicroscopeSampleDialog.this);
                JOptionPane.showMessageDialog(AddTiledMicroscopeSampleDialog.this,
                        "Error submitting job: "+error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        };

        executeWorker.execute();
    }

    public void showDialog() {
        packAndShow();
    }
}
