package org.janelia.workstation.common.gui.dialogs;

import org.janelia.workstation.core.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

/**
 * This dialog maker, informs the user when they do not have enough memory
 * for some operation/facility, and informs them they need to bump the setting.
 *
 * @author fosterl
 */
public class MemoryCheckDialog {

    private static final Logger log = LoggerFactory.getLogger(MemoryCheckDialog.class);

    private static final long GIGA = 1024*1024*1024;
    private static final String MEM_ALLOC_CHECK_ERR = "Failed to obtain memory allocation.";

    public boolean unusedIfInsufficientMemory(String facility, int requiredSize, JFrame parent) {
        return checkMemory( facility, requiredSize, "Not using", parent);
    }
    
    public boolean warnOfInsufficientMemory(String facility, int requiredSize, JFrame parent) {
        return checkMemory( facility, requiredSize, "You could see problems with", parent );
    }
    
    public boolean checkMemory( String facility, int requiredSize, String bailMessage, JFrame parent ) {
        boolean sufficient = true;
        // Must warn about memory use.
        try {
            Integer userSetMemoryGigs = Utils.getMemoryAllocation();
            Long memoryGigs = null;
            // Prefer the user setting over all else.  If not found,
            // as in development, look at maximum of all other possibilities.
            if (userSetMemoryGigs != null) {
                memoryGigs = userSetMemoryGigs.longValue();
                log.info("Got user set memory: {} GB", memoryGigs);
            }
            else {
                // Find the largest memory estimate, and use  for this purpose.
                MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
                memoryGigs = (memoryBean.getHeapMemoryUsage().getMax()
                        + memoryBean.getNonHeapMemoryUsage().getMax()) / GIGA;
                log.info("Got heap memory: {} GB", memoryGigs);
                long tempGigs = Runtime.getRuntime().maxMemory() / GIGA;
                if (tempGigs > memoryGigs) {
                    memoryGigs = tempGigs;
                    log.info("Got max runtime memory: {} GB", memoryGigs);
                }
            }

            if (memoryGigs < requiredSize) {
                JOptionPane.showMessageDialog(
                        parent,
                        String.format(
                                "%s requires a memory size of %dGb. Your memory setting is approximately %dGb. %s %s.\n"
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
        } 
        catch (Exception ex) {
            throw new RuntimeException(MEM_ALLOC_CHECK_ERR);
        }
    }
}
