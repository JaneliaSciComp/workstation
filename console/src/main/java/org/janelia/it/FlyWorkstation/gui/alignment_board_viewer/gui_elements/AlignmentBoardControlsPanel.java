package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.gui_elements;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 4/4/13
 * Time: 4:43 PM
 *
 * This will have the responsibility of presenting the user with choices for modifying data and behavior about
 * the Alignment Board Viewer.
 */
public class AlignmentBoardControlsPanel extends JPanel {
    private static final String GEO_SEARCH_TOOLTIP = "<html>" +
            "Adjust the X, Y, and Z sliders, to leave only the template volume of <br>" +
            "search lit.  This template volume will then be used to derive the coordinates, <br>" +
            "to search other specimens and present the resulting overlappoing volume." +
            "</html>";

    private static final int WIDTH = 640;
    private static final int HEIGHT = 640;

    private static final Dimension SIZE = new Dimension( WIDTH, HEIGHT);
    private static final Dimension DN_SAMPLE_DROPDOWN_SIZE = new Dimension(120, 50);
    private AlignmentBoardControls controls;

    private final Logger logger = LoggerFactory.getLogger( AlignmentBoardControlsPanel.class );

    /**
     * @param centering this dialog will be centered over the "centering" component.  Push externally-created settings
     *                  in here as a seed.
     */
    public AlignmentBoardControlsPanel(AlignmentBoardControls controls ) {
        this.setName(AlignmentBoardControlsDialog.CONTAINING_DIALOG_NAME);
        this.setSize(SIZE);
        this.controls = controls;

        positionGui();
    }

    /** Update all controls. */
    public void update( boolean visible ) {
        if ( visible ) {
            controls.update(visible);
        }
    }

    /** Control who observes.  Synchronized for thread safety. */
    public synchronized void addSettingsListener( ControlsListener listener ) {
        controls.addSettingsListener(listener);
    }

    public synchronized void removeAllSettingsListeners() {
        controls.removeAllSettingsListeners();
    }

    public void setVolumeMaxima( int x, int y, int z ) {
        controls.setVolumeMaxima(x, y, z);
    }

    public void dispose() {
        setVisible(false);
        removeAllSettingsListeners();
        controls.dispose();
        removeAll();
    }

    public void setReadyForOutput( boolean isReady ) {
        controls.setReadyForOutput( isReady );
    }

    /**
     * Gray out or un-gray all child components, as the acto fo disable/enable.
     *
     * @param enabled on or off
     */
    public void setEnabled( boolean enabled ) {
        super.setEnabled( enabled );

        controls.getMinimumVoxelCountTF().setEnabled( enabled );
        controls.getMaxNeuronCountTF().setEnabled( enabled );

        controls.getxSlider().setEnabled( enabled );
        controls.getySlider().setEnabled( enabled );
        controls.getzSlider().setEnabled( enabled );
        controls.getBrightnessSlider().setEnabled( enabled );

        controls.getClearButton().setEnabled( enabled );
        controls.getOrButton().setEnabled( enabled );
        controls.getDownSampleRateDropdown().setEnabled( enabled );
        controls.getUseSignalDataCheckbox().setEnabled( enabled );
        setReadyForOutput( enabled );
    }

    //--------------------------------------------HELPERS
    private void positionGui() {
        setLayout( new BorderLayout() );

        JPanel regionSelectionPanel = new JPanel();
        regionSelectionPanel.setLayout(new GridLayout(4, 1));
        regionSelectionPanel.add(controls.getxSlider());
        regionSelectionPanel.add(controls.getySlider());
        regionSelectionPanel.add(controls.getzSlider());

        JPanel accumulatorButtonsPanel = new JPanel();
        accumulatorButtonsPanel.setLayout(new BorderLayout());
        accumulatorButtonsPanel.add( controls.getOrButton(), BorderLayout.EAST );
        accumulatorButtonsPanel.add( controls.getClearButton(), BorderLayout.WEST );
        regionSelectionPanel.add(accumulatorButtonsPanel);

        regionSelectionPanel.setToolTipText(GEO_SEARCH_TOOLTIP);


        JPanel centralPanel = new JPanel();
        centralPanel.setLayout( new GridBagLayout() );
        /*
         * Run-down of GBC constructor params.
         * @param gridx     The initial gridx value.
         * @param gridy     The initial gridy value.
         * @param gridwidth The initial gridwidth value.
         * @param gridheight        The initial gridheight value.
         * @param weightx   The initial weightx value.
         * @param weighty   The initial weighty value.
         * @param anchor    The initial anchor value.
         * @param fill      The initial fill value.
         * @param insets    The initial insets value.
         * @param ipadx     The initial ipadx value.
         * @param ipady     The initial ipady value.
         */
        Insets insets = new Insets( 8, 8, 8, 8 );
        int rowHeight = 1;
        int nextRow = 0;
        GridBagConstraints brightnessConstraints = new GridBagConstraints(
                0, nextRow, 4, rowHeight, 1.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.BOTH, insets, 0, 0
        );

        nextRow += rowHeight;
        GridBagConstraints downSampleConstraints = new GridBagConstraints(
                0, nextRow, 3, rowHeight, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.VERTICAL, insets, 0, 0
        );
        controls.getDownSampleRateDropdown().setMinimumSize(DN_SAMPLE_DROPDOWN_SIZE);
        controls.getDownSampleRateDropdown().setMaximumSize(DN_SAMPLE_DROPDOWN_SIZE);
        controls.getDownSampleRateDropdown().setPreferredSize(DN_SAMPLE_DROPDOWN_SIZE);
        // This sits beside the downsample dropdown.
        GridBagConstraints downSampleGuessConstraints = new GridBagConstraints(
                2, nextRow, 1, rowHeight, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.VERTICAL, insets, 0, 0
        );

        nextRow += rowHeight;
        GridBagConstraints signalDataConstraints = new GridBagConstraints(
                0, nextRow, 2, rowHeight, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.VERTICAL, insets, 0, 0
        );

        nextRow += rowHeight;
        rowHeight = 1;
        GridBagConstraints minimumVoxelCountConstraints = new GridBagConstraints(
                0, nextRow, 1, rowHeight, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.VERTICAL, insets, 0, 0
        );

        GridBagConstraints maxNeuronCountConstraints = new GridBagConstraints(
                1, nextRow, 1, rowHeight, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.VERTICAL, insets, 0, 0
        );

        GridBagConstraints commitBtnConstraints = new GridBagConstraints(
                2, nextRow, 2, rowHeight, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, insets, 0, 0
        );

        nextRow += rowHeight;
        GridBagConstraints regionSelectionPanelConstraints = new GridBagConstraints(
                0, nextRow, 3, rowHeight, 1.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.BOTH, insets, 0, 0
        );

        centralPanel.add( controls.getBrightnessSlider(), brightnessConstraints );
        centralPanel.add( controls.getDownSampleRateDropdown(), downSampleConstraints );
        centralPanel.add( controls.getDownSampleGuess(), downSampleGuessConstraints );
        centralPanel.add( controls.getMinimumVoxelCountTF(), minimumVoxelCountConstraints );
        centralPanel.add( controls.getMaxNeuronCountTF(), maxNeuronCountConstraints );

        centralPanel.add( controls.getUseSignalDataCheckbox(), signalDataConstraints );
        centralPanel.add( controls.getCommitButton(), commitBtnConstraints );

        centralPanel.add( regionSelectionPanel, regionSelectionPanelConstraints );
        add(centralPanel, BorderLayout.CENTER);
    }

}
