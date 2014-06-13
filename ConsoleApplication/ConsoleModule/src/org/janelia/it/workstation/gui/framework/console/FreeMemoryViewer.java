package org.janelia.it.workstation.gui.framework.console;

import org.janelia.it.workstation.shared.util.FreeMemoryWatcher;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Observable;
import java.util.Observer;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 12:47 PM
 * Class to monitor the memory usage of the application
 */
public class FreeMemoryViewer extends JPanel implements Observer {
    public static final int MEMORY_CONSTANT = 1024000; // Value to display memory in MB
    private BoxLayout boxLayout = new BoxLayout(this, BoxLayout.X_AXIS);
    private JLabel label = new JLabel("  Memory Usage  ");
    private JLabel percentageLabel = new JLabel("0 %");
    private long totalMemory;
    private static int FIRST_WARNING_PERCENT = 10;
    private static int SECOND_WARNING_PERCENT = 5;
    private static int FINAL_WARNING_PERCENT = 1;
    private static int BAR_WIDTH = 200;
    private static int RED_BAR = 10;
    private static int YELLOW_BAR = 30;
    private boolean reachedFirstWarningPercent;
    private boolean reachedSecondWarningPercent;
    private boolean reachedFinalWarningPercent;
    private JOptionPane warningPane;
    private JDialog warningDialog;
    private JOptionPane errorPane;
    private JDialog errorDialog;

    public FreeMemoryViewer() {
        warningPane = new JOptionPane("Available memory is currently at x percent.", JOptionPane.WARNING_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        warningDialog = warningPane.createDialog(this.getParent(), "Warning: Low Available Memory");
        errorPane = new JOptionPane("Available memory is currently at x and is critically low!", JOptionPane.ERROR_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        errorDialog = errorPane.createDialog(this.getParent(), "Error: Critically Low Available Memory");

        setLayout(boxLayout);
        this.add(label);
        this.add(percentageLabel);
        this.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showMemoryDialog();
                }
            }

            public void mousePressed(MouseEvent e) {
            }

            public void mouseReleased(MouseEvent e) {
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseExited(MouseEvent e) {
            }
        });
    }

    public void update(Observable observable, Object obj) {
        if (observable instanceof FreeMemoryWatcher && obj instanceof Integer) {
            int value = ((Integer) obj).intValue();
            if (value >= YELLOW_BAR) percentageLabel.setBackground(Color.green);
            if (value >= RED_BAR && value < YELLOW_BAR) percentageLabel.setBackground(Color.yellow);
            //   System.out.println("yello reached");
            if (value < RED_BAR) percentageLabel.setBackground(Color.red);
            percentageLabel.setText(String.valueOf(100 - value));
            checkWarning(value);
        }
    }

    /**
     * Overrides of getters for sizeing, so that layout managers will not
     * allow this widget to be obliterated.
     */
    public Dimension getPreferredSize() {
        int width = BAR_WIDTH + label.getWidth();
        int height = this.getFontMetrics(this.getFont()).getHeight() + 4;
        return new Dimension(width, height);
    }

    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    private void checkWarning(int percentRemaining) {
        if (percentRemaining > FIRST_WARNING_PERCENT) {
            reachedSecondWarningPercent = false;
            return;
        }

        if (percentRemaining <= FIRST_WARNING_PERCENT && percentRemaining > SECOND_WARNING_PERCENT && !reachedFirstWarningPercent) {

            showMemWarningDialog(percentRemaining);
            reachedFirstWarningPercent = true;
            reachedFinalWarningPercent = false;
        }

        if (percentRemaining <= SECOND_WARNING_PERCENT && percentRemaining > FINAL_WARNING_PERCENT && !reachedSecondWarningPercent) {

            showMemWarningDialog(percentRemaining);
            reachedFirstWarningPercent = true;
            reachedSecondWarningPercent = true;
        }

        if (percentRemaining <= FINAL_WARNING_PERCENT && !reachedFinalWarningPercent) {

            showMemErrorDialog(percentRemaining);
            reachedFirstWarningPercent = true;
            reachedSecondWarningPercent = true;
            reachedFinalWarningPercent = true;
        }


    }

    private void showMemWarningDialog(int percentAvailable) {
        warningPane.setMessage("Available memory is " + percentAvailable + " percent.");
        warningPane.updateUI();
        if (!warningDialog.isShowing()) {
            warningDialog.setVisible(true);
        }
    }

    private void showMemErrorDialog(int percentAvailable) {
        // If we are critical, hide the warning message
        if (warningDialog.isShowing()) {
            warningDialog.setVisible(false);
        }
        errorPane.setMessage("Available memory is " + percentAvailable + " and is critically low!");
        errorPane.updateUI();
        if (!errorDialog.isShowing()) {
            errorDialog.setVisible(true);
        }
    }

    private void showMemoryDialog() {
        String[] strings = new String[6];
        strings[0] = "Total Memory:\t" + (getTotalMemory() / MEMORY_CONSTANT) + " MB";
        strings[1] = "Free Memory :\t" + (getFreeMemory() / MEMORY_CONSTANT) + " MB";
        strings[2] = "-----------------------------------";
        strings[3] = "Used Memory:\t" + (getUsedMemory() / MEMORY_CONSTANT) + " MB";
        strings[4] = "                                   ";
        strings[5] = "Would you like to compact memory now?";
        int ans = JOptionPane.showConfirmDialog(this.getParent().getParent(), strings, "Memory Usage", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
        if (ans == JOptionPane.YES_OPTION) System.gc();
    }

    private long getTotalMemory() {
        return FreeMemoryWatcher.getFreeMemoryWatcher().getTotalMemory();
    }

    private long getUsedMemory() {
        return FreeMemoryWatcher.getFreeMemoryWatcher().getUsedMemory();
    }

    private long getFreeMemory() {
        return FreeMemoryWatcher.getFreeMemoryWatcher().getFreeMemory();
    }
}
