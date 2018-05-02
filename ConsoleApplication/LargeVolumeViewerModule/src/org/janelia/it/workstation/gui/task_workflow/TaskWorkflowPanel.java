package org.janelia.it.workstation.gui.task_workflow;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;

import org.janelia.console.viewerapi.SampleLocation;
import org.janelia.console.viewerapi.SynchronizationHelper;
import org.janelia.console.viewerapi.Tiled3dSampleLocationProviderAcceptor;
import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.gui.keybind.ShortcutTextField;
import org.janelia.it.workstation.browser.gui.support.MouseHandler;
import org.janelia.it.workstation.gui.large_volume_viewer.ComponentUtil;
import org.janelia.it.workstation.gui.large_volume_viewer.dialogs.NeuronGroupsDialog;
import static org.janelia.it.workstation.gui.large_volume_viewer.dialogs.NeuronGroupsDialog.PROPERTY_CROSSCHECK;
import static org.janelia.it.workstation.gui.large_volume_viewer.dialogs.NeuronGroupsDialog.PROPERTY_RADIUS;
import static org.janelia.it.workstation.gui.large_volume_viewer.dialogs.NeuronGroupsDialog.PROPERTY_READONLY;
import static org.janelia.it.workstation.gui.large_volume_viewer.dialogs.NeuronGroupsDialog.PROPERTY_VISIBILITY;
import org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerLocationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This panel contains the UI for a task workflow similar to what I implemented
 * in Neu3 for Fly EM.  Users will be given a list of tasks to complete.
 *
 * For the first implementation, the tasks will be a list of points.  If we need
 * something more sophisticated later, I'll expand it into generic tasks as in Neu3.
 */
public class TaskWorkflowPanel extends JPanel {
    private final TaskDataSourceI dataSource;

    private String[] reviewOptions;
    
    List<ReviewPoint> pointList;
    private JTable pointTable;
    private PointTableModel pointModel = new PointTableModel();

    private static final Logger log = LoggerFactory.getLogger(TaskWorkflowPanel.class);

    public TaskWorkflowPanel(TaskDataSourceI dataSource) {
        this.dataSource = dataSource;

        setupUI();
    }

    private void setupUI() {

        setLayout(new GridBagLayout());
        setBorder(new EmptyBorder(5, 5, 5, 5));

        GridBagConstraints cTop = new GridBagConstraints();
        cTop.gridx = 0;
        cTop.gridy = 0;
        cTop.weightx = 1.0;
        cTop.weighty = 0.0;
        cTop.anchor = GridBagConstraints.PAGE_START;
        cTop.fill = GridBagConstraints.HORIZONTAL;
        cTop.insets = new Insets(10, 0, 0, 0);
        add(new JLabel("Point review workflow", JLabel.CENTER));


        // point table
        pointTable = new JTable(pointModel);
        pointTable.addMouseListener(new MouseHandler() {
            @Override
            protected void singleLeftClicked(MouseEvent me) {
                if (me.isConsumed()) {
                    return;
                }
                JTable table = (JTable) me.getSource();
                int viewRow = table.rowAtPoint(me.getPoint());
                if (viewRow >= 0) {
                    // we don't do anything on click in the boolean column (it'll
                    //  toggle by itself); on a click in x, y, z columns, go to
                    //  the point
                    int viewColumn = table.columnAtPoint(me.getPoint());
                    int modelColumn = pointTable.convertColumnIndexToModel(viewColumn);
                    if (modelColumn != 4) {
                        selectGoto(viewRow);
                    }
                }
                me.consume();
            }
        });
        pointTable.setRowHeight(20);
        pointTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableColumn col = pointTable.getColumnModel().getColumn(4);
        col.setCellEditor(new ReviewEditor()); 

        JScrollPane scrollPane = new JScrollPane(pointTable);
        pointTable.setFillsViewportHeight(true);

        // table should take available space
        GridBagConstraints cTable = new GridBagConstraints();
        cTable.gridx = 0;
        cTable.gridy = GridBagConstraints.RELATIVE;
        cTable.weightx = 1.0;
        cTable.weighty = 1.0;
        cTable.anchor = GridBagConstraints.PAGE_START;
        cTable.fill = GridBagConstraints.BOTH;
        add(scrollPane, cTable);




        // I want most of the components to stack vertically;
        //  components should fill or align left as appropriate
        GridBagConstraints cVert = new GridBagConstraints();
        cVert.gridx = 0;
        cVert.gridy = GridBagConstraints.RELATIVE;
        cVert.anchor = GridBagConstraints.PAGE_START;
        cVert.fill = GridBagConstraints.HORIZONTAL;
        cVert.weighty = 0.0;



        // task transition buttons (next, previous, etc)
        // this isn't ready yet
        JPanel taskButtonsPanel = new JPanel();
        taskButtonsPanel.setLayout(new BoxLayout(taskButtonsPanel, BoxLayout.LINE_AXIS));
        taskButtonsPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        add(taskButtonsPanel, cVert);

        JButton nextButton = new JButton("Next unreviewed");
        nextButton.addActionListener(event -> onNextButton());
        taskButtonsPanel.add(nextButton);


        // workflow management buttons: load, done (?)
        JPanel workflowButtonsPanel = new JPanel();
        workflowButtonsPanel.setLayout(new BoxLayout(workflowButtonsPanel, BoxLayout.LINE_AXIS));
        workflowButtonsPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        add(workflowButtonsPanel, cVert);

        workflowButtonsPanel.add(Box.createHorizontalGlue());

        JButton loadButton = new JButton("Load point list...");
        loadButton.addActionListener(event -> onLoadButton());
        workflowButtonsPanel.add(loadButton);
        
        JButton saveButton = new JButton("Save reviewed list...");
        saveButton.addActionListener(event -> onSaveButton());
        workflowButtonsPanel.add(saveButton);
       

        /*
        // not sure I need this: it'll push content up so it
        //  doesn't stretch; so far, it's fine without it, but
        //  I haven't checked the appearance if the user undocks
        //  the window and lets it get big
        GridBagConstraints cBottom = new GridBagConstraints();
        cBottom.gridx = 0;
        cBottom.gridy = GridBagConstraints.RELATIVE;
        cBottom.anchor = GridBagConstraints.PAGE_START;
        cBottom.fill = GridBagConstraints.BOTH;
        cBottom.weighty = 1.0;
        add(Box.createVerticalGlue(), cBottom);
        */

    }

    /**
     * given a list of points, start the workflow from scratch
     */
    private void startWorkflow(List<ReviewPoint> pointList) {
        pointModel.clear();
        for (ReviewPoint point: pointList) {
            pointModel.addPoint(point);
        }

        // this shouldn't be needed, but Windows doesn't redraw without it:
        pointModel.fireTableDataChanged();
    }

    private void onLoadButton() {
        // don't load list if no data is loaded
        if (dataSource.getAnnotationModel() == null) {
            JOptionPane.showMessageDialog(ComponentUtil.getLVVMainWindow(),
                    "You can't load a point file until a workspace is opened!",
                    "Can't read point file",
                    JOptionPane.ERROR_MESSAGE);

            return;
        }

        pointList = readPointFile();
        startWorkflow(pointList);

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
                    String review = (String) pointTable.getModel().getValueAt(i, 3);
                    if (review != null) {
                        Map pointReview = new HashMap<String,String>();
                        pointReview.put("x", pointList.get(i).getLocation().getX());
                        pointReview.put("y", pointList.get(i).getLocation().getY());
                        pointReview.put("z", pointList.get(i).getLocation().getZ());
                        pointReview.put("reviewNote", review);
                        pointReviews.add(pointReview);
                    }                    
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

    /**
     * the next button brings you to the next unreviewed point
     */
    private void onNextButton() {
        if (hasPoints()) {
            // note: "next" is relative to view state, not model!
            int viewRow = pointTable.getSelectedRow();
            int startRow;
            if (viewRow >= 0) {
                // selection exists
                startRow = viewRow + 1;
                if (startRow >= pointModel.getRowCount()) {
                    startRow = 0;
                }
            } else {
                // no selection
                startRow = 0;
            }
            // working from start row, find the next unreviewed row:
            int testRow = startRow;
            while (pointModel.isReviewed(pointTable.convertRowIndexToModel(testRow))) {
                testRow++;
                if (testRow >= pointModel.getRowCount()) {
                    testRow = 0;
                }
                if (testRow == startRow) {
                    JOptionPane.showMessageDialog(ComponentUtil.getLVVMainWindow(),
                            "All points have been reviewed.",
                            "Nothing left",
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
            }
            selectGoto(testRow);
        }
    }

    /**
     * select the given table row and go to the point it contains
     */
    private void selectGoto(int viewRow) {
        pointTable.getSelectionModel().setSelectionInterval(viewRow, viewRow);

        int modelRow = pointTable.convertRowIndexToModel(viewRow);
        gotoPoint(pointList.get(viewRow));
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
            sampleLocation.setInterpolate(point.getInterpolate());
            
            
            // the order you do these determines which will be at front when you're done;
            //  do LVV first so it matches the behavior from FilteredAnnList

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

    /**
     * pop a file chooser; load and parse a json list of points
     *
     * file format:
     *      -- one point per line = whitespace-delimited x, y, z
     * (preferred) -- one point per line = [x, y, z] (allowed, matches "copy
     * coord to clipboard" format) -- blank lines allowed -- comment lines start
     * with #
     */
    private List<ReviewPoint> readPointFile() {

        List<ReviewPoint> pointList = new ArrayList<>();

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
                return pointList;
            }
            
            if (reviewData!=null) {
                List pointGroups = (List)reviewData.get("reviewGroups");
                if (pointGroups != null && pointGroups.size() > 0) {
                    for (int i=0; i<pointGroups.size(); i++) {
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
        return pointList;
    }

    /**
     * are there any points loaded?
     */
    private boolean hasPoints() {
        return pointModel.getRowCount() > 0;
    }

    /**
     * this method is called when the top component is destroyed
     */
    public void close() {

        // do clean up here, which I expect I will need
    }

    class ReviewEditor extends AbstractCellEditor implements TableCellEditor {
        // This is the component that will handle the editing of the cell value

        JComponent component;
        int cellType;

        // This method is called when a cell value is edited by the user.
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int rowIndex, int vColIndex) {
            component = new JComboBox(reviewOptions);
            for (int i = 0; i < reviewOptions.length; i++) {
                if (value == reviewOptions[i]) {
                    ((JComboBox) component).setSelectedIndex(i);
                    break;
                }
            }
            return ((JComboBox) component);
        }

        // This method is called when editing is completed.
        // It must return the new value to be stored in the cell.
        public Object getCellEditorValue() {
            return ((JComboBox) component).getSelectedItem();
        }

        /**
         * @return the reviewOptions
         */
        public String[] getReviewOptions() {
            return reviewOptions;
        }

        /**
         * @param reviewOptions the reviewOptions to set
         */
        public void setReviewOptions(String[] options) {
            reviewOptions = options;
        }
    }
}

class ReviewPoint {
    private Vec3 location;
    private float[] rotation;
    private float zoomLevel;
    private boolean interpolate;

    public Vec3 getLocation() {
        return location;
    }
    
    public void setLocation(Vec3 location) {
        this.location = location;
    }

    public float[] getRotation() {
        return rotation;
    }

    public void setRotation(float[] rotation) {
        this.rotation = rotation;
    }
    
    public float getZoomLevel() {
        return zoomLevel;
    }

    public void setZoomLevel(float zoomLevel) {
        this.zoomLevel = zoomLevel;
    }
    
    public boolean getInterpolate() {
        return interpolate;
    }
    
    public void setInterpolate(boolean interpolate) {
        this.interpolate = interpolate;
    }
}


class PointTableModel extends AbstractTableModel {
    private String[] columnNames = {"x (µm)", "y (µm)", "z (µm)", "Rotation", "Review Note"};

    private List<ReviewPoint> points = new ArrayList<>();
    private List<String> notes = new ArrayList<>();

    public void clear() {
        points.clear();
        notes.clear();
    }

    public void addPoint(ReviewPoint point) {
        points.add(point);
        notes.add("");
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public int getRowCount() {
        return points.size();
    }

    @Override
    public Object getValueAt(int row, int column) {
        switch (column) {
            case 0:
                return points.get(row).getLocation().getX();
            case 1:
                return points.get(row).getLocation().getY();
            case 2:
                return points.get(row).getLocation().getZ();
            case 3:
                float[] rotation = points.get(row).getRotation();
                return "[" + rotation[0] + "," +  rotation[1] + "," + 
                        rotation[2] + "," + rotation[3] + "]";
            case 4:
                return notes.get(row);
            default:
                return null;
        }
    }

    public boolean isReviewed(int row) {
        String reviewed = (String)getValueAt(row, 4);
        if (reviewed!=null && reviewed.length()>0) 
            return true;
        return false;
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
        switch (column) {
            case 4:
                notes.get(row);
            default:
                // nothing
        }
    }

    @Override
    public Class<?> getColumnClass(int column) {
        switch (column) {
            case 0:
            case 1:
            case 2:
                return double.class;
            case 3:
                return float[].class;
            case 4:
                return String.class;                
            default:
                return Object.class;
        }
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return column == 4;
    }

}