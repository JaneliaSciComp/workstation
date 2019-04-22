package org.janelia.workstation.browser.actions;

import javax.swing.Action;

import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.interfaces.HasFiles;
import org.janelia.workstation.browser.gui.support.SampleUIUtils;
import org.janelia.workstation.browser.tools.ToolMgr;
import org.janelia.workstation.common.actions.DomainObjectNodeAction;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.janelia.workstation.integration.spi.domain.ContextualActionUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position = 250)
public class OpenInFijiViewerBuilder implements ContextualActionBuilder {

    private static final OpenInVaa3dTriViewAction action = new OpenInVaa3dTriViewAction();

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof DomainObject;
    }

    @Override
    public Action getAction(Object obj) {
        return action;
    }

    public static class OpenInVaa3dTriViewAction extends DomainObjectNodeAction {

        private String filepath;

        @Override
        public String getName() {
            return OpenInToolAction.getName(ToolMgr.TOOL_FIJI, null);
        }

        @Override
        public void setViewerContext(ViewerContext viewerContext) {
            ContextualActionUtils.setVisible(this, false);
            if (!viewerContext.isMultiple()) {
                HasFiles fileProvider = SampleUIUtils.getSingle3dResult(viewerContext);
                if (fileProvider != null) {
                    this.filepath = DomainUtils.getDefault3dImageFilePath(fileProvider);
                    if (filepath != null) {
                        ContextualActionUtils.setVisible(this, true);
                    }
                }
            }
        }

        @Override
        protected void executeAction() {
            OpenInToolAction action = new OpenInToolAction(ToolMgr.TOOL_FIJI, filepath, null);
            action.actionPerformed(null);
        }
    }
}
