package org.janelia.workstation.browser.actions.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.Action;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.enums.FileType;
import org.janelia.model.domain.interfaces.HasFiles;
import org.janelia.model.domain.sample.Image;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.browser.gui.colordepth.CreateMaskFromImageAction;
import org.janelia.workstation.browser.gui.colordepth.CreateMaskFromSampleAction;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.common.gui.model.DomainObjectImageModel;
import org.janelia.workstation.common.gui.util.DomainUIUtils;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.model.descriptors.ArtifactDescriptor;
import org.janelia.workstation.core.model.descriptors.DescriptorUtils;
import org.janelia.workstation.integration.spi.domain.ContextualActionUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

@ActionID(
        category = "Actions",
        id = "CreateColorDepthMaskAction"
)
@ActionRegistration(
        displayName = "#CTL_CreateColorDepthMaskAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions/Samples", position = 570)
})
@NbBundle.Messages("CTL_CreateColorDepthMaskAction=Create Color Depth Mask")
public class CreateColorDepthMaskAction extends BaseContextualNodeAction {

    private DomainObject selectedObject;
    private Action innerAction;

    @Override
    protected void processContext() {

        selectedObject = null;
        if (getNodeContext().isSingleObjectOfType(Sample.class)) {
            this.selectedObject = getNodeContext().getSingleObjectOfType(Sample.class);
        }
        else if (getNodeContext().isSingleObjectOfType(Image.class)) {
            this.selectedObject = getNodeContext().getSingleObjectOfType(Image.class);
        }

        setEnabledAndVisible(false);
        if (selectedObject!=null) {

            ViewerContext viewerContext = getViewerContext();
            DomainObjectImageModel doim = DomainUIUtils.getDomainObjectImageModel(viewerContext);
            if (doim != null) {

                Collection selectedObjects = viewerContext.getSelectedObjects();
                ArtifactDescriptor resultDescriptor = doim.getArtifactDescriptor();
                String typeName = doim.getImageTypeName();

                List<Sample> samples = new ArrayList<>();
                List<Image> images = new ArrayList<>();
                for (Object obj : viewerContext.getSelectedObjects()) {
                    if (obj instanceof Sample) {
                        samples.add((Sample) obj);
                    }
                    else if (obj instanceof Image) {
                        images.add((Image) obj);
                    }
                }

                if (samples.size() == selectedObjects.size()) {
                    if (resultDescriptor.isAligned()) {
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
                } else if (images.size() == selectedObjects.size()) {
                    this.innerAction = new CreateMaskFromImageAction(images.get(0));
                    setEnabledAndVisible(true);
                }
            }
        }
    }

    @Override
    public String getName() {
        return innerAction==null?super.getName():ContextualActionUtils.getName(innerAction);
    }

    @Override
    public void performAction() {
        if (innerAction != null) {
            innerAction.actionPerformed(null);
        }
    }
}