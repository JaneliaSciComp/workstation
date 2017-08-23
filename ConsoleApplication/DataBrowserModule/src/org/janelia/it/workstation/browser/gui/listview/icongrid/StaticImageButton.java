package org.janelia.it.workstation.browser.gui.listview.icongrid;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.JComponent;

import org.janelia.it.workstation.browser.events.selection.SelectionModel;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.gui.support.MouseForwarder;
import org.janelia.it.workstation.browser.model.ImageDecorator;
import org.janelia.it.workstation.browser.util.Utils;

/**
 * An AnnotatedImageButton with a static icon and label.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class StaticImageButton<T,S> extends AnnotatedImageButton<T,S> {

    private static final BufferedImage MISSING_ICON = Icons.getImage("file_missing.png");

    private List<ImageDecorator> decorators;
    private DecoratedErrorPanel infoPanel;
    private BufferedImage maxSizeImage;

    public StaticImageButton(T imageObject, ImageModel<T,S> imageModel, SelectionModel<T,S> selectionModel, ImagesPanel<T,S> imagesPanel, String filepath) {
        super(imageObject, imageModel, selectionModel, imagesPanel, filepath);
        this.decorators = imageModel.getDecorators(imageObject);
    }

    @Override
    public JComponent init(T imageObject, ImageModel<T,S> imageModel, String filepath) {
        this.maxSizeImage = imageModel.getStaticIcon(imageObject);
        String errorText = null;
        if (maxSizeImage==null) {
            maxSizeImage = MISSING_ICON;
            errorText = "Selected result type not available";
        }
        this.infoPanel = new DecoratedErrorPanel(decorators, errorText, null);
        infoPanel.addMouseListener(new MouseForwarder(this, "DecoratedInfoPanel->StaticImageButton"));
        return infoPanel;
    }

    @Override
    public void setImageSize(int width, int height) {
        super.setImageSize(width, height);
        infoPanel.setPreferredSize(new Dimension(width, height));
        
        if (maxSizeImage!=null) {
            // Only scale icons down, never higher than their max resolution
            if (maxSizeImage.getWidth()>width) {
                infoPanel.setImage(Utils.getScaledImageByWidth(maxSizeImage, width));    
            }   
        }
        
        infoPanel.revalidate();
        infoPanel.repaint();
    }

    @Override
    public void setViewable(boolean viewable) {
        if (viewable) {
            if (infoPanel.getImage()==null) {
                infoPanel.setImage(maxSizeImage);
            }
        }
        else {
            infoPanel.setImage(null);
        }

        infoPanel.revalidate();
        infoPanel.repaint();
    }
}
