package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import org.janelia.it.FlyWorkstation.gui.dialogs.ModalDialog;

/**
 * A persistent heads-up display for a synchronized image. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Hud extends ModalDialog {

	private Long entityId;
	private JLabel previewLabel;
	
	public Hud() {
		setModalityType(ModalityType.MODELESS);
		previewLabel = new JLabel(new ImageIcon());
		add(previewLabel);
	}
	
	public void toggleDialog() {
		if (isVisible()) {
			setVisible(false);
		}
		else {
			packAndShow();	
		}
	}
	
	public void hideDialog() {
		setVisible(false);
	}
	
	public Long getEntityId() {
		return entityId;
	}
	
	public void setEntityId(Long entityId) {
		this.entityId = entityId;
	}

	public void setImage(BufferedImage bufferedImage) {
		previewLabel.setIcon(bufferedImage==null?null:new ImageIcon(bufferedImage));
	}
	
}
