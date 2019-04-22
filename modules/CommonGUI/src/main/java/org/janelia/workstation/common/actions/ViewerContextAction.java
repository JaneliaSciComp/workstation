package org.janelia.workstation.common.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;

import org.janelia.workstation.core.actions.PopupMenuGenerator;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.actions.ViewerContextReceiver;
import org.janelia.workstation.integration.spi.domain.ContextualActionUtils;

/**
 * A convenience class for implementing actions based on a ViewerContext. Provides access to the viewer context
 * via a getViewerContext method.
 *
 * When using this class as a base class, override getName to give your action a name. Then you can choose to override
 * either executeAction or getPopupPresenter, depending on whether or not you need submenus. You can also override
 * isVisible to make your action conditionally visible (conditional on anything in the ViewerContext).
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class ViewerContextAction extends AbstractAction implements ViewerContextReceiver, PopupMenuGenerator {

    private ViewerContext viewerContext;

    @Override
    public void setViewerContext(ViewerContext viewerContext) {
        this.viewerContext = viewerContext;
        setup();
        ContextualActionUtils.setName(this, getName());
        if (isVisible() != null) {
            ContextualActionUtils.setVisible(this, isVisible());
        }
    }

    /**
     * Override this method to do something after the viewContext has been injected, but before methods like
     * getName() and isVisible() are called.
     */
    protected void setup() {
    }

    protected ViewerContext getViewerContext() {
        return viewerContext;
    }

    public abstract String getName();

    protected Boolean isVisible() {
        return null;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        executeAction();
    }

    protected void executeAction() {
    }

    @Override
    public JMenuItem getPopupPresenter() {
        return null;
    }
}
