package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;

public class SwcExport {

    /** Somewhat complex interaction with file chooser. */
    public ExportParameters getExportParameters( String seedName ) throws HeadlessException {
        AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save SWC file");
        chooser.setSelectedFile(new File(seedName + AnnotationModel.STD_SWC_EXTENSION));
        JPanel layoutPanel = new JPanel();
        layoutPanel.setLayout(new BorderLayout());
        // Force-out to desired size.
        JTextField downsampleModuloField = new JTextField("10");
        final Dimension dimension = new Dimension(80, 40);
        downsampleModuloField.setMinimumSize( dimension );
        downsampleModuloField.setSize( dimension );
        downsampleModuloField.setPreferredSize( dimension );
        layoutPanel.add( downsampleModuloField, BorderLayout.SOUTH );

        final TitledBorder titledBorder = new TitledBorder( 
                new EmptyBorder(8, 2, 0, 0), "Density", TitledBorder.LEADING, TitledBorder.DEFAULT_POSITION, layoutPanel.getFont().deriveFont(8)
        );
        downsampleModuloField.setBorder(titledBorder);
        downsampleModuloField.setToolTipText("Only every Nth autocomputed point will be exported.");
        downsampleModuloField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent ke) {
                if (!Character.isDigit(ke.getKeyChar())) {
                    // Eliminate non-numeric characters, including signs.
                    ke.consume();
                }
            }
        });
        chooser.setAccessory(layoutPanel);
        int returnValue = chooser.showSaveDialog(FrameworkImplProvider.getMainFrame());
        final String textInput = downsampleModuloField.getText().trim();
        
        ExportParameters rtnVal = null;
        try {
            int downsampleModulo = Integer.parseInt(textInput);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                rtnVal = new ExportParameters();
                rtnVal.setDownsampleModulo(downsampleModulo);
                rtnVal.setSelectedFile(chooser.getSelectedFile().getAbsoluteFile());
            }
        } catch (NumberFormatException nfe) {
            annotationMgr.presentError("Failed to parse input text as number: " + textInput, "Invalid Downsample");
            JOptionPane.showMessageDialog(FrameworkImplProvider.getMainFrame(), nfe);
        }
        return rtnVal;
    }

    public class ExportParameters {
        private File selectedFile;
        private int downsampleModulo;

        public File getSelectedFile() { return selectedFile; }
        public void setSelectedFile(File selectedFile) {
            this.selectedFile = selectedFile;
        }

        public int getDownsampleModulo() { return downsampleModulo; }
        public void setDownsampleModulo(int downsampleModulo) {
            this.downsampleModulo = downsampleModulo;
        }
    }
}
