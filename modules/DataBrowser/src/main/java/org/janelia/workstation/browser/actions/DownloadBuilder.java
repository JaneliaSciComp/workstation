package org.janelia.workstation.browser.actions;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.janelia.model.domain.DomainObject;
import org.janelia.workstation.browser.gui.dialogs.download.DownloadWizardAction;
import org.janelia.workstation.common.actions.DomainObjectNodeAction;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.openide.util.lookup.ServiceProvider;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position=400)
public class DownloadBuilder implements ContextualActionBuilder {

    private static final DownloadAction action = new DownloadAction();

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof DomainObject;
    }

    @Override
    public Action getAction(Object obj) {
        return action;
    }

    @Override
    public Action getNodeAction(Object obj) {
        return action;
    }

    public static class DownloadAction extends DomainObjectNodeAction {

        @Override
        public String getName() {
            return "Download Files...";
        }

        @Override
        public JMenuItem getPopupPresenter() {
            String label = domainObjectList.size() > 1 ? "Download " + domainObjectList.size() + " Items..." : "Download...";
            JMenuItem menuItem = new JMenuItem(label);
            menuItem.addActionListener(new DownloadWizardAction(domainObjectList, null));
            menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.META_DOWN_MASK));
            return menuItem;
        }
    }
}
