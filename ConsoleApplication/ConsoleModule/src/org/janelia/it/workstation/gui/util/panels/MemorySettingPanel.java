package org.janelia.it.workstation.gui.util.panels;

import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.WindowLocator;
import org.janelia.it.workstation.shared.util.SystemInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import org.janelia.it.workstation.shared.workers.SimpleWorker;

/**
 * This panel holds the memory settings widgets, including the action
 * to perform to change those settings.
 * 
 * @author fosterl
 */
public class MemorySettingPanel extends JPanel {
    
    private final static Logger logger = LoggerFactory.getLogger(MemorySettingPanel.class);
    
    public static final String TOOLTIP_TEXT = "Change maximum memory available to this application.\n"
            + "Affects only the current computer.";
    private static final Long ONE_GIG = 1024L*1024L*1024L;
    
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
        final String memorySettingStr = memorySetting.getText();
        SimpleWorker worker = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                try {
                    final Integer memorySetting = Integer.parseInt(memorySettingStr.trim());
                    Integer totalAvailableGb = (int) (SystemInfo.getTotalSystemMemory() / ONE_GIG);
                    if (memorySetting<=0 || memorySetting > totalAvailableGb) {
                        throw new Exception("Unacceptable value " + memorySettingStr + ".  The system has " + totalAvailableGb + "GB to work with.");
                    } else if (memorySettingStr.trim().equals(existingMemorySetting)) {
                        // Do nothing.
                    } else {
                        setMemorySetting(memorySetting);

                    }

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
            }
            @Override
            protected void hadSuccess() {
                // Now, let user know we will bring down the client,
                // and subsequently do so.
                JOptionPane.showMessageDialog(
                        WindowLocator.getMainFrame(),
                        "In order to complete this change, the client must be restarted.",
                        "Please Restart for Updated Setting",
                        JOptionPane.INFORMATION_MESSAGE,
                        null
                );
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };

        worker.execute();

        return true;
    }
    
    private void initUI() {
        memorySetting = new JTextField(6);
        memorySetting.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
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
        
        final BoxLayout boxLayout = new BoxLayout( this, BoxLayout.X_AXIS);
        this.setLayout(boxLayout);
        memorySetting.setMaximumSize( memorySetting.getPreferredSize() );
        memorySetting.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.add( memorySetting );
        this.add( new JLabel(" (requires restart)") );
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
        logger.info("Saving memory setting: "+value);
        SystemInfo.setMemoryAllocation( value );
    }
    
    public static interface SettingListener {
        void settingChanged();
    }
}
