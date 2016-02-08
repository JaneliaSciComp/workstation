package org.janelia.it.workstation.gui.browser.gui.listview.icongrid;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.janelia.it.workstation.gui.browser.gui.listview.ViewerToolbar;
import org.janelia.it.workstation.gui.browser.gui.support.MouseForwarder;
import org.janelia.it.workstation.gui.util.Icons;

import de.javasoft.swing.JYPopupMenu;
import de.javasoft.swing.SimpleDropDownButton;

/**
 * Tool bar for icon panels.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class IconGridViewerToolbar extends ViewerToolbar {

    protected JToggleButton showTitlesButton;
    protected JToggleButton showTagsButton;
    protected SimpleDropDownButton defaultResultButton;
    protected SimpleDropDownButton defaultTypeButton;
    protected JSlider imageSizeSlider;

    protected int currImageSize;

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

        toolbar.addSeparator();

        defaultResultButton = new SimpleDropDownButton();
        JYPopupMenu popupMenu = new JYPopupMenu();
        popupMenu.setVisibleElements(20);
        defaultResultButton.setPopupMenu(popupMenu);
        defaultResultButton.setIcon(Icons.getIcon("folder_open_page.png"));
        defaultResultButton.setFocusable(false);
        defaultResultButton.setToolTipText("Select the result to display");
        // For some reason, this seems to break the mouse interaction with this button. Why?
//        defaultResultButton.addMouseListener(new MouseForwarder(toolbar, "DefaultResultButton->JToolBar"));
        toolbar.add(defaultResultButton);

        defaultTypeButton = new SimpleDropDownButton();
        JYPopupMenu popupMenu2 = new JYPopupMenu();
        popupMenu2.setVisibleElements(20);
        defaultTypeButton.setPopupMenu(popupMenu2);
        defaultTypeButton.setIcon(Icons.getIcon("page.png"));
        defaultTypeButton.setFocusable(false);
        defaultTypeButton.setToolTipText("Select the result type to display");
//        defaultTypeButton.addMouseListener(new MouseForwarder(toolbar, "DefaultTypeButton->JToolBar"));
        toolbar.add(defaultTypeButton);
                
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

    protected abstract void showTitlesButtonPressed();

    protected abstract void showTagsButtonPressed();

    protected abstract void currImageSizeChanged(int imageSize);

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

    public SimpleDropDownButton getDefaultResultButton() {
        return defaultResultButton;
    }

    public SimpleDropDownButton getDefaultTypeButton() {
        return defaultTypeButton;
    }
    
    public JSlider getImageSizeSlider() {
        return imageSizeSlider;
    }

    public int getCurrImageSize() {
        return currImageSize;
    }
}
