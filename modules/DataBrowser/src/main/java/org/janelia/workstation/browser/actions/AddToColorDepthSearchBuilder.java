package org.janelia.workstation.browser.actions;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.gui.colordepth.ColorDepthMask;
import org.janelia.workstation.browser.gui.colordepth.ColorDepthSearchDialog;
import org.janelia.workstation.common.actions.DomainObjectNodeAction;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.janelia.workstation.integration.spi.domain.ContextualActionUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position=580)
public class AddToColorDepthSearchBuilder implements ContextualActionBuilder {

    private static AddToColorDepthSearchAction action = new AddToColorDepthSearchAction();

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof ColorDepthMask;
    }

    @Override
    public Action getAction(Object obj) {
        return action;
    }

    @Override
    public Action getNodeAction(Object obj) {
        return action;
    }

    public static class AddToColorDepthSearchAction extends DomainObjectNodeAction {

        private ColorDepthMask colorDepthMask;

        @Override
        public String getName() {
            return "Add Mask to Color Depth Search...";
        }

        @Override
        public void setViewerContext(ViewerContext viewerContext) {

            List<DomainObject> domainObjectList = viewerContext.getDomainObjectList();

            // reset values
            ContextualActionUtils.setVisible(this, false);
            ContextualActionUtils.setEnabled(this, true);

            if (!viewerContext.isMultiple()) {

                List<ColorDepthMask> masks = new ArrayList<>();
                for(DomainObject domainObject : domainObjectList) {
                    if (domainObject instanceof ColorDepthMask) {
                        masks.add((ColorDepthMask)domainObject);
                    }
                }

                if (masks.size()==1) {
                    this.colorDepthMask = masks.get(0);
                    ContextualActionUtils.setVisible(this, true);
                }
            }
        }

        @Override
        protected void executeAction() {
            new ColorDepthSearchDialog().showForMask(colorDepthMask);
        }
    }
}