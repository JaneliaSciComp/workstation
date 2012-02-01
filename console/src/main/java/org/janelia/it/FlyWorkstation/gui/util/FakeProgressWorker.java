package org.janelia.it.FlyWorkstation.gui.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

/**
 * A SimpleWorker that fakes its progress by incrementing one percent per second. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class FakeProgressWorker extends SimpleWorker {

    private Timer timer;

    @Override
    protected Void doInBackground() throws Exception {
        timer = new Timer(1000, new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (getProgress() < 100) {
		        	setProgress(getProgress()+1);
				}
				else {
					timer.stop();
				}
			}
		});
        timer.start();
    	addPropertyChangeListener(this);
        setProgress(0);
        try {
            doStuff();
        }
        catch (Throwable e) {
            this.error = e;
        }
        return null;
    }

	@Override
	protected void done() {
		if (timer.isRunning()) timer.stop();
		super.done();
	}
}