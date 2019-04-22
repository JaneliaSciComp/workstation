package org.janelia.workstation.browser.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.janelia.model.domain.DomainObject;
import org.janelia.workstation.browser.gui.hud.Hud;
import org.janelia.workstation.common.actions.ViewerContextAction;
import org.janelia.workstation.common.gui.model.DomainObjectImageModel;
import org.janelia.workstation.common.gui.util.DomainUIUtils;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.model.descriptors.ArtifactDescriptor;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.janelia.workstation.integration.spi.domain.ContextualActionUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position=1000)
public class LightboxToggleBuilder implements ContextualActionBuilder {

    private static final LightboxToggleAction action = new LightboxToggleAction();

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

    public static class LightboxToggleAction extends ViewerContextAction {

        private DomainObject domainObject;
        private ArtifactDescriptor resultDescriptor;
        private String typeName;

        @Override
        public String getName() {
            return "Show in Lightbox";
        }

        @Override
        public void setup() {
            ViewerContext viewerContext = getViewerContext();
            ContextualActionUtils.setVisible(this, false);
            DomainObjectImageModel doim = DomainUIUtils.getDomainObjectImageModel(viewerContext);
            if (doim != null) {
                this.domainObject = DomainUIUtils.getLastSelectedDomainObject(viewerContext);
                this.resultDescriptor = doim.getArtifactDescriptor();
                this.typeName = doim.getImageTypeName();
                ContextualActionUtils.setVisible(this, domainObject!=null && !viewerContext.isMultiple());
            }
        }

        @Override
        public JMenuItem getPopupPresenter() {
            JMenuItem menuItem = new JMenuItem(getName());
            menuItem.addActionListener(this);
            menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0));
            return menuItem;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ViewerContext viewerContext = getViewerContext();
            ActivityLogHelper.logUserAction("DomainObjectContentMenu.showInLightbox", domainObject);
            Hud.getSingletonInstance().setObjectAndToggleDialog(domainObject, resultDescriptor, typeName, true, true);
        }
    }
}
