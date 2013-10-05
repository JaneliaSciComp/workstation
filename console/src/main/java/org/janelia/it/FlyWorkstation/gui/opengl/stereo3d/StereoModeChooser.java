package org.janelia.it.FlyWorkstation.gui.opengl.stereo3d;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Vector;

import javax.media.opengl.GLDrawable;
import javax.media.opengl.awt.GLCanvas;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.janelia.it.FlyWorkstation.signal.Signal1;

public class StereoModeChooser 
{
    private StereoMode stereoMode = null;
    private List<StereoAction> stereoActions = new Vector<StereoAction>();
    boolean bEyesSwapped = false;
    private Action swapEyesAction = new AbstractAction("Swap Eyes") {
        {
            // Initialize SELECTED_KEY, otherwise it remains null
            putValue(Action.SELECTED_KEY, bEyesSwapped);
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            boolean doSwap = getValue(Action.SELECTED_KEY) == Boolean.TRUE;
            setEyesSwapped(doSwap);
        }
    };
    
    public Signal1<StereoMode> stereoModeChangedSignal = new Signal1<StereoMode>();
	private GLDrawable glPanel;
    
	
    public StereoModeChooser(GLDrawable glPanel) 
    {
    	this.glPanel = glPanel;
    	
    	// Create a series of swing Actions.
    	stereoActions.add(new StereoAction(new MonoStereoMode(), "Off (Monoscopic)"));
    	stereoActions.add(new StereoAction(new LeftEyeStereoMode(), "Left Eye View"));
    	stereoActions.add(new StereoAction(new RightEyeStereoMode(), "Right Eye View"));
    	stereoActions.add(new StereoAction(new LeftRightStereoMode(), "Side-by-Side"));
    	stereoActions.add(new StereoAction(new AnaglyphGreenMagentaStereoMode(), "Green/Magenta"));
    	stereoActions.add(new StereoAction(new AnaglyphRedCyanStereoMode(), "Red/Cyan"));
    	stereoActions.add(new HardwareStereoAction(
    			new HardwareStereoMode(), "Quad Buffered"));
    	
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
        stereoActions.get(0).select(); // Check mark by "Mono" option
		menu.addSeparator();
		menu.add(new JCheckBoxMenuItem(swapEyesAction));
		return menu;
	}
	
	public void setEyesSwapped(boolean isSwapped) {
	    if (bEyesSwapped == isSwapped)
	        return; // no change
        for (StereoAction action : stereoActions) {
            action.getMode().setEyesSwapped(isSwapped);
        }
        bEyesSwapped = isSwapped;
        stereoModeChangedSignal.emit(stereoMode);
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
			select();
			stereoModeChangedSignal.emit(mode);
		}
		
		public void select() {
			putValue(Action.SELECTED_KEY, Boolean.TRUE);
			stereoMode = mode;
		}
	}
	
	class HardwareStereoAction extends StereoAction 
	{
		HardwareStereoMode hardwareMode;
		
		HardwareStereoAction(HardwareStereoMode mode, String menuString) {
			super(mode, menuString);
			hardwareMode = mode;
		}
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			// Grey out option if it cannot be supported
			if (! hardwareMode.canDisplay(glPanel)) {
				setEnabled(false);
				putValue(Action.SELECTED_KEY, Boolean.FALSE);
				return;
			}
			else
				super.actionPerformed(arg0);
		}
	}

}
