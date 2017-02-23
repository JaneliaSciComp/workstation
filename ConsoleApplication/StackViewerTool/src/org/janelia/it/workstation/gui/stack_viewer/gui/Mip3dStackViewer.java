package org.janelia.it.workstation.gui.stack_viewer.gui;

import org.janelia.it.workstation.gui.opengl.GLActor;
import org.janelia.it.workstation.gui.static_view.RGBExcludableVolumeBrick;
import org.janelia.it.workstation.gui.viewer3d.resolver.TrivialFileResolver;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataI;
import org.janelia.it.workstation.gui.viewer3d.Mip3d;
import org.janelia.it.workstation.gui.viewer3d.VolumeBrickActorBuilder;
import org.janelia.it.workstation.gui.viewer3d.VolumeModel;

import org.janelia.it.workstation.gui.viewer3d.VolumeBrickFactory;
import org.janelia.it.workstation.gui.viewer3d.VolumeBrickI;

import javax.swing.*;
import org.janelia.it.workstation.browser.workers.SimpleWorker;

/**
 * View a wide variety of stack formats used in the workstation and JACS.
 *
 * @author brunsc
 * @author fosterl
 */
public class Mip3dStackViewer {

    private JFrame frame;

    /**
     * This launches a GUI frame at time of writing.
     *
     * @param args
     */
    public void launch(final String... args) {
        SimpleWorker worker = new SimpleWorker() {
            private Mip3d mipWidget;
            private GLActor actor;

            @Override
            protected void doStuff() throws Exception {
                System.out.println("Has JavaCPP? " + System.getProperty("java.class.path").contains("javacpp"));
                String fn = "C:\\Users\\FOSTERL\\Documents\\jvone-79\\raw\\ConsolidatedLabel.v3dpbd";

                // All black.
                if (args.length > 0) {
                    fn = args[ 0];
                }

                VolumeBrickFactory factory = new VolumeBrickFactory() {
                    @Override
                    public VolumeBrickI getVolumeBrick(VolumeModel model) {
                        return new RGBExcludableVolumeBrick(model);
                    }

                    @Override
                    public VolumeBrickI getVolumeBrick(VolumeModel model, TextureDataI maskTextureData, TextureDataI colorMapTextureData) {
                        return null;
                    }
                };

                mipWidget = new Mip3d();
                VolumeBrickActorBuilder actorBuilder = new VolumeBrickActorBuilder();
                actor = actorBuilder.buildVolumeBrickActor(
                        mipWidget.getVolumeModel(), factory, new TrivialFileResolver(), fn
                );

                if (actor == null) {
                    throw new Exception("Volume load failed.");
                }
            }

            @Override
            protected void hadSuccess() {
                frame = new JFrame("Mip3d Stack Viewer");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                JLabel label = new JLabel("Mip3d Stack Viewer");
                frame.getContentPane().add(label);
                mipWidget.clear();
                mipWidget.addActor(actor);
                frame.getContentPane().add(mipWidget);

                //Display the window.
                frame.pack();
                frame.setSize(frame.getContentPane().getPreferredSize());
                frame.setVisible(true);
            }

            @Override
            protected void hadError(Throwable error) {
                JOptionPane.showMessageDialog(frame, error.getMessage());
            }

        };
        worker.execute();
//        SwingUtilities.invokeLater(new Runnable() {
//            public void run() {
//                try {
//                } catch (Exception exc) {
//                    exc.printStackTrace();
//                }
//            }
//        });
    }

    public void close() {
        frame.setVisible(false);
        frame.dispose();
    }

}
