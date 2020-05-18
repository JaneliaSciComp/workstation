package org.janelia.workstation.gui.large_volume_viewer.annotation;

import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.gui.large_volume_viewer.ComponentUtil;
import org.janelia.workstation.gui.large_volume_viewer.action.*;
import org.janelia.workstation.gui.large_volume_viewer.controller.PanelController;
import org.janelia.workstation.gui.large_volume_viewer.controller.ViewStateListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * this is the main class for large volume viewer annotation GUI; it instantiates and contains
 * the various other panels and whatnot.
 *
 * djo, 5/13
 */
public class AnnotationPanel extends JPanel
{
    public static final int SUBPANEL_STD_HEIGHT = 150;
    
    // things we get data from
    // not clear these belong here!  should all info be shuffled through signals and actions?
    // on the other hand, even if so, we still need them just to hook everything up
    private AnnotationManager annotationMgr;
    private AnnotationModel annotationModel;
    private LargeVolumeViewerTranslator largeVolumeViewerTranslator;


    // UI components
    private FilteredAnnotationList filteredList;
    private WorkspaceInfoPanel workspaceInfoPanel;
    private WorkspaceNeuronList workspaceNeuronList;
    private ViewStateListener viewStateListener;

    // other UI stuff
    private static final int width = 250;

    private static final boolean defaultAutomaticTracing = false;
    private static final boolean defaultAutomaticRefinement = false;

    // ----- actions
    private final NeuronCreateAction createNeuronAction = new NeuronCreateAction();
    private final NeuronDeleteAction deleteNeuronAction = new NeuronDeleteAction();

    private final Action createWorkspaceAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            annotationMgr.createWorkspace();
        }
    };

    // ----- Actions
    private final Action centerAnnotationAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            viewStateListener.centerNextParent();
        }
    };
    
    private JButton createWorkspaceButtonPlus;
    private WorkspaceSaveAsAction saveAsAction;
    private JCheckBoxMenuItem automaticTracingMenuItem;
    private JCheckBoxMenuItem automaticRefinementMenuItem;
    private JCheckBoxMenuItem tempOwnerAdminItem;
    private NeuronExportAllAction exportAllSWCAction;
    private NeuronFilterAction neuronFilterAction;
    private ImportSWCAction importSWCAction;
    private ImportSWCAction importSWCActionMulti;
    private AbstractAction saveColorModelAction;

    private AbstractAction showAllNeuronsAction;
    private AbstractAction hideAllNeuronsAction;
    private AbstractAction hideOtherNeuronsAction;
    private AbstractAction bulkChangeNeuronStyleAction;
    private AbstractAction bulkNeuronTagAction;
    private AbstractAction bulkNeuronOwnerAction;
    private AbstractAction bulkExportNeuronAction;

    private LVVDebugTestDialogAction lvvDebugTestDialogAction;
    

    private JMenu sortSubmenu;


    public AnnotationPanel(AnnotationManager annotationMgr, AnnotationModel annotationModel,
        LargeVolumeViewerTranslator largeVolumeViewerTranslator) {
        this.annotationMgr = annotationMgr;
        this.annotationModel = annotationModel;
        this.largeVolumeViewerTranslator = largeVolumeViewerTranslator;

        setupUI();
        setupSignals();

        // testing; add a border so I can visualize size vs. alignment problems:
        // showOutline(this);

    }

    public AnnotationModel getAnnotationModel() {
        return annotationModel;
    }

    public void setViewStateListener(ViewStateListener listener) {
        this.viewStateListener = listener;        
    }
    
    public void loadWorkspace(TmWorkspace workspace) {
        
        if (workspace != null) {
            automaticRefinementMenuItem.setSelected(workspace.isAutoPointRefinement());
            automaticTracingMenuItem.setSelected(workspace.isAutoTracing());
        }
    
        // Disable all change functionality if the user has no write access to the workspace
        boolean enabled = annotationMgr.editsAllowed();
        automaticRefinementMenuItem.setEnabled(enabled);
        automaticTracingMenuItem.setEnabled(enabled);
        importSWCAction.setEnabled(enabled);
        importSWCActionMulti.setEnabled(enabled);
        saveColorModelAction.setEnabled(enabled);
        bulkNeuronTagAction.setEnabled(enabled);
        bulkChangeNeuronStyleAction.setEnabled(enabled);
        bulkNeuronOwnerAction.setEnabled(enabled);
        showAllNeuronsAction.setEnabled(enabled);
        hideAllNeuronsAction.setEnabled(enabled);
        sortSubmenu.setEnabled(enabled);
        // These actions override isEnabled, but they still need to be set in order to fire the right updates
        createNeuronAction.fireEnabledChangeEvent();
        deleteNeuronAction.fireEnabledChangeEvent();
        exportAllSWCAction.fireEnabledChangeEvent();
        saveAsAction.fireEnabledChangeEvent();
        
        updateUI();
    }
    
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, 0);
    }

    private void setupSignals() {
        // outgoing from the model:
        PanelController panelController = new PanelController(this,
                filteredList, workspaceNeuronList, largeVolumeViewerTranslator);
        panelController.registerForEvents(annotationModel);
        panelController.registerForEvents(annotationMgr);
        panelController.registerForEvents(workspaceInfoPanel);
    }

    private void setupUI() {
        setLayout(new GridBagLayout());

        // ----- workspace information; show name, whatever attributes
        workspaceInfoPanel = new WorkspaceInfoPanel();
        GridBagConstraints cTop = new GridBagConstraints();
        cTop.gridx = 0;
        cTop.gridy = 0;
        cTop.anchor = GridBagConstraints.PAGE_START;
        cTop.fill = GridBagConstraints.HORIZONTAL;
        cTop.insets = new Insets(10, 0, 0, 0);
        cTop.weightx = 1.0;
        cTop.weighty = 0.0;
        add(workspaceInfoPanel, cTop);

        // testing
        // showOutline(workspaceInfoPanel, Color.red);

        // I want the rest of the components to stack vertically;
        //  components should fill or align left as appropriate
        GridBagConstraints cVert = new GridBagConstraints();
        cVert.gridx = 0;
        cVert.gridy = GridBagConstraints.RELATIVE;
        cVert.anchor = GridBagConstraints.PAGE_START;
        cVert.fill = GridBagConstraints.HORIZONTAL;
        cTop.weightx = 1.0;
        cVert.weighty = 0.0;

        // buttons for doing workspace things
        JPanel workspaceButtonsPanel = new JPanel();
        workspaceButtonsPanel.setLayout(new BoxLayout(workspaceButtonsPanel, BoxLayout.LINE_AXIS));
        add(workspaceButtonsPanel, cVert);

        // testing
        // showOutline(workspaceButtonsPanel, Color.green);

        createWorkspaceButtonPlus = new JButton("+");
        workspaceButtonsPanel.add(createWorkspaceButtonPlus);
        createWorkspaceAction.putValue(Action.NAME, "+");
        createWorkspaceAction.putValue(Action.SHORT_DESCRIPTION, "Create a new workspace");
        createWorkspaceButtonPlus.setAction(createWorkspaceAction);

        // workspace tool pop-up menu (triggered by button, below)
        final JPopupMenu workspaceToolMenu = new JPopupMenu();
        
        automaticRefinementMenuItem = new JCheckBoxMenuItem("Automatic point refinement");
        automaticRefinementMenuItem.setSelected(defaultAutomaticRefinement);
        automaticRefinementMenuItem.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                annotationMgr.setAutomaticRefinement(itemEvent.getStateChange() == ItemEvent.SELECTED);
            }
        });
        workspaceToolMenu.add(automaticRefinementMenuItem);

        automaticTracingMenuItem = new JCheckBoxMenuItem("Automatic path tracing");
        automaticTracingMenuItem.setSelected(defaultAutomaticTracing);
        automaticTracingMenuItem.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                annotationMgr.setAutomaticTracing(itemEvent.getStateChange() == ItemEvent.SELECTED);
            }
        });
        workspaceToolMenu.add(automaticTracingMenuItem);

        exportAllSWCAction = new NeuronExportAllAction();
        workspaceToolMenu.add(new JMenuItem(exportAllSWCAction));

        importSWCAction = new ImportSWCAction(this, annotationModel, annotationMgr);
        importSWCAction.putValue(Action.NAME, "Import SWC file as one neuron...");
        importSWCAction.putValue(Action.SHORT_DESCRIPTION,
                "Import one or more SWC files into the workspace");
        workspaceToolMenu.add(new JMenuItem(importSWCAction));

        importSWCActionMulti = new ImportSWCAction(true, this, annotationModel, annotationMgr);
        importSWCActionMulti.putValue(Action.NAME, "Import SWC file as separate neurons...");
        importSWCActionMulti.putValue(Action.SHORT_DESCRIPTION,
                "Import one or more SWC files into the workspace");
        workspaceToolMenu.add(new JMenuItem(importSWCActionMulti));

        saveColorModelAction = new AbstractAction("Save color model") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                annotationMgr.saveQuadViewColorModel();
            }
        };
        workspaceToolMenu.add(new JMenuItem(saveColorModelAction));

        saveAsAction = new WorkspaceSaveAsAction();
        workspaceToolMenu.add(new JMenuItem(saveAsAction));

        tempOwnerAdminItem = new JCheckBoxMenuItem("Temp ownership admin");
        tempOwnerAdminItem.setSelected(false);
        tempOwnerAdminItem.addItemListener(e -> {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        // about to get temporary ownership admin: dialog warning!
                        int ans = JOptionPane.showConfirmDialog(
                                ComponentUtil.getLVVMainWindow(),
                                "You are about to give yourself permission to change any neuron's owner. Be careful!\n\nContinue?",
                                "Become owner admin?",
                                JOptionPane.OK_CANCEL_OPTION,
                                JOptionPane.WARNING_MESSAGE
                        );
                        if (ans == JOptionPane.CANCEL_OPTION) {
                            tempOwnerAdminItem.setSelected(false);
                        } else {
                            annotationMgr.setTempOwnershipAdmin(true);
                        }
                    } else {
                        // giving up admin
                        annotationMgr.setTempOwnershipAdmin(false);
                    }
                }
        );
        workspaceToolMenu.add(tempOwnerAdminItem);

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
        workspaceNeuronList = new WorkspaceNeuronList(annotationMgr, annotationModel, width);
        add(workspaceNeuronList, cVert);

        // testing
        // showOutline(workspaceNeuronList, Color.blue);

        // neuron tool pop-up menu (triggered by button, below)
        final JPopupMenu neuronToolMenu = new JPopupMenu();

        JMenuItem titleMenuItem = new JMenuItem("Operate on neurons showing above:");
        titleMenuItem.setEnabled(false);
        neuronToolMenu.add(titleMenuItem);

        showAllNeuronsAction = new AbstractAction("Show neurons") {
            @Override
            public void actionPerformed(ActionEvent e) {
                annotationMgr.setBulkNeuronVisibility(workspaceNeuronList.getNeuronList(), true);
            }
        };
        neuronToolMenu.add(showAllNeuronsAction);
        
        hideAllNeuronsAction = new AbstractAction("Hide neurons") {
            @Override
            public void actionPerformed(ActionEvent e) {
                annotationMgr.setBulkNeuronVisibility(workspaceNeuronList.getNeuronList(), false);
            }
        };
        neuronToolMenu.add(hideAllNeuronsAction);
        
        hideOtherNeuronsAction = new AbstractAction("Hide other neurons") {
            @Override
            public void actionPerformed(ActionEvent e) {
                annotationMgr.setBulkNeuronVisibility(workspaceNeuronList.getUnshownNeuronList(), false);
            }
        };
        neuronToolMenu.add(hideOtherNeuronsAction);

        bulkChangeNeuronStyleAction = new BulkChangeNeuronColorAction(annotationModel, workspaceNeuronList);
        neuronToolMenu.add(bulkChangeNeuronStyleAction);
        
        bulkNeuronTagAction = new BulkNeuronTagAction(annotationModel, workspaceNeuronList);
        neuronToolMenu.add(bulkNeuronTagAction);

        bulkNeuronOwnerAction = new BulkChangeNeuronOwnerAction(annotationMgr, annotationModel, workspaceNeuronList);
        neuronToolMenu.add(bulkNeuronOwnerAction);

        bulkExportNeuronAction = new BulkExportNeuronAction(annotationMgr, annotationModel, workspaceNeuronList);
        neuronToolMenu.add(bulkExportNeuronAction);

        neuronToolMenu.add(new JSeparator());

        neuronFilterAction = new NeuronFilterAction(annotationModel, annotationMgr);
        neuronFilterAction.putValue(Action.NAME, "Set Neuron Filter Strategy...");
        neuronFilterAction.putValue(Action.SHORT_DESCRIPTION,
                "Sets the filtering strategy for neuron fragments");
        neuronToolMenu.add(new JMenuItem(neuronFilterAction));

        neuronToolMenu.add(new WorkspaceInformationAction(annotationModel, workspaceNeuronList));

        sortSubmenu = new JMenu("Sort");
        JRadioButtonMenuItem alphaSortButton = new JRadioButtonMenuItem(new AbstractAction("Alphabetical by name") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                workspaceNeuronList.sortOrderChanged(WorkspaceNeuronList.NeuronSortOrder.ALPHABETICAL);
            }
            });
        sortSubmenu.add(alphaSortButton);
        JRadioButtonMenuItem ownerSortButton = new JRadioButtonMenuItem(new AbstractAction("Alphabetical by owner") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                workspaceNeuronList.sortOrderChanged(WorkspaceNeuronList.NeuronSortOrder.OWNER);
            }
            });
        sortSubmenu.add(ownerSortButton);
        JRadioButtonMenuItem creationSortButton = new JRadioButtonMenuItem(new AbstractAction("Creation date") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                workspaceNeuronList.sortOrderChanged(WorkspaceNeuronList.NeuronSortOrder.CREATIONDATE);
            }
        });
        sortSubmenu.add(creationSortButton);
        ButtonGroup neuronSortGroup = new ButtonGroup();
        neuronSortGroup.add(alphaSortButton);
        neuronSortGroup.add(ownerSortButton);
        neuronSortGroup.add(creationSortButton);
        neuronToolMenu.add(sortSubmenu);

        // initial sort order:
        creationSortButton.setSelected(true);

        // buttons for acting on neurons (which are in the list immediately above):
        JPanel neuronButtonsPanel = new JPanel();
        neuronButtonsPanel.setLayout(new BoxLayout(neuronButtonsPanel, BoxLayout.LINE_AXIS));
        add(neuronButtonsPanel, cVert);

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


        // ----- interesting annotations
        add(Box.createRigidArea(new Dimension(0, 20)), cVert);
        filteredList = FilteredAnnotationList.createInstance(annotationMgr, annotationModel, width);
        add(filteredList, cVert);


        // buttons for acting on annotations
        // NOTE: there's only one button and we don't really use it, so this
        //  is hidden for now (but not removed in case we want it later)
        // NOTE 2: the same functionality is still available on the right-click menu
        JPanel neuriteButtonsPanel = new JPanel();
        neuriteButtonsPanel.setLayout(new BoxLayout(neuriteButtonsPanel, BoxLayout.LINE_AXIS));
        // add(neuriteButtonsPanel, cVert);

        JButton centerAnnotationButton = new JButton("Center");
        centerAnnotationAction.putValue(Action.NAME, "Center");
        centerAnnotationAction.putValue(Action.SHORT_DESCRIPTION, "Center on current annotation [C]");
        centerAnnotationButton.setAction(centerAnnotationAction);
        String parentIconFilename = "ParentAnchor16.png";
        ImageIcon anchorIcon = Icons.getIcon(parentIconFilename);
        centerAnnotationButton.setIcon(anchorIcon);
        centerAnnotationButton.setHideActionText(true);
        neuriteButtonsPanel.add(centerAnnotationButton);


        // the bilge...
        GridBagConstraints cBottom = new GridBagConstraints();
        cBottom.gridx = 0;
        cBottom.gridy = GridBagConstraints.RELATIVE;
        cBottom.anchor = GridBagConstraints.PAGE_START;
        cBottom.fill = GridBagConstraints.BOTH;
        cTop.weightx = 1.0;
        cBottom.weighty = 1.0;
        add(Box.createVerticalGlue(), cBottom);


        // launch a dialog with some testing/debugging tools on it; show only to me (djo)
        //  for now; could widen this to all admins later
        if (AccessManager.getAccessManager().getActualSubject().getName().equals("olbrisd")) {
            JButton devButton = new JButton("Debug/test dialog...");
            lvvDebugTestDialogAction = new LVVDebugTestDialogAction();
            devButton.setAction(lvvDebugTestDialogAction);
            add(devButton, cVert);
        }
    }

//    /**
//     * add a visible border (default green) to a panel to help debug alignment/packing problems
//     */
//    private void showOutline(JPanel panel) {
//        showOutline(panel, Color.green);
//
//    }
//    
//    private void showOutline(JPanel panel, Color color) {
//        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(color), getBorder()));
//    }
}

