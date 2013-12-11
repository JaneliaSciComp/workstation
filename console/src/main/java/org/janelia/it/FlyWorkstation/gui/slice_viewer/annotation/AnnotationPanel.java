package org.janelia.it.FlyWorkstation.gui.slice_viewer.annotation;


// std lib imports

import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.signal.Signal;
import org.janelia.it.FlyWorkstation.signal.Slot1;
import org.janelia.it.FlyWorkstation.tracing.PathTraceRequest;
import org.janelia.it.FlyWorkstation.tracing.TracedPathSegment;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmAnchoredPathEndpoints;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * this is the main class for slice viewer annotation GUI; it instantiates and contains
 * the various other panels and whatnot.
 *
 * djo, 5/13
 */
public class AnnotationPanel extends JPanel
{
    // things we get data from
    // not clear these belong here!  should all info be shuffled through signals and actions?
    // on the other hand, even if so, we still need them just to hook everything up
    private AnnotationManager annotationMgr;
    private AnnotationModel annotationModel;
    private SliceViewerTranslator sliceViewerTranslator;


    // UI components
    private NeuronInfoPanel neuronInfoPanel;
    private WorkspaceInfoPanel workspaceInfoPanel;
    private WorkspaceNeuronList workspaceNeuronList;
    private PathTracingStatusPanel pathStatusPanel;

    // ----- actions
    private final Action createNeuronAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            annotationMgr.createNeuron();
            }
        };

    private final Action deleteNeuronAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            annotationMgr.deleteCurrentNeuron();
        }
    };

    private final Action createWorkspaceAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            annotationMgr.createWorkspace();
            }
        };

    // ----- signals & slots
    public Signal centerAnnotationSignal = new Signal();
    private final Action centerAnnotationAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            centerAnnotationSignal.emit();
        }
    };

    public Slot1<PathTraceRequest> tracingStartSlot = new Slot1<PathTraceRequest>() {
        @Override
        public void execute(PathTraceRequest request) {
            pathStatusPanel.startTracing(new TmAnchoredPathEndpoints(request.getAnchor1Guid(),
                    request.getAnchor2Guid()));
        }
    };

    public Slot1<TracedPathSegment> tracingStopSlot = new Slot1<TracedPathSegment>() {
        @Override
        public void execute(TracedPathSegment segment) {
            pathStatusPanel.stopTracing(new TmAnchoredPathEndpoints(segment.getRequest().getAnchor1Guid(),
                    segment.getRequest().getAnchor2Guid()));
        }
    };

    public AnnotationPanel(AnnotationManager annotationMgr, AnnotationModel annotationModel,
        SliceViewerTranslator sliceViewerTranslator) {
        this.annotationMgr = annotationMgr;
        this.annotationModel = annotationModel;
        this.sliceViewerTranslator = sliceViewerTranslator;

        setupUI();
        setupSignals();
    }

    @Override
    public Dimension getPreferredSize() {
        // since we create components without data, they tend to start too narrow
        //  for what they will eventually need, and that causes the split pane not
        //  to size right; so, give it a hint   
        return new Dimension(200, 0);
    }

    private void setupSignals() {
        // outgoing from the model:
        annotationModel.neuronSelectedSignal.connect(neuronInfoPanel.neuronSelectedSlot);
        annotationModel.neuronSelectedSignal.connect(workspaceNeuronList.neuronSelectedSlot);

        annotationModel.workspaceLoadedSignal.connect(workspaceInfoPanel.workspaceLoadedSlot);
        annotationModel.workspaceLoadedSignal.connect(workspaceNeuronList.workspaceLoadedSlot);

        // us to model:
        workspaceNeuronList.neuronClickedSignal.connect(annotationModel.neuronClickedSlot);

        // us to graphics UI
        neuronInfoPanel.cameraPanToSignal.connect(sliceViewerTranslator.cameraPanToSlot);
        neuronInfoPanel.annotationClickedSignal.connect(sliceViewerTranslator.annotationClickedSlot);
        workspaceNeuronList.cameraPanToSignal.connect(sliceViewerTranslator.cameraPanToSlot);

    }

    private void setupUI() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // add a little breathing space at the top of the panel
        add(Box.createRigidArea(new Dimension(0, 20)));

        // ----- workspace information; show name, whatever attributes
        workspaceInfoPanel = new WorkspaceInfoPanel();
        add(workspaceInfoPanel);

        // buttons for doing workspace things
        JPanel workspaceButtonsPanel = new JPanel();
        workspaceButtonsPanel.setLayout(new BoxLayout(workspaceButtonsPanel, BoxLayout.LINE_AXIS));
        add(workspaceButtonsPanel);

        JButton createWorkspaceButtonPlus = new JButton("+");
        workspaceButtonsPanel.add(createWorkspaceButtonPlus);
        createWorkspaceAction.putValue(Action.NAME, "+");
        createWorkspaceAction.putValue(Action.SHORT_DESCRIPTION, "Create a new workspace");
        createWorkspaceButtonPlus.setAction(createWorkspaceAction);

        // workspace tool pop-up menu (triggered by button, below)
        final JPopupMenu workspaceToolMenu = new JPopupMenu();

        ChooseAnnotationColorAction changeGlobalAnnotationColorAction = new ChooseAnnotationColorAction();
        changeGlobalAnnotationColorAction.putValue(Action.NAME, "Set global annotation color...");
        changeGlobalAnnotationColorAction.putValue(Action.SHORT_DESCRIPTION,
                "Change global color of annotations");
        workspaceToolMenu.add(new JMenuItem(changeGlobalAnnotationColorAction));

        // workspace tool menu button
        final JButton workspaceToolButton = new JButton();
        String gearIconFilename = "cog.png";
        ImageIcon gearIcon = Icons.getIcon(gearIconFilename);
        workspaceToolButton.setIcon(gearIcon);
        workspaceToolButton.setHideActionText(true);
        workspaceToolButton.setMinimumSize(workspaceButtonsPanel.getPreferredSize());
        workspaceButtonsPanel.add(workspaceToolButton);
        workspaceToolButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                workspaceToolMenu.show(workspaceToolButton,
                        workspaceToolButton.getBounds().x - workspaceToolButton.getBounds().width,
                        workspaceToolButton.getBounds().y + workspaceToolButton.getBounds().height);
            }
        });


        // list of neurons in workspace
        workspaceNeuronList = new WorkspaceNeuronList();
        add(workspaceNeuronList);

        // neuron tool pop-up menu (triggered by button, below)
        final JPopupMenu neuronToolMenu = new JPopupMenu();
        neuronToolMenu.add(new JMenuItem(new AbstractAction("Rename") {
            public void actionPerformed(ActionEvent e) {
                annotationMgr.renameNeuron();
            }
        }));
        // neuron sort submenu
        JMenu sortSubmenu = new JMenu("Sort");
        JRadioButtonMenuItem alphaSortButton = new JRadioButtonMenuItem(new AbstractAction("Alphabetical") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                workspaceNeuronList.sortOrderChanged(WorkspaceNeuronList.NeuronSortOrder.ALPHABETICAL);
            }
            });
        sortSubmenu.add(alphaSortButton);
        JRadioButtonMenuItem creationSortButton = new JRadioButtonMenuItem(new AbstractAction("Creation date") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                workspaceNeuronList.sortOrderChanged(WorkspaceNeuronList.NeuronSortOrder.CREATIONDATE);
            }
        });
        sortSubmenu.add(creationSortButton);
        ButtonGroup neuronSortGroup = new ButtonGroup();
        neuronSortGroup.add(alphaSortButton);
        neuronSortGroup.add(creationSortButton);
        neuronToolMenu.add(sortSubmenu);

        // initial sort order:
        creationSortButton.setSelected(true);
        workspaceNeuronList.sortOrderChanged(WorkspaceNeuronList.NeuronSortOrder.CREATIONDATE);

        // buttons for acting on neurons (which are in the list immediately above):
        JPanel neuronButtonsPanel = new JPanel();
        neuronButtonsPanel.setLayout(new BoxLayout(neuronButtonsPanel, BoxLayout.LINE_AXIS));
        add(neuronButtonsPanel);

        JButton createNeuronButtonPlus = new JButton("+");
        neuronButtonsPanel.add(createNeuronButtonPlus);
        createNeuronAction.putValue(Action.NAME, "+");
        createNeuronAction.putValue(Action.SHORT_DESCRIPTION, "Create a new neuron");
        createNeuronButtonPlus.setAction(createNeuronAction);

        JButton deleteNeuronButton = new JButton("-");
        neuronButtonsPanel.add(deleteNeuronButton);
        deleteNeuronAction.putValue(Action.NAME, "-");
        deleteNeuronAction.putValue(Action.SHORT_DESCRIPTION, "Delete current neuron");
        deleteNeuronButton.setAction(deleteNeuronAction);

        // this button pops up the tool menu
        final JButton neuronToolButton = new JButton();
        // we load the gear icon above
        // String gearIconFilename = "cog.png";
        // ImageIcon gearIcon = Icons.getIcon(gearIconFilename);
        neuronToolButton.setIcon(gearIcon);
        neuronToolButton.setHideActionText(true);
        neuronToolButton.setMinimumSize(neuronButtonsPanel.getPreferredSize());
        neuronButtonsPanel.add(neuronToolButton);
        neuronToolButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                neuronToolMenu.show(neuronToolButton,
                        neuronToolButton.getBounds().x - neuronToolButton.getBounds().width,
                        neuronToolButton.getBounds().y + neuronToolButton.getBounds().height);
            }
        });




        // ----- neuron information; show name, whatever attributes, list of neurites
        add(Box.createRigidArea(new Dimension(0, 20)));
        neuronInfoPanel = new NeuronInfoPanel();
        add(neuronInfoPanel);

        // buttons for acting on annotations or neurites (which are in the list immediately above):
        JPanel neuriteButtonsPanel = new JPanel();
        neuriteButtonsPanel.setLayout(new BoxLayout(neuriteButtonsPanel, BoxLayout.LINE_AXIS));
        add(neuriteButtonsPanel);

        JButton centerAnnotationButton = new JButton("Center");
        centerAnnotationAction.putValue(Action.NAME, "Center");
        centerAnnotationAction.putValue(Action.SHORT_DESCRIPTION, "Center on current annotation [C]");
        centerAnnotationButton.setAction(centerAnnotationAction);
        String parentIconFilename = "ParentAnchor16.png";
        ImageIcon anchorIcon = Icons.getIcon(parentIconFilename);
        centerAnnotationButton.setIcon(anchorIcon);
        centerAnnotationButton.setHideActionText(true);
        neuriteButtonsPanel.add(centerAnnotationButton);


        // ----- temporary tracing indicator
        // this will eventually be shown via styling of lines in 2D view
        pathStatusPanel = new PathTracingStatusPanel();
        add(pathStatusPanel);


        // the bilge...
        add(Box.createVerticalGlue());
    }

    class ChooseAnnotationColorAction extends AbstractAction {
        // adapted from ChannelColorAction in ColorChannelWidget

        JDialog colorDialog;
        JColorChooser colorChooser;
        private Color currentColor;

        public ChooseAnnotationColorAction() {

            ActionListener okListener = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    annotationMgr.setGlobalAnnotationColor(colorChooser.getColor());
                }
            };
            ActionListener cancelListener = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    // unused right now
                }
            };

            // arbitrary initial color
            colorChooser = new JColorChooser(Color.RED);

            colorDialog = JColorChooser.createDialog(AnnotationPanel.this,
                    "Set global annotation color",
                    false,
                    colorChooser,
                    okListener,
                    cancelListener);
            colorDialog.setVisible(false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (annotationModel.getCurrentWorkspace() == null) {
                return;
            }

            // I'd like to grab the current color as the initial color,
            //  but annotation panel has no way to get it at this time
            colorChooser.setColor(AnnotationsConstants.DEFAULT_ANNOTATION_COLOR_GLOBAL);
            colorDialog.setVisible(true);
        }
    }

}

