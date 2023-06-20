package org.janelia.workstation.site.jrc.action.context;

import org.janelia.model.domain.sample.LineRelease;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.common.actions.BaseContextualPopupAction;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.site.jrc.util.SiteUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.SystemAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

@ActionID(
        category = "actions",
        id = "ViewLineReleasesAction"
)
@ActionRegistration(
        displayName = "#CTL_ViewLineReleasesAction"
)
@ActionReferences({
    @ActionReference(path = "Menu/Actions/Sample", position = 541)
})
@Messages("CTL_ViewLineReleasesAction=View Line Releases")
public final class ViewLineReleasesAction extends BaseContextualPopupAction {

    private static final Logger log = LoggerFactory.getLogger(ViewLineReleasesAction.class);

    public static ViewLineReleasesAction get() {
        return SystemAction.get(ViewLineReleasesAction.class);
    }

    private Sample sample;

    @Override
    protected void processContext() {
        sample = null;
        if (getNodeContext().isSingleObjectOfType(Sample.class)) {
            sample = getNodeContext().getSingleObjectOfType(Sample.class);
            setEnabledAndVisible(true);
        }
        else {
            setEnabledAndVisible(false);
        }
    }

    @Override
    public String getName() {
        return "View Line Releases";
    }

    @Override
    protected java.util.List<JComponent> getItems() {
        List<JComponent> items = new ArrayList<>();

        if (sample != null) {
            try {
                DomainModel model = DomainMgr.getDomainMgr().getModel();
                if (model != null) {
                    List<LineRelease> sampleLineReleases = model.getLineReleases(sample);

                    // Add all those releases to the menu
                    for (LineRelease lineRelease : sampleLineReleases) {
                        JMenuItem releaseItem = new JMenuItem(lineRelease.getName() + " (" + lineRelease.getOwnerName() + ")");
                        releaseItem.addActionListener(actionEvent -> SiteUtils.navigateToLineRelease(lineRelease.getId()));
                        items.add(releaseItem);
                    }
                }

            }
            catch (Exception e) {
                FrameworkAccess.handleException(e);
            }
        }

        if (items.isEmpty()) {
            JMenuItem emptyItem = new JMenuItem("Selected samples are not released");
            emptyItem.setEnabled(false);
            items.add(emptyItem);
        }

        return items;
    }
}
