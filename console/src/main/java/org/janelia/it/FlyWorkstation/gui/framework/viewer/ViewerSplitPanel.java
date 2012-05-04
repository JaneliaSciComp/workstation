package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.border.EtchedBorder;

import org.janelia.it.FlyWorkstation.gui.util.Icons;

/**
 * The main viewer panel that contains subviewers in tabs (or with no tabs if there is just one).
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ViewerSplitPanel extends JPanel {
	
	private boolean mainViewerOnly = true;
	private JSplitPane mainSplitPane;
	private ViewerPane mainViewerPane;
	private ViewerPane secViewerPane;
	
	public ViewerSplitPanel() {
		super(new BorderLayout());

		mainViewerPane = new ViewerPane(false);
		mainViewerPane.setLabel("Main viewer");
		secViewerPane = new ViewerPane(true);
		secViewerPane.setLabel("Secondary viewer");
		
		this.mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, mainViewerPane, secViewerPane);
		mainSplitPane.setOneTouchExpandable(false);
		mainSplitPane.setBorder(BorderFactory.createEmptyBorder());
        
        setSecViewer(null);
	}

	public Viewer getActiveViewer() {
		// TODO: determine which one is active
		return mainViewerPane.getViewer();
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
			remove(mainSplitPane);
	        add(mainViewerPane, BorderLayout.CENTER);
			revalidate();
			repaint();
		}
	}
	
	private class ViewerPane extends JPanel {
		private JLabel titleLabel;
		private Viewer viewer;
		
		public ViewerPane(boolean showHideButton) {

			setLayout(new BorderLayout());
			setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
			
	        titleLabel = new JLabel(" ");
	        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
	        JPanel mainTitlePane = new JPanel();
	        mainTitlePane.setLayout(new BoxLayout(mainTitlePane, BoxLayout.LINE_AXIS));
	        mainTitlePane.add(titleLabel);

			if (showHideButton) {
		        JButton hideButton = new JButton(Icons.getIcon("close.png"));
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
