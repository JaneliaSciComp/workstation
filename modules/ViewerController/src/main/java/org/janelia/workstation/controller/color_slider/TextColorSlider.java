package org.janelia.workstation.controller.color_slider;

import org.janelia.workstation.controller.listener.ColorModelListener;
import org.janelia.workstation.controller.model.color.ChannelColorModel;
import org.janelia.workstation.controller.model.color.ImageColorModel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.text.NumberFormat;

public class TextColorSlider extends JPanel {

    protected int channelIndex;
    protected ImageColorModel imageColorModel;

    // not clear if I'm going to use this
    private ColorChannelRangeModel rangeModel = new ColorChannelRangeModel();
    private boolean updatingFromModel = false; // flag to prevent recursion

    // accessible outside
    private int blackLevel;
    private int grayLevel;
    private int whiteLevel;

    // probably not going to use:
    // private Color whiteColor = Color.white;

    // UI elements
    JFormattedTextField blackField;
    JButton blackButton;

    public TextColorSlider(int channelIndex, ImageColorModel imageColorModel) {
        this.channelIndex = channelIndex;
        this.imageColorModel = imageColorModel;
        updateSliderValuesFromColorModel();

        setupUI();

        imageColorModel.addColorModelListener(new ColorModelListener() {
            @Override
            public void colorModelChanged() {
                updateSliderValuesFromColorModel();
            }
        });
        rangeModel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent arg0) {
                updateColorModelFromSliderValues();
            }
        });

    }

    private void setupUI() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        // going to do this three times eventually...
        NumberFormat intFormat = NumberFormat.getIntegerInstance();
        NumberFormatter numberFormatter = new NumberFormatter(intFormat);
        numberFormatter.setValueClass(Integer.class);
        numberFormatter.setAllowsInvalid(false);
        numberFormatter.setMinimum(0);

        add(new JLabel("Black:"));
        blackField = new JFormattedTextField(intFormat);
        add(blackField);
        blackButton = new JButton("Set");
        add(blackButton);


    }

    public int getBlackLevel() {
        return blackLevel;
    }

    public void setBlackLevel(int blackLevel) {
        this.blackLevel = blackLevel;
    }

    public int getGrayLevel() {
        return grayLevel;
    }

    public void setGrayLevel(int grayLevel) {
        this.grayLevel = grayLevel;
    }

    public int getWhiteLevel() {
        return whiteLevel;
    }

    public void setWhiteLevel(int whiteLevel) {
        this.whiteLevel = whiteLevel;
    }

    public void setWhiteColor(Color color) {
        // we don't currently plan to indicate the color visually
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
        // first compute ratio between white and black
        float grayLevel = (float)Math.pow(0.5, 1.0/ccm.getGamma());
        grayLevel = getBlackLevel() + grayLevel*(getWhiteLevel() - getBlackLevel());
        setGrayLevel((int)Math.round(grayLevel));

        updatingFromModel = false;
    }

    private void updateColorModelFromSliderValues() {

    }

    }
