package org.janelia.workstation.colordepth.actions;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.enums.FileType;
import org.janelia.model.domain.interfaces.HasFiles;
import org.janelia.model.domain.sample.AlignedImage2d;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.colordepth.gui.CreateMaskFromImageAction;
import org.janelia.workstation.colordepth.gui.CreateMaskFromSampleAction;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.common.gui.model.DomainObjectImageModel;
import org.janelia.workstation.common.gui.util.DomainUIUtils;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.model.descriptors.ArtifactDescriptor;
import org.janelia.workstation.core.model.descriptors.DescriptorUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

@ActionID(
        category = "actions",
        id = "CreateColorDepthMaskAction"
)
@ActionRegistration(
        displayName = "#CTL_CreateColorDepthMaskAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions/Sample", position = 570)
})
@NbBundle.Messages("CTL_CreateColorDepthMaskAction=Create Color Depth Mask")
public class CreateColorDepthMaskAction extends BaseContextualNodeAction {

    private final static Logger log = LoggerFactory.getLogger(CreateColorDepthMaskAction.class);

    private Action innerAction;

    @Override
    protected void processContext() {

        this.innerAction = null;

        DomainObject selectedObject = null;
        if (getNodeContext().isSingleObjectOfType(Sample.class)) {
            selectedObject = getNodeContext().getSingleObjectOfType(Sample.class);
        }
        else if (getNodeContext().isSingleObjectOfType(AlignedImage2d.class)) {
            selectedObject = getNodeContext().getSingleObjectOfType(AlignedImage2d.class);
        }

        setEnabledAndVisible(false);
        if (selectedObject!=null) {

            log.trace("Processing selected object: {}", selectedObject.getName());

            ViewerContext<?,?> viewerContext = getViewerContext();
            DomainObjectImageModel doim = DomainUIUtils.getDomainObjectImageModel(viewerContext);
            if (doim != null) {

                ArtifactDescriptor resultDescriptor = doim.getArtifactDescriptor();
                String typeName = doim.getImageTypeName();

                List<Sample> samples = new ArrayList<>();
                List<AlignedImage2d> images = new ArrayList<>();
                for (Object obj : viewerContext.getSelectedObjects()) {
                    if (obj instanceof Sample) {
                        samples.add((Sample) obj);
                    }
                    else if (obj instanceof AlignedImage2d) {
                        images.add((AlignedImage2d) obj);
                    }
                }

                if (samples.size() == 1) {
                    if (resultDescriptor!=null && resultDescriptor.isAligned()) {
                        setVisible(true);

                        Sample sample = samples.get(0);
                        this.innerAction = new CreateMaskFromSampleAction(sample, resultDescriptor, typeName);

                        HasFiles fileProvider = DescriptorUtils.getResult(sample, resultDescriptor);
                        if (fileProvider != null) {
                            FileType fileType = FileType.valueOf(typeName);
                            if (fileType == FileType.ColorDepthMip1
                                    || fileType == FileType.ColorDepthMip2
                                    || fileType == FileType.ColorDepthMip3
                                    || fileType == FileType.ColorDepthMip4) {
                                setEnabled(true);
                            }
                        }
                    }
                } else if (images.size() == 1) {
                    this.innerAction = new CreateMaskFromImageAction(images.get(0));
                    setEnabledAndVisible(true);
                }
            }
        }
    }

    @Override
    public String getName() {
        return innerAction==null?super.getName():(String)innerAction.getValue(Action.NAME);
    }

    @Override
    public void performAction() {
        if (innerAction != null) {
            innerAction.actionPerformed(null);
        }
    }
}