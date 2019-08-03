package org.janelia.workstation.browser.gui.colordepth;

import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.apache.commons.io.FilenameUtils;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.interfaces.HasFiles;
import org.janelia.model.domain.sample.Sample;
import org.janelia.model.domain.sample.SampleAlignmentResult;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.FileMgr;
import org.janelia.workstation.core.model.descriptors.ArtifactDescriptor;
import org.janelia.workstation.core.model.descriptors.DescriptorUtils;
import org.janelia.workstation.core.util.Utils;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
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

    public CreateMaskFromSampleAction(Sample sample, ArtifactDescriptor resultDescriptor, String typeName) {
        super("Create Mask for Color Depth Search...");
        this.sample = sample;
        this.resultDescriptor = resultDescriptor;
        this.typeName = typeName;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        HasFiles fileProvider = DescriptorUtils.getResult(sample, resultDescriptor);

        if (!(fileProvider instanceof SampleAlignmentResult)) {
            
            JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                    "Must select an aligned image", 
                    "Image not aligned", JOptionPane.ERROR_MESSAGE);
            
            return;
        }
        
        alignment = (SampleAlignmentResult)fileProvider;
        imagePath = DomainUtils.getFilepath(alignment, typeName);
        
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
                try (InputStream imageStream = FileMgr.getFileMgr().openFileInputStream(imagePath, false)) {
                    this.image = Utils.readImageFromInputStream(imageStream, FilenameUtils.getExtension(imagePath));
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
        try {
            String maskName = "Mask derived from "+sample.getLine();
            MaskCreationDialog maskCreationDialog = new MaskCreationDialog(
                    image, null, alignmentSpaces, alignment.getAlignmentSpace(), maskName, sample, alignment.getAnatomicalArea(), true);
            maskCreationDialog.showForMask();
        }
        catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
    }
}
