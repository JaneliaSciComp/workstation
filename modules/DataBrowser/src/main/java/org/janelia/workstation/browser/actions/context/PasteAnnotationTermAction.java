package org.janelia.workstation.browser.actions.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.swing.ProgressMonitor;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.api.StateMgr;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

/**
 * Paste the Annotation in the clipboard onto one or more items.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "actions",
        id = "PasteAnnotationTermAction"
)
@ActionRegistration(
        displayName = "#CTL_PasteAnnotationTermAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions", position = 10)
})
@NbBundle.Messages("CTL_PasteAnnotationTermAction=Paste Annotation")
public class PasteAnnotationTermAction extends BaseContextualNodeAction {

    private Collection<DomainObject> domainObjects = new ArrayList<>();

    @Override
    protected void processContext() {
        domainObjects.clear();
        if (getViewerContext()!=null
                && StateMgr.getStateMgr().getCurrentSelectedOntologyAnnotation()!=null
                && getNodeContext().isOnlyObjectsOfType(DomainObject.class)) {
            domainObjects.addAll(getNodeContext().getOnlyObjectsOfType(DomainObject.class));
            setEnabledAndVisible(true);
        }
        else {
            setEnabledAndVisible(false);
        }
    }

    @Override
    public void performAction() {

        Collection<DomainObject> domainObjects = new ArrayList<>(this.domainObjects);

        ActivityLogHelper.logUserAction("PasteAnnotationTermAction.doAction");

        final DomainModel model = DomainMgr.getDomainMgr().getModel();

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {

                int i = 1;
                for (DomainObject domainObject : domainObjects) {
                    Annotation baseAnnotation = StateMgr.getStateMgr().getCurrentSelectedOntologyAnnotation();
                    model.createAnnotation(Reference.createFor(domainObject), baseAnnotation.getKeyTerm(), baseAnnotation.getValue());
                    setProgress(i++, domainObjects.size());
                }
            }

            @Override
            protected void hadSuccess() {
                // No need to do anything
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
            }
        };

        worker.setProgressMonitor(new ProgressMonitor(FrameworkAccess.getMainFrame(), "Pasting Annotations", "", 0, 100));
        worker.execute();
    }
}
