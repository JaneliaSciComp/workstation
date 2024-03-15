package org.janelia.workstation.controller.color_slider;

import org.janelia.workstation.controller.listener.ColorModelListener;
import org.janelia.workstation.controller.model.color.ChannelColorModel;
import org.janelia.workstation.controller.model.color.ImageColorModel;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.text.NumberFormat;

/**
 * this UI element is a drop-in replacement for UglyColorSlider, but it uses three
 * textual spin boxes instead of a slider with three thumbs
 */
public class TextColorSlider extends JPanel {

    protected int channelIndex;
    protected ImageColorModel imageColorModel;

    private boolean updatingFromModel = false; // flag to prevent recursion

    // probably not going to use but keep around in case:
    // private Color whiteColor = Color.white;

    // UI elements
    SpinnerModel blackSpinnerModel;
    JSpinner blackSpinner;

    SpinnerModel graySpinnerModel;
    JSpinner graySpinner;

    SpinnerModel whiteSpinnerModel;
    JSpinner whiteSpinner;

    JButton setButton;


    public TextColorSlider(int channelIndex, ImageColorModel imageColorModel) {
        this.channelIndex = channelIndex;
        this.imageColorModel = imageColorModel;

        setupUI();

        // do not update before UI is set up!!
        updateSliderValuesFromColorModel();

        imageColorModel.addColorModelListener(new ColorModelListener() {
            @Override
            public void colorModelChanged() {
                updateSliderValuesFromColorModel();
            }
        });
        blackSpinner.addChangeListener(e -> updateColorModelFromSliderValues());
        graySpinner.addChangeListener(e -> updateColorModelFromSliderValues());
        whiteSpinner.addChangeListener(e -> updateColorModelFromSliderValues());
    }

    private void setupUI() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        int maxData;
        if (channelIndex < imageColorModel.getChannelCount()) {
            maxData = imageColorModel.getChannel(channelIndex).getDataMax();
        } else {
            // pretend it's 8-bit
            maxData = 255;
        }

        Dimension size;

        add(new JLabel("Black:"));
        blackSpinnerModel = new SpinnerNumberModel(
                0,
                0,
                maxData,
                1
        );
        blackSpinner = new JSpinner(blackSpinnerModel);
        size = blackSpinner.getPreferredSize();
        size.setSize(90, size.getHeight());
        blackSpinner.setPreferredSize(size);
        blackSpinner.setMaximumSize(blackSpinner.getPreferredSize());
        blackSpinner.setEditor(new JSpinner.NumberEditor(blackSpinner,"#" ));
        add(blackSpinner);

        add(new JLabel("Gray:"));
        graySpinnerModel = new SpinnerNumberModel(
                0,
                0,
                maxData,
                1
        );
        graySpinner = new JSpinner(graySpinnerModel);
        size = graySpinner.getPreferredSize();
        size.setSize(90, size.getHeight());
        graySpinner.setPreferredSize(size);
        graySpinner.setMaximumSize(graySpinner.getPreferredSize());
        graySpinner.setEditor(new JSpinner.NumberEditor(graySpinner,"#" ));
        add(graySpinner);

        add(new JLabel("White:"));
        whiteSpinnerModel = new SpinnerNumberModel(
                0,
                0,
                maxData,
                1
                );
        whiteSpinner = new JSpinner(whiteSpinnerModel);
        size = whiteSpinner.getPreferredSize();
        size.setSize(90, size.getHeight());
        whiteSpinner.setPreferredSize(size);
        whiteSpinner.setMaximumSize(whiteSpinner.getPreferredSize());
        whiteSpinner.setEditor(new JSpinner.NumberEditor(whiteSpinner,"#" ));
        add(whiteSpinner);

        setButton = new JButton("Set");
        setButton.addActionListener(e -> updateColorModelFromSliderValues());
        add(setButton);
    }

    public int getBlackLevel() {
        return (int) blackSpinnerModel.getValue();
    }

    public void setBlackLevel(int blackLevel) {
        blackSpinnerModel.setValue(blackLevel);
    }

    public int getGrayLevel() {
        return (int) graySpinnerModel.getValue();
    }

    public void setGrayLevel(int grayLevel) {
        graySpinnerModel.setValue(grayLevel);
    }

    public int getWhiteLevel() {
        return (int) whiteSpinnerModel.getValue();
    }

    public void setWhiteLevel(int whiteLevel) {
        whiteSpinnerModel.setValue(whiteLevel);
    }

    public void setWhiteColor(Color color) {
        // we don't currently plan to indicate the color visually, but if we
        //  do, this is where we'd do the update if it changes
    }

    private void updateSliderValuesFromColorModel() {
        // adapted code from UglyColorSlider
        if (imageColorModel == null)
            return;
        if (imageColorModel.getChannelCount() <= channelIndex)
            return;

        updatingFromModel = true;

        ChannelColorModel ccm = imageColorModel.getChannel(channelIndex);
        setBlackLevel(ccm.getBlackLevel());
        setWhiteLevel(ccm.getWhiteLevel());
        // convert gamma to gray level
        float grayLevel = (float)Math.pow(0.5, 1.0/ccm.getGamma());
        grayLevel = getBlackLevel() + grayLevel*(getWhiteLevel() - getBlackLevel());
        setGrayLevel((int)Math.round(grayLevel));

        updatingFromModel = false;
    }

    private void updateColorModelFromSliderValues() {
        if (updatingFromModel) // don't change model while populating slider
            return;
        if (imageColorModel == null)
            return;
        if (imageColorModel.getChannelCount() <= channelIndex)
            return;
        ChannelColorModel ccm = imageColorModel.getChannel(channelIndex);
        ccm.setBlackLevel(getBlackLevel());
        ccm.setWhiteLevel(getWhiteLevel());

        // compute gamma from gray level (invert the formula from above)
        // gray =   black +    (0.5 ^ (1/gamma)) (white - black)
        // g - b = (1/2) ^ (1/gamma) (w - b)
        // (g - b) / (w - b) = (1/2) ^ (1/gamma)
        // ln(g-b / w-b) = (1/gamma) ln(1/2)
        // ln(w-b / g-b) / ln(2) = 1/gamma
        // ln(2) / ln(w-b / g-b) = gamma
        double ratio = (double) (getWhiteLevel() - getBlackLevel()) / (getGrayLevel() - getBlackLevel());
        double newGamma = Math.log(2.0) / (Math.log(ratio));
        ccm.setGamma(newGamma);
    }

}
