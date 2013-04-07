package org.janelia.it.FlyWorkstation.gui.viewer3d.gui_elements;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListDataListener;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 4/4/13
 * Time: 4:43 PM
 *
 * This will have the responsibility of presenting the user with choices for modifying data and behavior about
 * the Alignment Board Viewer.
 */
public class AlignmentBoardSettingsDialog extends JDialog {
    public static final double DEFAULT_DOWNSAMPLE_RATE = 2.0;

    private static final String LAUNCH_AS = "Alignment Board Settings";
    private static final String LAUNCH_DESCRIPTION = "Present a dialog allowing users to change settings.";
    private static final Dimension SIZE = new Dimension( 400, 400 );

    private Component centering;
    private JButton go;
    private JSlider brightnessSlider;
    private JComboBox downSampleRateDropdown;

    private double currentGamma;
    private double currentDownSampleRate;

    private boolean readyForOutput = false;

    private Map<Integer,Integer> downSampleRateToIndex;
    private Collection<SettingsListener> listeners;

    /**
     * @param centering this dialog will be centered over the "centering" component.
     */
    public AlignmentBoardSettingsDialog( Component centering ) {
        this.setModal( true );
        this.setSize(SIZE);
        this.centering = centering;
        this.listeners = new ArrayList<SettingsListener>();
        this.setDefaultCloseOperation( WindowConstants.HIDE_ON_CLOSE );
        createGui();
    }

    /** Control who observes.  Synchronized for thread safety. */
    public synchronized void addSettingsListener( SettingsListener listener ) {
        listeners.add( listener );
    }

    public synchronized void removeSettingsListener( SettingsListener listener ) {
        listeners.remove( listener );
    }

    /**
     * Returns something sufficient to get this thing on screen.
     *
     * @return an Action that will launch the settings dialog.
     */
    public AbstractAction getLaunchAction() {
        return new LaunchAction();
    }

    /** These getters may be called after successful launch. */
    public double getDownsampleRate() {
        if ( ! readyForOutput )
            return currentDownSampleRate;
        return (Integer)downSampleRateDropdown.getItemAt( downSampleRateDropdown.getSelectedIndex() );
    }

    public double getGammaFactor() {
        if ( ! readyForOutput )
            return currentGamma;
        int value = brightnessSlider.getValue();
        double rtnVal = 11.0 - value;
        return 1.0;
    }

    public boolean isReadyForOutput() {
        return readyForOutput;
    }

    public void setDownSampleRate( double downSampleRate ) {
        int downSampleRateInt = (int)Math.round(downSampleRate);
        Integer downsampleIndex = downSampleRateToIndex.get( downSampleRateInt );
        downSampleRateDropdown.setSelectedIndex( downsampleIndex );
        currentDownSampleRate = downSampleRate;
    }

    //--------------------------------------------HELPERS

    /**
     * This will fire only those listener methods affected by the current user commitment.
     */
    private synchronized void fireSettingsEvent() {
        for ( SettingsListener listener: listeners ) {
            double newGamma = getGammaFactor();
            if ( newGamma != currentGamma ) {
                currentGamma = newGamma;
                listener.setBrightness( currentGamma );
            }
            double newDownSampleRate = getDownsampleRate();
            if ( newDownSampleRate != currentDownSampleRate ) {
                currentDownSampleRate = newDownSampleRate;
                listener.setDownsampleRate( currentDownSampleRate );
            }
        }
    }

    private void createGui() {
        setLayout( new BorderLayout() );
        brightnessSlider = new JSlider();
        brightnessSlider.setMaximum( 10 );
        brightnessSlider.setMinimum(1);
        brightnessSlider.setOrientation( JSlider.HORIZONTAL );

        downSampleRateToIndex = new HashMap<Integer,Integer>();
        downSampleRateToIndex.put( 0, 0 );
        downSampleRateToIndex.put( 2, 1 );
        downSampleRateToIndex.put( 4, 2 );
        downSampleRateToIndex.put( 6, 3 );

        downSampleRateDropdown = new JComboBox(
                new ABSDComboBoxModel( downSampleRateToIndex )
        );

        JPanel centralPanel = new JPanel();
        centralPanel.setLayout( new GridLayout( 2, 1 ) );
        brightnessSlider.setBorder( new TitledBorder( "Brightness" ) );
        downSampleRateDropdown.setBorder( new TitledBorder( "Down Sample Rate" ) );
        downSampleRateDropdown.setToolTipText(
                "Data shown on screen may be too large for your graphics card.\n" +
                "  Therefore, they are downsampled at a default rate of 1.0:" + DEFAULT_DOWNSAMPLE_RATE +
                ", however, you may wish to move that rate up or down, depending on your knowledge of the\n" +
                "advanced hardware on your system."
        );
        centralPanel.add( brightnessSlider );
        centralPanel.add( downSampleRateDropdown );
        add( centralPanel, BorderLayout.CENTER );

        JPanel bottomButtonPanel = new JPanel();
        bottomButtonPanel.setLayout( new BorderLayout() );
        go = new JButton( "OK" );
        go.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent ae ) {
                readyForOutput = true;
                fireSettingsEvent();
            }
        });
        JButton cancel = new JButton( "Cancel" );
        cancel.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent ae ) {
                readyForOutput = false;
                setVisible( false );
            }
        });
        // Mac-like layout for buttons.
        bottomButtonPanel.add( cancel, BorderLayout.WEST );
        bottomButtonPanel.add( go, BorderLayout.EAST );

        add( bottomButtonPanel, BorderLayout.SOUTH );
    }

    //-------------------------------------------------INNER CLASSES/INTERFACES

    /** Simple list-and-map-driven combo box model. */
    class ABSDComboBoxModel implements ComboBoxModel {

        private List<Integer> rates = new ArrayList<Integer>();
        private Object selectedItem;

        public ABSDComboBoxModel( Map<Integer,Integer> rateToIndex ) {
            for ( Integer rate: rateToIndex.keySet() ) {
                rates.add( rate );
            }
            selectedItem = DEFAULT_DOWNSAMPLE_RATE;
        }

        @Override
        public int getSize() {
            return rates.size();
        }

        @Override
        public Object getElementAt(int index) {
            return rates.get( index );
        }

        @Override
        public void addListDataListener(ListDataListener l) {
            // NADA.  Ignore.
        }

        @Override
        public void removeListDataListener(ListDataListener l) {
            // NADA.  Ignore.
        }

        @Override
        public void setSelectedItem(Object anItem) {
            selectedItem = anItem;
        }

        @Override
        public Object getSelectedItem() {
            return selectedItem;
        }
    }

    /** Callers should implmeent this to observe the input settings provided by uesr. */
    public static interface SettingsListener {
        void setBrightness( double brightness );
        void setDownsampleRate( double downsampleRate );
    }

    public class LaunchAction extends AbstractAction {
        public LaunchAction() {
            super( LAUNCH_AS );
        }

        @Override
        public Object getValue(String key) {
            if ( key.equals( AbstractAction.ACTION_COMMAND_KEY ) )
                return LAUNCH_AS;
            else if ( key.equals( AbstractAction.LONG_DESCRIPTION ) )
                return LAUNCH_DESCRIPTION;
            else if ( key.equals( AbstractAction.NAME ) )
                return LAUNCH_AS;
            else
                return null;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            readyForOutput = false;

            int width = (int)SIZE.getWidth();
            int height = (int)SIZE.getHeight();
            int x = centering.getLocation().x + ( centering.getWidth() / 2 ) - ( width / 2 );
            int y = centering.getLocation().y + ( centering.getHeight() / 2 ) - ( height / 2 );
            AlignmentBoardSettingsDialog.this.setLocation( x, y );
            AlignmentBoardSettingsDialog.this.setVisible( true );
        }

    }
}
