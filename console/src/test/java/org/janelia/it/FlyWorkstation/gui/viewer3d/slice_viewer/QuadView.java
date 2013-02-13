package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import org.janelia.it.FlyWorkstation.gui.util.Icons;

public class QuadView extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected SliceViewer sliceViewer = new SliceViewer();
	protected Action panModeAction = new PanModeAction(sliceViewer);
	protected Action zoomScrollModeAction = new ZoomScrollModeAction(sliceViewer);
	protected Action zoomMouseModeAction = new ZoomMouseModeAction(sliceViewer);
	
	static {
		// Use top menu bar on Mac
		if (System.getProperty("os.name").contains("Mac")) {
			  System.setProperty("apple.laf.useScreenMenuBar", "true");
			  System.setProperty("com.apple.mrj.application.apple.menu.about.name", "QuadView");
		}
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			System.out.println("Warning: Failed to set native look and feel.");
		}
	}
	
	public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            		new QuadView();
            }
        });
	}

	public QuadView() {
		setTitle("QuadView");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // toolBar requires BorderLayout
		getContentPane().setLayout(new BorderLayout(5,5));
        // Toolbar
        createToolBar();
        // menus
        createMenus();
        // Slice widget
        getContentPane().add(sliceViewer, BorderLayout.CENTER);
        //
        createStatusBar();
        //Display the window.
        pack();
        setSize( getContentPane().getPreferredSize() );
        setLocation(100, 100); // TODO persist latest geometry
        setVisible(true);
	}
	
	protected void createMenus() {
		JMenuBar menuBar = new JMenuBar();
		JMenu menu, submenu;
		JMenuItem item;

		menu = new JMenu("File");
		item = new JMenuItem("Open Folder...");
		menu.add(item);
		menuBar.add(menu);

		menu = new JMenu("Edit");
		menuBar.add(menu);

		menu = new JMenu("View");
		submenu = new JMenu("Mouse Mode");
		// only one mouse mode is active at a time
		ButtonGroup group = new ButtonGroup();
		item = new JRadioButtonMenuItem(panModeAction);
		group.add(item);
		submenu.add(item);
		item.setSelected(true);
		item = new JRadioButtonMenuItem(zoomMouseModeAction);
		group.add(item);
		submenu.add(item);
		menu.add(submenu);
		menuBar.add(menu);

		menu = new JMenu("Help");
		menuBar.add(menu);

		setJMenuBar(menuBar);
	}
	
	protected void createStatusBar() {
		// http://stackoverflow.com/questions/3035880/how-can-i-create-a-bar-in-the-bottom-of-a-java-app-like-a-status-bar
		JPanel statusPanel = new JPanel();
		getContentPane().add(statusPanel, BorderLayout.SOUTH);
		statusPanel.setPreferredSize(new Dimension(getContentPane().getWidth(), 16));
		statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
		JLabel statusLabel = new JLabel("");
		statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
		statusPanel.add(statusLabel);		
	}

	protected void createToolBar() {
		JToolBar toolBar = new JToolBar();

		JLabel mouseModeLabel = new JLabel(Icons.getIcon("mouse_left.png"));
		mouseModeLabel.setToolTipText("Mouse mode:");
		mouseModeLabel.setEnabled(false);
		toolBar.add(mouseModeLabel);
		toolBar.addSeparator();
		toolBar.add(panModeAction);
		toolBar.add(zoomMouseModeAction);
		
		toolBar.addSeparator();
		toolBar.addSeparator();

		mouseModeLabel = new JLabel(Icons.getIcon("mouse_scroll.png"));
		mouseModeLabel.setToolTipText("Scroll wheel mode:");
		mouseModeLabel.setEnabled(false);
		toolBar.add(mouseModeLabel);
		toolBar.addSeparator();
		toolBar.add(zoomScrollModeAction);

		getContentPane().add(toolBar, BorderLayout.PAGE_START);
	}
	
}
