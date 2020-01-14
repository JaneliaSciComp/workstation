package org.janelia.workstation.browser.gui.colordepth;

import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.apache.commons.io.FilenameUtils;
import org.janelia.filecacheutils.ContentStream;
import org.janelia.filecacheutils.FileProxy;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.enums.FileType;
import org.janelia.model.domain.interfaces.HasAnatomicalArea;
import org.janelia.model.domain.sample.AlignedImage2d;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.FileMgr;
import org.janelia.workstation.core.util.Utils;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows the user to create a mask for color depth search from an existing image.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CreateMaskFromImageAction extends AbstractAction {

    private static final Logger log = LoggerFactory.getLogger(CreateMaskFromImageAction.class);

    private AlignedImage2d image;
    private String imagePath;

    public CreateMaskFromImageAction() {
        super("Create Mask for Color Depth Search...");
    }
    
    public CreateMaskFromImageAction(AlignedImage2d image) {
        this(); // call no-args constructor to set title
        this.image = image;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        log.debug("image: "+image);
        
        imagePath = DomainUtils.getFilepath(image, FileType.FirstAvailable2d);
        log.debug("imagePath: "+imagePath);
        
        if (imagePath==null) {
            JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                    "No image selected", 
                    "No image", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SimpleWorker worker = new SimpleWorker()     {

            private BufferedImage image;
            private List<String> alignmentSpaces;
            
            @Override
            protected void doStuff() throws Exception {
                FileProxy imageFileProxy = FileMgr.getFileMgr().getFile(imagePath, false);
                try (ContentStream imageStream = imageFileProxy.openContentStream()) {
                    this.image = Utils.readImageFromInputStream(imageStream.asInputStream(), FilenameUtils.getExtension(imageFileProxy.getFileId()));
                }
                alignmentSpaces = DomainMgr.getDomainMgr().getModel().getAlignmentSpaces();
            }

            @Override
            protected void hadSuccess() {
                showMaskDialog(image, alignmentSpaces);
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
            }
        };

        worker.execute();
    }
    
    private void showMaskDialog(BufferedImage image, List<String> alignmentSpaces) {

        // could be null, but that's okay, in that case the user has to pick
        String imageAlignmentSpace = this.image.getAlignmentSpace();

        String anatomicalArea = null;
        if (image instanceof HasAnatomicalArea) {
            anatomicalArea = ((HasAnatomicalArea) image).getAnatomicalArea();
        }

        try {
            String maskName = "Mask derived from "+this.image.getName();
            MaskCreationDialog maskCreationDialog = new MaskCreationDialog(
                    image, null, alignmentSpaces, imageAlignmentSpace, maskName, null, anatomicalArea,true);
            maskCreationDialog.showForMask();
        }
        catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
    }
}
