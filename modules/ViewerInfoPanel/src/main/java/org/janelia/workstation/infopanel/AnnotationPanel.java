package org.janelia.workstation.infopanel;

import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.ComponentUtil;
import org.janelia.workstation.controller.TmViewerManager;
import org.janelia.workstation.controller.ViewerEventBus;
import org.janelia.workstation.controller.action.*;
import org.janelia.workstation.controller.eventbus.*;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.controller.scripts.spatialfilter.NeuronFilterAction;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.infopanel.action.*;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;

/**
 * this is the main class for large volume viewer annotation GUI; it instantiates and contains
 * the various other panels and whatnot.
 *
 * djo, 5/13
 */
public class AnnotationPanel extends JPanel
{
    public static final int SUBPANEL_STD_HEIGHT = 150;
    private NeuronManager neuronManager;
    private TmModelManager annotationModel;

    // UI components
    private JScrollPane scrollPane;
    private JPanel mainPanel;

    private FilteredAnnotationList filteredList;
    private WorkspaceInfoPanel workspaceInfoPanel;
    private WorkspaceNeuronList workspaceNeuronList;

    // other UI stuff
    private static final int width = 250;

    private static final boolean defaultAutomaticTracing = false;
    private static final boolean defaultAutomaticRefinement = false;

    // ----- actions
    private final NeuronCreateAction createNeuronAction = new NeuronCreateAction();
    private final NeuronDeleteAction deleteNeuronAction = new NeuronDeleteAction();

    private final Action createWorkspaceAction = new CreateWorkspaceAction();

    // ----- actions
    private final Action centerAnnotationAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            //viewStateListener.centerNextParent();
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

    private AbstractAction showAllNeuronsAction;
    private AbstractAction hideAllNeuronsAction;
    private AbstractAction hideOtherNeuronsAction;
    private AbstractAction bulkChangeNeuronStyleAction;
    private AbstractAction bulkNeuronTagAction;
    private AbstractAction bulkNeuronOwnerAction;
    private AbstractAction bulkExportNeuronAction;

    private JCheckBox openHorta;
    private JCheckBox openLVV;
    private JCheckBox openNeuronCam;
    PanelController panelController;
    

    private JMenu sortSubmenu;


    public AnnotationPanel() {
        this.annotationModel = TmModelManager.getInstance();
        this.neuronManager = NeuronManager.getInstance();

        setupUI();
        panelController = new PanelController(this,
                      filteredList, workspaceNeuronList, workspaceInfoPanel);

    }

    public TmModelManager getAnnotationModel() {
        return annotationModel;
    }

    
    public void loadWorkspace(TmWorkspace workspace) {
        if (workspace != null) {
            automaticRefinementMenuItem.setSelected(workspace.isAutoPointRefinement());
            automaticTracingMenuItem.setSelected(workspace.isAutoTracing());
        }
    
        // Disable all change functionality if the user has no write access to the workspace
        boolean enabled = TmViewerManager.getInstance().editsAllowed() && workspace!=null;

        automaticRefinementMenuItem.setEnabled(enabled);
        automaticTracingMenuItem.setEnabled(enabled);
        importSWCAction.setEnabled(enabled);
        importSWCActionMulti.setEnabled(enabled);
        bulkNeuronTagAction.setEnabled(enabled);
        bulkChangeNeuronStyleAction.setEnabled(enabled);
        bulkNeuronOwnerAction.setEnabled(enabled);
        showAllNeuronsAction.setEnabled(enabled);
        hideAllNeuronsAction.setEnabled(enabled);
        sortSubmenu.setEnabled(enabled);

        saveAsAction.fireEnabledChangeEvent();

        updateUI();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, 0);
    }

    private void setupUI() {
        // put a scroll pane around the whole thing, because we've had problems with
        //  some people with small screens not seeing all the controls, especially the
        //  "new workspace" button
        mainPanel = new JPanel();
        scrollPane = new JScrollPane(mainPanel);
        setLayout(new BorderLayout());
        add(scrollPane);

        mainPanel.setLayout(new GridBagLayout());

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
        mainPanel.add(workspaceInfoPanel, cTop);

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
        mainPanel.add(workspaceButtonsPanel, cVert);
        JPanel workspaceButtonsPanel2 = new JPanel();
        workspaceButtonsPanel2.setLayout(new BoxLayout(workspaceButtonsPanel2, BoxLayout.LINE_AXIS));
        mainPanel.add(workspaceButtonsPanel2, cVert);

        // testingFil
        // showOutline(workspaceButtonsPanel, Color.green);

        openLVV = new JCheckBox("Open 2D");
        workspaceButtonsPanel.add(openLVV);
        openLVV.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TopComponent tc = WindowManager.getDefault().findTopComponent("LargeVolumeViewerTopComponent");
                if (tc != null) {
                    if (!tc.isOpened()) {
                        tc.open();
                    } else {
                        tc.close();
                    }
                    tc.requestActive();
                }
            }
        });

        openHorta = new JCheckBox("Open 3D");
        workspaceButtonsPanel.add(openHorta);
        openHorta.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TopComponent tc = WindowManager.getDefault().findTopComponent("NeuronTracerTopComponent");
                if (tc != null) {
                    if (!tc.isOpened()) {
                        tc.open();
                    } else {
                        tc.close();
                    }
                    tc.requestActive();
                }
            }
        });

        openNeuronCam = new JCheckBox("Open Proofreader");
        workspaceButtonsPanel.add(openNeuronCam);
        openNeuronCam.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TopComponent tc = WindowManager.getDefault().findTopComponent("TaskWorkflowViewTopComponent");
                if (tc != null) {
                    if (!tc.isOpened()) {
                        tc.open();
                    } else {
                        tc.close();
                    }
                    tc.requestActive();
                }
            }
        });

        createWorkspaceButtonPlus = new JButton("+");
        workspaceButtonsPanel2.add(createWorkspaceButtonPlus);
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
                try {
                    TmWorkspace workspace = TmModelManager.getInstance().getCurrentWorkspace();
                    workspace.setAutoPointRefinement(itemEvent.getStateChange() == ItemEvent.SELECTED);
                    TmModelManager.getInstance().saveWorkspace(workspace);
                }
                catch(Exception e) {
                    FrameworkAccess.handleException(e);
                }
            }
        });
        workspaceToolMenu.add(automaticRefinementMenuItem);

        automaticTracingMenuItem = new JCheckBoxMenuItem("Automatic path tracing");
        automaticTracingMenuItem.setSelected(defaultAutomaticTracing);
        automaticTracingMenuItem.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                try {
                    TmWorkspace workspace = TmModelManager.getInstance().getCurrentWorkspace();
                    if (workspace != null) {
                        workspace.setAutoTracing(itemEvent.getStateChange() == ItemEvent.SELECTED);
                        TmModelManager.getInstance().saveWorkspace(workspace);
                    }
                }
                catch(Exception e) {
                    FrameworkAccess.handleException(e);
                }
            }
        });
        workspaceToolMenu.add(automaticTracingMenuItem);

        exportAllSWCAction = new NeuronExportAllAction();
        workspaceToolMenu.add(new JMenuItem(exportAllSWCAction));

        importSWCAction = new ImportSWCAction(neuronManager,annotationModel);
        importSWCAction.putValue(Action.NAME, "Import SWC file as one neuron...");
        importSWCAction.putValue(Action.SHORT_DESCRIPTION,
                "Import one or more SWC files into the workspace");
        workspaceToolMenu.add(new JMenuItem(importSWCAction));

        importSWCActionMulti = new ImportSWCAction(true, neuronManager, annotationModel);
        importSWCActionMulti.putValue(Action.NAME, "Import SWC file as separate neurons...");
        importSWCActionMulti.putValue(Action.SHORT_DESCRIPTION,
                "Import one or more SWC files into the workspace");
        workspaceToolMenu.add(new JMenuItem(importSWCActionMulti));

        saveAsAction = new WorkspaceSaveAsAction();
        workspaceToolMenu.add(new JMenuItem(saveAsAction));

        tempOwnerAdminItem = new JCheckBoxMenuItem("Temp ownership admin");
        tempOwnerAdminItem.setSelected(false);
        tempOwnerAdminItem.addItemListener(e -> {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        // about to get temporary ownership admin: dialog warning!
                        int ans = JOptionPane.showConfirmDialog(
                                ComponentUtil.getMainWindow(),
                                "You are about to give yourself permission to change any neuron's owner. Be careful!\n\nContinue?",
                                "Become owner admin?",
                                JOptionPane.OK_CANCEL_OPTION,
                                JOptionPane.WARNING_MESSAGE
                        );
                        if (ans == JOptionPane.CANCEL_OPTION) {
                            tempOwnerAdminItem.setSelected(false);
                        } else {
                            TmViewerManager.getInstance().setTempOwnershipAdmin(true);
                        }
                    } else {
                        // giving up admin
                        TmViewerManager.getInstance().setTempOwnershipAdmin(false);
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
        workspaceButtonsPanel2.add(workspaceToolButton);
        workspaceToolButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                workspaceToolMenu.show(workspaceToolButton,
                        workspaceToolButton.getBounds().x - workspaceToolButton.getBounds().width,
                        workspaceToolButton.getBounds().y + workspaceToolButton.getBounds().height);
            }
        });


        // list of neurons in workspace
        workspaceNeuronList = new WorkspaceNeuronList(width);
        mainPanel.add(workspaceNeuronList, cVert);

        // neuron tool pop-up menu (triggered by button, below)
        final JPopupMenu neuronToolMenu = new JPopupMenu();

        JMenuItem titleMenuItem = new JMenuItem("Operate on neurons showing above:");
        titleMenuItem.setEnabled(false);
        neuronToolMenu.add(titleMenuItem);

        showAllNeuronsAction = new AbstractAction("Show neurons") {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (TmNeuronMetadata neuron: workspaceNeuronList.getNeuronList()) {
                    TmModelManager.getInstance().getCurrentView().removeAnnotationFromHidden(neuron.getId());
                }
                NeuronUpdateEvent event = new NeuronUpdateEvent(this,
                        workspaceNeuronList.getNeuronList());
                ViewerEventBus.postEvent(event);
            }

        };
        neuronToolMenu.add(showAllNeuronsAction);
        
        hideAllNeuronsAction = new AbstractAction("Hide neurons") {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (TmNeuronMetadata neuron: workspaceNeuronList.getNeuronList()) {
                    TmModelManager.getInstance().getCurrentView().addAnnotationToHidden(neuron.getId());
                }
                NeuronUpdateEvent event = new NeuronUpdateEvent(this,
                        workspaceNeuronList.getNeuronList());
                ViewerEventBus.postEvent(event);
            }
        };
        neuronToolMenu.add(hideAllNeuronsAction);
        
        hideOtherNeuronsAction = new AbstractAction("Hide other neurons") {
            @Override
            public void actionPerformed(ActionEvent e) {
                java.util.List<TmNeuronMetadata> hiddenNeurons = new ArrayList<>();
                for (TmNeuronMetadata neuron: workspaceNeuronList.getUnshownNeuronList()) {
                    TmModelManager.getInstance().getCurrentView().addAnnotationToHidden(neuron.getId());
                    hiddenNeurons.add(neuron);
                }
                NeuronUpdateEvent event = new NeuronUpdateEvent(this,
                        hiddenNeurons);
                ViewerEventBus.postEvent(event);
            }
        };
        neuronToolMenu.add(hideOtherNeuronsAction);

        bulkChangeNeuronStyleAction = new BulkChangeNeuronColorAction(neuronManager, workspaceNeuronList);
        neuronToolMenu.add(bulkChangeNeuronStyleAction);
        
        bulkNeuronTagAction = new BulkNeuronTagAction(neuronManager, workspaceNeuronList);
        neuronToolMenu.add(bulkNeuronTagAction);

        bulkNeuronOwnerAction = new BulkChangeNeuronOwnerAction(neuronManager, workspaceNeuronList);
        neuronToolMenu.add(bulkNeuronOwnerAction);

        bulkExportNeuronAction = new BulkExportNeuronAction(neuronManager, workspaceNeuronList);
        neuronToolMenu.add(bulkExportNeuronAction);

        neuronToolMenu.add(new JSeparator());

        neuronFilterAction = new NeuronFilterAction(neuronManager);
        neuronFilterAction.putValue(Action.NAME, "Set Neuron Filter Strategy...");
        neuronFilterAction.putValue(Action.SHORT_DESCRIPTION,
                "Sets the filtering strategy for neuron fragments");
        neuronToolMenu.add(new JMenuItem(neuronFilterAction));

        neuronToolMenu.add(new WorkspaceInformationAction(annotationModel, workspaceNeuronList));

        if (AccessManager.getAccessManager().isAdmin()) {
            neuronToolMenu.add(new MessageSnooperAction());
        }

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
        mainPanel.add(neuronButtonsPanel, cVert);

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
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)), cVert);
        filteredList = FilteredAnnotationList.createInstance(neuronManager, width);
        mainPanel.add(filteredList, cVert);


        // buttons for acting on annotations
        // NOTE: there's only one button and we don't really use it, so this
        //  is hidden for now (but not removed in case we want it later)
        // NOTE 2: the same functionality is still available on the right-click menu
        JPanel neuriteButtonsPanel = new JPanel();
        neuriteButtonsPanel.setLayout(new BoxLayout(neuriteButtonsPanel, BoxLayout.LINE_AXIS));
        // mainPanel.add(neuriteButtonsPanel, cVert);

        JButton centerAnnotationButton = new JButton("Center");
        centerAnnotationAction.putValue(Action.NAME, "Center");
        centerAnnotationAction.putValue(Action.SHORT_DESCRIPTION, "Center on current annotation [C]");
        centerAnnotationButton.setAction(centerAnnotationAction);
        String parentIconFilename = "ParentAnchor16.png";
        ImageIcon anchorIcon = Icons.getIcon(parentIconFilename);
        centerAnnotationButton.setIcon(anchorIcon);
        centerAnnotationButton.setHideActionText(true);
        neuriteButtonsPanel.add(centerAnnotationButton);


        // developer panel, only shown to me; used for various testing things
        /*if (AccessManager.getAccessManager().getActualSubject().getName().equals("olbrisd")) {
            //lvvDevPanel = new LVVDevPanel(annotationMgr, annotationModel, largeVolumeViewerTranslator);
            //mainPanel.add(lvvDevPanel, cVert);
        }*/


        // the bilge...
        GridBagConstraints cBottom = new GridBagConstraints();
        cBottom.gridx = 0;
        cBottom.gridy = GridBagConstraints.RELATIVE;
        cBottom.anchor = GridBagConstraints.PAGE_START;
        cBottom.fill = GridBagConstraints.BOTH;
        cTop.weightx = 1.0;
        cBottom.weighty = 1.0;
        mainPanel.add(Box.createVerticalGlue(), cBottom);
    }

    public void viewerClosed(ViewerCloseEvent event) {
        switch (event.getViewer()) {
            case HORTA:
                openHorta.setSelected(false);
                break;
            case LVV:
                openLVV.setSelected(false);
                break;
        }
    }

    public void viewerOpened(ViewerOpenEvent event) {
        switch (event.getViewer()) {
            case HORTA:
                openHorta.setSelected(true);
                break;
            case LVV:
                openLVV.setSelected(true);
                break;
        }
    }


}

