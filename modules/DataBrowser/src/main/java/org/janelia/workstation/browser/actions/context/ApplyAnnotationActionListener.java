package org.janelia.workstation.browser.actions.context;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.*;

import com.google.common.collect.Multimap;
import org.janelia.workstation.core.util.Progress;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.ontology.Category;
import org.janelia.model.domain.ontology.EnumItem;
import org.janelia.model.domain.ontology.Ontology;
import org.janelia.model.domain.ontology.OntologyTerm;
import org.janelia.model.domain.ontology.OntologyTermReference;
import org.janelia.model.security.util.PermissionTemplate;
import org.janelia.workstation.browser.api.state.DataBrowserMgr;
import org.janelia.workstation.browser.gui.ontology.AnnotationEditor;
import org.janelia.workstation.browser.gui.options.BrowserOptions;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.events.selection.GlobalDomainObjectSelectionModel;
import org.janelia.workstation.core.workers.ResultWorker;
import org.janelia.workstation.core.workers.SimpleListenableFuture;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action class for applying ontology terms to domain objects.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ApplyAnnotationActionListener implements ActionListener {

    private final static Logger log = LoggerFactory.getLogger(ApplyAnnotationActionListener.class);

    private Collection<OntologyTerm> ontologyTerms;
    private final boolean isDuplicateAnnotationAllowed;
    private final boolean overrideExisting;
    private boolean batchMode;

    public ApplyAnnotationActionListener() {
        this(new ArrayList<>());
    }

    public ApplyAnnotationActionListener(boolean overrideExisting) {
        this(new ArrayList<>(), overrideExisting);
    }

    public ApplyAnnotationActionListener(Collection<OntologyTerm> ontologyTerms) {
        this(ontologyTerms, true);
    }

    public ApplyAnnotationActionListener(Collection<OntologyTerm> ontologyTerms, boolean overrideExisting) {
        this.ontologyTerms = ontologyTerms;
        this.overrideExisting = overrideExisting;
        this.isDuplicateAnnotationAllowed = BrowserOptions.getInstance().isDuplicateAnnotationAllowed();
    }

    public void setBatchMode(boolean batchMode) {
        this.batchMode = batchMode;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ActivityLogHelper.logUserAction("ApplyAnnotationActionListener.actionPerformed");
        for(OntologyTerm ontologyTerm : ontologyTerms) {
            performAction(ontologyTerm);
        }
    }

    public void performAction(final OntologyTerm ontologyTerm) {

        if (ontologyTerm instanceof Category || ontologyTerm instanceof org.janelia.model.domain.ontology.Enum) {
            // Cannot annotate with a category or enum
            return;
        }

        try {
            final List<Reference> selectedIds = GlobalDomainObjectSelectionModel.getInstance().getSelectedIds();

            if (selectedIds.isEmpty()) {
                // Cannot annotate nothing
                log.warn("ApplyAnnotationAction called without any objects being selected");
                JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                        "Select some items to annotate",
                        "No items selected",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            String keyTermValue = ontologyTerm.getName();
            Long keyTermId = ontologyTerm.getId();
            log.info("Will annotate all selected objects with: {} ({})", keyTermValue, keyTermId);
            log.info("Selected items: "+selectedIds);

            annotateReferences(ontologyTerm, selectedIds);
        }
        catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
    }

    public SimpleListenableFuture<List<Annotation>> annotateReferences(final OntologyTerm ontologyTerm, final List<Reference> references) {
        Ontology ontology = ontologyTerm.getOntology();

        AnnotationEditor editor = new AnnotationEditor(ontology, ontologyTerm);

        String value = null;
        if (editor.needsEditor()) {
            value = editor.showEditor();
            if (value==null) return null;
        }

        final String finalValue = value;
        ResultWorker<List<Annotation>> worker = new ResultWorker<List<Annotation>>() {

            @Override
            protected List<Annotation> createResult() throws Exception {
                return setReferenceAnnotations(references, ontologyTerm, finalValue, this);
            }

            @Override
            protected void hadSuccess() {
                // UI will be updated by events
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
            }

        };

        worker.setProgressMonitor(new ProgressMonitor(FrameworkAccess.getMainFrame(), "Adding annotations", "", 0, 100));
        return worker.executeWithFuture();
    }

    public List<Annotation> setReferenceAnnotations(List<Reference> targetIds, OntologyTerm ontologyTerm, String value, Progress progress) throws Exception {
        DomainModel model = DomainMgr.getDomainMgr().getModel();
        List<DomainObject> domainObjects = model.getDomainObjects(targetIds);
        return setObjectAnnotations(domainObjects, ontologyTerm, value, progress);
    }

    public List<Annotation> setObjectAnnotations(List<? extends DomainObject> domainObjects, OntologyTerm ontologyTerm, String value, Progress progress) throws Exception {

        DomainModel model = DomainMgr.getDomainMgr().getModel();
        try {
            if (batchMode) {
                log.info("Batch mode ON");
                model.setNotify(false);
            }
            List<Reference> refs = DomainUtils.getReferences(domainObjects);
            Multimap<Long, Annotation> annotationMap = DomainUtils.getAnnotationsByDomainObjectId(model.getAnnotations(refs));

            List<Annotation> createdAnnotations = new ArrayList<>();
            int i = 1;
            for (DomainObject domainObject : domainObjects) {

                Annotation existingAnnotation = null;
                if (!isDuplicateAnnotationAllowed) {

                    Collection<Annotation> annotations = annotationMap.get(domainObject.getId());
                    if (annotations != null) {

                        OntologyTerm keyTerm = ontologyTerm;
                        if (keyTerm instanceof EnumItem) {
                            keyTerm = ontologyTerm.getParent();
                        }

                        for (Annotation annotation : annotations) {
                            if (annotation.getKeyTerm()!=null
                                    && annotation.getKeyTerm().getOntologyTermId() != null
                                    && annotation.getKeyTerm().getOntologyTermId().equals(keyTerm.getId())) {
                                existingAnnotation = annotation;
                                break;
                            }
                        }
                    }
                }

                if (existingAnnotation != null) {
                    if (!overrideExisting) {
                        createdAnnotations.add(existingAnnotation);
                    } else {
                        log.info("Found existing annotation to update: " + existingAnnotation);
                        createdAnnotations.add(doAnnotation(domainObject, existingAnnotation, ontologyTerm, value));
                    }
                } else {
                    createdAnnotations.add(doAnnotation(domainObject, null, ontologyTerm, value));
                }

                // Update progress
                if (progress != null) {
                    if (progress.isCancelled()) return createdAnnotations;
                    progress.setProgress(i++, domainObjects.size());
                }
            }
            return createdAnnotations;
        }
        finally {
            if (batchMode) {
                log.info("Batch mode OFF");
                model.setNotify(true);
            }
        }
    }

    public Annotation addAnnotation(DomainObject target, OntologyTerm ontologyTerm, String value) throws Exception {
        return doAnnotation(target, null, ontologyTerm, value);
    }

    private Annotation doAnnotation(DomainObject target, Annotation existingAnnotation, OntologyTerm ontologyTerm, String value) throws Exception {

        DomainModel model = DomainMgr.getDomainMgr().getModel();
        Annotation savedAnnotation = model.createAnnotation(Reference.createFor(target), OntologyTermReference.createFor(ontologyTerm), value);
        log.info("Saved annotation: " + savedAnnotation);

        PermissionTemplate template = DataBrowserMgr.getDataBrowserMgr().getAutoShareTemplate();
        if (template!=null) {
            model.changePermissions(savedAnnotation, template.getSubjectKey(), template.getPermissions());
            log.info("Auto-shared annotation with " + template.getSubjectKey());
        }

        return savedAnnotation;
    }

}
