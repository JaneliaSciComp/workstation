package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.border.Border;

import org.janelia.it.FlyWorkstation.gui.util.Icons;

/**
 * The main viewer panel that contains subviewers in tabs (or with no tabs if there is just one).
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ViewerSplitPanel extends JPanel {
	
	private static final Font titleLabelFont = new Font("Sans Serif", Font.PLAIN, 12);
	
	private boolean mainViewerOnly = true;
	private JSplitPane mainSplitPane;
	private ViewerPane mainViewerPane;
	private ViewerPane secViewerPane;
	private ViewerPane activeViewerPane;
	
	private final Border normalBorder;
	private final Border focusBorder;
	
	public ViewerSplitPanel() {
		super(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder());
		
		Color panelColor = (Color)UIManager.get("Panel.background");
		Color normalColor = (Color)UIManager.get("windowBorder");
		Color focusColor = (Color)UIManager.get("Focus.color");
		
		normalBorder = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(normalColor, 1), BorderFactory.createLineBorder(panelColor, 1));
		focusBorder = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(focusColor, 1), BorderFactory.createLineBorder(focusColor, 1));
		
		mainViewerPane = new ViewerPane(false);
		mainViewerPane.setLabel("");
		secViewerPane = new ViewerPane(true);
		secViewerPane.setLabel("");
		
		this.mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, mainViewerPane, secViewerPane);
		mainSplitPane.setOneTouchExpandable(false);
		mainSplitPane.setBorder(BorderFactory.createEmptyBorder());
        
        setSecViewer(null);
	}
	
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

	public void setTitle(Viewer viewer, String title) {
		activeViewerPane = getViewerPane(viewer);
		activeViewerPane.setLabel(title);
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
	
	private class ViewerPane extends JPanel {
		private JLabel titleLabel;
		private Viewer viewer;
		
		public ViewerPane(boolean showHideButton) {
			
			setLayout(new BorderLayout());
			
	        titleLabel = new JLabel(" ");
	        titleLabel.setBorder(BorderFactory.createEmptyBorder(1, 5, 3, 0));
	        titleLabel.setFont(titleLabelFont);
	        
	        JPanel mainTitlePane = new JPanel();
	        mainTitlePane.setLayout(new BoxLayout(mainTitlePane, BoxLayout.LINE_AXIS));
	        mainTitlePane.add(titleLabel);
	        
			if (showHideButton) {
		        JButton hideButton = new JButton(Icons.getIcon("close_red.png"));
		        hideButton.setPreferredSize(new Dimension(16, 16));
		        hideButton.setBorderPainted(false);
		        hideButton.setToolTipText("Close this viewer");
		        hideButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						setSecViewer(null);
					}
				});
		        mainTitlePane.add(Box.createHorizontalGlue());
		        mainTitlePane.add(hideButton);
			}
			
	        add(mainTitlePane, BorderLayout.NORTH);
		}
		
		public void clearViewer() {
			if (this.viewer!=null) {
				remove(this.viewer);
			}
			viewer = null;
		}

		public void setLabel(String label) {
			titleLabel.setText(label);
		}

		public void setViewer(Viewer viewer) {
			clearViewer();
			this.viewer = viewer;
			if (viewer!=null) {
				add(viewer, BorderLayout.CENTER);
			}
		}
		
		public Viewer getViewer() {
			return viewer;
		}
	}
}
