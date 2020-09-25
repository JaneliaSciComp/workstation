package org.janelia.workstation.controller.action;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.ViewerEventBus;
import org.janelia.workstation.controller.eventbus.SelectionAnnotationEvent;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;

@ActionID(
        category = "Horta",
        id = "DeleteVertexLinkAction"
)
@ActionRegistration(
        displayName = "#CTL_DeleteVertexLinkAction",
        lazy = false
)
@NbBundle.Messages("CTL_DeleteVertexLinkAction=Delete Vertex Link")
/**
 * delete the annotation with the input ID; the annotation must be a "link",
 * which is an annotation that is not a root (no parent) or branch point
 * (many children); in other words, it's an end point, or an annotation with
 * a parent and single child that can be connected up unambiguously
 */
public class DeleteVertexLinkAction extends AbstractAction {
    private static final Logger log = LoggerFactory.getLogger(DeleteVertexLinkAction.class);

    public DeleteVertexLinkAction() {
        super("Delete Vertex Link");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // get current neuron and selected annotation
        TmNeuronMetadata currNeuron = TmModelManager.getInstance().getCurrentSelections().getCurrentNeuron();
        TmGeoAnnotation currVertex = TmModelManager.getInstance().getCurrentSelections().getCurrentVertex();
        if (currNeuron==null || currVertex==null)
            return;

        deleteLink(currNeuron, currVertex);
    }

    public void execute(Long neuronID, Long vertexID) {
        NeuronManager manager = NeuronManager.getInstance();
        TmGeoAnnotation vertex = manager.getGeoAnnotationFromID(neuronID, vertexID);
        TmNeuronMetadata neuron = manager.getNeuronFromNeuronID(neuronID);
        deleteLink(neuron, vertex);
    }

    public void deleteLink(TmNeuronMetadata neuron, TmGeoAnnotation vertex) {
        Long neuronID = neuron.getId();
        Long annotationID = vertex.getId();

        if (TmModelManager.getInstance().getCurrentWorkspace() == null) {
            // dialog?
            return;
        }

        // verify it's a link and not a root or branch:
        NeuronManager manager = NeuronManager.getInstance();
        final TmGeoAnnotation annotation = manager.getGeoAnnotationFromID(neuronID, annotationID);
        final TmGeoAnnotation parentAnnotation = manager.getGeoAnnotationFromID(neuronID, annotation.getParentId());
        if (annotation == null) {
            FrameworkAccess.handleException(new Throwable(
                    "No annotation to delete."));
            return;
        }

        if (!TmModelManager.getInstance().checkOwnership(neuronID))
            return;

        if (annotation.isRoot() && annotation.getChildIds().size() > 0) {
            FrameworkAccess.handleException(new Throwable(
                    "This annotation is a root with children, not a link!"));
            return;
        }
        if (annotation.getChildIds().size() > 1) {
            FrameworkAccess.handleException(new Throwable(
                    "This annotation is a branch (many children), not a link!"));
            return;
        }

        SimpleWorker deleter = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                manager.deleteLink(annotation);
            }

            @Override
            protected void hadSuccess() {
                if (parentAnnotation!=null) {
                    TmModelManager.getInstance().getCurrentSelections().setCurrentVertex(parentAnnotation);
                    SelectionAnnotationEvent event = new SelectionAnnotationEvent();
                    event.setItems(Arrays.asList(new TmGeoAnnotation[]{parentAnnotation}));
                    ViewerEventBus.postEvent(event);
                }
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
            }
        };
        deleter.execute();
        }
    }
