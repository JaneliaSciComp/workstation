package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import groovy.ui.Console;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.jacs.model.user_data.tiled_microscope_builder.TmModelManipulator;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.large_volume_viewer.model_adapter.ModelManagerTmModelAdapter;
import org.janelia.it.workstation.shared.workers.SimpleWorker;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * this panel is only shown to me; I use it when I need to insert
 * pieces of code for testing, etc.
 *
 * prints output to std out rather than logs; this stuff should
 * only be run by a dev who's paying attention!
 *
 * djo, 11/14
 */
public class LVVDevPanel extends JPanel {
    // these are useful to have around when testing:
    private AnnotationManager annotationMgr;
    private AnnotationModel annotationModel;
    private LargeVolumeViewerTranslator largeVolumeViewerTranslator;

    // 2016: new neuron persistance
    private ModelManagerTmModelAdapter modelAdapter;
    private TmModelManipulator neuronManager;

    public LVVDevPanel(AnnotationManager annotationMgr, AnnotationModel annotationModel,
                       LargeVolumeViewerTranslator largeVolumeViewerTranslator) {
        this.annotationMgr = annotationMgr;
        this.annotationModel = annotationModel;
        this.largeVolumeViewerTranslator = largeVolumeViewerTranslator;

        modelAdapter = new ModelManagerTmModelAdapter();
        neuronManager = new TmModelManipulator(modelAdapter);

        setupUI();
    }


    private void setupUI() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        add(Box.createRigidArea(new Dimension(0, 10)));
        add(new JLabel("Debug functions", JLabel.CENTER));


        // remember, can't call modelMgr from GUI thread

        // for testing detection/repair of root ann not in ann map
        JButton testButton1 = new JButton("Test 1");
        testButton1.setAction(new AbstractAction("Lose a root") {
            @Override
            public void actionPerformed(ActionEvent e) {

                SimpleWorker worker = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {
                        // remove the first root of the selected neurite from the annotation map
                        TmNeuron neuron = annotationModel.getCurrentNeuron();
                        if (neuron == null) {
                            System.out.println("no selected neuron");
                            return;
                        }
                        if (neuron.getRootAnnotationCount() == 0) {
                            System.out.printf("neuron has no roots");
                            return;
                        }

                        neuron.getGeoAnnotationMap().remove(neuron.getRootAnnotations().get(0).getId());
                        // at this point, the data should be internally INconsistent,
                        //  which is what we want
                        neuronManager.saveNeuronData(neuron);
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
        add(testButton1);



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
                console.setVariable("modelMgr", ModelMgr.getModelMgr());
                console.run();
            }
        });
        add(groovyButton);
        */

    }

}
