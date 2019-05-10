package org.janelia.workstation.browser.actions;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.interfaces.HasFiles;
import org.janelia.workstation.browser.gui.support.SampleUIUtils;
import org.janelia.workstation.common.actions.DomainObjectNodeAction;
import org.janelia.workstation.common.gui.support.DesktopApi;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.util.FileCallable;
import org.janelia.workstation.core.util.SystemInfo;
import org.janelia.workstation.core.util.Utils;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.janelia.workstation.integration.spi.domain.ContextualActionUtils;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.util.lookup.ServiceProvider;

import javax.swing.Action;
import javax.swing.JOptionPane;
import java.io.File;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position = 200)
public class OpenInFinderBuilder implements ContextualActionBuilder {

    private static final OpenInFinderAction action = new OpenInFinderAction();

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof DomainObject;
    }

    @Override
    public boolean isPrecededBySeparator() {
        return true;
    }

    @Override
    public Action getAction(Object obj) {
        return action;
    }

    public static class OpenInFinderAction extends DomainObjectNodeAction {

        private String filepath;

        @Override
        public String getName() {
            if (SystemInfo.isMac) {
                return "Reveal In Finder";
            } else if (SystemInfo.isLinux) {
                return "Reveal In File Manager";
            } else if (SystemInfo.isWindows) {
                return "Reveal In Windows Explorer";
            }
            return "Unsupported";
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
                ActivityLogHelper.logUserAction("OpenInFinderAction.doAction", filepath);
                Utils.processStandardFilepath(filepath, new FileCallable() {
                    @Override
                    public void call(File file) throws Exception {
                        if (file == null) {
                            JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                                    "Could not open file path", "Error", JOptionPane.ERROR_MESSAGE);
                        } else {
                            if (!DesktopApi.browse(file)) {
                                JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                                        "Error opening file path", "Error", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                });
            } catch (Exception e) {
                FrameworkAccess.handleException(e);
            }
        }
    }
}
