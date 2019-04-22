package org.janelia.workstation.browser.actions;

import java.io.File;

import javax.swing.Action;
import javax.swing.JOptionPane;

import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.interfaces.HasFiles;
import org.janelia.workstation.browser.gui.support.SampleUIUtils;
import org.janelia.workstation.common.gui.support.DesktopApi;
import org.janelia.workstation.common.actions.DomainObjectNodeAction;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.util.FileCallable;
import org.janelia.workstation.core.util.SystemInfo;
import org.janelia.workstation.core.util.Utils;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.janelia.workstation.integration.spi.domain.ContextualActionUtils;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.util.lookup.ServiceProvider;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position = 210)
public class OpenInDefaultAppBuilder implements ContextualActionBuilder {

    private static final OpenInDefaultAppAction action = new OpenInDefaultAppAction();

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof DomainObject && (SystemInfo.isMac || SystemInfo.isLinux);
    }

    @Override
    public Action getAction(Object obj) {
        return action;
    }

    public static class OpenInDefaultAppAction extends DomainObjectNodeAction {

        private String filepath;

        @Override
        public String getName() {
            return "Open With OS";
        }

        @Override
        public void setViewerContext(ViewerContext viewerContext) {
            ContextualActionUtils.setVisible(this, false);
            if (!viewerContext.isMultiple()) {
                HasFiles fileProvider = SampleUIUtils.getSingleResult(viewerContext);
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
            try {
                if (filepath == null) {
                    throw new Exception("Entity has no file path");
                }
                ActivityLogHelper.logUserAction("OpenInFinderAction.doAction", filepath);
                Utils.processStandardFilepath(filepath, new FileCallable() {
                    @Override
                    public void call(File file) throws Exception {
                        if (file == null) {
                            JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                                    "Could not open file path", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                        else {
                            if (!DesktopApi.open(file)) {
                                JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                                        "Error opening file path", "Error", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                });
            }
            catch (Exception e) {
                FrameworkAccess.handleException(e);
            }
        }
    }
}
