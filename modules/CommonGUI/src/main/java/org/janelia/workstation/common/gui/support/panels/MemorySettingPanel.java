package org.janelia.workstation.common.gui.support.panels;

import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.util.SystemInfo;

/**
 * This panel holds the memory settings widgets, including the action
 * to perform to change those settings.
 * 
 * @author fosterl
 */
public class MemorySettingPanel extends JPanel {
    
    public static final String TOOLTIP_TEXT = "Change maximum memory available to this application.\n" + "Affects only the current computer.";
    private static final Long ONE_GIG = 1024L * 1024L * 1024L;

    private JTextField memorySetting;
    private SettingListener listener;

    public MemorySettingPanel() {
        initUI();
    }

    public void setSettingListener(SettingListener listener) {
        this.listener = listener;
    }

    private void initUI() {
        memorySetting = new JTextField(6);
        memorySetting.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (listener != null) {
                    listener.settingChanged();
                }
            }
        });

        this.setToolTipText(TOOLTIP_TEXT);

        final BoxLayout boxLayout = new BoxLayout(this, BoxLayout.X_AXIS);
        this.setLayout(boxLayout);
        memorySetting.setMaximumSize(memorySetting.getPreferredSize());
        memorySetting.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.add(memorySetting);
        this.add(new JLabel(" (requires restart)"));
    }

    public String getError() {
        Integer totalAvailableGb = (int) (SystemInfo.getTotalSystemMemory() / ONE_GIG);
        String memorySettingStr = memorySetting.getText();
        // Blank value is acceptable
        if (StringUtils.isBlank(memorySettingStr)) return null;
        try {
            final Integer memorySetting = Integer.parseInt(memorySettingStr.trim());
            if (memorySetting<=0 || memorySetting > totalAvailableGb) {
                return "Unacceptable max memory value " + memorySettingStr + ". The system has " + totalAvailableGb + "GB to work with.";
            }
        }
        catch (NumberFormatException e) {
            return "Unacceptable max memory value " + memorySettingStr + ". Value needs to be a number between 1 and "+totalAvailableGb+".";
        }
        return null;
    }
    
    public Integer getMemorySetting() {
        try {
            return Integer.parseInt(memorySetting.getText().trim());
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    public void setMemorySetting(Integer value) {
        if (value == null) {
            memorySetting.setText("");
        }
        else {
            memorySetting.setText(value.toString());
        }
    }
    
    public static interface SettingListener {
        void settingChanged();
    }
}
