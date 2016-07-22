package org.janelia.it.workstation.gui.browser.gui.listview.icongrid;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.janelia.it.workstation.gui.browser.gui.listview.ViewerToolbar;
import org.janelia.it.workstation.gui.browser.gui.support.MouseForwarder;
import org.janelia.it.workstation.gui.util.Icons;

/**
 * Tool bar for icon panels.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class IconGridViewerToolbar extends ViewerToolbar {

    protected JToggleButton showTitlesButton;
    protected JToggleButton showTagsButton;
    protected JButton configButton;
    protected JSlider imageSizeSlider;

    protected int currImageSize;
    protected int customComponentIndex;
    
    public IconGridViewerToolbar() {
        super();

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

        configButton = new JButton("Titles...");
        configButton.setIcon(Icons.getIcon("cog.png"));
        configButton.setFocusable(false);
        configButton.setToolTipText("Configure the icon viewer.");
        configButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                configButtonPressed();
            }
        });
        configButton.addMouseListener(new MouseForwarder(toolbar, "ConfigButton->JToolBar"));
        toolbar.add(configButton);

        toolbar.addSeparator();

        customComponentIndex = toolbar.getComponentCount();
                
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

    public JButton getConfigButton() {
        return configButton;
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

    protected abstract void configButtonPressed();

    protected abstract void currImageSizeChanged(int imageSize);

}
