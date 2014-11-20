package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import groovy.ui.Console;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.shared.workers.SimpleWorker;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * this panel is only shown to me; I use it when I need to insert
 * pieces of code for testing, etc.
 *
 * djo, 11/14
 */
public class LVVDevPanel extends JPanel {
    // these are useful to have around when testing:
    private AnnotationManager annotationMgr;
    private AnnotationModel annotationModel;
    private LargeVolumeViewerTranslator largeVolumeViewerTranslator;


    public LVVDevPanel(AnnotationManager annotationMgr, AnnotationModel annotationModel,
                       LargeVolumeViewerTranslator largeVolumeViewerTranslator) {
        this.annotationMgr = annotationMgr;
        this.annotationModel = annotationModel;
        this.largeVolumeViewerTranslator = largeVolumeViewerTranslator;

        setupUI();
    }


    private void setupUI() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        add(Box.createRigidArea(new Dimension(0, 10)));
        add(new JLabel("Debug functions", JLabel.CENTER));


        // NOTE: this doesn't work! you apparently can't call modelMgr
        //  methods from the GUI thread, where this is; I'd have to call
        //  into some dummy function in annModel, or maybe even a new
        //  testAnnModel class, and do it in a thread; not sure I want
        //  to do all that, which kind of defeats the purpose of this
        //  whole dev panel!  there are days when Java just repeatedly
        //  gets in the way of everything I want to do, ugh

        // this is for testing the "fix connectivity" feature; it
        //  grabs the current neuron and deletes one of its geo annotations
        JButton testButton1 = new JButton("Test 1");
        testButton1.setAction(new AbstractAction("Break connectivity") {
            @Override
            public void actionPerformed(ActionEvent e) {

                SimpleWorker worker = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {
                        // since this is for debugging, it can be kind of sloppy;
                        //  delete second annotation in first neurite; that's the
                        //  first child of the root
                        TmNeuron neuron = annotationModel.getCurrentNeuron();
                        if (neuron.getRootAnnotations().size() == 0) {
                            System.out.println("no annotations");
                            return;
                        }
                        TmGeoAnnotation root = neuron.getRootAnnotations().get(0);
                        if (root.getChildIds().size() == 0) {
                            System.out.println("root has no children");
                            return;
                        }
                        Long childID = root.getChildIds().get(0);

                        // now we do surgery--horrific, brutal surgery that is an
                        //  offense against man and God and the ethics board; I
                        //  hope Entities and EntityDatas feel no pain...
                        ModelMgr modelMgr = ModelMgr.getModelMgr();
                            Entity neuronEntity = modelMgr.getEntityById(neuron.getId());
                            for (EntityData ed: neuronEntity.getEntityData()) {
                                if (ed.getId().equals(childID)) {
                                    modelMgr.removeEntityData(ed);
                                }
                            }
                    }

                    @Override
                    protected void hadSuccess() {
                        // blah, blah
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        System.out.println("connectivity breaking failed!");
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
