package org.janelia.it.workstation.gui.large_volume_viewer.annotation;


// std lib imports

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmWorkspace;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;

import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.PanelController;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.ViewStateListener;


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
    private JCheckBoxMenuItem automaticTracingMenuItem;
    private JCheckBoxMenuItem automaticRefinementMenuItem;
    private ViewStateListener viewStateListener;
    private LVVDevPanel lvvDevPanel;

    // other UI stuff
    private static final int width = 250;

    private static final boolean defaultAutomaticTracing = false;
    private static final boolean defaultAutomaticRefinement = false;

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

    // ----- Actions
    private final Action centerAnnotationAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            viewStateListener.centerNextParent();
        }
    };

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

    public void setViewStateListener(ViewStateListener listener) {
        this.viewStateListener = listener;        
    }
    
    public void loadWorkspace(TmWorkspace workspace) {
        if (workspace != null) {
            boolean state;
            String automaticRefinementPref = workspace.getPreferences().getProperty(AnnotationsConstants.PREF_AUTOMATIC_POINT_REFINEMENT);
            if (automaticRefinementPref != null) {
                state = Boolean.parseBoolean(automaticRefinementPref);
            } else {
                state = false;
            }
            automaticRefinementMenuItem.setSelected(state);
            String automaticTracingPref = workspace.getPreferences().getProperty(AnnotationsConstants.PREF_AUTOMATIC_TRACING);
            if (automaticTracingPref != null) {
                state = Boolean.parseBoolean(automaticTracingPref);
            } else {
                state = false;
            }
            automaticTracingMenuItem.setSelected(state);
        }
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
        cVert.weighty = 0.0;

        // buttons for doing workspace things
        JPanel workspaceButtonsPanel = new JPanel();
        workspaceButtonsPanel.setLayout(new BoxLayout(workspaceButtonsPanel, BoxLayout.LINE_AXIS));
        add(workspaceButtonsPanel, cVert);

        // testing
        // showOutline(workspaceButtonsPanel, Color.green);

        JButton createWorkspaceButtonPlus = new JButton("+");
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

        ExportAllSWCAction exportAllSWCAction = new ExportAllSWCAction();
        exportAllSWCAction.putValue(Action.NAME, "Export SWC file...");
        exportAllSWCAction.putValue(Action.SHORT_DESCRIPTION,
                "Export all neurons to SWC file");
        workspaceToolMenu.add(new JMenuItem(exportAllSWCAction));

        ImportSWCAction importSWCAction = new ImportSWCAction();
        importSWCAction.putValue(Action.NAME, "Import SWC Data as Single Neuron...");
        importSWCAction.putValue(Action.SHORT_DESCRIPTION,
                "Import one or more SWC files into the workspace");
        workspaceToolMenu.add(new JMenuItem(importSWCAction));

        ImportSWCAction importSWCActionMulti = new ImportSWCAction(true);
        importSWCActionMulti.putValue(Action.NAME, "Import SWC Data as Neuron-per-Root...");
        importSWCActionMulti.putValue(Action.SHORT_DESCRIPTION,
                "Import one or more SWC files into the workspace");
        workspaceToolMenu.add(new JMenuItem(importSWCActionMulti));

        workspaceToolMenu.add(new JMenuItem(new AbstractAction("Save color model") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                annotationMgr.saveColorModel();
            }
        }));

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
        neuronToolMenu.add(new AbstractAction("Choose neuron style...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                annotationMgr.chooseNeuronStyle();
            }
        });
        neuronToolMenu.add(new AbstractAction("Show neuron") {
            @Override
            public void actionPerformed(ActionEvent e) {
                annotationMgr.setNeuronVisibility(true);
            }
        });
        neuronToolMenu.add(new AbstractAction("Hide neuron") {
            @Override
            public void actionPerformed(ActionEvent e) {
                annotationMgr.setNeuronVisibility(false);
            }
        });
        neuronToolMenu.add(new AbstractAction("Show all neurons") {
            @Override
            public void actionPerformed(ActionEvent e) {
                annotationMgr.setAllNeuronVisibility(true);
            }
        });
        neuronToolMenu.add(new AbstractAction("Hide all neurons") {
            @Override
            public void actionPerformed(ActionEvent e) {
                annotationMgr.setAllNeuronVisibility(false);
            }
        });
        ExportCurrentSWCAction exportCurrentSWCAction = new ExportCurrentSWCAction();
        exportCurrentSWCAction.putValue(Action.NAME, "Export SWC file...");
        exportCurrentSWCAction.putValue(Action.SHORT_DESCRIPTION,
                "Export selected neuron as an SWC file");
        neuronToolMenu.add(exportCurrentSWCAction);
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
        filteredList = new FilteredAnnotationList(annotationMgr, annotationModel, width);
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


        // developer panel, only shown to me; used for various testing things
        if (SessionMgr.getSessionMgr().getSubject().getName().equals("olbrisd")) {
            lvvDevPanel = new LVVDevPanel(annotationMgr, annotationModel, largeVolumeViewerTranslator);
            add(lvvDevPanel, cVert);
        }


        // the bilge...
        GridBagConstraints cBottom = new GridBagConstraints();
        cBottom.gridx = 0;
        cBottom.gridy = GridBagConstraints.RELATIVE;
        cBottom.anchor = GridBagConstraints.PAGE_START;
        cBottom.fill = GridBagConstraints.BOTH;
        cBottom.weighty = 1.0;
        add(Box.createVerticalGlue(), cBottom);
    }

    /**
     * add a visible border (default green) to a panel to help debug alignment/packing problems
     */
    private void showOutline(JPanel panel) {
        showOutline(panel, Color.green);

    }

    /** Somewhat complex interaction with file chooser. */
    private ExportParameters getExportParameters( String seedName ) throws HeadlessException {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save swc file");
        chooser.setSelectedFile(new File(seedName + AnnotationModel.STD_SWC_EXTENSION));
        JPanel layoutPanel = new JPanel();
        layoutPanel.setLayout(new BorderLayout());
        // Force-out to desired size.
        JTextField downsampleModuloField = new JTextField("10");
        final Dimension dimension = new Dimension(80, 40);
        downsampleModuloField.setMinimumSize( dimension );
        downsampleModuloField.setSize( dimension );
        downsampleModuloField.setPreferredSize( dimension );
        layoutPanel.add( downsampleModuloField, BorderLayout.SOUTH );

        final TitledBorder titledBorder = new TitledBorder( 
                new EmptyBorder(8, 2, 0, 0), "Density", TitledBorder.LEADING, TitledBorder.DEFAULT_POSITION, getFont().deriveFont(8)
        );
        downsampleModuloField.setBorder(titledBorder);
        downsampleModuloField.setToolTipText("Only every Nth autocomputed point will be exported.");
        downsampleModuloField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent ke) {
                if (!Character.isDigit(ke.getKeyChar())) {
                    // Eliminate non-numeric characters, including signs.
                    ke.consume();
                }
            }
        });
        chooser.setAccessory(layoutPanel);
        int returnValue = chooser.showSaveDialog(AnnotationPanel.this);
        final String textInput = downsampleModuloField.getText().trim();
        
        ExportParameters rtnVal = null;
        try {
            int downsampleModulo = Integer.parseInt(textInput);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                rtnVal = new ExportParameters();
                rtnVal.setDownsampleModulo(downsampleModulo);
                rtnVal.setSelectedFile(chooser.getSelectedFile());
            }
        } catch (NumberFormatException nfe) {
            annotationMgr.presentError("Failed to parse input text as number: " + textInput, "Invalid Downsample");
            JOptionPane.showMessageDialog(AnnotationPanel.this, nfe);
        }
        return rtnVal;
    }

    private void showOutline(JPanel panel, Color color) {
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(color), getBorder()));
    }

    class ExportParameters {
        private File selectedFile;
        private int downsampleModulo;

        public File getSelectedFile() { return selectedFile; }
        public void setSelectedFile(File selectedFile) {
            this.selectedFile = selectedFile;
        }

        public int getDownsampleModulo() { return downsampleModulo; }
        public void setDownsampleModulo(int downsampleModulo) {
            this.downsampleModulo = downsampleModulo;
        }
    }

    class ExportAllSWCAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            ExportParameters params = getExportParameters(annotationModel.getCurrentWorkspace().getName());
            if ( params != null ) {
                annotationMgr.exportAllNeuronsAsSWC(params.getSelectedFile(), params.getDownsampleModulo());
            }
        }
    }

    class ExportCurrentSWCAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (annotationModel.getCurrentNeuron() == null) {
                annotationMgr.presentError("You must select a neuron prior to performing this action.", "No neuron selected");
            }
            else {
                ExportParameters params = getExportParameters(annotationModel.getCurrentNeuron().getName());
                if ( params != null ) {
                    annotationMgr.exportCurrentNeuronAsSWC(params.getSelectedFile(), params.getDownsampleModulo());
                }
            }
        }
    }

    class ImportSWCAction extends AbstractAction {
        private boolean neuronPerRoot = false;
        public ImportSWCAction(boolean neuronPerRoot) {
            this.neuronPerRoot = neuronPerRoot;
        }
        
        public ImportSWCAction() {
            this(false);
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {

            // note: when it's time to add toggle and/or options, you can look into
            //  adding an accesory view to dialog; however, not clear that it will
            //  give enough flexibility compared to doing a custom dialog from the start

            // could specify a dir to open in, but not sure what to choose
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Choose swc file or directory");
            chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            final FileFilter swcAndDirFilter = new SwcAndFileFilter();
            chooser.setFileFilter(swcAndDirFilter);
            int returnValue = chooser.showOpenDialog(AnnotationPanel.this);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                List<File> swcFiles = getFilesList(chooser.getSelectedFile());                
                if (swcFiles.size() > 1) {
                    AtomicInteger countDownSemaphor = new AtomicInteger(swcFiles.size());
                    // Unified notification across all the (possibly many) files.
                    CountdownBackgroundWorker progressNotificationWorker = 
                            new CountdownBackgroundWorker( 
                                    "Import " + chooser.getSelectedFile(), 
                                    countDownSemaphor 
                            );
                    progressNotificationWorker.executeWithEvents();
                    for (File swc: swcFiles) {
                        // Import all the little neurons from the file.
                        annotationMgr.importSWCFile(swc, countDownSemaphor);
                    }
                }
                else {
                    annotationMgr.importSWCFile(swcFiles.get(0), null);
                }
            }
        }
        
        private List<File> getFilesList(File selectedFile) {
            List<File> rtnVal = new ArrayList<>();
            List<File> rawFileList = new ArrayList<>();
            if (selectedFile.isDirectory()) {
                File[] swcFiles = selectedFile.listFiles(new SwcDirListFilter());
                rawFileList.addAll(Arrays.asList(swcFiles));                
            }
            else {
                rawFileList.add(selectedFile);
            }
            
            if (neuronPerRoot) {
                try {
                    // Now, we traverse list above, breaking any we see as
                    // having more than one root, into multiple input files.
                    for (File infile : rawFileList) {
                        rtnVal.addAll(annotationModel.breakOutByRoots(infile));
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    throw new RuntimeException(ioe);
                }
            }
            else {
                rtnVal.addAll(rawFileList);
            }
            return rtnVal;
        }
    }
    
    class SwcAndFileFilter extends FileFilter {
        @Override
        public boolean accept(File f) {
            return f.getName().endsWith(AnnotationModel.STD_SWC_EXTENSION) || f.isDirectory();
        }

        @Override
        public String getDescription() {
            return "*" + AnnotationModel.STD_SWC_EXTENSION;
        }
    }
    
    class SwcDirListFilter implements java.io.FileFilter {
        @Override
        public boolean accept(File file) {
            return file.isFile() && file.getName().endsWith(AnnotationModel.STD_SWC_EXTENSION);
        }

    }
}

