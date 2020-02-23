package org.janelia.workstation.gui.large_volume_viewer;

import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.UIManager;

import org.janelia.rendering.utils.ClientProxy;
import org.janelia.workstation.core.api.http.RestJsonClientManager;
import org.janelia.workstation.core.api.web.JadeServiceClient;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.controller.AnnotationModel;

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
                    JadeServiceClient jadeServiceClient = new JadeServiceClient(
                            ConsoleProperties.getString("jadestorage.rest.url"),
                            () -> new ClientProxy(RestJsonClientManager.getInstance().getHttpClient(true), false)
                    );
                    app.setContentPane(QuadViewUiProvider.createQuadViewUi(app, null, true, new AnnotationModel(null, null), jadeServiceClient));
                    app.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
