/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.util.panels;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.janelia.it.workstation.gui.util.WindowLocator;
import org.janelia.it.workstation.shared.util.SystemInfo;
import org.openide.LifecycleManager;

/**
 * This panel holds the memory settings widgets, including the action
 * to perform to change those settings.
 * 
 * @author fosterl
 */
public class MemorySettingPanel extends JPanel {
    private static final Long ONE_GIG = 1024L*1024L*1024L;
    
    private JTextField memorySetting;
    private JButton execButton;
    private String existingMemorySetting;
    
    public MemorySettingPanel() {
        initUI();
    }
    
    
    private void initUI() {
        memorySetting = new JTextField( 15 );
        execButton = new JButton( "Save" );
        execButton.addActionListener( new SaveSettingListener() );

        existingMemorySetting = getMemorySetting();
        memorySetting.setText( existingMemorySetting );

        layoutUI();
    }
    
    private void layoutUI() {
        final BoxLayout boxLayout = new BoxLayout( this, BoxLayout.Y_AXIS);
        this.setLayout(boxLayout);
        memorySetting.setMaximumSize( memorySetting.getPreferredSize() );
        memorySetting.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.add( memorySetting );
        execButton.setMaximumSize( execButton.getPreferredSize() );
        execButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.add( execButton );
        
    }
    
    private class SaveSettingListener implements ActionListener {
        @Override
        public void actionPerformed( ActionEvent ae ) {
            String memorySettingStr = memorySetting.getText();
            try {
                Integer memorySetting = Integer.parseInt( memorySettingStr.trim() );
                Integer totalAvailableGb = (int)(SystemInfo.getTotalSystemMemory() / ONE_GIG);
                if ( memorySetting > totalAvailableGb ) {                    
                    JOptionPane.showMessageDialog( 
                            WindowLocator.getMainFrame(),
                            "Unacceptable value " + memorySettingStr + ".  The system has only " + totalAvailableGb + "Gb.",
                            "Please Re-Enter", 
                            JOptionPane.ERROR_MESSAGE,
                            null 
                    );
                }
                else if ( memorySetting.equals(existingMemorySetting) ) {
                    return; // Do nothing.
                }
                else {
                    setMemorySetting( memorySetting.toString() );
                    // Now, let user know we will bring down the client,
                    // and subsequently do so.
                    JOptionPane.showMessageDialog(
                            WindowLocator.getMainFrame(),
                            "In order to complete this change, the client must be restarted.\nShutting down now.",
                            "Shutting Down Now",
                            JOptionPane.INFORMATION_MESSAGE,
                            null
                    );
                    // Force exit.
                    LifecycleManager.getDefault().exit();

                }
                
            } catch ( NullPointerException | NumberFormatException npe_nfe ) {
                JOptionPane.showMessageDialog( 
                        WindowLocator.getMainFrame(), 
                        "Unacceptable value " + memorySettingStr + ".  Integer required.",
                        "Please Re-Enter", 
                        JOptionPane.ERROR_MESSAGE, 
                        null
                );
            }
        }
    }
    
    /**
     * Does a file-dredge to find the existing memory setting.
     * @return 
     */
    private String getMemorySetting() {
        return "0";
    }
    
    /**
     * Looks up file and modifies it.
     * @param value 
     */
    private void setMemorySetting( String value ) {
        
    }
    
}
