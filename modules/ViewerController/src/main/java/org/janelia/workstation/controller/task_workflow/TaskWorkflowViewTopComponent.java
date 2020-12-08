package org.janelia.workstation.controller.task_workflow;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.Subscribe;
import com.mxgraph.model.mxCell;
import org.janelia.workstation.controller.widgets.SimpleIcons;
import org.janelia.model.domain.tiledMicroscope.*;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.ViewerEventBus;
import org.janelia.workstation.controller.eventbus.AnimationEvent;
import org.janelia.workstation.controller.eventbus.CreateNeuronReviewEvent;
import org.janelia.workstation.controller.eventbus.NeuronBranchReviewedEvent;
import org.janelia.workstation.controller.eventbus.ViewEvent;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.controller.model.TmViewState;
import org.janelia.workstation.controller.tileimagery.TileFormat;
import org.janelia.workstation.geom.Quaternion;
import org.janelia.workstation.geom.Vec3;
import org.janelia.workstation.common.gui.support.MouseHandler;
import org.janelia.workstation.controller.access.TiledMicroscopeDomainMgr;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.explorer.ExplorerManager;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

@ConvertAsProperties(
        dtd = "-//org.janelia.workstation.gui.task_workflow//TaskWorkflowViewTopComponent//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = TaskWorkflowViewTopComponent.PREFERRED_ID,
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_NEVER
)
@TopComponent.Registration(mode = "explorer", openAtStartup = false)
@ActionID(category = "Window", id = "org.janelia.workstation.gui.task_workflow.TaskWorkflowViewTopComponentTopComponent")
@ActionReference(path = "Menu/Window/Horta", position = 103)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_TaskWorkflowViewTopComponentAction",
        preferredID = TaskWorkflowViewTopComponent.PREFERRED_ID
)
@Messages({
    "CTL_TaskWorkflowViewTopComponentAction=Task Workflow View",
    "CTL_TaskWorkflowViewTopComponentTopComponent=" + TaskWorkflowViewTopComponent.LABEL_TEXT,
    "HINT_TaskWorkflowViewTopComponentTopComponent=Task Workflow View"
})
public final class TaskWorkflowViewTopComponent extends TopComponent implements ExplorerManager.Provider, MouseListener {
    public static final String PREFERRED_ID = "TaskWorkflowViewTopComponent";
    public static final String LABEL_TEXT = "Proofreader";

    @Override
    public void mouseClicked(MouseEvent event) {
        mxCell selectedCell = navigator.getCellAt(event.getX(), event.getY());
        if (selectedCell != null) {
            PointDisplay point = pointLookup.get(selectedCell.getId());
            // we have a cell; check for double-click for folding or single-click for selection
            if ( (event.getClickCount() == 2) && (event.getButton() == MouseEvent.BUTTON1) && point!=null) {
                // determine the fold status of cell and hide/show subcells
                List<NeuronTree> children = ((NeuronTree)point).getChildren();
                point.toggleFolded();
                foldTree(children, point.isFolded());
                navigator.foldCell(selectedCell, ((NeuronTree)point).getTotalNumChildren(), point.isFolded());
                navigator.repaint();
            } else if (event.getClickCount() == 1) {
                Integer branch = branchLookup.get(selectedCell.getId());
                if (branch!=null) {
                    clearSelection();
                    selectBranch(branch);
                }
                if (point!=null) {
                    selectPoint(point);
                }
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    enum REVIEW_CATEGORY {
        NEURON_REVIEW, POINT_REVIEW
    };
    static final int NEURONREVIEW_WIDTH = 1300;    
    static final int COLUMN_LOADTASK = 5;
    static final int COLUMN_DELETETASK = 6;
    
    private final ExplorerManager reviewManager = new ExplorerManager();
    private LinkedList<Vec3> normalGroup;
    private String[] reviewOptions;
    
    // task management
    JToolBar taskCrudToolbar;
    JToolBar taskActionItemsToolbar;
    private ImageIcon loadTaskIcon;
    private ImageIcon deleteTaskIcon;
    private JTable taskReviewTable;
    private TmNeuronMetadata currNeuron;
    private TmReviewTask currTask;
    private REVIEW_CATEGORY currCategory;
    private JScrollPane taskPane;
    private JScrollPane dendroPane;
    private JPanel dendroContainerPanel;
    private JCheckBox reviewCheckbox;
    private JCheckBox rotationCheckbox;
    private JTextField speedSpinner;
    private JTextField numStepsSpinner;
    JPanel taskButtonsPanel1;
    JPanel taskButtonsPanel2;
    JToolBar parametersToolbar;
    JToolBar flyThroughActionsToolbar;
        
    boolean firstTime = true;
    boolean selectMode = false;
    
    // point management for Horta endoscopy
    List<ReviewPoint> pointList;
    List<ReviewGroup> groupList;
    HashMap<String, Integer> branchLookup;
    HashMap<String, PointDisplay> pointLookup;
    HashMap<Long, PointDisplay> annotationLookup;
    private final ButtonGroup dendroModeGroup = new ButtonGroup();
    int currGroupIndex;
    int currPointIndex;
    
    // selection mode items
    List<PointDisplay> selectedPoints = new ArrayList<>();
    
    
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
     //   if (annManager==null || annManager.getCurrentWorkspace()==null)
       //     return;
        loadHistory();
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
       navigator = new ReviewTaskNavigator();
       ViewerEventBus.registerForEvents(this);
       
    }// </editor-fold>//GEN-END:initComponents

    @Override
    public ExplorerManager getExplorerManager()
    {
        return reviewManager;
    }

    private static final Logger log = LoggerFactory.getLogger(TaskWorkflowViewTopComponent.class);

    public void prevBranch() {
       if (groupList != null && currGroupIndex!=-1) {
           ReviewGroup currGroup = groupList.get(currGroupIndex);
            if (currCategory==REVIEW_CATEGORY.NEURON_REVIEW) {
                // clear current review markers
                List<ReviewPoint> pointList = currGroup.getPointList();
                Object[] cells = new Object[pointList.size()];
                for (int i=0; i<pointList.size(); i++) {
                    NeuronTree point = (NeuronTree)pointList.get(i).getDisplay();                    
                    cells[i] = point.getGUICell();
                    if (point.isReviewed()) {
                        navigator.updateCellStatus(cells, ReviewTaskNavigator.CELL_STATUS.REVIEWED);
                    } else {                        
                        navigator.updateCellStatus(cells, ReviewTaskNavigator.CELL_STATUS.OPEN);
                    }
                }
                navigator.updateCellStatus(cells, ReviewTaskNavigator.CELL_STATUS.OPEN);
            }
            if (currGroupIndex>0) {
                currGroupIndex--;
                selectedPoints.clear();
                currGroup = groupList.get(currGroupIndex);
                if (!currGroup.isReviewed() && currCategory == REVIEW_CATEGORY.NEURON_REVIEW) {
                    List<ReviewPoint> pointList = currGroup.getPointList();
                    Object[] cells = new Object[pointList.size()];
                    for (int i = 0; i < pointList.size(); i++) {                        
                        selectedPoints.add(pointList.get(i).getDisplay());
                        cells[i] = ((NeuronTree) pointList.get(i).getDisplay()).getGUICell();
                    }
                    navigator.updateCellStatus(cells, ReviewTaskNavigator.CELL_STATUS.UNDER_REVIEW);
                }
            }
        }  
    }
    
    public void nextBranch(boolean nextToReview) {
        if (groupList != null && currGroupIndex!=-1) {
            ReviewGroup currGroup = groupList.get(currGroupIndex);
            if (currCategory==REVIEW_CATEGORY.NEURON_REVIEW) {
                // clear current review markers
                List<ReviewPoint> pointList = currGroup.getPointList();
                Object[] cells = new Object[pointList.size()];
                for (int i=0; i<pointList.size(); i++) {
                    NeuronTree point = (NeuronTree)pointList.get(i).getDisplay();                    
                    cells[i] = point.getGUICell();
                    if (point.isReviewed()) {
                        navigator.updateCellStatus(cells, ReviewTaskNavigator.CELL_STATUS.REVIEWED);
                    } else {                        
                        navigator.updateCellStatus(cells, ReviewTaskNavigator.CELL_STATUS.OPEN);
                    }
                }
               
            }
            if (currGroupIndex<groupList.size()-1) {
                currGroupIndex++;
                selectedPoints.clear();
                currGroup = groupList.get(currGroupIndex);
                while (nextToReview && currGroup.isReviewed() && currGroupIndex<groupList.size()-1) {
                    currGroupIndex++;
                    currGroup = groupList.get(currGroupIndex);
                }
                if (!currGroup.isReviewed() && currCategory == REVIEW_CATEGORY.NEURON_REVIEW) {
                    List<ReviewPoint> pointList = currGroup.getPointList();

                    Object[] cells = new Object[pointList.size()];
                    for (int i = 0; i < pointList.size(); i++) {
                        selectedPoints.add(pointList.get(i).getDisplay());
                        cells[i] = ((NeuronTree) pointList.get(i).getDisplay()).getGUICell();
                    }
                    navigator.updateCellStatus(cells, ReviewTaskNavigator.CELL_STATUS.UNDER_REVIEW);
                }
            }            
        }        
    }
    
    private void clearSelection() {
        List reviewedCells = new ArrayList();
        List normalCells = new ArrayList();

        for (ReviewGroup group: groupList) {
            List<ReviewPoint> pointList = group.getPointList();
            for (ReviewPoint point: pointList) {
                if (point.getDisplay().isReviewed()) 
                    reviewedCells.add(((NeuronTree)point.getDisplay()).getGUICell());
                else                    
                    normalCells.add(((NeuronTree)point.getDisplay()).getGUICell());
            }
        }
        selectedPoints.clear();
        navigator.updateCellStatus(normalCells.toArray(), ReviewTaskNavigator.CELL_STATUS.OPEN);
        navigator.updateCellStatus(reviewedCells.toArray(), ReviewTaskNavigator.CELL_STATUS.REVIEWED);
    }
    
    public void selectBranch(int groupIndex) {
        if (currGroupIndex != -1) {
            ReviewGroup currGroup = groupList.get(currGroupIndex);
            if (currCategory == REVIEW_CATEGORY.NEURON_REVIEW) {
                // clear current review markers
                List<ReviewPoint> pointList = currGroup.getPointList();
                Object[] cells = new Object[pointList.size()];
                for (int i=0; i<pointList.size(); i++) {
                    NeuronTree point = (NeuronTree) pointList.get(i).getDisplay();
                    if (point.isReviewed()) {
                        navigator.updateCellStatus(cells, ReviewTaskNavigator.CELL_STATUS.REVIEWED);
                    } else {
                        navigator.updateCellStatus(cells, ReviewTaskNavigator.CELL_STATUS.OPEN);       
                    }
                    cells[i] = point.getGUICell();
                }
            }
        }
        currGroupIndex = groupIndex;
        ReviewGroup currGroup = groupList.get(currGroupIndex);
        if (currCategory == REVIEW_CATEGORY.NEURON_REVIEW) {            
            selectedPoints.clear();
            List<ReviewPoint> pointList = currGroup.getPointList();
            Object[] cells = new Object[pointList.size()];
            for (int i = 0; i < pointList.size(); i++) {
                selectedPoints.add(pointList.get(i).getDisplay());
                cells[i] = ((NeuronTree) pointList.get(i).getDisplay()).getGUICell();
            }
            navigator.updateCellStatus(cells, ReviewTaskNavigator.CELL_STATUS.UNDER_REVIEW);
        } 
        
        // navigate viewer to starting point
        ReviewPoint startPoint = currGroup.getPointList().get(0);
        gotoPoint(startPoint);
    }
    
    public void selectPoint(PointDisplay point) {
        Object[] cellArray = new Object[]{((NeuronTree)point).getGUICell()};
        selectedPoints.add(point);
        navigator.updateCellStatus(cellArray, ReviewTaskNavigator.CELL_STATUS.UNDER_REVIEW);
    }
    
    public void selectPoint(Long annotationId) {
        PointDisplay point = annotationLookup.get(annotationId);
        selectPoint(point);
    }

    public void loadHistory() {        
        ((TaskReviewTableModel)taskReviewTable.getModel()).clear();
        retrieveTasks();
    }

    private void setupUI() {
        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));

        loadTaskIcon = SimpleIcons.getIcon("open_action.png");
     
        deleteTaskIcon = SimpleIcons.getIcon("delete.png");
        
        // neuron review table        
        JPanel taskPanel = new JPanel();
        taskPanel.setLayout(new BoxLayout(taskPanel,BoxLayout.Y_AXIS));   
        taskPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton createTaskButton = new JButton("");
        createTaskButton.setIcon(SimpleIcons.getIcon("plus-button.png"));
        createTaskButton.setToolTipText("Create a New Task From Current Neuron Review");
        createTaskButton.addActionListener(event -> createNewTask());
        
        JToolBar taskCRUDToolbar = new JToolBar();
        taskCRUDToolbar.setAlignmentX(Component.LEFT_ALIGNMENT);
        taskCRUDToolbar.add(createTaskButton);    
        taskPanel.add(taskCRUDToolbar);
                
        TaskReviewTableModel taskReviewTableModel = new TaskReviewTableModel();
        taskReviewTable = new JTable(taskReviewTableModel);
        taskReviewTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);   
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
                            ((TaskReviewTableModel)taskReviewTable.getModel()).deleteTask(selectedReview, viewRow);
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
        JScrollPane taskTableScroll = new JScrollPane(taskReviewTable);
        taskPanel.add(taskTableScroll);
        
        dendroContainerPanel = new JPanel();
        dendroContainerPanel.setLayout(new BoxLayout(dendroContainerPanel,BoxLayout.Y_AXIS));
       // JScrollPane dendroScroll = new JScrollPane(dendroContainerPanel);
        
        JSplitPane taskWindow = new JSplitPane(JSplitPane.VERTICAL_SPLIT, taskPanel, dendroContainerPanel);
        taskWindow.setDividerLocation(400);
        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        add(taskWindow);
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
        int result = chooser.showSaveDialog(FrameworkAccess.getMainFrame());
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
                log.error("Error saving reviewed points", e);
                JOptionPane.showMessageDialog(null,
                        "Could not write out reviewed points " + exportFile,
                        "Error writing point reviews file",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

   public void markCurrentGroupAsReviewed() {
       selectBranch(currGroupIndex);
       setSelectedAsReviewed(true);
   }
    
     private void reviewGroup(int groupIndex) {
         ReviewGroup group = groupList.get(groupIndex);
         currGroupIndex = groupIndex;

         List<TmViewState> playList = new ArrayList<TmViewState>();
          Object[] guiCells = new Object[group.getPointList().size()];
         if (currCategory==REVIEW_CATEGORY.NEURON_REVIEW) {
             int i=0;
             for (ReviewPoint point : group.getPointList()) {
                 guiCells[i++] = (((NeuronTree)point.getDisplay()).getGUICell());
             }
             navigator.updateCellStatus(guiCells, ReviewTaskNavigator.CELL_STATUS.UNDER_REVIEW);
         }

         for (ReviewPoint point : group.getPointList()) {
             TmViewState sampleLocation = new TmViewState();
             sampleLocation.setCameraFocusX(point.getLocation().getX());
             sampleLocation.setCameraFocusY(point.getLocation().getY());
             sampleLocation.setCameraFocusZ(point.getLocation().getZ());
             sampleLocation.setZoomLevel(point.getZoomLevel());
             sampleLocation.setCameraRotation(point.getRotation());
             playList.add(sampleLocation);
         }
         AnimationEvent event = new AnimationEvent(this,
                 playList, rotationCheckbox.isSelected(),
                 Integer.parseInt(speedSpinner.getText()),
                 Integer.parseInt(numStepsSpinner.getText()));
         ViewerEventBus.postEvent(event);
     }

    /**
     * move the camera to the indicated point in LVV and Horta
     * this is possibly a bit hacky...I followed the example in FilteredAnnList;
     * we use the LVV sample provider to get the sample location, then poke
     * our values in; that's sent to the appropriate Horta acceptor; then
     * since we know that LVV is an acceptor, too, we can just put the altered
     * sample location back into the originator to trigger that move
     */
    private void gotoPoint(ReviewPoint point) {

        // not sure what the try/catch is preventing, but it was in the code I copied
        try {
            ViewEvent cameraEvent = new ViewEvent(this,point.getLocation().getX(),
                    point.getLocation().getY(),
                    point.getLocation().getZ(),
                    point.getZoomLevel(),
                    point.getRotation(),
                    false);

            ViewerEventBus.postEvent(cameraEvent);
        } catch (Exception e) {
            FrameworkAccess.handleException(e);
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

    private List<List<PointDisplay>> generatePlayList (NeuronTree tree) {
        List<NeuronTree> leaves = new ArrayList<NeuronTree>();
        generateLeaves (leaves, tree);
        List<List<PointDisplay>> pathList = new ArrayList<>();
        for (NeuronTree leaf: leaves) {
            pathList.add(leaf.generateRootToLeaf());
        }
        return pathList;
    }

    private void foldTree(List<NeuronTree> children, boolean fold) {
        for (NeuronTree neuronTree: children) {
            neuronTree.getGUICell().setVisible(!fold);
            if (neuronTree.getChildren()!=null && neuronTree.getChildren().size()>0 &&
                    !neuronTree.isFolded()) {
                foldTree(neuronTree.getChildren(), fold);
            }
        }
    }

    private void initSelection(List<List<PointDisplay>> pathList) {
                // generate quick lookup table linking mxCells and branches
        branchLookup = new HashMap<String,Integer>();
        pointLookup = new HashMap<String,PointDisplay>();
        annotationLookup = new HashMap<Long,PointDisplay>();
        for (int i=0; i<pathList.size(); i++) {
            List<PointDisplay> branch = pathList.get(i);
            for (PointDisplay point: branch) {
                String guiCellId = ((NeuronTree)point).getGUICell().getId().toString();
                branchLookup.put(guiCellId,i);
                pointLookup.put(guiCellId,point);
                annotationLookup.put(((NeuronTree)point).getAnnotationId(), point);
            }
        }
    }

    public void createNeuronReview (TmNeuronMetadata neuron, NeuronTree tree) {
        currTask = null;
        // from leaf nodes of neurontree generate branches to play back
        List<List<PointDisplay>> pathList = generatePlayList (tree);

        // generate gui and mxCells for neurontree
        dendroPane = navigator.createGraph(tree, pathList.size(), this.getWidth(), this.getHeight());
        initSelection(pathList);
        addTaskButtons();
        currNeuron = neuron;
        currCategory = REVIEW_CATEGORY.NEURON_REVIEW;

        // add reference between review point and neuronTree, for updates to the GUI
        // when point has been reviewed
        loadPointList(pathList);
        currGroupIndex = 0;
        selectBranch(0);

        repaint();
    }

    private void addTaskButtons() {
        dendroContainerPanel.removeAll();
        taskButtonsPanel1 = new JPanel();
        taskButtonsPanel1.setLayout(new BoxLayout(taskButtonsPanel1, BoxLayout.LINE_AXIS));
        taskButtonsPanel1.setBorder(new EmptyBorder(5, 5, 5, 5));
        taskButtonsPanel1.setAlignmentX(LEFT_ALIGNMENT);
        taskButtonsPanel2 = new JPanel();
        taskButtonsPanel2.setLayout(new BoxLayout(taskButtonsPanel2, BoxLayout.LINE_AXIS));
        taskButtonsPanel2.setBorder(new EmptyBorder(5, 5, 5, 5));
        taskButtonsPanel2.setAlignmentX(LEFT_ALIGNMENT);

        // FLYTHRU TOOLBAR
        flyThroughActionsToolbar = new JToolBar();
        flyThroughActionsToolbar.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton prevBranchButton = new JButton("");
        prevBranchButton.setToolTipText("Previous Neuron Branch");
        prevBranchButton.setIcon(SimpleIcons.getIcon("prev_node.png"));
        prevBranchButton.addActionListener(event -> prevBranch());
        flyThroughActionsToolbar.add(prevBranchButton);

        JButton nextBranchButton = new JButton("");
        nextBranchButton.setToolTipText("Next Branch");
        nextBranchButton.setIcon(SimpleIcons.getIcon("next_node.png"));
        nextBranchButton.addActionListener(event -> nextBranch(false));
        flyThroughActionsToolbar.add(nextBranchButton);

        JButton nextUnreviewedButton = new JButton("");
        nextUnreviewedButton.setToolTipText("Next Unreviewed Branch");
        nextUnreviewedButton.setIcon(SimpleIcons.getIcon("next_item.png"));
        nextUnreviewedButton.addActionListener(event -> nextBranch(true));
        flyThroughActionsToolbar.add(nextUnreviewedButton);

        JButton reviewButton = new JButton("");
        reviewButton.setToolTipText("Mark selected items as reviewed");
        reviewButton.setIcon(SimpleIcons.getIcon("review_completed.png"));
        reviewButton.addActionListener(event -> setSelectedAsReviewed(true));
        reviewButton.setMargin(new Insets(0, 0, 0, 0));
        reviewButton.setHideActionText(true);
        reviewButton.setFocusable(false);
        flyThroughActionsToolbar.add(reviewButton);

        JButton unreviewButton = new JButton("");
        unreviewButton.setToolTipText("Clear selected items from being reviewed");
        unreviewButton.setIcon(SimpleIcons.getIcon("review_cleared.png"));
        unreviewButton.addActionListener(event -> setSelectedAsReviewed(false));
        reviewButton.setMargin(new Insets(0, 0, 0, 0));
        reviewButton.setHideActionText(true);
        reviewButton.setFocusable(false);
        flyThroughActionsToolbar.add(unreviewButton);

        JButton playButton = new JButton("");
        playButton.setToolTipText("Play Branch");
        playButton.setIcon(SimpleIcons.getIcon("play.png"));
        playButton.addActionListener(event -> playBranch());
        flyThroughActionsToolbar.add(playButton);
        taskButtonsPanel1.add(flyThroughActionsToolbar);

        // REGULAR ACTIONS TOOLBAR
        parametersToolbar = new JToolBar();
        parametersToolbar.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel infoPane = new JPanel();
        infoPane.setPreferredSize(new Dimension(100, 50));

        rotationCheckbox = new JCheckBox("Auto Rotation");
        parametersToolbar.add(rotationCheckbox);

        speedSpinner = new JTextField(3);
        speedSpinner.setText("20");
        speedSpinner.setMaximumSize(new Dimension(100, 50));
        parametersToolbar.add(speedSpinner);
        JLabel speedSpinnerLabel = new JLabel("Speed");
        parametersToolbar.add(speedSpinnerLabel);

        numStepsSpinner = new JTextField(3);
        numStepsSpinner.setText("1");
        numStepsSpinner.setMaximumSize(new Dimension(50,50));
        parametersToolbar.add(numStepsSpinner);
        JLabel numStepsSpinnerLabel = new JLabel("Smoothness");
        parametersToolbar.add(numStepsSpinnerLabel);

        taskButtonsPanel2.add(parametersToolbar);

        dendroContainerPanel.add(taskButtonsPanel1);
        dendroContainerPanel.add(taskButtonsPanel2);
        dendroContainerPanel.add(dendroPane);

        dendroContainerPanel.validate();
        dendroContainerPanel.repaint();
    }

    public void setSelectedAsReviewed(boolean review) {
        if (currGroupIndex!=-1 && !selectMode) {
            ReviewGroup currGroup = groupList.get(currGroupIndex);
            currGroup.setReviewed(true);
        }
        Object[] guiCells = new Object[selectedPoints.size()];
        List<Long> annotationList = new ArrayList<>();
        for (int i=0; i<selectedPoints.size(); i++) {
             NeuronTree point = (NeuronTree)selectedPoints.get(i);
             point.setReviewed(review);
             annotationList.add(point.getAnnotationId());
             guiCells[i] = point.getGUICell();
             if (review)
                 TmModelManager.getInstance().getCurrentReviews().addReviewedAnnotation(point.getAnnotationId());
             else
                 TmModelManager.getInstance().getCurrentReviews().removeReviewedAnnotation(point.getAnnotationId());
        }
        if (review) {
            navigator.updateCellStatus(guiCells, ReviewTaskNavigator.CELL_STATUS.REVIEWED);
        } else {
            navigator.updateCellStatus(guiCells, ReviewTaskNavigator.CELL_STATUS.OPEN);
        }
        Collection<TmGeoAnnotation> realAnnList = new ArrayList<>();
        NeuronManager manager = NeuronManager.getInstance();
        for (Long annId: annotationList) {
            realAnnList.add(manager.getGeoAnnotationFromID(currNeuron.getId(), annId));
        }
        NeuronBranchReviewedEvent branchReviewedEvent = new NeuronBranchReviewedEvent(this,
                realAnnList);
        ViewerEventBus.postEvent(branchReviewedEvent);

        // update persistence
        if (currTask!=null) {
            List<TmReviewItem> groups = currTask.getReviewItems();
            for (TmReviewItem group : groups) {
                if (!selectMode && currGroupIndex!=-1 && Integer.parseInt(group.getName())==currGroupIndex) {
                    ((TaskReviewTableModel)taskReviewTable.getModel()).updateTask(currTask);
                    this.createNewTask();
                    break;
                } else {
                    // need to add logic for individual points
                }
            }
        }
        clearSelection();
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
                if (i==0)
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
        int result = chooser.showOpenDialog(FrameworkAccess.getMainFrame());
        if (result == JFileChooser.APPROVE_OPTION) {
            File pointFile = chooser.getSelectedFile();

            Map<String,Object> reviewData = null;
            Map<String,Object> pointData = null;
            try {
                ObjectMapper mapper = new ObjectMapper();
                reviewData = mapper.readValue(new FileInputStream(pointFile), new TypeReference<Map<String,Object>>(){});
            } catch (IOException e) {
                log.error("Error reading point file "+pointFile, e);
                JOptionPane.showMessageDialog(null,
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
                            JOptionPane.showMessageDialog(null,
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
            TaskReviewTableModel tableModel = (TaskReviewTableModel)taskReviewTable.getModel();
            for (TmReviewTask task: reviewTasks) {
                String ref = task.getWorkspaceRef();
                ref = ref.replace("TmWorkspace#","");
                // cheat since I didn't want to update the model for a patch
                if (TmModelManager.getInstance().getCurrentWorkspace() != null &&
                        Long.parseLong(ref) == TmModelManager.getInstance().getCurrentWorkspace().getId()) {
                    tableModel.addReviewTask(task);
                }
            }
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        return reviewTasks;
    }

    @Subscribe
    public void createNeuronReview (CreateNeuronReviewEvent event) {
        TmNeuronMetadata neuron = event.getNeuron();
        if (neuron!=null) {
            NeuronTree tree = generateNeuronTreeForReview(neuron.getId());
            createNeuronReview(neuron, tree);
        }
        navigator.addMouseListener(this);
    }

    public NeuronTree generateNeuronTreeForReview(Long neuronId) {
        NeuronManager neuronManager = NeuronManager.getInstance();
        TmNeuronMetadata neuron = neuronManager.getNeuronFromNeuronID(neuronId);
        TmGeoAnnotation rootAnnotation = neuron.getFirstRoot();
        if (rootAnnotation!=null) {
            NeuronTree rootNode = createNeuronTreeNode(null, rootAnnotation);
            exploreNeuronBranches(rootNode, neuron, rootAnnotation);
            return rootNode;
        }
        return null;
    }

    void exploreNeuronBranches (NeuronTree node, TmNeuronMetadata neuron, TmGeoAnnotation currVertex) {
        List<TmGeoAnnotation> children = neuron.getChildrenOfOrdered(currVertex);

        // now start new branches for each of the other children
        for (TmGeoAnnotation child: children) {
            NeuronTree childNode = createNeuronTreeNode(node, child);
            exploreNeuronBranches(childNode, neuron, child);
        }
    }

    NeuronTree createNeuronTreeNode(NeuronTree parentNode, TmGeoAnnotation annotation) {
        TileFormat tileFormat = TmModelManager.getInstance().getTileServer().getLoadAdapter().getTileFormat();
        Vec3 tempLocation =  tileFormat.micronVec3ForVoxelVec3Centered(
                new Vec3(annotation.getX(), annotation.getY(), annotation.getZ()));
        NeuronTree childNode = new NeuronTree(parentNode, tempLocation, annotation.getId());
        if (parentNode!=null)
            parentNode.addChild(childNode);
        return childNode;
    }

    /**
     * loads the review task from the review table into the dendrogram view.
     * @param reviewTask
     */
    public void loadReviewTask (TmReviewTask reviewTask) {
        if (dendroContainerPanel!=null) {
            dendroContainerPanel.removeAll();
        }
        List<TmReviewItem> itemList = reviewTask.getReviewItems();
        if (itemList.size() > 0 && reviewTask.getCategory().equals(REVIEW_CATEGORY.NEURON_REVIEW.toString())) {
            TmNeuronReviewItem sample = (TmNeuronReviewItem) itemList.get(0);
            NeuronTree root = generateNeuronTreeForReview(sample.getNeuronId());
            List<List<PointDisplay>> pathList = generatePlayList(root);

            loadPointList(pathList);

            // generate gui and mxCells for neurontree
            dendroPane = navigator.createGraph(root, pathList.size(), this.getWidth(), this.getHeight() / 2);
            initSelection(pathList);
            addTaskButtons();

            currNeuron = NeuronManager.getInstance().getNeuronFromNeuronID(sample.getNeuronId());
            currCategory = REVIEW_CATEGORY.NEURON_REVIEW;

            // awkwardly walk through the tree using the stored path lists to mark branches as reviewed
            for (TmReviewItem item : itemList) {
                if (item.isReviewed()) {
                    TmNeuronReviewItem neuronItem = (TmNeuronReviewItem) item;
                    Long pathRootAnnotationId = Long.parseLong((String) neuronItem.getReviewItems().get(0));

                    for (int i = 0; i < groupList.size(); i++) {
                        ReviewGroup group = groupList.get(i);
                        currGroupIndex = i;
                        ReviewPoint pathRoot = group.getPointList().get(0);
                        if (((NeuronTree) pathRoot.getDisplay()).getAnnotationId().longValue() == pathRootAnnotationId.longValue()
                                && neuronItem.getReviewItems().size() == group.getPointList().size()) {
                            for (ReviewPoint point : group.getPointList()) {
                                selectedPoints.add(point.getDisplay());
                            }
                            setSelectedAsReviewed(true);
                        }
                        selectedPoints.clear();
                    }
                }
            }

            currGroupIndex = 0;
            selectBranch(0);

            repaint();
        }
        currTask = reviewTask;
        navigator.addMouseListener(this);
    }

    /**
     * takes current point lists and generates a ReviewTask object for persistence.
     */
    public void createNewTask() {
        currTask = new TmReviewTask();
        if (currNeuron == null) {
            // user clicked button with no neuron/task loaded
            return;
        }
        currTask.setWorkspaceRef(currNeuron.getWorkspaceRef().toString());
        currTask.setCategory(currCategory.toString());
        currTask.setOwnerKey(AccessManager.getSubjectKey());
        if (currCategory==REVIEW_CATEGORY.NEURON_REVIEW) {
            currTask.setTitle(currNeuron.getName());
        }
        for (int i=0; i<groupList.size(); i++) {
            ReviewGroup branch = groupList.get(i);
             TmReviewItem reviewItem = null;
             switch (currCategory) {
                 case NEURON_REVIEW:
                     reviewItem = new TmNeuronReviewItem();
                     ((TmNeuronReviewItem)reviewItem).setNeuronId(currNeuron.getId());
                     reviewItem.setName(Integer.toString(i));
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
                                "Owner",
                                "Date",
                                "% Complete",
                                "", ""};
        List<List<Object>> data = new ArrayList<List<Object>>();

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return data.size();
        }

        public void clear() {
            data = new ArrayList<List<Object>>();
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        // get tmReviewTask stored at end of row
        public TmReviewTask getReviewTaskAtRow (int row) {
            return (TmReviewTask)data.get(row).get(7);
        }

        public Object getValueAt(int row, int col) {
            return data.get(row).get(col);
        }

        public Class getColumnClass(int c) {
            return (getValueAt(0, c)==null?String.class:getValueAt(0,c).getClass());
        }

        public void addReviewTask(TmReviewTask reviewTask) {
            List row = new ArrayList<Object>();
            row.add(reviewTask.getTitle());
            row.add(reviewTask.getCategory());
            row.add(reviewTask.getOwnerKey());
            row.add(reviewTask.getCreationDate());
            float completed = 0;
            if (reviewTask.getReviewItems().size() > 0) {
                for (TmReviewItem reviewItem : reviewTask.getReviewItems()) {
                    completed += reviewItem.isReviewed() ? 1 : 0;
                }
                row.add(100*completed / reviewTask.getReviewItems().size());
            }
            row.add(loadTaskIcon);
            row.add(deleteTaskIcon);
            row.add(reviewTask);

            data.add(row);
            this.fireTableDataChanged();
        }

        public void deleteTask(TmReviewTask task, int deletedRow) {
             try {
                 TiledMicroscopeDomainMgr persistenceMgr = TiledMicroscopeDomainMgr.getDomainMgr();
                 persistenceMgr.remove(task);
                 data.remove(deletedRow);
                 fireTableRowsDeleted(deletedRow, deletedRow);
             } catch (Exception ex) {
                 Exceptions.printStackTrace(ex);
             }
        }

        public void updateTask(TmReviewTask task) {
             try {
                 for (int i=0;i<data.size(); i++) {
                     TmReviewTask dataTask = (TmReviewTask)data.get(i).get(7);
                     if (dataTask.getId()==task.getId()) {
                         TiledMicroscopeDomainMgr persistenceMgr = TiledMicroscopeDomainMgr.getDomainMgr();
                         persistenceMgr.remove(task);
                         data.remove(i);
                         fireTableRowsDeleted(i, i);
                     }
                 }



             } catch (Exception ex) {
                 Exceptions.printStackTrace(ex);
             }
        }
    }
}
