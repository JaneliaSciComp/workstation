/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.passive_3d;

import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import org.janelia.it.workstation.gui.large_volume_viewer.ImageColorModel;
import org.janelia.it.workstation.gui.large_volume_viewer.SliderPanel;

/**
 * Represents the group of controls.  Actions against controls are encapsulated
 * together, and getters against this group can be used to fetch them for
 * layout purposes, anywhere.
 * 
 * @author fosterl
 */
public class Snapshot3dControls {
    private SliderPanel sliderPanel;
    private List<JComponent> components;
    private AbstractButton[] addSubButtons;
    
    public Snapshot3dControls( ImageColorModel colorModel ) {
        initComponents(colorModel);
    }

    public void cleanup() {
        for ( JComponent component: components ) {
            Container parent = component.getParent();
            parent.remove( component );
        }
    }

    private void initComponents(ImageColorModel colorModel) {
        this.components = new ArrayList<>();
        this.sliderPanel = new SliderPanel( colorModel );
        this.components.add( getSliderPanel());
        getSliderPanel().guiInit();
        getSliderPanel().updateLockButtons();
        getSliderPanel().setVisible(true);
        
        addSubButtons = new JCheckBox[ colorModel.getChannelCount() ];
        for ( int i = 0; i < colorModel.getChannelCount(); i++ ) {
            addSubButtons[ i ] = new JCheckBox("+");
            addSubButtons[ i ].setToolTipText( "Checked means add this color;\nunchecked will subtract." );
        }
    }

    /**
     * @return the sliderPanel
     */
    public SliderPanel getSliderPanel() {
        return sliderPanel;
    }

    /**
     * Fetch-back the buttons which make it possible to place them.
     * 
     * @return the addSubButton
     */
    public AbstractButton[] getAddSubButton() {
        return addSubButtons;
    }
    
    public boolean[] getAddSubChoices() {
        boolean[] rtnVal = new boolean[ addSubButtons.length ];
        for ( int i = 0; i < addSubButtons.length; i++ ) {
            rtnVal[ i ] = addSubButtons[ i ].isSelected();
        }
        return rtnVal;
    }
    
}
