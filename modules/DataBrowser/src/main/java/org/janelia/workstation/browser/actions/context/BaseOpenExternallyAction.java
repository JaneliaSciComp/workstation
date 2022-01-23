package org.janelia.workstation.browser.actions.context;

import javax.swing.*;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.enums.FileType;
import org.janelia.model.domain.interfaces.HasFiles;
import org.janelia.model.domain.sample.PipelineResult;
import org.janelia.model.domain.sample.Sample;
import org.janelia.model.domain.sample.SamplePostProcessingResult;
import org.janelia.workstation.browser.gui.support.ResultSelectionButton;
import org.janelia.workstation.browser.gui.support.SampleUIUtils;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.common.gui.model.DomainObjectImageModel;
import org.janelia.workstation.common.gui.model.SampleResultModel;
import org.janelia.workstation.common.gui.util.DomainUIUtils;
import org.janelia.workstation.common.gui.util.UIUtils;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.model.ImageModel;
import org.janelia.workstation.core.model.descriptors.ArtifactDescriptor;
import org.janelia.workstation.core.model.descriptors.DescriptorUtils;
import org.janelia.workstation.core.model.descriptors.LatestDescriptor;
import org.janelia.workstation.core.model.descriptors.ResultArtifactDescriptor;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for actions which want to open a stack with an external tool.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class BaseOpenExternallyAction extends BaseContextualNodeAction {

    private final static Logger log = LoggerFactory.getLogger(BaseOpenExternallyAction.class);

    private DomainObject selectedObject;
    private ArtifactDescriptor rd;
    private String filepath;

    @Override
    protected void processContext() {

        this.selectedObject = null;
        this.rd = null;
        this.filepath = null;

        if (getNodeContext().isSingleObjectOfType(DomainObject.class)) {
            ViewerContext<?,?> viewerContext = getViewerContext();
            DomainObjectImageModel doim = DomainUIUtils.getDomainObjectImageModel(viewerContext);
            if (doim != null) {
                rd = doim.getArtifactDescriptor();
                log.trace("descriptor={}", rd);
            }
            HasFiles fileProvider = SampleUIUtils.getSingle3dResult(viewerContext);
            if (fileProvider != null) {
                this.filepath = DomainUtils.getDefault3dImageFilePath(fileProvider);
                log.trace("filepath={}", filepath);
            }
            this.selectedObject = getNodeContext().getSingleObjectOfType(DomainObject.class);
        }
        else if (getNodeContext().isSingleObjectOfType(PipelineResult.class)) {
            SampleResultModel srm = DomainUIUtils.getSampleResultModel(getViewerContext());
            if (srm != null) {
                PipelineResult pipelineResult = getNodeContext().getSingleObjectOfType(PipelineResult.class);
                this.filepath = DomainUtils.getFilepath(pipelineResult, srm.getFileType());
                log.trace("filepath={}", filepath);
                this.selectedObject = pipelineResult.getParentRun().getParent().getParent();
            }
        }

        // Enable the option if any filepath is available, we'll clarify it later.
        setEnabledAndVisible(filepath != null);
    }

    public DomainObject getSelectedObject() {
        return selectedObject;
    }

    /**
     * This method can be called by subclasses to get the filepath that should be opened. It may show a dialog
     * to the user to disambiguate the filepath, so it must be called in the EDT.
     * @return filepath
     */
    protected String getFilepath() {
        if (!SwingUtilities.isEventDispatchThread()) {
            log.warn("getFilepath illegally called from non-EDT thread");
        }
        if (rd != null && needsClarity(rd)) {
            return showSelectionDialog();
        }
        return filepath;
    }

    private boolean needsClarity(ArtifactDescriptor artifactDescriptor) {
        log.trace("needsClarity({})?", artifactDescriptor);
        if (selectedObject instanceof Sample) {
            if (artifactDescriptor instanceof LatestDescriptor) return true;
            if (artifactDescriptor instanceof ResultArtifactDescriptor) {
                ResultArtifactDescriptor rad = (ResultArtifactDescriptor) artifactDescriptor;
                if (SamplePostProcessingResult.class.getName().equals(rad.getResultClass())) {
                    return true;
                }
            }
        }
        return false;
    }

    private String showSelectionDialog() {

        ResultSelectionButton resultButton = new ResultSelectionButton(
                true, false, false, true) {
            @Override
            protected JMenuItem createMenuItem(String text, boolean selected) {
                // Override to remove radio buttons from menu items
                return new JMenuItem(text);
            }
            @Override
            protected void resultChanged(ArtifactDescriptor resultDescriptor) {
                JDialog dialog = UIUtils.getAncestorWithType(this, JDialog.class);
                if (dialog != null) {
                    // Resize the dialog to accommodate the selected type
                    dialog.pack();
                }
            }
        };
        resultButton.populate(selectedObject);

        final JComponent[] inputs = new JComponent[] {
                resultButton
        };

        int result = JOptionPane.showConfirmDialog(FrameworkAccess.getMainFrame(), inputs,
                "Choose result to open", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            ArtifactDescriptor rd = resultButton.getResultDescriptor();
            Sample sample = (Sample) selectedObject;
            HasFiles fileProvider = DescriptorUtils.getLatestResult(sample, rd);
            this.filepath = DomainUtils.getDefault3dImageFilePath(fileProvider);
            return filepath;
        }

        return null;
    }

}
