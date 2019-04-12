package org.janelia.it.workstation.browser.gui.colordepth;

import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.FileMgr;
import org.janelia.it.workstation.browser.util.Utils;
import org.janelia.it.workstation.browser.filecache.URLProxy;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.domain.enums.FileType;
import org.janelia.model.domain.sample.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows the user to create a mask for color depth search from an existing image.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CreateMaskFromImageAction extends AbstractAction {

    private static final Logger log = LoggerFactory.getLogger(CreateMaskFromImageAction.class);

    private Image image;
    private String imagePath;

    public CreateMaskFromImageAction() {
        super("Create Mask for Color Depth Search...");
    }
    
    public CreateMaskFromImageAction(Image image) {
        this();
        this.image = image;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        log.debug("image: "+image);
        
        imagePath = DomainUtils.getFilepath(image, FileType.FirstAvailable2d);
        log.debug("imagePath: "+imagePath);
        
        if (imagePath==null) {
            JOptionPane.showMessageDialog(FrameworkImplProvider.getMainFrame(),
                    "No image selected", 
                    "No image", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SimpleWorker worker = new SimpleWorker()     {

            private BufferedImage image;
            private List<String> alignmentSpaces;
            
            @Override
            protected void doStuff() throws Exception {
                URLProxy imageFileURL = FileMgr.getFileMgr().getURL(imagePath, true);
                this.image = Utils.readImage(imageFileURL);
                alignmentSpaces = DomainMgr.getDomainMgr().getModel().getAlignmentSpaces();
            }

            @Override
            protected void hadSuccess() {
                showMaskDialog(image, alignmentSpaces);
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkImplProvider.handleException(error);
            }
        };

        worker.execute();
    }
    
    private void showMaskDialog(BufferedImage image, List<String> alignmentSpaces) {

        // could be null, but that's okay, in that case the user has to pick
        String imageAlignmentSpace = this.image.getAlignmentSpace(); 
        
        try {
            String maskName = "Mask derived from "+this.image.getName();
            MaskCreationDialog maskCreationDialog = new MaskCreationDialog(
                    image, null, alignmentSpaces, imageAlignmentSpace, maskName, null, true);
            maskCreationDialog.showForMask();
        }
        catch (Exception e) {
            FrameworkImplProvider.handleException(e);
        }
    }
}
