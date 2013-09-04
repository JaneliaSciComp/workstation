package org.janelia.it.FlyWorkstation.gui.slice_viewer;

import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.UIManager;

public class QuadViewApp extends JFrame {
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

	public QuadViewApp() {
        setTitle("QuadView");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        setBounds(100, 100, 994, 653);
        QuadViewUi contentPane = new QuadViewUi(this, null, true);
        setContentPane(contentPane);
	}
	
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					QuadViewApp app = new QuadViewApp();
					app.setVisible(true);
                } catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

}
