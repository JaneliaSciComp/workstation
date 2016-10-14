package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.janelia.it.jacs.model.domain.tiledMicroscope.TmAnchoredPathEndpoints;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.it.jacs.model.user_data.tiled_microscope_builder.TmModelManipulator;
import org.janelia.it.workstation.gui.large_volume_viewer.model_adapter.DomainMgrTmModelAdapter;
import org.janelia.it.workstation.gui.browser.workers.SimpleWorker;

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
        add(new JLabel("Debug functions", JLabel.CENTER));

        JPanel buttons = new JPanel();
        add(buttons);

        buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));

        // for testing detection/repair of root ann not in ann map
        JButton testButton1 = new JButton("Test 1");
        testButton1.setAction(new AbstractAction("Lose a root") {
            @Override
            public void actionPerformed(ActionEvent e) {

                SimpleWorker worker = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {
                        // remove the first root of the selected neurite from the annotation map
                        TmNeuronMetadata tmNeuronMetadata = annotationModel.getCurrentNeuron();
                        if (tmNeuronMetadata == null) {
                            System.out.println("no selected neuron");
                            return;
                        }
                        if (tmNeuronMetadata.getRootAnnotationCount() == 0) {
                            System.out.printf("neuron has no roots");
                            return;
                        }

                        tmNeuronMetadata.getGeoAnnotationMap().remove(tmNeuronMetadata.getRootAnnotations().get(0).getId());
                        // at this point, the data should be internally INconsistent,
                        //  which is what we want
                        neuronManager.saveNeuronData(tmNeuronMetadata);
                    }

                    @Override
                    protected void hadSuccess() {
                        System.out.println("lose a root had no exceptions");
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        System.out.println("lose a root reported exception");
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
                        TmGeoAnnotation ann2 = annotationModel.getGeoAnnotationFromID(ann1.getChildIds().get(0));

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


                        annotationModel.addAnchoredPath(endpoints, pointList);
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
        buttons.add(testButton2);


        /*
        // this would have been an embedded Groovy console, but we decided not
        //  to go this way; there were inconvenient unsolved problems (below),
        //  plus Todd wasn't keen on having this in production, though he did
        //  leave the door open to having it as a module that only deployed
        //  to developers
        // main inconvenient issues found so far: (1) didn't see how to set
        //  class path for the console; (2) the variables sent into the
        //  console are frozen snapshots, and you'd really want them to
        //  stay up to date (maybe need to write some function that
        //  on-demand updates them?)
        JButton groovyButton = new JButton("Groovy");
        groovyButton.setAction(new AbstractAction("Groovy console") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Console console = new Console();

                // I want the same class path as running code, but it's
                //  not clear how; the console class doesn't have
                //  rational-looking calls for that, only calls that
                //  mimic the GUI (where the user provides info)

                // copy some variables we'll use (snapshots!  don't necessarily
                //  reflect current state of GUI!
                console.setVariable("annMgr", annotationMgr);
                console.setVariable("annModel", annotationModel);
                console.setVariable("lvvTrans", largeVolumeViewerTranslator);
                console.run();
            }
        });
        add(groovyButton);
        */

    }

}
