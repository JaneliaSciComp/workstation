package org.janelia.it.workstation.browser.actions;

import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.FileMgr;
import org.janelia.it.workstation.browser.gui.colordepth.MaskCreationDialog;
import org.janelia.it.workstation.browser.model.descriptors.ArtifactDescriptor;
import org.janelia.it.workstation.browser.model.descriptors.DescriptorUtils;
import org.janelia.it.workstation.browser.util.Utils;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.domain.interfaces.HasFiles;
import org.janelia.model.domain.sample.Sample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows the user to create a mask for color depth search.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ColorDepthMaskAction extends AbstractAction {

    private static final Logger log = LoggerFactory.getLogger(ColorDepthMaskAction.class);
    
    private Sample sample;
    private ArtifactDescriptor resultDescriptor;
    private String typeName;

    public ColorDepthMaskAction() {
        super("Create Color Depth Search Mask...");
    }
    
    public ColorDepthMaskAction(Sample sample, ArtifactDescriptor resultDescriptor, String typeName) {
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
        
        String imagePath = DomainUtils.getFilepath(fileProvider, typeName);
        log.debug("imagePath: "+imagePath);
        
        if (imagePath==null) {
            JOptionPane.showMessageDialog(ConsoleApp.getMainFrame(), 
                    "No image selected", 
                    "No image", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SimpleWorker worker = new SimpleWorker()     {

            private BufferedImage image;
            
            @Override
            protected void doStuff() throws Exception {
                URL imageFileURL = FileMgr.getURL(imagePath);
                this.image = Utils.readImage(imageFileURL);
            }

            @Override
            protected void hadSuccess() {
                showMaskDialog(image);
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }
        };

        worker.execute();
    }
    
    private void showMaskDialog(BufferedImage image) {

        MaskCreationDialog dialog = new MaskCreationDialog();
        try {
            BufferedImage mask = dialog.showForImage(image);
            log.debug("Got mask: "+mask);
            
            
            
        }
        catch (Exception e) {
            FrameworkImplProvider.handleException(e);
        }
    }
}
