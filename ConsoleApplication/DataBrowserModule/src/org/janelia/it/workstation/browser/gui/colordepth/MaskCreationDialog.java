package org.janelia.it.workstation.browser.gui.colordepth;

import java.awt.BorderLayout;
import java.awt.image.BufferedImage;

import org.janelia.it.workstation.browser.gui.dialogs.ModalDialog;
import org.janelia.it.workstation.browser.gui.lasso.ImageMaskingPanel;

/**
 * Create a new mask for color depth searching. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MaskCreationDialog extends ModalDialog {

    private ImageMaskingPanel maskingPanel;
    private BufferedImage mask;

    public BufferedImage showForImage(BufferedImage image) throws Exception {
        
        maskingPanel = new ImageMaskingPanel();
        maskingPanel.setImage(image);
        maskingPanel.setOnContinue((BufferedImage mask) -> {
            this.mask = mask;
            setVisible(false);
        });
        
        add(maskingPanel, BorderLayout.CENTER);
        
        packAndShow();
        
        return mask;
    }
}
