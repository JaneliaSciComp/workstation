package org.janelia.workstation.browser.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.janelia.model.domain.DomainObject;
import org.janelia.workstation.browser.gui.hud.Hud;
import org.janelia.workstation.core.actions.PopupMenuGenerator;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.actions.ViewerContextReceiver;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.model.descriptors.ArtifactDescriptor;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
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

    public static class LightboxToggleAction extends AbstractAction implements ViewerContextReceiver, PopupMenuGenerator {

        private DomainObject domainObject;
        private ArtifactDescriptor resultDescriptor;
        private String typeName;

        @Override
        public void setViewerContext(ViewerContext viewerContext) {
            this.domainObject = viewerContext.getDomainObject();
            this.resultDescriptor = viewerContext.getResultDescriptor();
            this.typeName = viewerContext.getTypeName();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ActivityLogHelper.logUserAction("DomainObjectContentMenu.showInLightbox", domainObject);
            Hud.getSingletonInstance().setObjectAndToggleDialog(domainObject, resultDescriptor, typeName, true, true);
        }

        @Override
        public JMenuItem getPopupPresenter() {
            JMenuItem menuItem = new JMenuItem("Show in Lightbox");
            menuItem.addActionListener(this);
            menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0));
            return menuItem;
        }
    }
}
