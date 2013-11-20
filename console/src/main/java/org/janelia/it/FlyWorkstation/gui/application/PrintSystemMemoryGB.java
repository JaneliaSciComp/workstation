package org.janelia.it.FlyWorkstation.gui.application;

import org.janelia.it.FlyWorkstation.shared.util.SystemInfo;

public class PrintSystemMemoryGB {

    public static void main(String[] args) {
        System.out.println(bytesToGb(SystemInfo.getTotalSystemMemory()));
    }
    
    private static double bytesToGb(double bytes) {
        return bytes/1024/1024/1024;
    }
}