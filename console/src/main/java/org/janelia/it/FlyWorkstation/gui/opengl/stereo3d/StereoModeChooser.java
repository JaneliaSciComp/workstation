package org.janelia.it.FlyWorkstation.gui.opengl.stereo3d;

import java.awt.CheckboxMenuItem;
import java.awt.Component;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Vector;

import javax.media.opengl.awt.GLJPanel;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.janelia.it.FlyWorkstation.signal.Signal1;

public class StereoModeChooser {
    private StereoMode stereoMode = null;

    private List<StereoAction> stereoActions = new Vector<StereoAction>();
    
    public Signal1<StereoMode> stereoModeChangedSignal = new Signal1<StereoMode>();
    
	private Component glPanel;
	
    public StereoModeChooser(Component glPanel) 
    {
    	this.glPanel = glPanel;
    	// Create a series of swing Actions.
    	stereoActions.add(new StereoAction(new MonoStereoMode(), "Off (Monoscopic)"));
    	stereoActions.add(new StereoAction(new LeftEyeStereoMode(), "Left Eye View"));
    	stereoActions.add(new StereoAction(new RightEyeStereoMode(), "Right Eye View"));
    	stereoActions.add(new StereoAction(new LeftRightStereoMode(), "Side-by-Side"));
    	stereoActions.add(new StereoAction(new AnaglyphGreenMagentaStereoMode(), "Green/Magenta"));
    	stereoActions.add(new StereoAction(new AnaglyphRedCyanStereoMode(), "Red/Cyan"));
    	stereoActions.add(new StereoAction(new HardwareStereoMode(), "Quad Buffered"));
    	
    	stereoMode = stereoActions.get(0).getMode();
    }
    
	public JMenuItem createJMenuItem() {
		JMenu menu = new JMenu("Stereo 3D");
		ButtonGroup buttonGroup = new ButtonGroup();
		for (StereoAction action : stereoActions) {
			JCheckBoxMenuItem item = new JCheckBoxMenuItem(action);
			buttonGroup.add(item);
			menu.add(item);
		}
		stereoActions.get(0).select();
		return menu;
	}
	
	@SuppressWarnings("serial")
	class StereoAction extends AbstractAction {
		StereoMode mode;
		StereoAction(StereoMode mode, String menuString) {
			super(menuString);
			this.mode = mode;
		}
		
		public StereoMode getMode() {
			return mode;
		}
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (stereoMode == mode)
				return; // no change
			mode.reshape(glPanel.getWidth(), glPanel.getHeight());
			stereoModeChangedSignal.emit(mode);
		}
		
		public void select() {
			putValue(Action.SELECTED_KEY, Boolean.TRUE);
		}
	}

}
