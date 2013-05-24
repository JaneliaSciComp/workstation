package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.RGBImageFilter;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import org.janelia.it.FlyWorkstation.gui.util.Icons;

// Non-interactive icon for tool bar; e.g. mouse and wheel mode header icons
public class ToolBarIcon extends JLabel 
{
	private static final long serialVersionUID = 1L;

	public ToolBarIcon(String imageFileName) {
		ImageIcon icon = Icons.getIcon(imageFileName);
		setIcon(icon);
		Image img = icon.getImage();
		// GrayFilter grayFilter = new GrayFilter(false, 30);
		ImageFilter grayFilter = new DisabledFilter();
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Image grayImage = toolkit.createImage(new FilteredImageSource(img.getSource(), grayFilter));
		setDisabledIcon(new ImageIcon(grayImage));
		setEnabled(false);
	}
	
	class DisabledFilter extends RGBImageFilter
	{
		@Override
		public int filterRGB(int x, int y, int rgb) {
			int result = rgb;

			int r = (rgb & 0x00ff0000) >> 16;
			int g = (rgb & 0x0000ff00) >> 8;
			int b = (rgb & 0x000000ff);
			int a = (rgb & 0xff000000) >> 24;
			
			r = (r >> 1) | 0x80; // dimmer
			g = (g >> 1) | 0x80;
			b = (b >> 1) | 0x80;
			
			result = (result & 0x00ffffff) | ((a << 24) & 0xff000000);
			result = (result & 0xff00ffff) | ((r << 16) & 0x00ff0000);
			result = (result & 0xffff00ff) | ((g <<  8) & 0x0000ff00);
			result = (result & 0xffffff00) | ((b <<  0) & 0x000000ff);
			
			return result;
		}
	}
}
