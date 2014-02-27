package org.janelia.it.FlyWorkstation.gui.alignment_board;

import org.openide.modules.OnStart;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/**
 * This runnable should be fired as soon as the module starts up.
 * 
 * @author fosterl
 */
//@OnStart
public class AlignmentBoardModuleStarter implements Runnable {
    public void run() {
        InstanceContent ic = new InstanceContent();
        
        // Q: how to make this known to global lookup system?
        AbstractLookup lup = new AbstractLookup( ic );
        ic.add( new Launcher() );
    }
}
