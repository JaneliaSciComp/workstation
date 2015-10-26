/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.dialogs;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.janelia.it.workstation.shared.util.SystemInfo;

/**
 * This dialog maker, informs the user when they do not have enough memory
 * for some operation/facility, and informs them they need to bump the setting.
 *
 * @author fosterl
 */
public class MemoryCheckDialog {
    private static final long GIGA = 1024*1024*1024;
    private static final String MEM_ALLOC_CHECK_ERR = "Failed to obtain memory allocation.";

    public boolean unusedIfInsufficientMemory(String facility, int requiredSize, JFrame parent) {
        return checkMemory( facility, requiredSize, "Not using", parent);
    }
    
    public boolean warnOfInsufficientMemory(String facility, int requiredSize, JFrame parent) {
        return checkMemory( facility, requiredSize, "You could see problems with ", parent );
    }
    
    public boolean checkMemory( String facility, int requiredSize, String bailMessage, JFrame parent ) {
        boolean sufficient = true;
        // Must warn about memory use.
        try {
            int userSetMemoryGigs = SystemInfo.getMemoryAllocation();
            long memoryGigs = userSetMemoryGigs;
            // Prefer the user setting over all else.  If not found,
            // as in development, look at maximum of all other possibilities.
            if (memoryGigs < 0) {
                // Find the largest memory estimate, and use  for this purpose.
                MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
                memoryGigs = (memoryBean.getHeapMemoryUsage().getMax()
                        + memoryBean.getNonHeapMemoryUsage().getMax()) / GIGA;
                long tempGigs = Runtime.getRuntime().maxMemory() / GIGA;
                if (tempGigs > memoryGigs) {
                    memoryGigs = tempGigs;
                }
            }

            if (memoryGigs < requiredSize) {
                JOptionPane.showMessageDialog(
                        parent,
                        String.format(
                                "%s requires a memory size of %dGb.  Your memory setting is approximately %dGb.  %s %s.\n"
                                 + "Go to Edit/Preferences/Application Settings... and change Max Memory",
                                facility,
                                requiredSize,
                                memoryGigs,
                                bailMessage,
                                facility
                        ),
                        "Please Increase Memory",
                        JOptionPane.INFORMATION_MESSAGE
                );
                sufficient = false;
            }
            return sufficient;
        } catch (Exception ex) {
            throw new RuntimeException(MEM_ALLOC_CHECK_ERR);
        }
    }
}
