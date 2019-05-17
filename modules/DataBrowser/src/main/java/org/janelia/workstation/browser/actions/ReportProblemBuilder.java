package org.janelia.workstation.browser.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.janelia.it.jacs.shared.utils.domain.DataReporter;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.ontology.OntologyTerm;
import org.janelia.workstation.common.actions.DomainObjectNodeAction;
import org.janelia.workstation.common.gui.util.DomainUIUtils;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.StateMgr;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.core.workers.SimpleListenableFuture;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.janelia.workstation.integration.spi.domain.ContextualActionUtils;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position=500)
public class ReportProblemBuilder implements ContextualActionBuilder {

    private static final Logger log = LoggerFactory.getLogger(ReportProblemBuilder.class);

    private static ReportProblemAction action = new ReportProblemAction();

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof DomainObject && !(obj instanceof OntologyTerm);
    }

    @Override
    public boolean isPrecededBySeparator() {
        return true;
    }

    @Override
    public Action getAction(Object obj) {
        return action;
    }

    @Override
    public Action getNodeAction(Object obj) {
        return action;
    }

    public static class ReportProblemAction extends DomainObjectNodeAction {

        private DomainObject domainObject;

        @Override
        public void setViewerContext(ViewerContext viewerContext) {
            this.domainObject = DomainUIUtils.getLastSelectedDomainObject(viewerContext);
            ContextualActionUtils.setVisible(this, domainObject!=null && !viewerContext.isMultiple());
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
                                        reportData(domainObject, annotations.get(0));
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

    private static void reportData(DomainObject domainObject, Annotation annotation) {

        String fromEmail = ConsoleProperties.getString("console.FromEmail", null);
        if (fromEmail==null) {
            log.error("Cannot send exception report: no value for console.FromEmail is configured.");
            return;
        }

        String toEmail = ConsoleProperties.getString("console.HelpEmail", null);
        if (toEmail==null) {
            log.error("Cannot send exception report: no value for console.HelpEmail is configured.");
            return;
        }

        String webstationUrl = ConsoleProperties.getString("webstation.url", null);

        DataReporter reporter = new DataReporter(fromEmail, toEmail, webstationUrl);
        reporter.reportData(domainObject, annotation.getName());
    }
}