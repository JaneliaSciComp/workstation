package org.janelia.workstation.browser.actions.context;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.interfaces.HasFiles;
import org.janelia.workstation.browser.gui.support.SampleUIUtils;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.util.SystemInfo;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class BaseOpenExternallyAction extends BaseContextualNodeAction {

    protected String filepath;

    @Override
    protected void processContext() {
        this.filepath = null;
        if (SystemInfo.isMac || SystemInfo.isLinux) {
            if (getNodeContext().isSingleObjectOfType(DomainObject.class)) {
                DomainObject selectedObject = getNodeContext().getSingleObjectOfType(DomainObject.class);
                ViewerContext viewerContext = getViewerContext();
                if (viewerContext != null) {
                    HasFiles fileProvider = SampleUIUtils.getSingleResult(viewerContext);
                    if (fileProvider != null) {
                        this.filepath = DomainUtils.getDefault3dImageFilePath(fileProvider);
                    }
                }
            }
        }
        setEnabledAndVisible(filepath != null);
    }
}
