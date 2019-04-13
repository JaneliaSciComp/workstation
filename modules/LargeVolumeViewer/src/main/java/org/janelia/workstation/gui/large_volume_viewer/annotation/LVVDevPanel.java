package org.janelia.workstation.gui.large_volume_viewer.annotation;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.gui.large_volume_viewer.model_adapter.DomainMgrTmModelAdapter;
import org.janelia.model.access.tiledMicroscope.TmModelManipulator;
import org.janelia.model.domain.tiledMicroscope.TmAnchoredPathEndpoints;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;

/**
 * this panel is only shown to me; I use it when I need to insert
 * pieces of code for testing, etc.
 *
 * prints output to std out rather than logs; this stuff should
 * only be run by a dev who's paying attention!
 *
 * djo, 11/14
 */
@SuppressWarnings("unused")
public class LVVDevPanel extends JPanel {
    // these are useful to have around when testing:
    private AnnotationManager annotationMgr;
    private AnnotationModel annotationModel;
    private LargeVolumeViewerTranslator largeVolumeViewerTranslator;

    // 2016: new neuron persistance
    private DomainMgrTmModelAdapter modelAdapter;
    private TmModelManipulator neuronManager;

    public LVVDevPanel(AnnotationManager annotationMgr, AnnotationModel annotationModel,
                       LargeVolumeViewerTranslator largeVolumeViewerTranslator) {
        this.annotationMgr = annotationMgr;
        this.annotationModel = annotationModel;
        this.largeVolumeViewerTranslator = largeVolumeViewerTranslator;

        modelAdapter = new DomainMgrTmModelAdapter();
        neuronManager = new TmModelManipulator(modelAdapter);

        setupUI();
    }


    private void setupUI() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        add(Box.createRigidArea(new Dimension(0, 10)));
        add(new JLabel("Debug functions", JLabel.LEADING));

        JPanel buttons = new JPanel();
        add(buttons);

        buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));

        // for testing if a neuron's roots are self-consistent
        JButton testButton1 = new JButton("Test 1");
        testButton1.setAction(new AbstractAction("Check roots of selected neuron") {
            @Override
            public void actionPerformed(ActionEvent e) {

                SimpleWorker worker = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {
                        TmNeuronMetadata tmNeuronMetadata = annotationModel.getCurrentNeuron();
                        if (tmNeuronMetadata == null) {
                            System.out.println("no selected neuron");
                            return;
                        }
                        if (tmNeuronMetadata.getRootAnnotationCount() == 0) {
                            System.out.printf("neuron has no roots");
                            return;
                        }

                        // roots in the root list we maintain
                        Set<Long> listedRoots = new HashSet<Long>();
                        // annotations whose parent ID = neuron ID
                        Set<Long> parentRoots = new HashSet<Long>();

                        for (TmGeoAnnotation ann: tmNeuronMetadata.getRootAnnotations()) {
                            listedRoots.add(ann.getId());
                        }
                        Long neuronID = tmNeuronMetadata.getId();
                        for (TmGeoAnnotation ann: tmNeuronMetadata.getGeoAnnotationMap().values()) {
                            if (ann.getId().equals(neuronID)) {
                                System.out.println("annotation found with same ID as neuron!");
                            }
                            if (ann.getParentId().equals(neuronID)) {
                                parentRoots.add(ann.getId());
                            }
                        }

                        if (!listedRoots.containsAll(parentRoots)) {
                            System.out.println("some roots missing from root list");
                        }
                        if (!parentRoots.containsAll(listedRoots)) {
                            System.out.println("some listed roots don't have neuron parent");
                        }

                    }

                    @Override
                    protected void hadSuccess() {
                        System.out.println("check roots had no exceptions");
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        System.out.println("check roots reported exception");
                        error.printStackTrace();
                    }
                };
                worker.execute();

            }
        });
        buttons.add(testButton1);


        // for testing how anchored paths are drawn; for the given
        //  neuron, generate an anchored path between its first
        //  two points, all in the same plane as the first point
        JButton testButton2 = new JButton("Test 2");
        testButton2.setAction(new AbstractAction("Add flat path") {
            @Override
            public void actionPerformed(ActionEvent e) {
                final TmNeuronMetadata currentNeuron = annotationModel.getCurrentNeuron();

                SimpleWorker worker = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {
                        TmGeoAnnotation ann1 = currentNeuron.getFirstRoot();
                        TmGeoAnnotation ann2 = annotationModel.getGeoAnnotationFromID(ann1.getNeuronId(), ann1.getChildIds().get(0));

                        // I wonder if you can get away with not having any points except the endpoints?
                        TmAnchoredPathEndpoints endpoints = new TmAnchoredPathEndpoints(ann1, ann2);
                        List<List<Integer>> pointList = new ArrayList<>();
                        int nsteps = 10;
                        double dx = (ann2.getX() - ann1.getX()) / nsteps;
                        double dy = (ann2.getY() - ann1.getY()) / nsteps;
                        double x0 = ann1.getX();
                        double y0 = ann1.getY();
                        double z0 = ann1.getZ();
                        for (int i=0; i<nsteps; i++) {
                            List<Integer> point = new ArrayList<>();
                            point.add((int) Math.floor(x0 + i * dx));
                            point.add((int) Math.floor(y0 + i * dy));
                            point.add((int) Math.floor(z0));
                            pointList.add(point);
                        }
                        List<Integer> lastPoint = new ArrayList<>();
                        // ugh, Java...
                        lastPoint.add((int) (1.0 * ann2.getX()));
                        lastPoint.add((int) (1.0 * ann2.getY()));
                        lastPoint.add((int) (1.0 * ann2.getZ()));
                        pointList.add(lastPoint);


                        annotationModel.addAnchoredPath(ann1.getNeuronId(), endpoints, pointList);
                    }

                    @Override
                    protected void hadSuccess() {
                        System.out.println("add flat path had no errors");
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        System.out.println("add flat path reported exception");
                        error.printStackTrace();
                    }
                };
                worker.execute();
            }
        });
    }

}
