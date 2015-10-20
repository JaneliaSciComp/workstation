package org.janelia.it.workstation.gui.browser.gui.listview.icongrid;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.workstation.gui.browser.events.selection.SelectionModel;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.shared.util.Utils;

/**
 * An AnnotatedImageButton with a static icon or label.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class StaticImageButton<T,S> extends AnnotatedImageButton<T,S> {

    private BufferedImage staticIcon;
    private JLabel label;

    public StaticImageButton(T imageObject, ImageModel<T,S> imageModel, SelectionModel<T,S> selectionModel, ImagesPanel<T,S> imagesPanel) {
        super(imageObject, imageModel, selectionModel, imagesPanel);
    }

    @Override
    public JComponent init(final T imageObject, final ImageModel<T,S> imageModel) {
        this.label = new JLabel(Icons.getLoadingIcon());
        return label;
    }

    @Override
    public void setImageSize(int width, int height) {
        super.setImageSize(width, height);
        if (staticIcon != null) {
            if (width <= staticIcon.getWidth()) { // Don't scale up icons
                label.setIcon(new ImageIcon(Utils.getScaledImage(staticIcon, width)));
            }
            else {
                label.setIcon(new ImageIcon(staticIcon));
            }
        }
        label.setPreferredSize(new Dimension(width, height));
        label.revalidate();
        label.repaint();
    }

    @Override
    public void setViewable(boolean viewable) {
        if (viewable) {
            Entity temp = new Entity();
            temp.setEntityTypeName("Folder");
            this.staticIcon = Icons.getLargeIconAsBufferedImage(temp);

            // Register our aspect ratio
            double w = label.getIcon().getIconWidth();
            double h = label.getIcon().getIconHeight();
            registerAspectRatio(w, h);

            int width = imagesPanel.getMaxImageWidth();
            if (width <= staticIcon.getWidth()) { // Don't scale up icons
                label.setIcon(new ImageIcon(Utils.getScaledImage(staticIcon, width)));
            }
            else {
                label.setIcon(new ImageIcon(staticIcon));
            }
        }
        else {
            this.staticIcon = null;
            label.setIcon(Icons.getLoadingIcon());
        }

        label.revalidate();
        label.repaint();
    }

}
