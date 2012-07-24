package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.awt.image.BufferedImage;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;

import com.explodingpixels.macwidgets.HudWindow;

/**
 * A persistent heads-up display for a synchronized image. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Hud {

	private Long entityId;
	private HudWindow hud;
	private JLabel previewLabel;
	
	public Hud() {
		hud = new HudWindow();
		hud.getJDialog().setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		previewLabel = new JLabel(new ImageIcon());
		previewLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		hud.getContentPane().add(previewLabel);
	}
	
	public void showDialog() {
		hud.getJDialog().setLocationRelativeTo(SessionMgr.getSessionMgr().getActiveBrowser());
		hud.getJDialog().setVisible(true);
	}
	
	public void hideDialog() {
		hud.getJDialog().setVisible(false);
	}
	
	public JDialog getJDialog() {
		return hud.getJDialog();
	}
	
	public Long getEntityId() {
		return entityId;
	}
	
	public void setEntityId(Long entityId) {
		this.entityId = entityId;
	}

	public void setTitle(String name) {
		hud.getJDialog().setTitle(name);
	}

	public void setImage(BufferedImage bufferedImage) {
		previewLabel.setIcon(bufferedImage==null?null:new ImageIcon(bufferedImage));
		hud.getJDialog().pack();
	}
	
}
