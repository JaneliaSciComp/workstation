package org.janelia.it.workstation.gui.large_volume_viewer.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import org.janelia.it.workstation.cache.large_volume.AbstractCacheFacade;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;

/**
 * Any settings required for the 3D cache, which are user-selectable.
 *
 * @author fosterl
 */
public class Cache3DSettings {
    public static final String SIZE_WARNING = "Please enter a number between " + AbstractCacheFacade.MIN_3D_CACHE_SIZE + " and " + AbstractCacheFacade.MAX_3D_CACHE_SIZE + ".";
    public static final String INVALID_NUM_WARNING = "Not a number. " + SIZE_WARNING;
    
    public void prompt() {
        final JDialog popup = new JDialog(); // Get parent;
        popup.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);        
        popup.setSize( 300, 180 );
        centerOnScreen( popup, true );

        final JTextField neighborhoodSize = new JTextField(10);
        TitledBorder border = new TitledBorder("3D Cache Size");
        neighborhoodSize.setBorder(border);
        neighborhoodSize.setToolTipText("Values must be in the range 200 to 500.");
        
        final JLabel statusLabel = new JLabel("                                    ");
        statusLabel.setForeground(Color.red);
        
        //NOTE: these limits are empirical values.  Later, more memory might
        // be dedicated to the cache (through its config file), and larger
        // neighborhoods might be possible.
        JPanel buttonPanel = new JPanel();
        buttonPanel.setToolTipText("<html>Enter a value > 0 to utilized 3D cache in Large Volume Viewer.<br/>"
                                 + "Values should not be larger than 500 (voxels represented), or less than 200.<br/>"
                                 + "A value of no more than 400 is recommended.</html>");
        buttonPanel.setLayout(new BorderLayout());
        JButton okButton = new JButton("OK");
        int neighborhoodSizeInt = AbstractCacheFacade.getNeighborhoodSize();
        neighborhoodSize.setText(neighborhoodSizeInt + "");
        
        okButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                try {
                    Integer size = Integer.parseInt(neighborhoodSize.getText().trim());
                    if (size <= AbstractCacheFacade.MAX_3D_CACHE_SIZE && size >= AbstractCacheFacade.MIN_3D_CACHE_SIZE) {
                        SessionMgr.getSessionMgr().setModelProperty(AbstractCacheFacade.CACHE_NAME, size);
                        popup.setVisible(false);
                        popup.dispose();
                    } else {
                        statusLabel.setText(SIZE_WARNING);
                    }

                } catch (NumberFormatException nfe) {
                    neighborhoodSize.setText("");
                    statusLabel.setText(INVALID_NUM_WARNING);
                }
            }            
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                popup.dispose();
            }
        });
        
        buttonPanel.add( okButton, BorderLayout.EAST );
        buttonPanel.add( cancelButton, BorderLayout.WEST );
        
        popup.setLayout( new GridLayout(3, 1) );
        popup.add( neighborhoodSize );
        popup.add( buttonPanel );
        popup.add( statusLabel );
        
        popup.setVisible(true);
    }
    
    /**
     * See http://stackoverflow.com/questions/213266/how-do-i-center-a-jdialog-on-screen
     * @param c what to center.
     * @param absolute right at center, or 25% from corner.
     */
    public void centerOnScreen(final JDialog c, final boolean absolute) {
        final int width = c.getWidth();
        final int height = c.getHeight();
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screenSize.width / 2) - (width / 2);
        int y = (screenSize.height / 2) - (height / 2);
        if (!absolute) {
            x /= 2;
            y /= 2;
        }
        c.setLocation(x, y);
    }    
}
