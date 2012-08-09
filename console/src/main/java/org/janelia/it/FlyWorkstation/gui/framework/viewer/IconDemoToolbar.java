package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
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
public abstract class IconDemoToolbar extends JPanel {

	protected JToolBar toolbar;
	protected JButton prevButton;
	protected JButton nextButton;
	protected JButton pathButton;
    protected JToggleButton invertButton;
    protected JToggleButton showTitlesButton;
	protected JButton imageRoleButton;
	protected JToggleButton showTagsButton;
	protected JButton refreshButton;
	protected JButton userButton;
	protected JSlider imageSizeSlider;
	
	protected int currImageSize;
	
	public IconDemoToolbar() {
		super(new BorderLayout());	

		toolbar = new JToolBar();
		toolbar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, (Color)UIManager.get("windowBorder")), BorderFactory.createEmptyBorder(0, 5, 2, 5)));
		toolbar.setFloatable(false);
		toolbar.setRollover(true);
		add(toolbar);

		prevButton = new JButton();
		prevButton.setIcon(Icons.getIcon("arrow_back.gif"));
		prevButton.setToolTipText("Go back in your browsing history");
//		prevButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
		prevButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				goBack();
			}
		});
		prevButton.addMouseListener(new MouseForwarder(toolbar, "PrevButton->JToolBar"));
		toolbar.add(prevButton);

		nextButton = new JButton();
		nextButton.setIcon(Icons.getIcon("arrow_forward.gif"));
		nextButton.setToolTipText("Go forward in your browsing history");
//		nextButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
		nextButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				goForward();
			}
		});
		nextButton.addMouseListener(new MouseForwarder(toolbar, "NextButton->JToolBar"));
		toolbar.add(nextButton);

		pathButton = new JButton();
		pathButton.setIcon(Icons.getIcon("path-blue.png"));
		pathButton.setToolTipText("See the current location");
		pathButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showPopupPathMenu();
			}
		});
		pathButton.addMouseListener(new MouseForwarder(toolbar, "ParentButton->JToolBar"));
		toolbar.add(pathButton);

		refreshButton = new JButton();
		refreshButton.setIcon(Icons.getRefreshIcon());
		refreshButton.setFocusable(false);
		refreshButton.setToolTipText("Refresh the current view");
//		refreshButton.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
		refreshButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				refresh();
			}
		});
		refreshButton.addMouseListener(new MouseForwarder(toolbar, "RefreshButton->JToolBar"));
		toolbar.add(refreshButton);

		toolbar.addSeparator();

        invertButton = new JToggleButton();
        invertButton.setIcon(Icons.getIcon("invert.png"));
        invertButton.setFocusable(false);
        invertButton.setToolTipText("Invert the color space on all images");
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

		imageSizeSlider = new JSlider(ImagesPanel.MIN_THUMBNAIL_SIZE, ImagesPanel.MAX_THUMBNAIL_SIZE,
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
	
	protected abstract void goBack();
	protected abstract void goForward();
	protected abstract void refresh();
	protected abstract void showTitlesButtonPressed();
	protected abstract void showTagsButtonPressed();
	protected abstract void currImageSizeChanged(int imageSize);
	protected abstract JPopupMenu getPopupPathMenu();
	protected abstract JPopupMenu getPopupUserMenu();
	protected abstract JPopupMenu getPopupImageRoleMenu();
	
	private void showPopupPathMenu() {
		JPopupMenu menu = getPopupPathMenu();
		if (menu==null) return;
		menu.show(pathButton, 0, pathButton.getHeight());
	}
	
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

	public JToolBar getToolbar() {
		return toolbar;
	}

	public JButton getPrevButton() {
		return prevButton;
	}

	public JButton getNextButton() {
		return nextButton;
	}

	public JButton getPathButton() {
		return pathButton;
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

	public JButton getRefreshButton() {
		return refreshButton;
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
