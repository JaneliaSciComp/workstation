/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.passive_3d;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import org.janelia.it.workstation.gui.large_volume_viewer.ImageColorModel;
import org.janelia.it.workstation.gui.large_volume_viewer.SliderPanel;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.gui.util.StateDrivenIconToggleButton;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataI;

/**
 * Represents the group of controls.  Actions against controls are encapsulated
 * together, and getters against this group can be used to fetch them for
 * layout purposes, anywhere.
 * 
 * @author fosterl
 */
public class Snapshot3dControls {
    private SliderPanel sliderPanel;
    private Collection<TextureDataI> textureDatas;
    private List<JComponent> components;
    private AbstractButton[] addSubButtons;
    private AbstractButton sharedColorModelButton;
    private Snapshot3d view;
    private SnapshotVolumeBrick brick;
    private ImageColorModel independentColorModel;
    private ImageColorModel sharedColorModel;
    private ImageColorModel activeColorModel;
    private List<Action> filterActions;
    private ViewUpdateListener viewUpdateListener;
    
    public Snapshot3dControls(
            Collection<TextureDataI> textureDatas,
            ImageColorModel independentColorModel,
            ImageColorModel sharedColorModel,
            Snapshot3d view,
            SnapshotVolumeBrick brick 
    ) {
        this.textureDatas = textureDatas;
        this.view = view;
        this.brick = brick;
        this.independentColorModel = independentColorModel;
        this.sharedColorModel = sharedColorModel;
        this.activeColorModel = independentColorModel;

        initComponents();
    }

    public void cleanup() {
        for ( JComponent component: components ) {
            Container parent = component.getParent();
            parent.remove( component );            
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
    
    /**
     * Controls about whether color sliders are synchronized or not.
     * @return the button for placement.
     */
    public AbstractButton getSharedColorModelButton() {
        return sharedColorModelButton;
    }
    
    public boolean isSharingColorModel() {
        return sharedColorModelButton.isSelected();
    }

    /**
     * @return the activeColorModel
     */
    public ImageColorModel getActiveColorModel() {
        return activeColorModel;
    }
    
    public void setIndependentColorModel( boolean isIndependent ) {
        if ( isIndependent ) {
            activeColorModel = independentColorModel;
        }
        else {
            activeColorModel = sharedColorModel;
        }
        if ( getSliderPanel() != null ) {
            getSliderPanel().setImageColorModel( activeColorModel );
        }
        view.validate();
        view.repaint();
    }
    
    public boolean isIndendentColorModel() {
        return activeColorModel == independentColorModel;
    }

    /**
     * @param activeColorModel the activeColorModel to set
     */
    public void setActiveColorModel(ImageColorModel activeColorModel) {
        this.activeColorModel = activeColorModel;
    }
    
    /**
     * @return the filterActions
     */
    public List<Action> getFilterActions() {
        return filterActions;
    }
    
    private void initComponents() {
        brick.setControls( this );

        this.components = new ArrayList<>();
        this.sliderPanel = new SliderPanel( activeColorModel );
        this.components.add( getSliderPanel());
        getSliderPanel().guiInit();
        getSliderPanel().updateLockButtons();
        getSliderPanel().setVisible(true);
        
        addSubButtons = new JCheckBox[ activeColorModel.getChannelCount() ];
        viewUpdateListener = new ViewUpdateListener( view );
        for ( int i = 0; i < activeColorModel.getChannelCount(); i++ ) {
            addSubButtons[ i ] = new JCheckBox("+");
            addSubButtons[ i ].setToolTipText( "Checked means add this color;\nunchecked will subtract." );
            addSubButtons[ i ].setSelected( true );
            addSubButtons[ i ].addActionListener( viewUpdateListener );
            components.add( addSubButtons[ i ]);
        }
        
        StateDrivenIconToggleButton localSharedColorModelBtn = new StateDrivenIconToggleButton( Icons.getIcon( "link.png" ), Icons.getIcon( "link_break.png" ) );
        localSharedColorModelBtn.setToolTipText( "Share main viewer color controls." );  
        localSharedColorModelBtn.setSelected( false );
        sharedColorModelButton = localSharedColorModelBtn;
        sharedColorModelButton.addActionListener(
                new ColorModelSwapListener( this, viewUpdateListener )
        );
        components.add( sharedColorModelButton );
        
        activeColorModel.getColorModelChangedSignal().addObserver( viewUpdateListener );
        
        filterActions = new ArrayList<>();
        // LATER: getFilterActions().add( new FilterMatrixAction( textureDatas, view ) );
    }

    private static class FilterMatrixAction extends AbstractAction {
        private Collection<TextureDataI> textureDatas;
        private Snapshot3d view;
        public FilterMatrixAction( Collection<TextureDataI> textureDatas, Snapshot3d view ) {
            this.textureDatas = textureDatas;
            this.view = view;
            putValue(Action.NAME, "Apply Filter 5x5 Average");            
        }
        public void actionPerformed( ActionEvent ae ) {
            // Need to apply a filter to each texture.
            view.validate();
            view.repaint();
        } 

    }

    private static class ViewUpdateListener implements ActionListener, Observer {
        private Snapshot3d view;
        
        public ViewUpdateListener( Snapshot3d view ) {
            this.view = view;
        }
        
        @Override
        public void actionPerformed(ActionEvent ae) {
            updateView();
        }

        @Override
        public void update(Observable o, Object arg) {
            updateView();
        }

        private void updateView() {
            view.validate();
            view.repaint();
        }

    }

    private static class ColorModelSwapListener implements ActionListener {
        private final Snapshot3dControls controls;
        private final ViewUpdateListener listener;
        public ColorModelSwapListener( Snapshot3dControls controls, ViewUpdateListener viewUpdateListener ) {
            this.controls = controls;
            this.listener = viewUpdateListener;
        }
        
        @Override
        public void actionPerformed(ActionEvent ae) {
            controls.getActiveColorModel().getColorModelChangedSignal().deleteObserver(listener);
            controls.setIndependentColorModel( ! controls.isIndendentColorModel() );
            controls.getActiveColorModel().getColorModelChangedSignal().addObserver( listener );
        }
    }
}
