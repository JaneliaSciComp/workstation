package org.janelia.it.workstation.run_config;

/**
 * This is a Run Configuration launchable. There must always be a Java.main to execute (if using Application)
 * so that the Ant launch that _really_ runs the application can have something to precede.
 * Please do not delete this file.
 *
 * Created by fosterl on 5/22/14.
 */
public class EndPoint {
    public static void main( String[] args ) throws Exception {
        System.out.println("End point for IntelliJ Run Configuration.  Retain this for that purpose.");
    }
}
