package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.*;
import javax.swing.border.Border;

/**
 * The main viewer panel that contains two viewers: a main viewer and a secondary viewer that may be closed (and begins
 * in a closed state). Also implements the concept of an "active" viewer and paints a selection border around the 
 * currently active viewer. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ViewerSplitPanel extends JPanel implements ViewerContainer {

	private final Border normalBorder;
	private final Border focusBorder;
	
	private boolean mainViewerOnly = true;
	private JSplitPane mainSplitPane;
	private ViewerPane mainViewerPane;
	private ViewerPane secViewerPane;
	private ViewerPane activeViewerPane;
	
	public ViewerSplitPanel() {
		
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder());
		
		Color panelColor = (Color)UIManager.get("Panel.background");
		Color normalColor = (Color)UIManager.get("windowBorder");
		Color focusColor = (Color)UIManager.get("Focus.color");
		
		normalBorder = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(normalColor, 1), BorderFactory.createLineBorder(panelColor, 1));
		focusBorder = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(focusColor, 1), BorderFactory.createLineBorder(focusColor, 1));
		
		mainViewerPane = new ViewerPane(false);
		mainViewerPane.setTitle("");
		secViewerPane = new ViewerPane(true) {
			@Override
			protected void closeButtonPressed() {
				setSecViewer(null);
			}
		};
		secViewerPane.setTitle("");
		
		this.mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, mainViewerPane, secViewerPane);
		mainSplitPane.setOneTouchExpandable(false);
		mainSplitPane.setBorder(BorderFactory.createEmptyBorder());
        
        setSecViewer(null);
	}
	
	public JSplitPane getMainSplitPane() {
		return mainSplitPane;
	}

	@Override
	public void setAsActive(Viewer viewer) {
		activeViewerPane = getViewerPane(viewer);

		if (activeViewerPane==mainViewerPane) {
			secViewerPane.setBorder(normalBorder);
			mainViewerPane.setBorder(focusBorder);
		}
		else if (activeViewerPane==secViewerPane) {
			mainViewerPane.setBorder(normalBorder);
			secViewerPane.setBorder(focusBorder);
		}
		else {
			throw new IllegalArgumentException("Unknown viewer with class "+viewer.getClass().getName());
		}
	}

	@Override
	public void setTitle(Viewer viewer, String title) {
		ViewerPane viewerPane = getViewerPane(viewer);
		if (viewerPane!=null) {
			viewerPane.setTitle(title);
		}
	}
	
	public Viewer getActiveViewer() {
		return activeViewerPane.getViewer();
	}
	
	public Viewer getMainViewer() {
		return mainViewerPane.getViewer();
	}

	public Viewer getSecViewer() {
		return secViewerPane.getViewer();
	}

	public void setMainViewer(Viewer viewer) {
		mainViewerPane.setViewer(viewer);
		mainViewerPane.setVisible(viewer!=null);
		setAsActive(viewer);
	}
	
	public void setSecViewer(Viewer viewer) {
		secViewerPane.setViewer(viewer);
		secViewerPane.setVisible(viewer!=null);
		
		if (viewer!=null) {
			if (mainViewerOnly) {
				remove(mainViewerPane);
				mainSplitPane.setLeftComponent(mainViewerPane);
				mainSplitPane.setRightComponent(secViewerPane);
				add(mainSplitPane, BorderLayout.CENTER);
				revalidate();
				repaint();
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						mainSplitPane.setDividerLocation(0.5);
					}
				});	
			}
			mainViewerOnly = false;
		}
		else {
			mainViewerOnly = true;
			activeViewerPane = mainViewerPane;
			remove(mainSplitPane);
	        add(mainViewerPane, BorderLayout.CENTER);
			revalidate();
			repaint();
		}

		if (mainViewerPane.getViewer()!=null) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					// always refresh the main viewer if something changes with the secondary viewer
					mainViewerPane.getViewer().refresh();
				}
			});	
		}
	}

	private ViewerPane getViewerPane(Viewer viewer) {
		if (mainViewerPane.getViewer()==viewer) {
			return mainViewerPane;
		}
		else if (secViewerPane.getViewer()==viewer) {
			return secViewerPane;
		}
		else {
			throw new IllegalArgumentException("Unknown viewer with class "+viewer.getClass().getName());
		}
	}
}
