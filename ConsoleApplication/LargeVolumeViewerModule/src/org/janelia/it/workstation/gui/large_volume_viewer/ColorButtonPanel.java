/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer;

import org.janelia.console.viewerapi.model.ChannelColorModel;
import org.janelia.console.viewerapi.model.ImageColorModel;
import java.awt.Color;
import java.util.List;
import javax.swing.AbstractButton;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.DefaultButtonModel;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import org.janelia.console.viewerapi.controller.ColorListener;
import org.janelia.console.viewerapi.controller.ColorModelInitListener;

/**
 * Arrange this alongside the slider panel, with channel-oriented buttons.
 * 
 * @author fosterl
 */
public class ColorButtonPanel extends JPanel {
    private ImageColorModel imageColorModel;
    private List<JCheckBox> checkboxes = new ArrayList<>();
    private List<AbstractButton> bottomControls = new ArrayList<>();
    private int verticalSpacer;
    private ColorModelInitListener icmInitListener;
    
    public ColorButtonPanel( ImageColorModel imageColorModel, int verticalSpacer ) {
        super();
        this.verticalSpacer = verticalSpacer;
        setImageColorModel( imageColorModel );
    }
    
    public ColorButtonPanel( ImageColorModel imageColorModel ) {
        this( imageColorModel, 0 );
    }

    public final void setImageColorModel( ImageColorModel imageColorModel ) {
        clearCheckboxes();
        cleanupObserver();
        this.imageColorModel = imageColorModel;        

        // Must initialize GUI as needed.        
        if ( imageColorModel.getChannelCount() > 0 ) {
            initGui();
        }
        
        icmInitListener = new ColorModelInitListener() {
            @Override
            public void colorModelInit() {
                initGui();
            }            
        };
        imageColorModel.addColorModelInitListener(icmInitListener);
    }

    public void addButton( AbstractButton btn ) {
        bottomControls.add( btn );
        this.add(btn);
    }
    
    public List<JCheckBox> getCheckboxes() {
        return checkboxes;
    }
    
    public void dispose() {
        clear();
        bottomControls = null;
        checkboxes = null;
        imageColorModel = null;
    }

    private void clear() {
        bottomControls.clear();
        clearCheckboxes();
    }

    private void clearCheckboxes() {
        for (JCheckBox checkbox : checkboxes) {
            ((ChannelColorButtonModel)checkbox.getModel()).dispose();
            this.remove(checkbox);
        }
        checkboxes.clear();
    }
    
    private void cleanupObserver() {
        if ( this.imageColorModel != null  &&  icmInitListener != null ) {
            this.imageColorModel.removeColorModelInitListener(icmInitListener);
        }
    }
    
    /**
     * Initialize GUI in one place.  Can add vertical spacing to the layout.
     * 
     * @param verticalSpacer extra slots to balance with neighboring widgets.
     */
    private void initGui() {
        // Ensure the button placement is identical to what was there at init.
        for ( AbstractButton ctrl: bottomControls ) {
            this.remove( ctrl );
        }
        clearCheckboxes();
        setLayout( new GridLayout( imageColorModel.getChannelCount() + verticalSpacer, 1 ) );
        for ( int i = 0; i < imageColorModel.getChannelCount(); i++ ) {
            JCheckBox btn = new JCheckBox("+");
            btn.setModel(new ChannelColorButtonModel( imageColorModel.getChannel(i), btn ));
            checkboxes.add(btn);
            btn.setToolTipText( "Checked means add this color;\nunchecked will subtract." );
            btn.setSelected( true );
            ActionListener listener = new CheckboxActionListener( imageColorModel.getChannel( i ), imageColorModel );            
            btn.addActionListener( listener );
            // Add this to the GUI.
            add(btn);
        }
        for ( AbstractButton ctrl: bottomControls ) {
            this.add( ctrl );
        }
    }
    
    private static class CheckboxActionListener implements ActionListener {

        private ChannelColorModel ccm;
        private ImageColorModel icm;
        
        public CheckboxActionListener( ChannelColorModel ccm, ImageColorModel icm ) {
            this.ccm = ccm;
            this.icm = icm;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            JCheckBox btn = (JCheckBox)e.getSource();
            btn.setSelected(! btn.isSelected());
            ccm.setCombiningConstant( btn.isSelected() ? 1.0f : -1.0f );
            ccm.fireColorChange( ccm.getColor() );
            // Signal time to change stuff on screen.
            icm.fireColorModelChanged();
        }
        
    }
    
    private static class ChannelColorButtonModel extends DefaultButtonModel {

        private ChannelColorModel ccm;
        private AbstractButton btn;

        public ChannelColorButtonModel(ChannelColorModel ccm, AbstractButton btn) {
            this.ccm = ccm;
            this.btn = btn;
            // Feed back to second checkbox which have may similar model.
            ColorListener colorChangeListener = new ColorListener() {
                @Override
                public void color(Color color) {
                    ChannelColorButtonModel.this.fireStateChanged();
                }
                
            };
            ccm.setColorListener(colorChangeListener);
        }
        
        @Override
        public boolean isSelected() {
            return ccm != null && ccm.getCombiningConstant() > 0.0;
        }

        @Override
        public void setSelected(boolean selected) {
            if ( ccm == null ) {
                return;
            }
            ccm.setCombiningConstant( selected ? 1.0f : -1.0f );
            super.setSelected(selected);
        }
        
        public void dispose() {
            if ( ccm != null ) {
                ccm.setColorListener(null);
                ccm = null;
            }
            btn = null;            
        }

    }
}
