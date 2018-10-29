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
import loci.plugins.config.SpringUtilities;
import org.janelia.console.viewerapi.SampleLocation;
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
import org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerLocationProvider;
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
    static final int NEURONREVIEW_WIDTH = 500;
    
    private final ExplorerManager reviewManager = new ExplorerManager();
    private LinkedList<Vec3> normalGroup;
    private String[] reviewOptions;
    
    // task management 
    private JTable taskReviewTable;
    private TmNeuronMetadata currNeuron;
    private TmReviewTask currTask;
    private REVIEW_CATEGORY currCategory;
    private JScrollPane taskPane;
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

    public void setAnnotationManager (AnnotationManager manager) {
        annManager = manager;
    }

    public void nextBranch() {
        if (currGroupIndex!=-1) {
            ReviewGroup currGroup = groupList.get(currGroupIndex);
            if (currPointIndex<currGroup.getPointList().size()-1) {
                currPointIndex++;
                gotoPoint(currGroup.getPointList().get(currPointIndex));
            } else {
                if (currGroupIndex<groupList.size()-1) {
                    currGroupIndex++;
                    currGroup = groupList.get(currGroupIndex);
                    currPointIndex = 0;
                    gotoPoint(currGroup.getPointList().get(currPointIndex));
                }
            }
        }        
    }

    public void prevBranch() {
       if (currGroupIndex!=-1) {
            if (currPointIndex>0) {
                ReviewGroup currGroup = groupList.get(currGroupIndex);
                currPointIndex--;
                gotoPoint(currGroup.getPointList().get(currPointIndex));
            } else {
                if (currGroupIndex>0) {
                    currGroupIndex--;
                    ReviewGroup currGroup = groupList.get(currGroupIndex);
                    currPointIndex = currGroup.getPointList().size()-1;
                    gotoPoint(currGroup.getPointList().get(currPointIndex));
                }
            }
        }  
    }

    private void setupUI() {
        setLayout(new BorderLayout());

        // neuron table
        TaskReviewTableModel taskReviewTableModel = new TaskReviewTableModel();
        taskReviewTable = new JTable(taskReviewTableModel);
        taskReviewTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        taskReviewTable.setAutoCreateRowSorter(true);
        
        JPanel taskPanel = new JPanel();
        taskPanel.setLayout(new BorderLayout());
        List<TmReviewTask> tasks = retrieveTasks();
        ((TaskReviewTableModel)taskReviewTable.getModel()).loadReviewTasks(tasks);
        JScrollPane scrollPane = new JScrollPane(taskReviewTable);
        taskReviewTable.setFillsViewportHeight(true);
        taskPanel.add(scrollPane, BorderLayout.CENTER);
        JButton addTaskButton = new JButton("Create Task From Current");
        addTaskButton.addActionListener(event -> createNewTask());
        taskPanel.add(addTaskButton);
        //add(scrollPane, BorderLayout.NORTH);
        
        GridBagConstraints cVert = new GridBagConstraints();
        cVert.gridx = 0;
        cVert.gridy = GridBagConstraints.RELATIVE;
        cVert.anchor = GridBagConstraints.PAGE_START;
        cVert.fill = GridBagConstraints.HORIZONTAL;
        cVert.weighty = 0.0;

        // task transition buttons (next, previous, etc)
        JPanel taskButtonsPanel = new JPanel();
        taskButtonsPanel.setLayout(new BoxLayout(taskButtonsPanel, BoxLayout.LINE_AXIS));
        taskButtonsPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        //add(taskButtonsPanel, BorderLayout.SOUTH);
        
        JButton playButton = new JButton("Review Group/Branch");
        playButton.addActionListener(event -> playBranch());
        taskButtonsPanel.add(playButton);
        

        // workflow management buttons: load, done (?)
        /*JPanel workflowButtonsPanel = new JPanel();
        workflowButtonsPanel.setLayout(new BoxLayout(workflowButtonsPanel, BoxLayout.LINE_AXIS));
        workflowButtonsPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        add(workflowButtonsPanel, BorderLayout.SOUTH);

        workflowButtonsPanel.add(Box.createHorizontalGlue());

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

         List<SampleLocation> playList = new ArrayList<SampleLocation>();
         SynchronizationHelper helper = new SynchronizationHelper();
         Tiled3dSampleLocationProviderAcceptor originator = helper.getSampleLocationProviderByName(LargeVolumeViewerLocationProvider.PROVIDER_UNIQUE_NAME);

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
    
    public void createNeuronReview (TmNeuronMetadata neuron, NeuronTree tree) {
        // from leaf nodes of neurontree generate branches to play back
        List<NeuronTree> leaves = new ArrayList<NeuronTree>();
        generateLeaves (leaves, tree);
        List<List<PointDisplay>> pathList = new ArrayList<>();
        for (NeuronTree leaf: leaves) {
            pathList.add(leaf.generateRootToLeaf());
        }

        // generate gui and mxCells for neurontree
        JScrollPane treePane = navigator.createGraph(tree, NEURONREVIEW_WIDTH);

        // lay out buttons and info in a floating panel on top of the dendrogram
        JLayeredPane containerPanel = new JLayeredPane();
        containerPanel.setPreferredSize(new Dimension(500, 500));
        containerPanel.setBackground(Color.blue);
        containerPanel.setBounds(0, 0, 500, 500);
        JPanel infoPane = new JPanel();
        infoPane.setPreferredSize(new Dimension(100, 50));
        JCheckBox reviewCheckbox = new JCheckBox("Reviewed");
        reviewCheckbox.addActionListener(evt -> setBranchReviewed());
        infoPane.add(reviewCheckbox);
        infoPane.add(new JTextField("ASDFASDFASF"));
        SpringLayout infoLayout = new SpringLayout();
        infoPane.setLayout(infoLayout);
        SpringUtilities.makeGrid(infoPane,
                1, 2,
                5, 5,
                15, 15);

        // peg the info panel in the upper right corner or the dendrogram
        JPanel northToolPane = new JPanel(new BorderLayout());
        northToolPane.setOpaque(false);
        northToolPane.add(infoPane, BorderLayout.EAST);
        JPanel toolPane = new JPanel(new BorderLayout());
        toolPane.setOpaque(false);
        toolPane.setLayout(new BorderLayout());
        toolPane.add(northToolPane, BorderLayout.NORTH);

        //containerPanel.add(treePane);
        containerPanel.add(toolPane, JLayeredPane.PALETTE_LAYER);


        add( containerPanel,
                BorderLayout.CENTER);

        currNeuron = neuron;
        currCategory = REVIEW_CATEGORY.NEURON_REVIEW;

        // add reference between review point and neuronTree, for updates to the GUI 
        // when point has been reviewed
        loadPointList(pathList);
    }

    public void setBranchReviewed() {
        if (currCategory==REVIEW_CATEGORY.NEURON_REVIEW) {
            // get the current branch tmGeoAnnotations and update dendrogram
            ReviewGroup branch = groupList.get(currGroupIndex);
            List<ReviewPoint> pointList = branch.getPointList();
            List<Long> annotationList = new ArrayList<>();
            for (ReviewPoint point : pointList) {
                NeuronTree pointData = (NeuronTree)point.getDisplay();
                annotationList.add(pointData.getAnnotationId());
                pointData.getGUICell().setAttribute("fillColor", "white");
            }
            //annManager.setBranchReviewed(currNeuron, annotationList);
        }
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
            for (int i=0; i<branch.size(); i++) {
                PointDisplay node = branch.get(i);                
                Vec3 vecPoint = node.getVertexLocation();                
                ReviewPoint point = new ReviewPoint();
                point.setDisplay(node);
                point.setLocation(vecPoint);
                point.setZoomLevel(50);
                // calculate quicky normal
                Quaternion q;
                
                List<Vec3> segments = new ArrayList<Vec3>();
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
                point.setInterpolate(true);
                pointList.add(point);
            }

            ReviewGroup group = new ReviewGroup();
            group.setPointList(pointList);
            groupList.add(group);
        }
    }

    private Quaternion calculateRotation (List<Vec3> vertexPoints) {
        Vec3 first = vertexPoints.get(1).minus(vertexPoints.get(0));
        Vec3 second = vertexPoints.get(2).minus(vertexPoints.get(1));
        Vec3 normal = first.cross(second);
        double length = Math.sqrt(normal.getX()*normal.getX()+normal.getY()*normal.getY()+normal.getZ()*normal.getZ());
        normal.setX(normal.getX()/length);
        normal.setY(normal.getY()/length);
        normal.setZ(normal.getZ()/length);
        
        // add normal to queue, if queue is greater than 5, pop off one, then average normal
        if (normalGroup.size()>5) 
            normalGroup.pop();
        normalGroup.add(normal);
       
        // figure out average normal so we don't get such crazy rotations
        Vec3 normalAvg = new Vec3();
        double x = 0, y = 0, z = 0;
        for (Vec3 unitNorm : normalGroup) {
            x += unitNorm.getX();
            y += unitNorm.getY();
            z += unitNorm.getZ();
        }
        normalAvg.setX(x/normalGroup.size());
        normalAvg.setY(y/normalGroup.size());
        normalAvg.setZ(z/normalGroup.size());
        
        double angle = Math.atan2(normalAvg.getX(),normalAvg.getZ());
        
        //Quaternion foo = new Quaternion();
        float qx = (float) (normalAvg.getX() * Math.sin(angle / 2));
        float qy = (float) (normalAvg.getY() * Math.sin(angle / 2));
        float qz = (float) (normalAvg.getZ() * Math.sin(angle / 2));
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
                 reviewItem.setWorkspaceRef(currNeuron.getWorkspaceRef().toString());
                 reviewItem.setReviewed(branch.isReviewed());
             }
             
        }
        try {
            TiledMicroscopeDomainMgr persistenceMgr = TiledMicroscopeDomainMgr.getDomainMgr();
            currTask = persistenceMgr.save(currTask);
            ((TaskReviewTableModel)taskReviewTable.getModel()).addReviewTask(currTask);
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
        
    public void deleteTask (TmReviewTask task) {
        try {
            TiledMicroscopeDomainMgr persistenceMgr = TiledMicroscopeDomainMgr.getDomainMgr();
            persistenceMgr.remove(task);
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
                                "Percentage Completed"};
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

        public Object getValueAt(int row, int col) {
            return data.get(row).get(col);
        }

        public Class getColumnClass(int c) {
            return (getValueAt(0, c)==null?String.class:getValueAt(0,c).getClass());
        }
        
        public void loadReviewTasks(List<TmReviewTask> reviewTasks) {            
            for (TmReviewTask task: reviewTasks) {                
                addReviewTask(task);
            }
        }
        
        public void addReviewTask(TmReviewTask reviewTask) {            
            List row = new ArrayList<Object>();
            row.add(reviewTask.getTitle());
            row.add(reviewTask.getCategory());
            row.add(reviewTask.getOwnerKey());
            int completed = 0;
            for (TmReviewItem reviewItem : reviewTask.getReviewItems()) {
                completed += reviewItem.isReviewed() ? 1 : 0;
            }
            row.add(completed / reviewTask.getReviewItems().size());

            data.add(row);
        }
    }
   

}
