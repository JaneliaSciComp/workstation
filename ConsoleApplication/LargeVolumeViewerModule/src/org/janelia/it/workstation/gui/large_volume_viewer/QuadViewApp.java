package org.janelia.it.workstation.gui.large_volume_viewer;

import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.UIManager;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationModel;

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

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    QuadViewApp app = new QuadViewApp();
                    app.setTitle("QuadView");
                    app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    app.setResizable(true);
                    app.setBounds(100, 100, 994, 653);
                    app.setContentPane(new URLBasedQuadViewUi(app, null, true, new AnnotationModel(null, null)));
                    app.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
