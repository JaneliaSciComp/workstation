package org.janelia.workstation.browser.gui.colordepth;

import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collection;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;

import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.workstation.browser.gui.editor.SingleSelectionButton;
import org.janelia.workstation.browser.gui.lasso.ImageMaskingPanel;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.common.gui.dialogs.ModalDialog;
import org.janelia.workstation.common.gui.support.WrapLayout;
import org.janelia.workstation.core.workers.IndeterminateProgressMonitor;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.model.domain.gui.cdmip.ColorDepthMask;
import org.janelia.model.domain.sample.Sample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create a new mask for color depth searching. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MaskCreationDialog extends ModalDialog {

    private static final Logger log = LoggerFactory.getLogger(MaskCreationDialog.class);
    
    private static final String THRESHOLD_LABEL_PREFIX = "Mask Threshold:";
    private static final int DEFAULT_THRESHOLD_VALUE = 100;

    private JPanel optionsPanel;
    private final JTextField maskNameField;
    private final JSlider thresholdSlider;
    private final JLabel thresholdLabel;
    private final JPanel thresholdPanel;
    private final SingleSelectionButton<String> alignmentSpaceButton;
    private final ImageMaskingPanel maskingPanel;
    
    private Sample sample;
    private BufferedImage originalImage;
    private String originalImagePath;
    private BufferedImage mask;
    private String alignmentSpace;
    private boolean isContinue = false;
    
    public MaskCreationDialog(BufferedImage originalImage, String originalImagePath, List<String> alignmentSpaces, 
            String selectedAlignmentSpace, String defaultMaskName, Sample sample, boolean allowMasking) {

        this.originalImage = originalImage;
        this.originalImagePath = originalImagePath;
        this.alignmentSpace = selectedAlignmentSpace;
        this.sample = sample;
        
        this.optionsPanel = new JPanel(new WrapLayout(false, WrapLayout.LEFT, 15, 10));

        this.maskNameField = new JTextField(40);
        maskNameField.setText(defaultMaskName);
        
        JPanel maskNamePanel = new JPanel(new BorderLayout());
        maskNamePanel.add(new JLabel("Mask name"), BorderLayout.NORTH);
        maskNamePanel.add(maskNameField, BorderLayout.CENTER);
        optionsPanel.add(maskNamePanel);
        
        thresholdLabel = new JLabel();
        thresholdSlider = new JSlider(1, 255);
        thresholdSlider.putClientProperty("Slider.paintThumbArrowShape", Boolean.TRUE);
        thresholdSlider.setFocusable(false);
        thresholdSlider.addChangeListener((ChangeEvent e) -> {
            setThreshold(thresholdSlider.getValue());
        });
        thresholdPanel = new JPanel(new BorderLayout());
        thresholdPanel.add(thresholdLabel, BorderLayout.NORTH);
        thresholdPanel.add(thresholdSlider, BorderLayout.CENTER);
        setThreshold(DEFAULT_THRESHOLD_VALUE);
                
        alignmentSpaceButton = new SingleSelectionButton<String>("Alignment Space") {
            
            @Override
            public Collection<String> getValues() {
                return alignmentSpaces;
            }

            @Override
            public String getSelectedValue() {
                return alignmentSpace;
            }
            
            @Override
            public void updateSelection(String value) {
                alignmentSpace = value;
            }
        };
        alignmentSpaceButton.update();

        optionsPanel.add(thresholdPanel);
        optionsPanel.add(alignmentSpaceButton);
        
        maskingPanel = new ImageMaskingPanel();
        if (!allowMasking) {
            maskingPanel.getMaskButton().setVisible(false);
            maskingPanel.getResetButton().setVisible(false);
        }
        maskingPanel.setImage(originalImage);
        maskingPanel.setOnContinue((BufferedImage mask) -> {
            this.mask = mask;
            upload();
            isContinue = true;
        });
        maskingPanel.setOnCancel((Void v) -> {
            setVisible(false);
        });

        add(optionsPanel, BorderLayout.NORTH);
        add(maskingPanel, BorderLayout.CENTER);    
    }
    
    public boolean showForMask() {
        packAndShow();
        return isContinue;
    }

    private BufferedImage getMask() {
        return mask;
    }
    
    private String getMaskName() {
        return maskNameField.getText();
    }
    
    private int getThreshold() {
        return thresholdSlider.getValue();
    }

    private String getAlignmentSpace() {
        return alignmentSpace;
    }

    private void setThreshold(int threshold) {
        if (thresholdSlider.getValue() != threshold) {
            thresholdSlider.setValue(threshold);
        }
        thresholdLabel.setText(String.format("%s %d", THRESHOLD_LABEL_PREFIX, threshold));
    }
    
    private void upload() {

        final BufferedImage mask = getMask();
        final String maskName = getMaskName();
        final String alignmentSpace = getAlignmentSpace();
        final int threshold = getThreshold();
        
        if (mask==null) {
            JOptionPane.showMessageDialog(MaskCreationDialog.this, "You need to mask an area first.");
            return;
        }
        
        if (StringUtils.isBlank(maskName)) {
            JOptionPane.showMessageDialog(MaskCreationDialog.this, "You must specify a name for your mask");
            return;
        }
        
        if (alignmentSpace==null) {
            JOptionPane.showMessageDialog(MaskCreationDialog.this, "You need to select an alignment space.");
            return;
        }
        
        SimpleWorker worker = new SimpleWorker()     {

            private ColorDepthMask colorDepthMask;
            
            @Override
            protected void doStuff() throws Exception {

                String uploadPath;
                if (mask.equals(originalImage) && !StringUtils.isBlank(originalImagePath)) {
                    uploadPath = originalImagePath;
                    log.info("Using existing mask: {}", uploadPath);
                }
                else {
                    // Write the mask to disk temporarily
                    // TODO: in the future, the uploader should support byte stream input
                    File tempFile = File.createTempFile("mask", ".png");
                    tempFile.deleteOnExit();
                    ImageIO.write(mask, "png", tempFile);
                    log.info("Wrote mask to temporary file: {}", tempFile);
                    uploadPath = MaskUtils.uploadMask(tempFile);
                    log.info("Uploaded mask to: ", uploadPath);
                }

                DomainModel model = DomainMgr.getDomainMgr().getModel();
                colorDepthMask = model.createColorDepthMask(maskName, alignmentSpace, uploadPath, threshold, sample);
            }

            @Override
            protected void hadSuccess() {
                setVisible(false);
                ColorDepthSearchDialog dialog = new ColorDepthSearchDialog();
                dialog.showForMask(colorDepthMask);
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
            }
        };

        worker.setProgressMonitor(new IndeterminateProgressMonitor(FrameworkAccess.getMainFrame(), "Uploading mask", ""));
        worker.execute();
        
    }
}
