package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class HudPanel extends JPanel 
{
	private static final long serialVersionUID = 1L;

	public HudPanel() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setOpaque(false);
		add(Box.createVerticalStrut(80));
		SubPanel p = new SubPanel();
		add(p);
		p.add(new JLabel("HUD"));
	}
	
	
	static class SubPanel extends JPanel
	{
		private static final long serialVersionUID = 1L;
		private Color backgroundColor = new Color(0.5f, 0.5f, 0.5f, 0.5f);

		public SubPanel() {
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			setOpaque(false);
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D)g;
			g2.setPaint(backgroundColor);
			Rectangle r = getBounds();
			g2.fillRect(0, 0, r.width, r.height);
			super.paintComponent(g);
		}
		
	}
}
