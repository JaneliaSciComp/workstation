package org.janelia.it.workstation.gui.browser.gui.listview.icongrid;

import java.awt.Dimension;
import java.util.concurrent.Callable;

import javax.swing.JComponent;
import org.janelia.it.workstation.gui.browser.events.selection.SelectionModel;

/**
 * An AnnotatedImageButton with a dynamic image, i.e. one that is loaded
 * from via the network, not a locally available icon.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DynamicImageButton<T,S> extends AnnotatedImageButton<T,S> {

    private DynamicImagePanel dynamicImagePanel;

    public DynamicImageButton(T imageObject, ImageModel<T,S> imageModel, SelectionModel<T,S> selectionModel, ImagesPanel<T,S> imagesPanel, String filepath) {
        super(imageObject, imageModel, selectionModel, imagesPanel, filepath);
    }

    public JComponent init(T imageObject, ImageModel<T,S> imageModel, String filepath) {
        this.dynamicImagePanel = new DynamicImagePanel(filepath, ImagesPanel.MAX_IMAGE_WIDTH);
        return dynamicImagePanel;
    }

    public void cancelLoad() {
        dynamicImagePanel.cancelLoad();
    }

    @Override
    public void setImageSize(int width, int height) {
        super.setImageSize(width, height);
        dynamicImagePanel.rescaleImage(width);
        dynamicImagePanel.setPreferredSize(new Dimension(width, height));
    }

    @Override
    public void setViewable(boolean viewable) {
        dynamicImagePanel.setViewable(viewable, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                // Register our image height
                if (dynamicImagePanel.getMaxSizeImage() != null && dynamicImagePanel.getImage() != null) {
                    double w = dynamicImagePanel.getImage().getIconWidth();
                    double h = dynamicImagePanel.getImage().getIconHeight();
                    registerAspectRatio(w, h);
                }
                return null;
            }

        });
    }
}
