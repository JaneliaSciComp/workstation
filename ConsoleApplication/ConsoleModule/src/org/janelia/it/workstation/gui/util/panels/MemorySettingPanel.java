/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.util.panels;

import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.WindowLocator;
import org.janelia.it.workstation.shared.util.SystemInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;

/**
 * This panel holds the memory settings widgets, including the action
 * to perform to change those settings.
 * 
 * @author fosterl
 */
public class MemorySettingPanel extends JPanel {
    public static final String TOOLTIP_TEXT = "Change maximum memory available to this application.\n"
            + "Affects only the current computer.";
    private static final Long ONE_GIG = 1024L*1024L*1024L;
    
    private Logger logger = LoggerFactory.getLogger( MemorySettingPanel.class );
    private JTextField memorySetting;
    private String existingMemorySetting;
    private SettingListener listener;
    
    public MemorySettingPanel() {
        initUI();
    }

    public boolean isChanged() {
        return (! existingMemorySetting.equals(memorySetting.getText()));
    }
    
    public void setSettingListener( SettingListener listener ) {
        this.listener = listener;
    }
    
    public boolean saveSettings() throws HeadlessException {
        String memorySettingStr = memorySetting.getText();
        try {
            Integer memorySetting = Integer.parseInt(memorySettingStr.trim());
            Integer totalAvailableGb = (int) (SystemInfo.getTotalSystemMemory() / ONE_GIG);
            if (memorySetting<=0 || memorySetting > totalAvailableGb) {
                JOptionPane.showMessageDialog(
                        WindowLocator.getMainFrame(),
                        "Unacceptable value " + memorySettingStr + ".  The system has " + totalAvailableGb + "GB to work with.",
                        "Please Re-Enter",
                        JOptionPane.ERROR_MESSAGE,
                        null
                );
            } else if (memorySetting.equals(existingMemorySetting)) {
                return true; // Do nothing.
            } else {
                setMemorySetting(memorySetting);
                // Now, let user know we will bring down the client,
                // and subsequently do so.
                JOptionPane.showMessageDialog(
                        WindowLocator.getMainFrame(),
                        "In order to complete this change, the client must be restarted.",
                        "Please Restart for Updated Setting",
                        JOptionPane.INFORMATION_MESSAGE,
                        null
                );
                // Forced exit does not pickup new setting!
                //LifecycleManager.getDefault().markForRestart();
                //LifecycleManager.getDefault().exit();
            }
        } catch (IOException ioe) {
            logger.error("IOException " + ioe + " while pushing memory setting.");
            SessionMgr.getSessionMgr().handleException(ioe);
        } catch (NullPointerException | NumberFormatException npe_nfe) {
            npe_nfe.printStackTrace();
            JOptionPane.showMessageDialog(
                    WindowLocator.getMainFrame(),
                    "Unacceptable value " + memorySettingStr + ".  Integer required.",
                    "Please Re-Enter",
                    JOptionPane.ERROR_MESSAGE,
                    null
            );
        }
        return false;
    }
    
    private void initUI() {
        memorySetting = new JTextField( 15 );
        memorySetting.addKeyListener( new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {
                exec();
            }

            @Override
            public void keyPressed(KeyEvent e) {
                exec();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                exec();
            }
            
            private void exec() {
                if ( listener != null ) {
                    listener.settingChanged();
                }
            }
    
        });

        try {
            existingMemorySetting = getMemorySetting();
        }
        catch ( IOException ioe ) {
            logger.error("IOException " + ioe + " while gathering memory setting");
            existingMemorySetting = "-1";
        }
        memorySetting.setText( existingMemorySetting );

        layoutUI();
    }

    private void layoutUI() {
        this.setToolTipText(TOOLTIP_TEXT);
        
        final BoxLayout boxLayout = new BoxLayout( this, BoxLayout.Y_AXIS);
        this.setLayout(boxLayout);
        memorySetting.setMaximumSize( memorySetting.getPreferredSize() );
        memorySetting.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.add( memorySetting );        
    }
    
    /**
     * Does a file-dredge to find the existing memory setting.
     * @return 
     */
    private String getMemorySetting() throws IOException {
        return new Integer(SystemInfo.getMemoryAllocation()).toString();
    }
    
    /**
     * Looks up file and modifies it.
     * @param value 
     */
    private void setMemorySetting( Integer value ) throws IOException {
        SystemInfo.setMemoryAllocation( value );
    }
    
    public static interface SettingListener {
        void settingChanged();
    }
}
