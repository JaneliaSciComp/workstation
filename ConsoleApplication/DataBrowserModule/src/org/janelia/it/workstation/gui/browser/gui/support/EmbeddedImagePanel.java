package org.janelia.it.workstation.gui.browser.gui.support;

import java.util.concurrent.Callable;
import javax.swing.JPanel;
import org.janelia.it.workstation.gui.browser.gui.listview.icongrid.DynamicImagePanel;
import org.janelia.it.workstation.gui.browser.gui.listview.icongrid.ImagesPanel;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EmbeddedImagePanel<T> extends JPanel {
    
    private DynamicImagePanel dynamicImagePanel;
    
    public EmbeddedImagePanel(final T imageObject, String filepath) {
        
        this.dynamicImagePanel = new DynamicImagePanel(filepath, ImagesPanel.MAX_IMAGE_WIDTH);
        
        dynamicImagePanel.setViewable(true, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                // Register our image height
                if (dynamicImagePanel.getMaxSizeImage() != null && dynamicImagePanel.getImage() != null) {
                    double w = dynamicImagePanel.getImage().getIconWidth();
                    double h = dynamicImagePanel.getImage().getIconHeight();
                    double a = w / h;
                    dynamicImagePanel.rescaleImage(WIDTH);
                }
                return null;
            }

        });
    }

}
