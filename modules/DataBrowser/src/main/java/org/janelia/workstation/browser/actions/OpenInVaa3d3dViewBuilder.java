package org.janelia.workstation.browser.actions;

import javax.swing.Action;

import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.interfaces.HasFiles;
import org.janelia.workstation.browser.gui.support.SampleUIUtils;
import org.janelia.workstation.browser.tools.ToolMgr;
import org.janelia.workstation.common.actions.ViewerContextAction;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.janelia.workstation.integration.spi.domain.ContextualActionUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position = 231)
public class OpenInVaa3d3dViewBuilder implements ContextualActionBuilder {

    private static final OpenInVaa3dTriViewAction action = new OpenInVaa3dTriViewAction();

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof DomainObject;
    }

    @Override
    public Action getAction(Object obj) {
        return action;
    }

    public static class OpenInVaa3dTriViewAction extends ViewerContextAction {

        private String filepath;

        @Override
        public String getName() {
            return OpenInToolAction.getName(ToolMgr.TOOL_VAA3D, ToolMgr.MODE_VAA3D_3D);
        }

        @Override
        public void setup() {
            ViewerContext viewerContext = getViewerContext();
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
            OpenInToolAction action = new OpenInToolAction(ToolMgr.TOOL_VAA3D, filepath, ToolMgr.MODE_VAA3D_3D);
            action.actionPerformed(null);

            /*
             TODO: this could be supported but it needs some changed to the DomainObjectHandler so that sample
                   nodes can be generated under the RecentlyOpenedItemsNode, but not otherwise
             */
//            Object obj = getViewerContext().getLastSelectedObject();
//            if (obj instanceof Sample) {
//                FrameworkAccess.getBrowsingController().updateRecentlyOpenedHistory(Reference.createFor((Sample)obj));
//            }
//            else if (obj instanceof LSMImage) {
//                FrameworkAccess.getBrowsingController().updateRecentlyOpenedHistory(Reference.createFor((LSMImage)obj));
//            }

        }
    }
}
