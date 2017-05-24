package org.janelia.it.workstation.gui.stack_viewer.gui;

import java.awt.BorderLayout;
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
	private JComponent parent;
	private Mip3d mipWidget;
	private GLActor actor;

	/**
	 * First half of the launch is to gather the data and prepare the
	 * widget.
	 * 
	 * @param filename read this for data.
	 * @throws Exception on error.
	 */
	public void prepareWidget(String filename) throws Exception {
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
				mipWidget.getVolumeModel(), factory, new TrivialFileResolver(), filename
		);

		if (actor == null) {
			throw new Exception("Volume load failed.");
		}

	}

	/**
	 * Show the prepared MIP widget on its own popup frame.
	 */
	public void displayWidgetInFrame() {
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
	
	/**
	 * Show the prepared MIP widget on the parent component provided, and
	 * with proper labeling.
	 * 
	 * @param parent holds widget and label.
	 * @param labelStr how to present to user.
	 */
	public void displayWidget(JComponent parent, String labelStr) {
		this.parent = parent;
		if (frame != null) {
			disposeFrame();
		}
		
		JLabel label = new JLabel(labelStr);
		mipWidget.clear();
		mipWidget.addActor(actor);
		parent.setLayout(new BorderLayout());
		parent.add(label, BorderLayout.SOUTH);
		parent.add(mipWidget, BorderLayout.CENTER);
	}

    public void close() {
		if (frame != null) {
		    disposeFrame();
		}
		else {
			parent.removeAll();
		}
    }
	
	private void disposeFrame() {
		frame.setVisible(false);
		frame.dispose();
	}

}
