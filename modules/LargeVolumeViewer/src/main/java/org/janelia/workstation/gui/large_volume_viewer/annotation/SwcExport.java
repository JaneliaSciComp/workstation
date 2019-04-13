package org.janelia.workstation.gui.large_volume_viewer.annotation;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;

public class SwcExport {

    private static Dimension dialogSize = new Dimension(1200, 800);

    private static Dimension getDialogSize() {
        return dialogSize;
    }

    private static void setDialogSize(Dimension dialogSize) {
        SwcExport.dialogSize = dialogSize;
    }

    public ExportParameters getExportParameters( String seedName ) throws HeadlessException {
        AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
        JFileChooser chooser = new JFileChooser(annotationMgr.getSwcDirectory());
        chooser.setDialogTitle("Save SWC file");
        chooser.setSelectedFile(new File(seedName + AnnotationModel.STD_SWC_EXTENSION));
        JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(10, 5, 10, 5));

        panel.setLayout(new GridBagLayout());

        // top label
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.PAGE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(10, 0, 0, 0);
        c.weightx = 1.0;
        c.weighty = 0.0;
        panel.add(new JLabel("Export options", JLabel.LEADING), c);

        // point density options
        JPanel densityPanel = new JPanel();
        densityPanel.setLayout(new BoxLayout(densityPanel, BoxLayout.LINE_AXIS));
        densityPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        densityPanel.add(new JLabel("Point density: ", JLabel.LEADING));

        JTextField downsampleModuloField = new JTextField("10");
        downsampleModuloField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent ke) {
                if (!Character.isDigit(ke.getKeyChar())) {
                    // Eliminate non-numeric characters, including signs.
                    ke.consume();
                }
            }
        });
        densityPanel.add(downsampleModuloField);

        JButton densityHelpButton = new JButton("Help");
        densityHelpButton.addActionListener(e -> JOptionPane.showMessageDialog(null,
                "0 = export no automatic points\n1 = export all automatic points\nn = export every nth automatic point\n\nAll manual points are always exported."));
        densityPanel.add(densityHelpButton);

        GridBagConstraints c2 = new GridBagConstraints();
        c2.gridx = 0;
        c2.gridy = GridBagConstraints.RELATIVE;
        c2.weighty = 0.0;
        c2.anchor = GridBagConstraints.PAGE_START;
        c2.fill = GridBagConstraints.BOTH;

        panel.add(densityPanel, c2);

        // export notes option
        JPanel notesPanel = new JPanel();
        notesPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        JCheckBox notesCheckBox = new JCheckBox("Export notes");
        notesCheckBox.setSelected(true);
        notesPanel.add(notesCheckBox);
        panel.add(notesPanel, c2);

        chooser.setPreferredSize(getDialogSize());
        chooser.setAccessory(panel);
        int returnValue = chooser.showSaveDialog(FrameworkAccess.getMainFrame());
        setDialogSize(chooser.getSize());

        final String textInput = downsampleModuloField.getText().trim();
        final boolean notesInput = notesCheckBox.isSelected();
        
        ExportParameters rtnVal = null;
        try {
            int downsampleModulo = Integer.parseInt(textInput);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                rtnVal = new ExportParameters();
                rtnVal.setDownsampleModulo(downsampleModulo);
                rtnVal.setSelectedFile(chooser.getSelectedFile().getAbsoluteFile());
                rtnVal.setExportNotes(notesInput);
                annotationMgr.setSwcDirectory(rtnVal.getSelectedFile().getParentFile());
            }
        } catch (NumberFormatException nfe) {
            annotationMgr.presentError("Failed to parse input text as number: " + textInput, "Invalid Downsample");
            JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(), nfe);
        }
        return rtnVal;
    }

    public class ExportParameters {
        private File selectedFile;
        private int downsampleModulo;
        private boolean exportNotes;

        public File getSelectedFile() { return selectedFile; }
        public void setSelectedFile(File selectedFile) {
            this.selectedFile = selectedFile;
        }

        public int getDownsampleModulo() { return downsampleModulo; }
        public void setDownsampleModulo(int downsampleModulo) {
            this.downsampleModulo = downsampleModulo;
        }

        public boolean getExportNotes() {
            return exportNotes;
        }
        public void setExportNotes(boolean exportNotes) {
            this.exportNotes = exportNotes;
        }

    }
}
