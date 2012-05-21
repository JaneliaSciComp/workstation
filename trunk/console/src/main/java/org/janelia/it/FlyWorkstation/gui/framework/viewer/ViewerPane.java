package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.gui.util.Icons;

/**
 * A wrapper around a Viewer that provides a title bar and a close button.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ViewerPane extends JPanel {
	
	private static final Font titleLabelFont = new Font("Sans Serif", Font.PLAIN, 12);
	
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
					closeButtonPressed();
				}
			});
	        mainTitlePane.add(Box.createHorizontalGlue());
	        mainTitlePane.add(hideButton);
		}
		
        add(mainTitlePane, BorderLayout.NORTH);
	}
	
	protected void closeButtonPressed() {
		throw new UnsupportedOperationException("This method has not been implemented for this ViewerPane instance");
	}
	
	public void clearViewer() {
		if (this.viewer!=null) {
			remove(this.viewer);
		}
		viewer = null;
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

	public void setTitle(String title) {
		titleLabel.setText(title);
	}
}