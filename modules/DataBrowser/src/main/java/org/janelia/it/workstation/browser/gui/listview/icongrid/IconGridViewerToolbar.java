package org.janelia.it.workstation.browser.gui.listview.icongrid;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.gui.listview.ViewerToolbar;
import org.janelia.it.workstation.browser.options.OptionConstants;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.gui.support.MouseForwarder;
import org.janelia.it.workstation.browser.gui.support.buttons.DropDownButton;

/**
 * Tool bar for icon panels.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class IconGridViewerToolbar extends ViewerToolbar {

    protected JToggleButton showTitlesButton;
    protected JToggleButton showTagsButton;
    protected DropDownButton configButton;
    protected JSlider imageSizeSlider;

    protected int currImageSize;
    protected int customComponentIndex;
    private JCheckBoxMenuItem mustHaveImageMenuItem;
    
    public IconGridViewerToolbar() {
        super();

        Boolean showTitles = FrameworkImplProvider.getModelProperty(
                OptionConstants.ICON_GRID_VIEWER_SHOW_TITLES, true);
        Boolean showAnnotations = FrameworkImplProvider.getModelProperty(
                OptionConstants.ICON_GRID_VIEWER_SHOW_TAGS, true);
        
        showTitlesButton = new JToggleButton();
        showTitlesButton.setIcon(Icons.getIcon("text_smallcaps.png"));
        showTitlesButton.setFocusable(false);
        showTitlesButton.setSelected(showTitles);
        showTitlesButton.setToolTipText("Show the image title above each image.");
        showTitlesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ActivityLogHelper.logUserAction("IconGridViewerToolbar.showTitlesButtonPressed");
                showTitlesButtonPressed();
                FrameworkImplProvider.setModelProperty(
                        OptionConstants.ICON_GRID_VIEWER_SHOW_TITLES, showTitlesButton.isSelected());
            }
        });
        showTitlesButton.addMouseListener(new MouseForwarder(toolbar, "ShowTitlesButton->JToolBar"));
        toolbar.add(showTitlesButton);
        
        showTagsButton = new JToggleButton();
        showTagsButton.setIcon(Icons.getIcon("page_white_stack.png"));
        showTagsButton.setFocusable(false);
        showTagsButton.setSelected(showAnnotations);
        showTagsButton.setToolTipText("Show annotations below each image");
        showTagsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ActivityLogHelper.logUserAction("IconGridViewerToolbar.showTagsButtonPressed");
                showTagsButtonPressed();
                FrameworkImplProvider.setModelProperty(
                        OptionConstants.ICON_GRID_VIEWER_SHOW_TAGS, showTagsButton.isSelected());
            }
        });
        showTagsButton.addMouseListener(new MouseForwarder(toolbar, "ShowTagsButton->JToolBar"));
        toolbar.add(showTagsButton);


        configButton = new DropDownButton();
        configButton.setIcon(Icons.getIcon("cog.png"));
        configButton.setFocusable(false);
        configButton.setToolTipText("Options for the image viewer");

        mustHaveImageMenuItem = new JCheckBoxMenuItem("Show only items with selected imagery");
        mustHaveImageMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setMustHaveImage(mustHaveImageMenuItem.isSelected());
            }
        });
        configButton.addMenuItem(mustHaveImageMenuItem);

        final JMenuItem titlesMenuItem = new JMenuItem("Customize titles...");
        titlesMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ActivityLogHelper.logUserAction("IconGridViewerToolbar.configButtonPressed");
                customizeTitlesPressed();
            }
        });
        configButton.addMenuItem(titlesMenuItem);
        
        toolbar.add(configButton);

        addSeparator();

        customComponentIndex = toolbar.getComponentCount();
                
        addSeparator();

        imageSizeSlider = new JSlider(ImagesPanel.MIN_IMAGE_WIDTH, ImagesPanel.MAX_IMAGE_WIDTH,
                ImagesPanel.DEFAULT_THUMBNAIL_SIZE);
        imageSizeSlider.putClientProperty("Slider.paintThumbArrowShape", Boolean.TRUE);
        imageSizeSlider.setFocusable(false);
        imageSizeSlider.setMaximumSize(new Dimension(120, Integer.MAX_VALUE));
        imageSizeSlider.setToolTipText("Image size percentage");
        imageSizeSlider.addMouseListener(new MouseForwarder(toolbar, "ImageSizeSlider->JToolBar"));
        toolbar.add(imageSizeSlider);

        imageSizeSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider) e.getSource();
                int imageSize = source.getValue();
                if (currImageSize == imageSize) {
                    return;
                }
                currImageSize = imageSize;
                currImageSizeChanged(currImageSize);
            }
        });
    }

    public boolean areTitlesVisible() {
        return showTitlesButton.isSelected();
    }

    public boolean areTagsVisible() {
        return showTagsButton.isSelected();
    }

    public JToggleButton getShowTitlesButton() {
        return showTitlesButton;
    }

    public JToggleButton getShowTagsButton() {
        return showTagsButton;
    }

    public DropDownButton getConfigButton() {
        return configButton;
    }

    public JCheckBoxMenuItem getMustHaveImageMenuItem() {
        return mustHaveImageMenuItem;
    }

    public JSlider getImageSizeSlider() {
        return imageSizeSlider;
    }

    public int getCurrImageSize() {
        return currImageSize;
    }
    
    public void addCustomComponent(JComponent component) {
        toolbar.add(component, null, customComponentIndex++);
    }

    protected abstract void showTitlesButtonPressed();

    protected abstract void showTagsButtonPressed();

    protected abstract void customizeTitlesPressed();
    
    protected abstract boolean isMustHaveImage();
    
    protected abstract void setMustHaveImage(boolean mustHaveImage);
    
    protected abstract void currImageSizeChanged(int imageSize);

}
