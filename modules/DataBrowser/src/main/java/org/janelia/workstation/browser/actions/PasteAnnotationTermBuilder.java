package org.janelia.workstation.browser.actions;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Collections;

import javax.swing.Action;
import javax.swing.ProgressMonitor;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.workstation.common.actions.ViewerContextAction;
import org.janelia.workstation.common.gui.util.DomainUIUtils;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.api.StateMgr;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.util.lookup.ServiceProvider;

/**
 * Paste the Annotation in the clipboard onto one or more items.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position=10)
public class PasteAnnotationTermBuilder implements ContextualActionBuilder {

    private static final PasteAnnotationTermAction action = new PasteAnnotationTermAction();

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof DomainObject;
    }

    @Override
    public Action getAction(Object obj) {
        if (StateMgr.getStateMgr().getCurrentSelectedOntologyAnnotation() == null) {
            return null;
        }
        return action;
    }

    @Override
    public Action getNodeAction(Object obj) {
        return null;
    }

    private static class PasteAnnotationTermAction extends ViewerContextAction {

        private Collection<DomainObject> domainObjects;

        @Override
        public String getName() {
            return "Paste Annotation";
        }

        @Override
        public void setup() {
            this.domainObjects = DomainUIUtils.getSelectedDomainObjects(getViewerContext());
        }

        @Override
        public void actionPerformed(ActionEvent e) {

            ActivityLogHelper.logUserAction("PasteAnnotationTermBuilder.doAction");

            final DomainModel model = DomainMgr.getDomainMgr().getModel();

            SimpleWorker worker = new SimpleWorker() {

                @Override
                protected void doStuff() throws Exception {

                    int i = 1;
                    for (DomainObject domainObject : domainObjects) {
                        Annotation baseAnnotation = StateMgr.getStateMgr().getCurrentSelectedOntologyAnnotation();
                        Annotation annotation = new Annotation(baseAnnotation);
                        // We may be copying an annotation we don't own. Don't copy the ownership.
                        annotation.setOwnerKey(null);
                        annotation.setReaders(Collections.emptySet());
                        annotation.setWriters(Collections.emptySet());
                        annotation.setTarget(Reference.createFor(domainObject));
                        model.create(annotation);
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
}
