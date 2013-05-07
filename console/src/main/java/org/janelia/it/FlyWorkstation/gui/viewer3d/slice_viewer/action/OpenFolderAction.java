package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action;

import java.awt.event.ActionEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.VolumeImage3d;

public class OpenFolderAction extends AbstractAction 
{
	private static final long serialVersionUID = 1L;
	private JComponent parent;
	private VolumeImage3d image;
	private JFileChooser fileChooser = new JFileChooser();
	
	public OpenFolderAction(VolumeImage3d image, JComponent parent) {
		this.image = image;
		this.parent = parent;
		fileChooser.setDialogTitle("Choose quadtree folder");
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fileChooser.setAcceptAllFileFilterUsed(false);
		putValue(NAME, "Open Folder...");
		putValue(SHORT_DESCRIPTION,
				"Load a volume image from a folder");
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		int returnVal = fileChooser.showOpenDialog(parent);
		if (returnVal != JFileChooser.APPROVE_OPTION)
			return; // canceled
		File file = fileChooser.getSelectedFile();
		// System.out.println(file.getName());
		URL url;
		try {
			url = file.toURI().toURL();
			if (image.loadURL(url)) {
				return; // it worked!
			}
		} catch (MalformedURLException e) {}
		// If we get this far, there was an error
		JOptionPane.showMessageDialog(parent,
				"Error opening folder " + file.getName(),
				"Could not load folder.",
				JOptionPane.ERROR_MESSAGE);
	}

}
