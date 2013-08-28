package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.MouseForwarder;
import org.janelia.it.FlyWorkstation.gui.util.panels.ViewerSettingsPanel;

/**
 * Toolbar for icon panels. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class IconDemoToolbar extends ViewerToolbar {

    protected JToggleButton invertButton;
    protected JToggleButton showTitlesButton;
	protected JButton imageRoleButton;
	protected JToggleButton showTagsButton;
	protected JButton userButton;
	protected JSlider imageSizeSlider;
	
	protected int currImageSize;
	
	public IconDemoToolbar() {
		super();
		
        invertButton = new JToggleButton();
        invertButton.setIcon(Icons.getIcon("invert.png"));
        invertButton.setFocusable(false);
        invertButton.setEnabled(false);
//        invertButton.setToolTipText("Invert the color space on all images");
        invertButton.setToolTipText("This feature has been disabled and may be removed in the future. If you need this functionality, please contact the Janelia Workstation Team.");
        invertButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SessionMgr.getSessionMgr().setModelProperty(ViewerSettingsPanel.INVERT_IMAGE_COLORS_PROPERTY,
                        Boolean.valueOf(invertButton.isSelected()));
            }
        });
        toolbar.add(invertButton);

        showTitlesButton = new JToggleButton();
		showTitlesButton.setIcon(Icons.getIcon("text_smallcaps.png"));
		showTitlesButton.setFocusable(false);
		showTitlesButton.setSelected(true);
		showTitlesButton.setToolTipText("Show the image title above each image.");
		showTitlesButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showTitlesButtonPressed();
			}
		});
		showTitlesButton.addMouseListener(new MouseForwarder(toolbar, "ShowTitlesButton->JToolBar"));
		toolbar.add(showTitlesButton);
		
		showTagsButton = new JToggleButton();
		showTagsButton.setIcon(Icons.getIcon("page_white_stack.png"));
		showTagsButton.setFocusable(false);
		showTagsButton.setSelected(true);
		showTagsButton.setToolTipText("Show annotations below each image");
		showTagsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showTagsButtonPressed();
			}
		});
		showTagsButton.addMouseListener(new MouseForwarder(toolbar, "ShowTagsButton->JToolBar"));
		toolbar.add(showTagsButton);

		toolbar.addSeparator();
		
		userButton = new JButton("Annotations from...");
		userButton.setIcon(Icons.getIcon("group.png"));
		userButton.setFocusable(false);
		userButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showPopupUserMenu();
			}
		});
		userButton.addMouseListener(new MouseForwarder(toolbar, "UserButton->JToolBar"));
		toolbar.add(userButton);
		
		toolbar.addSeparator();
		
		imageRoleButton = new JButton("Image type...");
		imageRoleButton.setIcon(Icons.getIcon("image.png"));
		imageRoleButton.setFocusable(false);
		imageRoleButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showPopupImageRoleMenu();
			}
		});
		imageRoleButton.addMouseListener(new MouseForwarder(toolbar, "ImageRoleButton->JToolBar"));
		toolbar.add(imageRoleButton);

		toolbar.addSeparator();

		imageSizeSlider = new JSlider(ImagesPanel.MIN_IMAGE_WIDTH, ImagesPanel.MAX_IMAGE_WIDTH,
				ImagesPanel.DEFAULT_THUMBNAIL_SIZE);
		imageSizeSlider.setFocusable(false);
		imageSizeSlider.setMaximumSize(new Dimension(200, Integer.MAX_VALUE));
		imageSizeSlider.setToolTipText("Image size percentage");
		imageSizeSlider.addMouseListener(new MouseForwarder(toolbar, "ImageSizeSlider->JToolBar"));
		toolbar.add(imageSizeSlider);
		
		imageSizeSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int imageSize = source.getValue();
				if (currImageSize == imageSize) return;
				currImageSize = imageSize;
				currImageSizeChanged(currImageSize);
			}
		});
	}
	
	protected abstract void showTitlesButtonPressed();
	protected abstract void showTagsButtonPressed();
	protected abstract void currImageSizeChanged(int imageSize);
	protected abstract JPopupMenu getPopupPathMenu();
	protected abstract JPopupMenu getPopupUserMenu();
	protected abstract JPopupMenu getPopupImageRoleMenu();
	
	
	private void showPopupUserMenu() {
		JPopupMenu menu = getPopupUserMenu();
		if (menu==null) return;
		menu.show(userButton, 0, userButton.getHeight());
	}

	private void showPopupImageRoleMenu() {
		JPopupMenu menu = getPopupImageRoleMenu();
		if (menu==null) return;
		menu.show(imageRoleButton, 0, imageRoleButton.getHeight());
	}
	
	public boolean areTitlesVisible() {
		return showTitlesButton.isSelected();
	}

	public boolean areTagsVisible() {
		return showTagsButton.isSelected();
	}

	public JToggleButton getInvertButton() {
		return invertButton;
	}

	public JToggleButton getShowTitlesButton() {
		return showTitlesButton;
	}

	public JButton getImageRoleButton() {
		return imageRoleButton;
	}

	public JToggleButton getShowTagsButton() {
		return showTagsButton;
	}

	public JButton getUserButton() {
		return userButton;
	}

	public JSlider getImageSizeSlider() {
		return imageSizeSlider;
	}

	public int getCurrImageSize() {
		return currImageSize;
	}
}
