package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.BoxLayout;
import java.awt.Dimension;
import javax.swing.JToolBar;
import javax.swing.JLabel;
import javax.swing.JSplitPane;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.JButton;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.UIManager;

import java.awt.Color;
import javax.swing.JSpinner;
import javax.swing.Action;
import javax.swing.JMenuItem;

import org.janelia.it.FlyWorkstation.gui.viewer3d.camera.BasicObservableCamera3d;
import javax.swing.JSeparator;
import java.awt.Component;
import javax.swing.Box;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.VolumeImage3d;
import javax.swing.JToggleButton;
import java.awt.Insets;
import javax.swing.ImageIcon;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.border.EtchedBorder;

public class QuadViewUi extends JFrame {

	private static final long serialVersionUID = 1L;

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

	private JPanel contentPane;
	
	// One shared camera for all viewers.
	// (there's only one viewer now actually, but you know...)
	private BasicObservableCamera3d camera = new BasicObservableCamera3d();
	private SliceViewer sliceViewer = new SliceViewer();
	private final Action zoomInAction = new ZoomInAction(camera);
	private final ZoomOutAction zoomOutAction = new ZoomOutAction(camera);
	private final ResetViewAction resetViewAction = new ResetViewAction(sliceViewer);
	private final ZoomMaxAction zoomMaxAction = new ZoomMaxAction(camera, sliceViewer);
	private final ResetZoomAction resetZoomAction = new ResetZoomAction(sliceViewer);
	private final ZoomMouseModeAction zoomMouseModeAction = new ZoomMouseModeAction(sliceViewer);
	private final PanModeAction panModeAction = new PanModeAction(sliceViewer);
	private final ButtonGroup mouseModeGroup = new ButtonGroup();
	private final ZScanScrollModeAction scanScrollModeAction = new ZScanScrollModeAction(sliceViewer, sliceViewer);
	private final ZoomScrollModeAction zoomScrollModeAction = new ZoomScrollModeAction(sliceViewer);
	private final ButtonGroup scrollModeGroup = new ButtonGroup();
	private final Action nextZSliceAction = new NextZSliceAction(sliceViewer, sliceViewer);
	private final Action previousZSliceAction = new PreviousZSliceAction(sliceViewer, sliceViewer);
	private final Action advanceZSlicesAction = new AdvanceZSlicesAction(sliceViewer, sliceViewer, 10);
	private final Action goBackZSlicesAction = new GoBackZSlicesAction(sliceViewer, sliceViewer, -10);

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					QuadViewUi frame = new QuadViewUi();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public QuadViewUi() {
		setResizable(false);
		setTitle("QuadView");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 967, 726);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
		
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		JMenu mnEdit = new JMenu("Edit");
		menuBar.add(mnEdit);
		
		JMenu mnView = new JMenu("View");
		menuBar.add(mnView);
		
		JMenu mnMouseMode = new JMenu("Mouse Mode");
		mnMouseMode.setIcon(new ImageIcon(QuadViewUi.class.getResource("/images/mouse_left.png")));
		mnView.add(mnMouseMode);
		
		JRadioButtonMenuItem panModeItem = new JRadioButtonMenuItem("New radio item");
		panModeItem.setSelected(true);
		panModeItem.setAction(panModeAction);
		mnMouseMode.add(panModeItem);
		
		JRadioButtonMenuItem zoomMouseModeItem = new JRadioButtonMenuItem("New radio item");
		zoomMouseModeItem.setAction(zoomMouseModeAction);
		mnMouseMode.add(zoomMouseModeItem);
		
		JMenu mnScrollMode = new JMenu("Scroll Mode");
		mnScrollMode.setIcon(new ImageIcon(QuadViewUi.class.getResource("/images/mouse_scroll.png")));
		mnView.add(mnScrollMode);
		
		JRadioButtonMenuItem rdbtnmntmNewRadioItem = new JRadioButtonMenuItem("New radio item");
		rdbtnmntmNewRadioItem.setSelected(true);
		rdbtnmntmNewRadioItem.setAction(scanScrollModeAction);
		mnScrollMode.add(rdbtnmntmNewRadioItem);
		
		JRadioButtonMenuItem mntmNewMenuItem_2 = new JRadioButtonMenuItem("New menu item");
		mntmNewMenuItem_2.setAction(zoomScrollModeAction);
		mnScrollMode.add(mntmNewMenuItem_2);
		
		JSeparator separator = new JSeparator();
		mnView.add(separator);
		
		JMenu mnZoom = new JMenu("Zoom");
		mnView.add(mnZoom);
		
		JMenuItem zoomMinItem = mnZoom.add(resetZoomAction);
		
		JMenuItem zoomOutItem = mnZoom.add(zoomOutAction);
		
		JMenuItem zoomInItem = mnZoom.add(zoomInAction);
		
		JMenuItem zoomMaxItem = mnZoom.add(zoomMaxAction);
		
		JSeparator separator_1 = new JSeparator();
		mnView.add(separator_1);
		
		JMenu mnZScan = new JMenu("Z Scan");
		mnView.add(mnZScan);
		
		JMenuItem mntmNewMenuItem = new JMenuItem("New menu item");
		mntmNewMenuItem.setAction(goBackZSlicesAction);
		mnZScan.add(mntmNewMenuItem);
		
		JMenuItem menuItem_2 = new JMenuItem("New menu item");
		menuItem_2.setAction(previousZSliceAction);
		mnZScan.add(menuItem_2);
		
		JMenuItem menuItem_1 = new JMenuItem("New menu item");
		menuItem_1.setAction(nextZSliceAction);
		mnZScan.add(menuItem_1);
		
		JMenuItem menuItem = new JMenuItem("New menu item");
		menuItem.setAction(advanceZSlicesAction);
		mnZScan.add(menuItem);
		
		JPanel toolBarPanel = new JPanel();
		contentPane.add(toolBarPanel);
		toolBarPanel.setLayout(new BorderLayout(0, 0));
		
		JToolBar toolBar = new JToolBar();
		toolBarPanel.add(toolBar, BorderLayout.NORTH);
		
		JLabel lblNewLabel_1 = new JLabel("");
		lblNewLabel_1.setToolTipText("Mouse Mode:");
		lblNewLabel_1.setFocusable(false);
		lblNewLabel_1.setIcon(new ImageIcon(QuadViewUi.class.getResource("/images/mouse_left.png")));
		toolBar.add(lblNewLabel_1);
		
		JToggleButton tglBtnPanMode = new JToggleButton("");
		mouseModeGroup.add(tglBtnPanMode);
		tglBtnPanMode.setSelected(true);
		tglBtnPanMode.setAction(panModeAction);
		tglBtnPanMode.setMargin(new Insets(0, 0, 0, 0));
		tglBtnPanMode.setHideActionText(true);
		tglBtnPanMode.setFocusable(false);
		toolBar.add(tglBtnPanMode);
		
		JToggleButton tglbtnZoomMouseMode = new JToggleButton("");
		mouseModeGroup.add(tglbtnZoomMouseMode);
		tglbtnZoomMouseMode.setMargin(new Insets(0, 0, 0, 0));
		tglbtnZoomMouseMode.setFocusable(false);
		tglbtnZoomMouseMode.setHideActionText(true);
		tglbtnZoomMouseMode.setAction(zoomMouseModeAction);
		toolBar.add(tglbtnZoomMouseMode);

		toolBar.addSeparator();
		
		JLabel scrollModeLabel = new JLabel("");
		scrollModeLabel.setIcon(new ImageIcon(QuadViewUi.class.getResource("/images/mouse_scroll.png")));
		scrollModeLabel.setFocusable(false);
		toolBar.add(scrollModeLabel);
		
		JToggleButton toggleButton = new JToggleButton("");
		scrollModeGroup.add(toggleButton);
		toggleButton.setSelected(true);
		toggleButton.setAction(scanScrollModeAction);
		toggleButton.setMargin(new Insets(0, 0, 0, 0));
		toggleButton.setHideActionText(true);
		toggleButton.setFocusable(false);
		toolBar.add(toggleButton);
		
		JToggleButton toggleButton_1 = new JToggleButton("");
		scrollModeGroup.add(toggleButton_1);
		toggleButton_1.setAction(zoomScrollModeAction);
		toggleButton_1.setMargin(new Insets(0, 0, 0, 0));
		toggleButton_1.setHideActionText(true);
		toggleButton_1.setFocusable(false);
		toolBar.add(toggleButton_1);
		
		toolBar.addSeparator();
		
		JSplitPane splitPane = new JSplitPane();
		splitPane.setResizeWeight(0.95);
		splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		toolBarPanel.add(splitPane, BorderLayout.CENTER);
		
		JPanel colorPanel = new JPanel();
		splitPane.setRightComponent(colorPanel);
		colorPanel.setLayout(new BoxLayout(colorPanel, BoxLayout.Y_AXIS));
		
		TripleSlider slider_1 = new TripleSlider();
		colorPanel.add(slider_1);
		
		JSplitPane splitPane_1 = new JSplitPane();
		splitPane_1.setResizeWeight(1.00);
		splitPane.setLeftComponent(splitPane_1);
		
		JPanel viewerPanel = new JPanel();
		splitPane_1.setLeftComponent(viewerPanel);
		viewerPanel.setLayout(new BoxLayout(viewerPanel, BoxLayout.Y_AXIS));
		
		// SliceViewer sliceViewer = new SliceViewer();
		sliceViewer.setCamera(camera);
		sliceViewer.setBackground(Color.DARK_GRAY);
		viewerPanel.add(sliceViewer);
		
		JPanel zScanPanel = new JPanel();
		viewerPanel.add(zScanPanel);
		zScanPanel.setLayout(new BoxLayout(zScanPanel, BoxLayout.X_AXIS));
		
		ToolButton button_2 = new ToolButton(goBackZSlicesAction);
		button_2.setAction(goBackZSlicesAction);
		button_2.setMargin(new Insets(0, 0, 0, 0));
		button_2.setHideActionText(true);
		button_2.setAlignmentX(0.5f);
		zScanPanel.add(button_2);
		
		ToolButton button_1 = new ToolButton(previousZSliceAction);
		button_1.setAction(previousZSliceAction);
		button_1.setMargin(new Insets(0, 0, 0, 0));
		button_1.setHideActionText(true);
		button_1.setAlignmentX(0.5f);
		zScanPanel.add(button_1);
		
		JSlider zScanSlider = new JSlider();
		zScanSlider.setPreferredSize(new Dimension(32767, 29));
		zScanSlider.setMajorTickSpacing(10);
		zScanSlider.setPaintTicks(true);
		zScanPanel.add(zScanSlider);
		
		ToolButton button_3 = new ToolButton(nextZSliceAction);
		button_3.setAction(nextZSliceAction);
		button_3.setMargin(new Insets(0, 0, 0, 0));
		button_3.setHideActionText(true);
		button_3.setAlignmentX(0.5f);
		zScanPanel.add(button_3);
		
		ToolButton button_4 = new ToolButton(advanceZSlicesAction);
		button_4.setAction(advanceZSlicesAction);
		button_4.setMargin(new Insets(0, 0, 0, 0));
		button_4.setHideActionText(true);
		button_4.setAlignmentX(0.5f);
		zScanPanel.add(button_4);
		
		JSpinner zSliceSpinner = new JSpinner();
		zSliceSpinner.setPreferredSize(new Dimension(75, 28));
		zSliceSpinner.setMaximumSize(new Dimension(120, 28));
		zSliceSpinner.setMinimumSize(new Dimension(65, 28));
		zScanPanel.add(zSliceSpinner);
		
		JPanel controlsPanel = new JPanel();
		splitPane_1.setRightComponent(controlsPanel);
		controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.X_AXIS));
		
		JPanel panel_1 = new JPanel();
		panel_1.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		controlsPanel.add(panel_1);
		panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.Y_AXIS));
		
		ToolButton btnNewButton_2 = new ToolButton(zoomInAction);
		btnNewButton_2.setAlignmentX(0.5f);
		btnNewButton_2.setMargin(new Insets(0, 0, 0, 0));
		btnNewButton_2.setHideActionText(true);
		btnNewButton_2.setAction(zoomInAction);
		panel_1.add(btnNewButton_2);
		
		JSlider slider = new JSlider();
		slider.setOrientation(SwingConstants.VERTICAL);
		panel_1.add(slider);
		
		ToolButton button = new ToolButton(zoomOutAction);
		button.setAction(zoomOutAction);
		button.setMargin(new Insets(0, 0, 0, 0));
		button.setHideActionText(true);
		button.setAlignmentX(0.5f);
		panel_1.add(button);
		
		JPanel buttonsPanel = new JPanel();
		controlsPanel.add(buttonsPanel);
		buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
		
		JButton btnNewButton_1 = new JButton("New button");
		btnNewButton_1.setAction(resetZoomAction);
		buttonsPanel.add(btnNewButton_1);
		
		JButton btnNewButton = new JButton("New button");
		btnNewButton.setAction(zoomMaxAction);
		buttonsPanel.add(btnNewButton);
		
		JButton resetViewButton = new JButton("New button");
		resetViewButton.setAction(resetViewAction);
		buttonsPanel.add(resetViewButton);
		
		Component verticalGlue = Box.createVerticalGlue();
		buttonsPanel.add(verticalGlue);
		
		JPanel statusBar = new JPanel();
		statusBar.setMaximumSize(new Dimension(32767, 30));
		statusBar.setMinimumSize(new Dimension(10, 30));
		contentPane.add(statusBar);
		statusBar.setLayout(new BoxLayout(statusBar, BoxLayout.X_AXIS));
		
		JLabel lblNewLabel = new JLabel("status area");
		statusBar.add(lblNewLabel);
	}

}
