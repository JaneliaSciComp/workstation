package org.janelia.workstation.colordepth.actions;

import org.janelia.model.domain.enums.FileType;
import org.janelia.model.domain.interfaces.HasFiles;
import org.janelia.model.domain.sample.AlignedImage2d;
import org.janelia.model.domain.sample.Sample;
import org.janelia.model.domain.sample.SampleAlignmentResult;
import org.janelia.workstation.colordepth.gui.CreateMaskFromImageAction;
import org.janelia.workstation.colordepth.gui.CreateMaskFromSampleAction;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.common.gui.model.DomainObjectImageModel;
import org.janelia.workstation.common.gui.model.SampleResultModel;
import org.janelia.workstation.common.gui.util.DomainUIUtils;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.model.descriptors.ArtifactDescriptor;
import org.janelia.workstation.core.model.descriptors.DescriptorUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.*;

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

    private Action innerAction;

    @Override
    protected void processContext() {

        setEnabledAndVisible(false);
        this.innerAction = null;

        if (getNodeContext().isSingleObjectOfType(AlignedImage2d.class)) {
            AlignedImage2d image = getNodeContext().getSingleObjectOfType(AlignedImage2d.class);
            this.innerAction = new CreateMaskFromImageAction(image);
            setEnabledAndVisible(true);
        }
        else if (getNodeContext().isSingleObjectOfType(Sample.class)) {
            Sample sample = getNodeContext().getSingleObjectOfType(Sample.class);

            ViewerContext<?,?> viewerContext = getViewerContext();
            DomainObjectImageModel doim = DomainUIUtils.getDomainObjectImageModel(viewerContext);
            if (doim != null) {
                ArtifactDescriptor resultDescriptor = doim.getArtifactDescriptor();
                String typeName = doim.getImageTypeName();

                if (resultDescriptor!=null && resultDescriptor.isAligned()) {
                    setVisible(true);
                    this.innerAction = new CreateMaskFromSampleAction(sample, resultDescriptor, typeName);
                    HasFiles fileProvider = DescriptorUtils.getLatestResult(sample, resultDescriptor);
                    if (fileProvider != null) {
                        FileType fileType = FileType.valueOf(typeName);
                        if (isColorDepthMip(fileType)) {
                            setEnabled(true);
                        }
                    }
                }
            }
        }
        else if (getNodeContext().isSingleObjectOfType(SampleAlignmentResult.class)) {
            SampleAlignmentResult alignmentResult = getNodeContext().getSingleObjectOfType(SampleAlignmentResult.class);
            Sample sample = alignmentResult.getParentRun().getParent().getParent();

            ViewerContext<?,?> viewerContext = getViewerContext();
            SampleResultModel srm = DomainUIUtils.getSampleResultModel(viewerContext);
            if (srm != null) {
                FileType fileType = srm.getFileType();
                setVisible(true);
                this.innerAction = new CreateMaskFromSampleAction(sample, alignmentResult, fileType.name());
                if (isColorDepthMip(fileType)) {
                    setEnabled(true);
                }
            }
        }
    }

    private boolean isColorDepthMip(FileType fileType) {
        return (fileType == FileType.ColorDepthMip1
                || fileType == FileType.ColorDepthMip2
                || fileType == FileType.ColorDepthMip3
                || fileType == FileType.ColorDepthMip4);
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