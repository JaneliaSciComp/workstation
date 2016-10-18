package org.janelia.it.workstation.gui.util.panels;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 12/14/12
 * Time: 3:07 PM
 *
 * Shows Red/Green/Blue checkboxes to let the user pick which are relevant.  A "channel selector".  This panel is
 * meant to be placed onto another widget (frame, applet, larger panel,etc.) such that the getter can be
 * called at the appropriate time.
 */
public class ChannelSelectionPanel extends JPanel {

    // NOTE: this happens as a result of construction. Initializing here causes no grief.
    private JCheckBox redCheckbox = new JCheckBox("Red");
    private JCheckBox greenCheckbox = new JCheckBox("Green");
    private JCheckBox blueCheckbox = new JCheckBox("Blue");

    private float[] rgbFloat;

    public ChannelSelectionPanel( float[] rgbFloat ) {

        this.rgbFloat = new float[3];
        this.rgbFloat[ 0 ] = rgbFloat[ 0 ];
        this.rgbFloat[ 1 ] = rgbFloat[ 1 ];
        this.rgbFloat[ 2 ] = rgbFloat[ 2 ];

        // Enforce the rule, even at "seed": something must be selected. Making it red.
        float total = 0.0f;
        for ( float fl: rgbFloat ) {
            total += fl;
        }
        if ( total == 0.0f ) {
            this.rgbFloat[ 0 ] = 1.0f;
        }

        initGUI();
    }

    /** Call this to get the current user choice of red, green and/or blue. */
    public float[] getRgbChoices() {
        rgbFloat[ 0 ] = redCheckbox.isSelected() ? 1.0f : 0.0f;
        rgbFloat[ 1 ] = greenCheckbox.isSelected() ? 1.0f : 0.0f;
        rgbFloat[ 2 ] = blueCheckbox.isSelected() ? 1.0f : 0.0f;

        return rgbFloat;
    }

    private void initGUI() {
        this.setSize( new Dimension( 150, 100 ) );
        this.setLayout( new GridLayout( 3, 1 ) );
        this.add( redCheckbox );
        this.add( greenCheckbox );
        this.add( blueCheckbox );

        redCheckbox.setSelected( rgbFloat[ 0 ] > 0.0f );
        greenCheckbox.setSelected( rgbFloat[ 1 ] > 0.0f );
        blueCheckbox.setSelected( rgbFloat[ 2 ] > 0.0f );

        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean goodSelection = testSelection();
                if ( ! goodSelection ) {
                    ((JCheckBox)e.getSource()).setSelected( true );
                    JOptionPane.showMessageDialog( ChannelSelectionPanel.this, "At least one color must be selected." );
                }
            }
        };

        redCheckbox.addActionListener( listener );
        greenCheckbox.addActionListener( listener );
        blueCheckbox.addActionListener( listener );
    }

    private boolean testSelection() {
        return redCheckbox.isSelected() || greenCheckbox.isSelected() || blueCheckbox.isSelected();
    }

}
