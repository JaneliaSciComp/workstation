package org.janelia.it.workstation.gui.application;

import org.janelia.it.workstation.shared.util.SystemInfo;

/**
 * Prints the approximate amount of system memory in gigabytes to STDOUT. Rounds to the nearest even number below the 
 * actual value, so if the system reports 31.0 or 31.9, it will print 30. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class PrintSystemMemoryGB {

    public static void main(String[] args) {
    	Long mem = SystemInfo.getTotalSystemMemory();
    	if (mem!=null) {
    		mem = (long)round(bytesToGb(mem), 2);
    		System.out.println(mem);
    	}

//    	System.out.println("28.0 -> " + round(28.0, 2));
//    	System.out.println("28.1 -> " + round(28.1, 2));
//    	System.out.println("28.8 -> " + round(28.8, 2));
//    	System.out.println("29.9 -> " + round(29.9, 2));
//		System.out.println("31.2 -> " + round(31.2, 2));
//		System.out.println("31.0 -> " + round(31.0, 2));
    }
    
    /**
     * Convert from bytes to gigabytes.
     * @param bytes
     * @return
     */
    private static double bytesToGb(double bytes) {
        return bytes/1024/1024/1024;
    }
    
    /**
     * Round i to the nearest multiple of v.
     */
    private static int round(double i, int v) {
    	return (int)(Math.floor(i/v) * v);
    }
}