package org.janelia.it.workstation.gui.large_volume_viewer.components;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import org.janelia.it.workstation.cache.large_volume.EHCacheFacade;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;

/**
 *
 * @author fosterl
 */
public class Cache3DSettings {
    public void prompt() {
        final JDialog popup = new JDialog(); // Get parent;
        popup.setSize( 200, 300 );
        centerOnScreen( popup, true );

        final JTextField neighborhoodSize = new JTextField(10);
        TitledBorder border = new TitledBorder("3D Cache Size");
        neighborhoodSize.setBorder(border);
        neighborhoodSize.setToolTipText("Values must be in the range 200 to 500.");
        
        //NOTE: these limits are empirical values.  Later, more memory might
        // be dedicated to the cache (through its config file), and larger
        // neighborhoods might be possible.
        JPanel buttonPanel = new JPanel();
        buttonPanel.setToolTipText("<html>Enter a value > 0 to utilized 3D cache in Large Volume Viewer.<br/>"
                                 + "Values should not be larger than 500 (voxels represented), or less than 200.<br/>"
                                 + "A value of no more than 400 is recommended.</html>");
        buttonPanel.setLayout(new BorderLayout());
        JButton okButton = new JButton("OK");
        String oldValue = (String)SessionMgr.getSessionMgr().getModelProperty(EHCacheFacade.CACHE_NAME);
        if (oldValue != null) {
            neighborhoodSize.setText(oldValue);
        }
        
        okButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                boolean valueChosen = false;
                while (! valueChosen) {
                    try {
                        Integer size = Integer.parseInt(neighborhoodSize.getText().trim());
                        if (size <= 500  &&  size <= 200) {
                            SessionMgr.getSessionMgr().setModelProperty(EHCacheFacade.CACHE_NAME, size);
                            popup.setVisible(false);
                            valueChosen = true;
                        }
                                                
                    } catch (NumberFormatException nfe) {
                        neighborhoodSize.setText("");
                    }
                }
            }            
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                popup.setVisible(false);
            }
        });
        
        buttonPanel.add( okButton, BorderLayout.CENTER );
        buttonPanel.add( cancelButton, BorderLayout.WEST );
        
        popup.setLayout( new BorderLayout() );
        popup.add( buttonPanel, BorderLayout.SOUTH );
        popup.add( neighborhoodSize, BorderLayout.NORTH );
        
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
