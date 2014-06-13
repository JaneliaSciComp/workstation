package org.janelia.it.workstation.gui.alignment_board_viewer.gui_elements;

import org.janelia.it.workstation.gui.alignment_board_viewer.AlignmentBoardSettings;
import org.janelia.it.workstation.gui.viewer3d.VolumeModel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 4/4/13
 * Time: 4:43 PM
 *
 * This will have the responsibility of presenting the user with choices for modifying data and behavior about
 * the Alignment Board Viewer.
 */
public class AlignmentBoardControlsDialog extends JDialog {
    public static final String CONTAINING_DIALOG_NAME = "AlignmentBoard::Controls";
    public static final double UNSELECTED_DOWNSAMPLE_RATE = 0.0;

    private static final String DISMISS_DIALOG = "Dismiss";

    private static final String LAUNCH_AS = "Controls";
    private static final String LAUNCH_DESCRIPTION = "Present a dialog allowing users to change settings.";

    private AlignmentBoardControlsPanel controlsPanel;
    private Component centering;

    /**
     * @param centering this dialog will be centered over the "centering" component.
     */
    public AlignmentBoardControlsDialog( Component centering, VolumeModel volumeModel, AlignmentBoardSettings settings ) {
        this(centering, new AlignmentBoardControls( centering, volumeModel, settings ));
    }

    /**
     * @param centering this dialog will be centered over the "centering" component.  Push externally-created settings
     *                  in here as a seed.
     */
    public AlignmentBoardControlsDialog( Component centering, AlignmentBoardControls controls ) {
        this.setName(CONTAINING_DIALOG_NAME);
        this.centering = centering;
        controlsPanel = new AlignmentBoardControlsPanel( controls );
        this.setSize( controlsPanel.getSize() );
        this.setPreferredSize( controlsPanel.getSize() );
        this.setLayout( new BorderLayout() );
        this.add( controlsPanel, BorderLayout.CENTER );

        JPanel bottomButtonPanel = new JPanel();
        bottomButtonPanel.setLayout(new BorderLayout());
        JButton cancel = new JButton( DISMISS_DIALOG );
        cancel.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent ae ) {
                controlsPanel.setReadyForOutput( false );
                setVisible( false );
            }
        });

        Insets insets = new Insets( 8, 8, 8, 8 );
        // Mac-like layout for buttons.
        bottomButtonPanel.add( cancel, BorderLayout.WEST );
        bottomButtonPanel.setBorder( new EmptyBorder( insets ) );
        add(bottomButtonPanel, BorderLayout.SOUTH);
    }

    public AlignmentBoardControlsPanel getControlsPanel() {
        return controlsPanel;
    }

    public void setVisible( boolean visible ) {
        super.setVisible( visible );
        controlsPanel.update( visible );
    }

    /**
     * Returns something sufficient to get this thing on screen.
     *
     * @return an Action that will launch the settings dialog.
     */
    public AbstractAction getLaunchAction() {
        return new LaunchAction();
    }

    public void dispose() {
        controlsPanel.dispose();
        super.dispose();
    }

    //-------------------------------------------------INNER CLASSES/INTERFACES
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

            int width = (int)controlsPanel.getSize().getWidth();
            int height = (int)controlsPanel.getSize().getHeight();
            int x = centering.getLocation().x + ( centering.getWidth() / 2 ) - ( width / 2 );
            int y = centering.getLocation().y + ( centering.getHeight() / 2 ) - ( height / 2 );
            AlignmentBoardControlsDialog.this.setLocation( x, y );
            AlignmentBoardControlsDialog.this.setVisible( true );
        }

    }

}
