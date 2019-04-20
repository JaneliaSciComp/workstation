package org.janelia.workstation.browser.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.janelia.it.jacs.shared.utils.domain.DataReporter;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.ontology.OntologyTerm;
import org.janelia.workstation.browser.nb_action.ApplyAnnotationAction;
import org.janelia.workstation.core.actions.PopupMenuGenerator;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.actions.ViewerContextReceiver;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.StateMgr;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.core.workers.SimpleListenableFuture;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.janelia.workstation.integration.spi.domain.ContextualActionUtils;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.util.lookup.ServiceProvider;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position=500)
public class ReportProblemBuilder implements ContextualActionBuilder {

    private static final String WEBSTATION_URL = ConsoleProperties.getInstance().getProperty("webstation.url");
    private static final String HELP_EMAIL = ConsoleProperties.getString("console.HelpEmail");

    private static ReportProblemAction action = new ReportProblemAction();

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

    public static class ReportProblemAction extends AbstractAction implements ViewerContextReceiver, PopupMenuGenerator {

        private DomainObject domainObject;

        @Override
        public void setViewerContext(ViewerContext viewerContext) {
            this.domainObject = viewerContext.getDomainObject();
            ContextualActionUtils.setVisible(this, !viewerContext.isMultiple());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // Handled by popup menu
        }

        @Override
        public JMenuItem getPopupPresenter() {

            JMenu errorMenu = new JMenu("Report A Problem With This Data");

            OntologyTerm errorOntology = StateMgr.getStateMgr().getErrorOntology();
            if (errorOntology==null) return null;

            for (final OntologyTerm term : errorOntology.getTerms()) {
                errorMenu.add(new JMenuItem(term.getName())).addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {

                        ActivityLogHelper.logUserAction("DomainObjectContentMenu.reportAProblemWithThisData", domainObject);

                        final ApplyAnnotationAction action = ApplyAnnotationAction.get();
                        SimpleListenableFuture<List<Annotation>> future = action.annotateReferences(term, Arrays.asList(Reference.createFor(domainObject)));

                        if (future!=null) {
                            future.addListener(() -> {
                                try {
                                    List<Annotation> annotations = future.get();
                                    if (annotations!=null && !annotations.isEmpty()) {
                                        DataReporter reporter = new DataReporter(AccessManager.getUserEmail(), HELP_EMAIL, WEBSTATION_URL);
                                        reporter.reportData(domainObject, annotations.get(0).getName());
                                    }
                                }
                                catch (Exception ex) {
                                    FrameworkAccess.handleException(ex);
                                }
                            });
                        }
                    }
                });

            }

            return errorMenu;
        }
    }
}