package org.janelia.it.workstation.browser.gui.colordepth;

import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.FileMgr;
import org.janelia.it.workstation.browser.util.Utils;
import org.janelia.it.workstation.browser.workers.IndeterminateProgressMonitor;
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
        super("Create Color Depth Search Mask...");
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
            JOptionPane.showMessageDialog(ConsoleApp.getMainFrame(), 
                    "No image selected", 
                    "No image", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SimpleWorker worker = new SimpleWorker()     {

            private BufferedImage image;
            private List<String> alignmentSpaces;
            
            @Override
            protected void doStuff() throws Exception {
                URL imageFileURL = FileMgr.getFileMgr().getURL(imagePath, true);
                this.image = Utils.readImage(imageFileURL);
                alignmentSpaces = DomainMgr.getDomainMgr().getModel().getAlignmentSpaces();
            }

            @Override
            protected void hadSuccess() {
                showMaskDialog(image, alignmentSpaces);
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }
        };

        worker.execute();
    }
    
    private void showMaskDialog(BufferedImage image, List<String> alignmentSpaces) {

        try {
            MaskCreationDialog maskCreationDialog = new MaskCreationDialog(
                    image, alignmentSpaces, null, true);
            if (!maskCreationDialog.showForMask()) {
                return; // User cancelled the operation
            }

            BufferedImage mask = maskCreationDialog.getMask();
            int threshold = maskCreationDialog.getThreshold();
            String alignmentSpace = maskCreationDialog.getAlignmentSpace();
            
            SimpleWorker worker = new SimpleWorker()     {

                private String uploadPath;
                
                @Override
                protected void doStuff() throws Exception {

                    // Write the mask to disk temporarily
                    // TODO: in the future, the uploader should support byte stream input
                    File tempFile = File.createTempFile("mask", ".png");
                    tempFile.deleteOnExit();
                    ImageIO.write(mask, "png", tempFile);
                    log.info("Wrote mask to temporary file: "+tempFile);
                    
                    uploadPath = MaskUtils.uploadMask(tempFile);
                }

                @Override
                protected void hadSuccess() {
                    new AddMaskDialog().showForMask(uploadPath, alignmentSpace, threshold, null);
                }

                @Override
                protected void hadError(Throwable error) {
                    ConsoleApp.handleException(error);
                }
            };

            worker.setProgressMonitor(new IndeterminateProgressMonitor(ConsoleApp.getMainFrame(), "Uploading mask", ""));
            worker.execute();
        }
        catch (Exception e) {
            FrameworkImplProvider.handleException(e);
        }
    }
}
