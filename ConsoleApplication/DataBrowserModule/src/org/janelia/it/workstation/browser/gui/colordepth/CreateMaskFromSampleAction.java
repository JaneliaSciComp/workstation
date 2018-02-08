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
import org.janelia.it.workstation.browser.model.descriptors.ArtifactDescriptor;
import org.janelia.it.workstation.browser.model.descriptors.DescriptorUtils;
import org.janelia.it.workstation.browser.util.Utils;
import org.janelia.it.workstation.browser.workers.IndeterminateProgressMonitor;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.domain.interfaces.HasFiles;
import org.janelia.model.domain.sample.Sample;
import org.janelia.model.domain.sample.SampleAlignmentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows the user to create a mask for color depth search from an existing color depth MIP on a sample.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CreateMaskFromSampleAction extends AbstractAction {

    private static final Logger log = LoggerFactory.getLogger(CreateMaskFromSampleAction.class);

    private Sample sample;
    private ArtifactDescriptor resultDescriptor;
    private String typeName;
    private SampleAlignmentResult alignment;
    private String imagePath;

    public CreateMaskFromSampleAction() {
        super("Create Color Depth Search Mask...");
    }
    
    public CreateMaskFromSampleAction(Sample sample, ArtifactDescriptor resultDescriptor, String typeName) {
        this();
        this.sample = sample;
        this.resultDescriptor = resultDescriptor;
        this.typeName = typeName;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        log.debug("sample: "+sample);
        log.debug("resultDescriptor: "+resultDescriptor);
        log.debug("typeName: "+typeName);
        
        HasFiles fileProvider = DescriptorUtils.getResult(sample, resultDescriptor);
        log.debug("fileProvider: "+fileProvider);
        
        if (!(fileProvider instanceof SampleAlignmentResult)) {
            
            JOptionPane.showMessageDialog(ConsoleApp.getMainFrame(), 
                    "Must select an aligned image", 
                    "Image not aligned", JOptionPane.ERROR_MESSAGE);
            
            return;
        }
        
        alignment = (SampleAlignmentResult)fileProvider;
        imagePath = DomainUtils.getFilepath(alignment, typeName);
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
                    image, alignmentSpaces, alignment.getAlignmentSpace(), true);
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
                    new AddMaskDialog().showForMask(uploadPath, alignmentSpace, threshold, sample);
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
