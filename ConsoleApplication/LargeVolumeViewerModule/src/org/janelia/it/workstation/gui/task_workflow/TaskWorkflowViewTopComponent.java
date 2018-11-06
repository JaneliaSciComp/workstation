package org.janelia.it.workstation.gui.task_workflow;

import Jama.Matrix;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellEditor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mxgraph.model.mxCell;
import java.awt.Color;
import java.awt.Event;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.beans.PropertyVetoException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.Stack;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;

import groovy.swing.impl.TableLayout;
import java.util.Date;
import loci.plugins.config.SpringUtilities;
import org.janelia.console.viewerapi.SampleLocation;
import org.janelia.console.viewerapi.SimpleIcons;
import org.janelia.console.viewerapi.SynchronizationHelper;
import org.janelia.console.viewerapi.Tiled3dSampleLocationProviderAcceptor;
import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.shared.geom.Quaternion;
import org.janelia.it.jacs.shared.geom.UnitVec3;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.gui.keybind.ShortcutTextField;
import org.janelia.it.workstation.browser.gui.support.MouseHandler;
import org.janelia.it.workstation.gui.large_volume_viewer.ComponentUtil;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import org.janelia.it.workstation.gui.large_volume_viewer.api.TiledMicroscopeDomainMgr;
import org.janelia.it.workstation.gui.large_volume_viewer.dialogs.ChangeNeuronOwnerDialog;
import org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerLocationProvider;
import org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmNeuronReviewItem;
import org.janelia.model.domain.tiledMicroscope.TmPointListReviewItem;
import org.janelia.model.domain.tiledMicroscope.TmReviewItem;
import org.janelia.model.domain.tiledMicroscope.TmReviewTask;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.explorer.view.OutlineView;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;
import org.openide.windows.WindowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Top component which displays something.
 */


@ConvertAsProperties(
        dtd = "-//org.janelia.it.workstation.gui.task_workflow//TaskWorkflowViewTopComponent//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = TaskWorkflowViewTopComponent.PREFERRED_ID,
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "explorer", openAtStartup = false)
@ActionID(category = "Window", id = "org.janelia.it.workstation.gui.task_workflow.TaskWorkflowViewTopComponentTopComponent")
@ActionReference(path = "Menu/Window/Large Volume Viewer" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_TaskWorkflowViewTopComponentAction",
        preferredID = TaskWorkflowViewTopComponent.PREFERRED_ID
)
@Messages({
    "CTL_TaskWorkflowViewTopComponentAction=Task Workflow View",
    "CTL_TaskWorkflowViewTopComponentTopComponent=" + TaskWorkflowViewTopComponent.LABEL_TEXT,
    "HINT_TaskWorkflowViewTopComponentTopComponent=Task Workflow View"
})
public final class TaskWorkflowViewTopComponent extends TopComponent implements ExplorerManager.Provider {
    public static final String PREFERRED_ID = "TaskWorkflowViewTopComponent";
    public static final String LABEL_TEXT = "Task Workflow";
    private AnnotationManager annManager;

    enum REVIEW_CATEGORY {
        NEURON_REVIEW, POINT_REVIEW
    };
    static final int NEURONREVIEW_WIDTH = 1300;
    static final int COLUMN_LOADTASK = 6;
    static final int COLUMN_DELETETASK = 7;
    static final int COLUMN_TMREVIEWTASK = 8;
    private ImageIcon loadTaskIcon;
    private ImageIcon deleteTaskIcon;
    
    private final ExplorerManager reviewManager = new ExplorerManager();
    private LinkedList<Vec3> normalGroup;
    private String[] reviewOptions;
    
    // task management 
    private JTable taskReviewTable;
    private TmNeuronMetadata currNeuron;
    private TmReviewTask currTask;
    private REVIEW_CATEGORY currCategory;
    private JScrollPane taskPane;
    private JScrollPane dendroPane;
    private JPanel dendroContainerPanel;
    private JCheckBox reviewCheckbox;
    int currGroupIndex;
    int currPointIndex;
    
    // point management for Horta endoscopy
    List<ReviewPoint> pointList;
    List<ReviewGroup> groupList;
    
    boolean firstTime = true;
    
    // dendrogram gui
    private JPanel viewPanel;
    ReviewTaskNavigator navigator;

    public TaskWorkflowViewTopComponent() {
        initComponents();
        setupUI();
        setName(Bundle.CTL_TaskWorkflowViewTopComponentTopComponent());
        setToolTipText(Bundle.HINT_TaskWorkflowViewTopComponentTopComponent());

    }
    
    public static final TaskWorkflowViewTopComponent getInstance() {
        return (TaskWorkflowViewTopComponent)WindowManager.getDefault().findTopComponent(PREFERRED_ID);
    }
    
    @Override
    public void componentOpened() {
    }

    @Override
    public void componentClosed() {
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        viewPanel = new javax.swing.JPanel();

        viewPanel.setLayout(new java.awt.BorderLayout());

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(viewPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(viewPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        
       navigator = new ReviewTaskNavigator();
    }// </editor-fold>//GEN-END:initComponents

    @Override
    public ExplorerManager getExplorerManager()
    {
        return reviewManager;
    }

    private static final Logger log = LoggerFactory.getLogger(TaskWorkflowViewTopComponent.class);

    public void nextBranch() {
        if (currGroupIndex!=-1) {
            ReviewGroup currGroup = groupList.get(currGroupIndex);
            if (!currGroup.isReviewed() && currCategory==REVIEW_CATEGORY.NEURON_REVIEW) {
                // clear current review markers
                List<ReviewPoint> pointList = currGroup.getPointList();
                Object[] cells = new Object[pointList.size()];
                for (int i=0; i<pointList.size(); i++) {
                    cells[i] = ((NeuronTree)pointList.get(i).getDisplay()).getGUICell();
                }
                navigator.updateCellStatus(cells, ReviewTaskNavigator.CELL_STATUS.OPEN);
            }
            if (currGroupIndex<groupList.size()-1) {
                currGroupIndex++;
                currGroup = groupList.get(currGroupIndex);
                if (!currGroup.isReviewed() && currCategory == REVIEW_CATEGORY.NEURON_REVIEW) {
                    List<ReviewPoint> pointList = currGroup.getPointList();

                    Object[] cells = new Object[pointList.size()];
                    for (int i = 0; i < pointList.size(); i++) {
                        cells[i] = ((NeuronTree) pointList.get(i).getDisplay()).getGUICell();
                    }
                    navigator.updateCellStatus(cells, ReviewTaskNavigator.CELL_STATUS.UNDER_REVIEW);
                    reviewCheckbox.setSelected(false);
                    reviewCheckbox.setEnabled(true);
                } else {                    
                    reviewCheckbox.setSelected(true);
                    reviewCheckbox.setEnabled(false);
                }
            }            
        }        
    }

    public void prevBranch() {
       if (currGroupIndex!=-1) {
           ReviewGroup currGroup = groupList.get(currGroupIndex);
            if (!currGroup.isReviewed() && currCategory==REVIEW_CATEGORY.NEURON_REVIEW) {
                // clear current review markers
                List<ReviewPoint> pointList = currGroup.getPointList();
                Object[] cells = new Object[pointList.size()];
                for (int i=0; i<pointList.size(); i++) {
                    cells[i] = ((NeuronTree)pointList.get(i).getDisplay()).getGUICell();
                }
                navigator.updateCellStatus(cells, ReviewTaskNavigator.CELL_STATUS.OPEN);
            }
            if (currGroupIndex>0) {
                currGroupIndex--;
                currGroup = groupList.get(currGroupIndex);
                if (!currGroup.isReviewed() && currCategory == REVIEW_CATEGORY.NEURON_REVIEW) {
                    List<ReviewPoint> pointList = currGroup.getPointList();
                    Object[] cells = new Object[pointList.size()];
                    for (int i = 0; i < pointList.size(); i++) {
                        cells[i] = ((NeuronTree) pointList.get(i).getDisplay()).getGUICell();
                    }
                    navigator.updateCellStatus(cells, ReviewTaskNavigator.CELL_STATUS.UNDER_REVIEW);
                    reviewCheckbox.setSelected(false);
                    reviewCheckbox.setEnabled(true);
                } else {                    
                    reviewCheckbox.setSelected(true);
                    reviewCheckbox.setEnabled(false);
                }
            }
        }  
    }

    private void setupUI() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // neuron table
        TaskReviewTableModel taskReviewTableModel = new TaskReviewTableModel();
        taskReviewTable = new JTable(taskReviewTableModel);
        taskReviewTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        taskReviewTable.setAutoCreateRowSorter(true);
        
        loadTaskIcon = SimpleIcons.getIcon("open_action.png");
        deleteTaskIcon = SimpleIcons.getIcon("delete.png");
        
        taskReviewTable.addMouseListener(new MouseHandler() {
            @Override
            protected void singleLeftClicked(MouseEvent me) {
                if (me.isConsumed()) return;
                JTable table = (JTable) me.getSource();
                int viewRow = table.rowAtPoint(me.getPoint());
                if (viewRow >= 0) {                   
                    int viewColumn = table.columnAtPoint(me.getPoint());
                    if (viewColumn==COLUMN_DELETETASK) {
                        TmReviewTask selectedReview = ((TaskReviewTableModel)taskReviewTable.getModel()).getReviewTaskAtRow(viewRow);                    
                        if (selectedReview!=null) {
                            deleteTask(selectedReview, viewRow);
                        }
                    } else if (viewColumn==COLUMN_LOADTASK) {
                        TmReviewTask selectedReview = ((TaskReviewTableModel)taskReviewTable.getModel()).getReviewTaskAtRow(viewRow);                    
                        if (selectedReview!=null) {
                            loadReviewTask(selectedReview);
                        }
                    }
                }
                me.consume();
            }
        });
        
        JPanel taskPanel = new JPanel();
        taskPanel.setLayout(new BorderLayout());
        List<TmReviewTask> tasks = retrieveTasks();
        taskReviewTable.setPreferredSize(new Dimension (500,200));
        JScrollPane scrollPane = new JScrollPane(taskReviewTable);
        taskReviewTable.setFillsViewportHeight(true);
        taskPanel.add(scrollPane, BorderLayout.CENTER);                                    
        add(taskPanel);
        
        GridBagConstraints cVert = new GridBagConstraints();
        cVert.gridx = 0;
        cVert.gridy = GridBagConstraints.RELATIVE;
        cVert.anchor = GridBagConstraints.PAGE_START;
        cVert.fill = GridBagConstraints.HORIZONTAL;
        cVert.weighty = 0.0;
        
        dendroContainerPanel = new JPanel();
        dendroContainerPanel.setLayout(new BoxLayout(dendroContainerPanel,BoxLayout.Y_AXIS));
        add(dendroContainerPanel);
/*
        JButton loadButton = new JButton("Load point list...");
        loadButton.addActionListener(event -> onLoadButton());
        workflowButtonsPanel.add(loadButton);

        JButton saveButton = new JButton("Save reviewed list...");
        saveButton.addActionListener(event -> onSaveButton());
        workflowButtonsPanel.add(saveButton);
*/
        
        
    }
    
    private void playBranch() {
        reviewGroup(currGroupIndex);
    }

    private void onLoadButton() {
        readPointFile();

        log.info("Loaded point file " + "my point file");
    }

    private void onSaveButton() {
        // dialog to get file
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose export file");
        chooser.setMultiSelectionEnabled(false);
        int result = chooser.showSaveDialog(FrameworkImplProvider.getMainFrame());
        if (result == JFileChooser.APPROVE_OPTION) {
            File exportFile = chooser.getSelectedFile();
            try {
                ObjectMapper mapper = new ObjectMapper();
                // get the review notes and output with point list
                List<Map<String,String>> pointReviews = new ArrayList<>();
                for (int i = 0; i < pointList.size(); i++) {
                   /* String review = (String) pointTable.getModel().getValueAt(i, 3);
                    if (review != null) {
                        Map pointReview = new HashMap<String,String>();
                        pointReview.put("x", pointList.get(i).getLocation().getX());
                        pointReview.put("y", pointList.get(i).getLocation().getY());
                        pointReview.put("z", pointList.get(i).getLocation().getZ());
                        pointReview.put("reviewNote", review);
                        pointReviews.add(pointReview);
                    }*/
                }
                mapper.writeValue(exportFile,pointReviews);

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(ComponentUtil.getLVVMainWindow(),
                        "Could not write out reviewed points " + exportFile,
                        "Error writing point reviews file",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
     private void reviewGroup(int groupIndex) {
         ReviewGroup group = groupList.get(groupIndex);
         currGroupIndex = groupIndex;
         
         List<SampleLocation> playList = new ArrayList<SampleLocation>();
         SynchronizationHelper helper = new SynchronizationHelper();
         Tiled3dSampleLocationProviderAcceptor originator = helper.getSampleLocationProviderByName(LargeVolumeViewerLocationProvider.PROVIDER_UNIQUE_NAME);
         Object[] guiCells = new Object[group.getPointList().size()];
         if (currCategory==REVIEW_CATEGORY.NEURON_REVIEW) {
             int i=0;
             for (ReviewPoint point : group.getPointList()) {
                 guiCells[i++] = (((NeuronTree)point.getDisplay()).getGUICell());                   
             }
             navigator.updateCellStatus(guiCells, ReviewTaskNavigator.CELL_STATUS.UNDER_REVIEW);
         }             
         
         for (ReviewPoint point : group.getPointList()) {           
             SampleLocation sampleLocation = originator.getSampleLocation();
             sampleLocation.setFocusUm(point.getLocation().getX(), point.getLocation().getY(), point.getLocation().getZ());
             sampleLocation.setMicrometersPerWindowHeight(point.getZoomLevel());
             sampleLocation.setRotationAsQuaternion(point.getRotation());
             playList.add(sampleLocation);
         }
         Tiled3dSampleLocationProviderAcceptor hortaViewer;
         Collection<Tiled3dSampleLocationProviderAcceptor> locationAcceptors = helper.getSampleLocationProviders(LargeVolumeViewerLocationProvider.PROVIDER_UNIQUE_NAME);
         for (Tiled3dSampleLocationProviderAcceptor acceptor : locationAcceptors) {
             if (acceptor.getProviderDescription().equals("Horta - Focus On Location")) {
                 acceptor.playSampleLocations(playList);
             }
         }
    }

    /**
     * move the camera to the indicated point in LVV and Horta
     */
    private void gotoPoint(ReviewPoint point) {
        // this is possibly a bit hacky...I followed the example in FilteredAnnList;
        //  we use the LVV sample provider to get the sample location, then poke
        //  our values in; that's sent to the appropriate Horta acceptor; then
        //  since we know that LVV is an acceptor, too, we can just put the altered
        //  sample location back into the originator to trigger that move

        // not sure what the try/catch is preventing, but it was in the code I copied
        try {
            SynchronizationHelper helper = new SynchronizationHelper();
            Tiled3dSampleLocationProviderAcceptor originator = helper.getSampleLocationProviderByName(LargeVolumeViewerLocationProvider.PROVIDER_UNIQUE_NAME);
            SampleLocation sampleLocation = originator.getSampleLocation();
            sampleLocation.setFocusUm(point.getLocation().getX(), point.getLocation().getY(), point.getLocation().getZ());
            sampleLocation.setMicrometersPerWindowHeight(point.getZoomLevel());
            sampleLocation.setRotationAsQuaternion(point.getRotation());   
            sampleLocation.setInterpolate(false);
            // LVV
            originator.setSampleLocation(sampleLocation);


            // Horta
            Collection<Tiled3dSampleLocationProviderAcceptor> locationAcceptors = helper.getSampleLocationProviders(LargeVolumeViewerLocationProvider.PROVIDER_UNIQUE_NAME);
            for (Tiled3dSampleLocationProviderAcceptor acceptor : locationAcceptors) {
                if (acceptor.getProviderDescription().equals("Horta - Focus On Location")) {
                    acceptor.setSampleLocation(sampleLocation);
                }
            }
        } catch (Exception e) {
            ConsoleApp.handleException(e);
        }
    }
    
    public double vectorLen (Vec3 vector) {
        return Math.sqrt(vector.getX()*vector.getX() + vector.getY()*vector.getY() + vector.getZ()*vector.getZ());
    }
    
    private void generateLeaves(List<NeuronTree> leaves, NeuronTree node) {
        for (NeuronTree childNode: node.getChildren()) {
            if (childNode.isLeaf()) {
                leaves.add(childNode);
            } else {
                generateLeaves (leaves, childNode);
            }
        }        
    }
    
    /**
     * If the neuron was edited, you want to automatically reload the dendrogram
    */
    public void regenerateBranchPath (ReviewGroup item) {
        
    }
    
    public void loadReviewTask (TmReviewTask reviewTask) {
        if (annManager==null)
            annManager = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
        if (dendroContainerPanel!=null) {
            dendroContainerPanel.removeAll();
        }
        List<TmReviewItem> itemList = reviewTask.getReviewItems();
        if (itemList.size()>0 && reviewTask.getCategory().equals(REVIEW_CATEGORY.NEURON_REVIEW.toString())) {
            TmNeuronReviewItem sample = (TmNeuronReviewItem)itemList.get(0);
            NeuronTree root = annManager.generateNeuronTreeForReview(sample.getNeuronId());
            List<List<PointDisplay>> pathList = generatePlayList (root);
            
            loadPointList (pathList);
                        
            // generate gui and mxCells for neurontree
            dendroPane = navigator.createGraph(root, pathList.size(), this.getWidth(), this.getHeight()/2);
            addTaskButtons();
            
            currNeuron = annManager.getAnnotationModel().getNeuronFromNeuronID(sample.getNeuronId());
            currCategory = REVIEW_CATEGORY.NEURON_REVIEW;
            
            // awkwardly walk through the tree using the stored path lists to mark branches as reviewed
            for (TmReviewItem item : itemList) {
                if (item.isReviewed()) {
                    TmNeuronReviewItem neuronItem = (TmNeuronReviewItem) item;
                    Long pathRootAnnotationId = Long.parseLong((String) neuronItem.getReviewItems().get(0));

                    for (ReviewGroup group : groupList) {
                        ReviewPoint pathRoot = group.getPointList().get(0);
                        if (((NeuronTree) pathRoot.getDisplay()).getAnnotationId().longValue() == pathRootAnnotationId.longValue()
                                && neuronItem.getReviewItems().size() == group.getPointList().size()) {
                            this.setNeuronBranchReviewed(group);
                        }
                    }
                }
            }                      
        }
    }
    
    private List<List<PointDisplay>> generatePlayList (NeuronTree tree) {
        List<NeuronTree> leaves = new ArrayList<NeuronTree>();
        generateLeaves (leaves, tree);
        List<List<PointDisplay>> pathList = new ArrayList<>();
        for (NeuronTree leaf: leaves) {
            pathList.add(leaf.generateRootToLeaf());
        }
        return pathList;
    }
    
    public void createNeuronReview (TmNeuronMetadata neuron, NeuronTree tree) {   
        // from leaf nodes of neurontree generate branches to play back
        List<List<PointDisplay>> pathList = generatePlayList (tree);

        // generate gui and mxCells for neurontree

        dendroPane = navigator.createGraph(tree, pathList.size(), this.getWidth(), this.getHeight());
        addTaskButtons();
        currNeuron = neuron;
        currCategory = REVIEW_CATEGORY.NEURON_REVIEW;
        

        // add reference between review point and neuronTree, for updates to the GUI 
        // when point has been reviewed
        loadPointList(pathList);
        
        List<PointDisplay> pointList = pathList.get(0);
        Object[] cells = new Object[pointList.size()];
        for (int i = 0; i < pointList.size(); i++) {
            cells[i] = ((NeuronTree) pointList.get(i)).getGUICell();
        }
        navigator.updateCellStatus(cells, ReviewTaskNavigator.CELL_STATUS.UNDER_REVIEW);
    }
    
    private void addTaskButtons() {
        dendroContainerPanel.removeAll();        

        JPanel taskButtonsPanel = new JPanel();
        taskButtonsPanel.setLayout(new BoxLayout(taskButtonsPanel, BoxLayout.LINE_AXIS));
        taskButtonsPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        JButton prevBranchButton = new JButton("Prev Branch");
        prevBranchButton.addActionListener(event -> prevBranch());
        taskButtonsPanel.add(prevBranchButton);

        JButton nextBranchButton = new JButton("Next Branch");
        nextBranchButton.addActionListener(event -> nextBranch());
        taskButtonsPanel.add(nextBranchButton);

        JButton playButton = new JButton("Play Branch");
        playButton.addActionListener(event -> playBranch());
        taskButtonsPanel.add(playButton);

        JButton createTaskButton = new JButton("Create Task From Current");
        createTaskButton.addActionListener(event -> createNewTask());
        taskButtonsPanel.add(createTaskButton);

        JPanel infoPane = new JPanel();
        infoPane.setPreferredSize(new Dimension(100, 50));
        reviewCheckbox = new JCheckBox("Reviewed");
        reviewCheckbox.addActionListener(evt -> setBranchReviewed());
        if (groupList!=null && groupList.get(currGroupIndex).isReviewed()) {
            reviewCheckbox.setSelected(true);
            reviewCheckbox.setEnabled(false);
        }
        taskButtonsPanel.add(reviewCheckbox);

        dendroContainerPanel.add(taskButtonsPanel);
        dendroContainerPanel.add(dendroPane);
    }

    public void setBranchReviewed() {
        if (reviewCheckbox.isSelected() && !groupList.get(currGroupIndex).isReviewed() && currCategory==REVIEW_CATEGORY.NEURON_REVIEW) {
            // get the current branch tmGeoAnnotations and update dendrogram
            ReviewGroup branch = groupList.get(currGroupIndex);
            setNeuronBranchReviewed(branch);             
            reviewCheckbox.setEnabled(false);
        }  
    }

    private void setNeuronBranchReviewed(ReviewGroup group) {
        group.setReviewed(true);
        List<ReviewPoint> pointList = group.getPointList();
        List<Long> annotationList = new ArrayList<>();
        Object[] guiCells = new Object[pointList.size()];
        for (int i = 0; i < pointList.size(); i++) {
            ReviewPoint point = pointList.get(i);
            NeuronTree pointData = (NeuronTree) point.getDisplay();
            pointData.setReviewed(true);
            annotationList.add(pointData.getAnnotationId());
            guiCells[i] = pointData.getGUICell();
        }
        navigator.updateCellStatus(guiCells, ReviewTaskNavigator.CELL_STATUS.REVIEWED);
        if (annManager==null)
            annManager = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
        annManager.setBranchReviewed(currNeuron, annotationList);

    }

    /**
     * generates a point review list from a list of points generated elsewhere
     * 
     */
    public void loadPointList (List<List<PointDisplay>> pathList) {
        groupList = new ArrayList<>();

        normalGroup = new LinkedList();        
        for (List<PointDisplay> branch: pathList) {
            pointList = new ArrayList<>();
          
            // calculate quicky normal
            Quaternion q;
            for (int i=0; i<branch.size(); i++) {
                PointDisplay node = branch.get(i);                
                Vec3 vecPoint = node.getVertexLocation();                
                ReviewPoint point = new ReviewPoint();
                point.setDisplay(node);
                point.setLocation(vecPoint);
                point.setZoomLevel(50);
                
                List<Vec3> segments = new ArrayList<Vec3>();
                 q = new Quaternion();
                 
                if (branch.size()<3) {
                    q = new Quaternion();
                } else {
                    if (i == 0) {
                        segments.add(branch.get(0).getVertexLocation());
                        segments.add(branch.get(1).getVertexLocation());
                        segments.add(branch.get(2).getVertexLocation());
                    } else if (i == branch.size()-1) {
                        segments.add(branch.get(branch.size()-3).getVertexLocation());
                        segments.add(branch.get(branch.size()-2).getVertexLocation());
                        segments.add(branch.get(branch.size()-1).getVertexLocation());
                    } else {
                        segments.add(branch.get(i-1).getVertexLocation());
                        segments.add(branch.get(i).getVertexLocation());
                        segments.add(branch.get(i+1).getVertexLocation());
                    }
                    q = this.calculateRotation(segments);
                }

                
                if (q!=null) {
                    point.setRotation(new float[]{(float)q.x(), (float)q.y(), (float)q.z(), (float)q.w()});
                }
                pointList.add(point);
            }

            ReviewGroup group = new ReviewGroup();
            group.setPointList(pointList);
            groupList.add(group);
        }
    }

    private Quaternion calculateRotation (List<Vec3> vertexPoints) {
        Vec3 tangent1 = vertexPoints.get(1).minus(vertexPoints.get(0));
        double tan1len = tangent1.norm();
        tangent1.setX(tangent1.getX()/tan1len);
        tangent1.setY(tangent1.getY()/tan1len);
        tangent1.setZ(tangent1.getZ()/tan1len);
        Vec3 tangent2 = vertexPoints.get(2).minus(vertexPoints.get(1));
        double tan2len = tangent2.norm();
        tangent2.setX(tangent2.getX()/tan2len);
        tangent2.setY(tangent2.getY()/tan2len);
        tangent2.setZ(tangent2.getZ()/tan2len);
        Vec3 axis = tangent1.cross(tangent2);               
        double angle = Math.acos(tangent1.dot(tangent2));
        
        //Quaternion foo = new Quaternion();
        float qx = (float) (axis.getX() * Math.sin(angle / 2));
        float qy = (float) (axis.getY() * Math.sin(angle / 2));
        float qz = (float) (axis.getZ() * Math.sin(angle / 2));
        float qw = (float) Math.cos(angle / 2);
        //return null;
        return new Quaternion(qx, qy, qz, qw, false);       
    }

    /**
     * pop a file chooser; load and parse a json list of points
     *
     * file format:
     *      -- one point per line = whitespace-delimited x, y, z
     * (preferred) -- one point per line = [x, y, z] (allowed, matches "copy
     * coord to clipboard" format) -- blank lines allowed -- comment lines start
     * with #
     */
    private void readPointFile() {
        groupList = new ArrayList<>();

        // dialog to get file
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose point file");
        chooser.setMultiSelectionEnabled(false);
        int result = chooser.showOpenDialog(FrameworkImplProvider.getMainFrame());
        if (result == JFileChooser.APPROVE_OPTION) {
            File pointFile = chooser.getSelectedFile();

            Map<String,Object> reviewData = null;
            Map<String,Object> pointData = null;
            try {
                ObjectMapper mapper = new ObjectMapper();
                reviewData = mapper.readValue(new FileInputStream(pointFile), new TypeReference<Map<String,Object>>(){});
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(ComponentUtil.getLVVMainWindow(),
                        "Could not read file " + pointFile,
                        "Error reading point file",
                        JOptionPane.ERROR_MESSAGE);
            }

            if (reviewData!=null) {
                List pointGroups = (List)reviewData.get("reviewGroups");
                if (pointGroups != null && pointGroups.size() > 0) {
                    for (int i=0; i<pointGroups.size(); i++) {
                        List<ReviewPoint> pointList = new ArrayList<>();
                        LinkedHashMap pointWrapper = (LinkedHashMap)pointGroups.get(i);
                        List<Map<String, Object>> rawPoints = (List<Map<String, Object>>) pointWrapper.get("points");
                        int nerrors = 0;

                        boolean interpolate = false;
                        if ((String)pointWrapper.get("interpolate")!=null)
                            interpolate = true;
                        for (Map<String, Object> pointMap : rawPoints) {
                            try {
                                ReviewPoint point = new ReviewPoint();
                                Vec3 pointLocation = new Vec3(Double.parseDouble((String)pointMap.get("x")),
                                        Double.parseDouble((String)pointMap.get("y")), Double.parseDouble((String)pointMap.get("z")));
                                point.setLocation(pointLocation);

                                // get quaternion rotation
                                List rotation = (List)pointMap.get("quaternionRotation");
                                if (rotation!=null && rotation.size()>0) {
                                    float[] quaternion = new float[4];
                                    quaternion[0] = Float.parseFloat((String)rotation.get(0));
                                    quaternion[1] = Float.parseFloat((String)rotation.get(1));
                                    quaternion[2] = Float.parseFloat((String)rotation.get(2));
                                    quaternion[3] = Float.parseFloat((String)rotation.get(3));
                                    point.setRotation(quaternion);
                                }

                                // get zoom level
                                String zoomLevel = (String)pointMap.get("zoomLevel");
                                if (zoomLevel!=null)
                                    point.setZoomLevel(Float.parseFloat(zoomLevel));

                                point.setInterpolate(interpolate);

                                pointList.add(point);
                            } catch (NumberFormatException e) {
                                nerrors++;
                                continue;
                            }
                        }
                        ReviewGroup group = new ReviewGroup();
                        group.setPointList(pointList);
                        groupList.add(group);

                        if (nerrors > 0) {
                            JOptionPane.showMessageDialog(ComponentUtil.getLVVMainWindow(),
                                    "Not all lines in point file could be parsed; " + nerrors + " errors.",
                                    "Errors parsing point file",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }

                    // load review options
                    List<String> reviewActions = (List<String>) reviewData.get("reviewOptions");
                    if (reviewActions != null) {
                        reviewOptions = reviewActions.toArray(new String[reviewActions.size()]);
                    }
                }
            }
        }
    }
    
    /**
     * retrieve all review tasks 
     */
    public List<TmReviewTask> retrieveTasks () {
        List<TmReviewTask> reviewTasks = null;
        TiledMicroscopeDomainMgr persistenceMgr = TiledMicroscopeDomainMgr.getDomainMgr();
        try {
            reviewTasks = persistenceMgr.getReviewTasks();
            ((TaskReviewTableModel)taskReviewTable.getModel()).loadReviewTasks(reviewTasks);
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        return reviewTasks;        
    }    
    
    /**
     * takes current point lists and generates a ReviewTask object for persistence.
     */
    public void createNewTask() {
        currTask = new TmReviewTask();   
        currTask.setWorkspaceRef(currNeuron.getWorkspaceRef().toString());
        currTask.setCategory(currCategory.toString());
        currTask.setOwnerKey(AccessManager.getSubjectKey());
        if (currCategory==REVIEW_CATEGORY.NEURON_REVIEW) {
            currTask.setTitle(currNeuron.getName());
        }
        for (ReviewGroup branch: groupList) {                      
             TmReviewItem reviewItem = null;
             switch (currCategory) {
                 case NEURON_REVIEW:        
                     reviewItem = new TmNeuronReviewItem();
                     ((TmNeuronReviewItem)reviewItem).setNeuronId(currNeuron.getId()); 
                     for (ReviewPoint point : branch.getPointList()) {
                         reviewItem.addReviewItem(((NeuronTree)point.getDisplay()).getAnnotationId());
                     }
                     
                     break;
                 case POINT_REVIEW:
                     reviewItem = new TmPointListReviewItem();      
                     for (ReviewPoint point : branch.getPointList()) {
                         reviewItem.addReviewItem(point.getDisplay().getVertexLocation());
                     }
                     break;
             }
             if (reviewItem!=null) {
                 reviewItem.setReviewed(branch.isReviewed());
                 currTask.addReviewItem(reviewItem); 
             }            
        }
        try {
            TiledMicroscopeDomainMgr persistenceMgr = TiledMicroscopeDomainMgr.getDomainMgr();
            currTask = persistenceMgr.save(currTask);
            ((TaskReviewTableModel)taskReviewTable.getModel()).addReviewTask(currTask);
            ((TaskReviewTableModel)taskReviewTable.getModel()).fireTableDataChanged();
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        
    }
    
    public void updateCurrentTask () {
        try {
            if (currTask != null) {
                TiledMicroscopeDomainMgr persistenceMgr = TiledMicroscopeDomainMgr.getDomainMgr();
                persistenceMgr.save(currTask);
            }
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        
    }
        
    public void deleteTask (TmReviewTask task, int deletedRow) {
        try {            
            TiledMicroscopeDomainMgr persistenceMgr = TiledMicroscopeDomainMgr.getDomainMgr();
            persistenceMgr.remove(task);
            ((TaskReviewTableModel)taskReviewTable.getModel()).deleteTask(deletedRow);
            ((TaskReviewTableModel)taskReviewTable.getModel()).fireTableRowsDeleted(deletedRow, deletedRow);
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }        
    }

    /**
     * are there any points loaded?
     */
    private boolean hasPoints() {
        return groupList.size()>0 && pointList.size()>0;
    }


    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
    }
    
    class TaskReviewTableModel extends AbstractTableModel {
        String[] columnNames = {"Name",
                                "Category",
                                "Workspace",
                                "Owner",
                                "Date",
                                "Percentage Completed",
                                "", ""};
        List<List<Object>> data = new ArrayList<List<Object>>();
        
        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return data.size();
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }
        
        // get tmReviewTask stored at end of row
        public TmReviewTask getReviewTaskAtRow (int row) {
            return (TmReviewTask)data.get(row).get(columnNames.length-2);
        }

        public Object getValueAt(int row, int col) {
            if (col<6) {
                return data.get(row).get(col);
            } else if (col==COLUMN_LOADTASK) {
                return loadTaskIcon;
            } else if (col==COLUMN_DELETETASK) {
                return deleteTaskIcon;
            }
            return null;
        }

        public Class getColumnClass(int c) {
            return (getValueAt(0, c)==null?String.class:getValueAt(0,c).getClass());
        }
        
        public void loadReviewTasks(List<TmReviewTask> reviewTasks) {            
            if (reviewTasks!=null) {
                for (TmReviewTask task: reviewTasks) {                
                    addReviewTask(task);
                }
            }
        }
        
        public void addReviewTask(TmReviewTask reviewTask) {            
            List row = new ArrayList<Object>();
            row.add(reviewTask.getTitle());
            row.add(reviewTask.getCategory());
            String workspace = reviewTask.getWorkspaceRef();
            if (workspace!=null) {
                workspace = workspace.replaceAll("TmWorkspace#", "");
            }
            row.add(workspace);
            row.add(reviewTask.getOwnerKey());
            row.add(reviewTask.getCreationDate());
            int completed = 0;           
            if (reviewTask.getReviewItems().size() > 0) {
                for (TmReviewItem reviewItem : reviewTask.getReviewItems()) {
                    completed += reviewItem.isReviewed() ? 1 : 0;
                }
                row.add(completed / reviewTask.getReviewItems().size());
            }
            row.add(reviewTask);

            data.add(row);
        }
        
         public void deleteTask(int deletedRow) {            
            data.remove(deletedRow);
        }
    }
   

}
