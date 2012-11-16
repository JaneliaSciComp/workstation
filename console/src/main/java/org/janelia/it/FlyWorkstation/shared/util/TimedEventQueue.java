package org.janelia.it.FlyWorkstation.shared.util;

import java.awt.AWTEvent;
import java.awt.EventQueue;

/**
 * Useful profiling class from http://stackoverflow.com/questions/5541493/how-do-i-profile-the-edt-in-java-swing
 * Enable by uncommenting in ConsoleApp.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TimedEventQueue extends EventQueue {
    @Override
    protected void dispatchEvent(AWTEvent event) {
        long startNano = System.nanoTime();
        super.dispatchEvent(event);
        long endNano = System.nanoTime();
        if (endNano - startNano > 50000000)
            System.out.println(((endNano - startNano) / 1000000)+"ms: "+event);
    }
}