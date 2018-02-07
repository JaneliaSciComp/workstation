package org.janelia.it.workstation.browser.gui.colordepth;

import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;

import org.janelia.it.workstation.browser.gui.dialogs.ModalDialog;
import org.janelia.it.workstation.browser.gui.editor.SingleSelectionButton;
import org.janelia.it.workstation.browser.gui.lasso.ImageMaskingPanel;
import org.janelia.it.workstation.browser.gui.support.WrapLayout;

/**
 * Create a new mask for color depth searching. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MaskCreationDialog extends ModalDialog {

    private static final String THRESHOLD_LABEL_PREFIX = "Mask Threshold:";
    private static final int DEFAULT_THRESHOLD_VALUE = 100;

    private JPanel optionsPanel;
    private JSlider thresholdSlider;
    private JLabel thresholdLabel;
    private final JPanel thresholdPanel;
    private SingleSelectionButton<String> alignmentSpaceButton;
    private ImageMaskingPanel maskingPanel;
    private BufferedImage mask;
    private String alignmentSpace;
    private boolean isContinue = false;
    
    public MaskCreationDialog(BufferedImage image, List<String> alignmentSpaces, String selectedAlignmentSpace, boolean allowMasking) {

        this.alignmentSpace = selectedAlignmentSpace;
        
        this.optionsPanel = new JPanel(new WrapLayout(false, WrapLayout.LEFT, 15, 10));

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
        maskingPanel.setImage(image);
        maskingPanel.setOnContinue((BufferedImage mask) -> {
            
            if (alignmentSpace==null) {
                JOptionPane.showMessageDialog(MaskCreationDialog.this, "You need to select an alignment space.");
                return;
            }
            
            this.mask = mask;
            isContinue = true;
            setVisible(false);
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
    
    public int getThreshold() {
        return thresholdSlider.getValue();
    }

    public String getAlignmentSpace() {
        return alignmentSpace;
    }

    public BufferedImage getMask() {
        return mask;
    }

    private void setThreshold(int threshold) {
        if (thresholdSlider.getValue() != threshold) {
            thresholdSlider.setValue(threshold);
        }
        thresholdLabel.setText(String.format("%s %d", THRESHOLD_LABEL_PREFIX, threshold));
    }
}
