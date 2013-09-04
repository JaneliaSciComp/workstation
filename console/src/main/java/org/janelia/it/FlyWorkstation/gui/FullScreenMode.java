package org.janelia.it.FlyWorkstation.gui;

import java.awt.DisplayMode;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * FullScreenMode is a decorator that allows any java.awt.Frame to be toggled
 * in and out of full screen mode using the "f" key.
 * 
 * @author brunsc
 *
 */
public class FullScreenMode 
implements KeyListener
{

	private boolean isFullScreen = false;
	private DisplayMode displayModeOld;
	private DisplayMode displayModeFullScreen;
	private GraphicsDevice screen;
	private Frame window;
	
	public FullScreenMode(Frame window) {
		this.window = window;
	};
	
	private void selectScreen() {
		// Get GraphicsDevice for current screen
		GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] screens = environment.getScreenDevices();
		screen = screens[0]; // default to first screen
		// Try to use the screen this window is in
		Rectangle windowBounds = window.getBounds();
		for (GraphicsDevice s : screens) {
			Rectangle screenBounds = s.getDefaultConfiguration().getBounds();
			if (screenBounds.intersects(windowBounds)) {
				screen = s;
				break; // use first matching screen
			}
		}		
	}
	
	private void selectDisplayMode(int w, int h) {
		if (screen == null)
			selectScreen();
		DisplayMode[] availableModes = screen.getDisplayModes();
		for (DisplayMode d : availableModes) {
			if (d.getWidth() != w)
				continue;
			if (d.getHeight() != h)
				continue;
			/*
			System.out.println(d.getWidth()+"x"
					+d.getHeight()+"@"
					+d.getRefreshRate()+"Hz"
					+d.getBitDepth()+"bpp");
					*/
			displayModeFullScreen = d;
			return;
		}
		displayModeFullScreen = null; // found no matches
	}
	
	public boolean showFullScreen(boolean doFullScreen) {
		if (screen == null) {
			selectScreen();
		}
		
		if (doFullScreen) {
			if (! screen.isFullScreenSupported())
				return false;
			if (isFullScreen)
				return false;
			displayModeOld = screen.getDisplayMode();
			// hide everything
			window.setVisible(false);
			// remove the frame from being displayable
			window.dispose();
			// remove the borders from the frame
			window.setUndecorated(true);
			// make this window full screen
			screen.setFullScreenWindow(window);
			//
			selectDisplayMode(1280, 800);
			if (displayModeFullScreen != null) {
				window.setSize(displayModeFullScreen.getWidth(), 
						displayModeFullScreen.getHeight());
				screen.setDisplayMode(displayModeFullScreen);
			}
			// show this frame
			window.setVisible(true);
			// remember this is full screen
			isFullScreen = true;
		}
		else {
			if (! isFullScreen)
				return false;
			// Use the same screen we used to raise full screen!
			screen.setDisplayMode(displayModeOld);
			// hide the screen so we can change it
			window.setVisible(false);
			// remove the frame from being displayable
			window.dispose();
			// put the borders back on the frame
			window.setUndecorated(false);
			// exit full screen mode
			screen.setFullScreenWindow(null);
			// show this window
			window.setVisible(true);
			// remember this is not full screen
			isFullScreen = false;
			screen = null; // so maybe it could be another screen next time.
		}
		return false;
	}

	@Override
	public void keyTyped(KeyEvent event) {}

	@Override
	public void keyPressed(KeyEvent event) {}

	@Override
	public void keyReleased(KeyEvent event) {
		// Use "f" key to toggle full screen mode
		if (event.getKeyCode() == KeyEvent.VK_F) {
			if (isFullScreen) {
				showFullScreen(false);
			}
			else
			{
				showFullScreen(true);
			}
		}
		// Use ESC key to exit full screen mode
		else if (event.getKeyCode() == KeyEvent.VK_ESCAPE) {
			showFullScreen(false);
		}		
	}
}
