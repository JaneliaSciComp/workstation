package org.janelia.workstation.browser.actions.context;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.interfaces.HasFiles;
import org.janelia.model.domain.sample.Sample;
import org.janelia.model.domain.sample.SamplePostProcessingResult;
import org.janelia.workstation.browser.gui.support.ResultSelectionButton;
import org.janelia.workstation.browser.gui.support.SampleUIUtils;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.common.gui.model.DomainObjectImageModel;
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
        this.filepath = null;
        if (getNodeContext().isSingleObjectOfType(DomainObject.class)) {
            this.selectedObject = getNodeContext().getSingleObjectOfType(DomainObject.class);
            ViewerContext viewerContext = getViewerContext();
            log.trace("viewerContext={}", viewerContext);

            if (viewerContext != null) {

                ImageModel imageModel = viewerContext.getImageModel();
                if (imageModel instanceof DomainObjectImageModel) {
                    DomainObjectImageModel doim = (DomainObjectImageModel) imageModel;
                    rd = doim.getArtifactDescriptor();
                    log.trace("descriptor={}", rd);
                }

                HasFiles fileProvider = SampleUIUtils.getSingle3dResult(viewerContext);
                if (fileProvider != null) {
                    this.filepath = DomainUtils.getDefault3dImageFilePath(fileProvider);
                    log.trace("filepath={}", filepath);
                }
            }
        }

        // Enable the option if any filepath is available, we'll clarify it later.
        setEnabledAndVisible(filepath != null);
    }

    protected String getFilepath(HasFiles fileProvider) {
        return DomainUtils.getDefault3dImageFilePath(fileProvider);
    }

    protected String getFilepath() {
        if (needsClarity(rd)) {
            return showSelectionDialog();
        }
        return filepath;
    }

    private boolean needsClarity(ArtifactDescriptor rd) {
        log.trace("needsClarity({})?", rd);
        if (selectedObject instanceof Sample) {
            if (rd instanceof LatestDescriptor) return true;
            if (rd instanceof ResultArtifactDescriptor) {
                ResultArtifactDescriptor rad = (ResultArtifactDescriptor) rd;
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
            HasFiles fileProvider = DescriptorUtils.getResult(sample, rd);
            this.filepath = DomainUtils.getDefault3dImageFilePath(fileProvider);
            return filepath;
        }

        return null;
    }

}
