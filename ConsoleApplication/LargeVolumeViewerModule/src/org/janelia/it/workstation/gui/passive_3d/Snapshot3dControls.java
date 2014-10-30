/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.passive_3d;

import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
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
    }

    /**
     * @return the sliderPanel
     */
    public SliderPanel getSliderPanel() {
        return sliderPanel;
    }
    
}
