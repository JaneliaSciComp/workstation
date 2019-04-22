package org.janelia.workstation.browser.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.Action;

import org.janelia.model.domain.enums.FileType;
import org.janelia.model.domain.interfaces.HasFiles;
import org.janelia.model.domain.sample.Image;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.browser.gui.colordepth.CreateMaskFromImageAction;
import org.janelia.workstation.browser.gui.colordepth.CreateMaskFromSampleAction;
import org.janelia.workstation.common.actions.ViewerContextAction;
import org.janelia.workstation.common.gui.model.DomainObjectImageModel;
import org.janelia.workstation.common.gui.util.DomainUIUtils;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.model.descriptors.ArtifactDescriptor;
import org.janelia.workstation.core.model.descriptors.DescriptorUtils;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.janelia.workstation.integration.spi.domain.ContextualActionUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position=570)
public class CreateColorDepthMaskBuilder implements ContextualActionBuilder {

    private static CreateColorDepthMaskAction action = new CreateColorDepthMaskAction();

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof Sample || obj instanceof Image;
    }

    @Override
    public Action getAction(Object obj) {
        return action;
    }

    public static class CreateColorDepthMaskAction extends ViewerContextAction {

        private Action innerAction;

        @Override
        public String getName() {
            return ContextualActionUtils.getName(innerAction);
        }

        @Override
        public void setup() {
            ViewerContext viewerContext = getViewerContext();

            // reset values
            ContextualActionUtils.setVisible(this, false);
            ContextualActionUtils.setEnabled(this, true);

            DomainObjectImageModel doim = DomainUIUtils.getDomainObjectImageModel(viewerContext);
            if (!viewerContext.isMultiple() && doim != null) {

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

                if (samples.size()==selectedObjects.size()) {
                    if (resultDescriptor.isAligned()) {
                        ContextualActionUtils.setVisible(this, true);

                        Sample sample = samples.get(0);
                        this.innerAction = new CreateMaskFromSampleAction(sample, resultDescriptor, typeName);

                        HasFiles fileProvider = DescriptorUtils.getResult(sample, resultDescriptor);
                        if (fileProvider == null) {
                            ContextualActionUtils.setEnabled(this, false);
                        } else {
                            FileType fileType = FileType.valueOf(typeName);
                            if (fileType == FileType.ColorDepthMip1
                                    || fileType == FileType.ColorDepthMip2
                                    || fileType == FileType.ColorDepthMip3
                                    || fileType == FileType.ColorDepthMip4) {
                                ContextualActionUtils.setEnabled(this, true);
                            }
                            else {
                                ContextualActionUtils.setEnabled(this, false);
                            }
                        }
                    }
                }
                else if (images.size()==selectedObjects.size()) {
                    ContextualActionUtils.setVisible(this, true);
                    this.innerAction = new CreateMaskFromImageAction(images.get(0));
                }
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (innerAction != null) {
                innerAction.actionPerformed(e);
            }
        }
    }
}