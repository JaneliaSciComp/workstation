package org.janelia.it.workstation.gui.task_workflow;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import org.janelia.console.viewerapi.SampleLocation;
import org.janelia.console.viewerapi.SynchronizationHelper;
import org.janelia.console.viewerapi.Tiled3dSampleLocationProviderAcceptor;
import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.gui.support.MouseHandler;
import org.janelia.it.workstation.gui.large_volume_viewer.ComponentUtil;
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

    private JTable pointTable;
    private PointTableModel pointModel = new PointTableModel();

    private static final Logger log = LoggerFactory.getLogger(TaskWorkflowPanel.class);

    public TaskWorkflowPanel(TaskDataSourceI dataSource) {
        this.dataSource = dataSource;

        setupUI();
    }

    private void setupUI() {

        setLayout(new GridBagLayout());

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
                    int modelRow = pointTable.convertRowIndexToModel(viewRow);

                    int viewColumn = table.columnAtPoint(me.getPoint());
                    int modelColumn = pointTable.convertColumnIndexToModel(viewColumn);

                    // we don't do anything on click in the boolean column (it'll
                    //  toggle by itself); on a click in x, y, z columns, go to
                    //  the point
                    if (modelColumn != 3) {
                        gotoPoint((double) pointModel.getValueAt(modelRow, 0),
                            (double) pointModel.getValueAt(modelRow, 1),
                            (double) pointModel.getValueAt(modelRow, 2));
                    }
                }
                me.consume();
            }
        });

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
        add(taskButtonsPanel, cVert);

        JButton nextButton = new JButton("Next");
        nextButton.addActionListener(event -> onNextButton());
        taskButtonsPanel.add(nextButton);


        // workflow management buttons: load, done (?)
        JPanel workflowButtonsPanel = new JPanel();
        workflowButtonsPanel.setLayout(new BoxLayout(workflowButtonsPanel, BoxLayout.LINE_AXIS));
        add(workflowButtonsPanel, cVert);

        workflowButtonsPanel.add(Box.createHorizontalGlue());

        JButton loadButton = new JButton("Load point list...");
        loadButton.addActionListener(event -> onLoadButton());
        workflowButtonsPanel.add(loadButton);

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
    private void startWorkflow(List<Vec3> pointList) {
        pointModel.clear();
        for (Vec3 point: pointList) {
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

        List<Vec3> pointList = readPointFile();
        startWorkflow(pointList);

        log.info("Loaded point file " + "my point file");
    }

    /**
     * the next button brings you to the next unreviewed point
     */
    private void onNextButton() {

        System.out.println("onNextButton()");

        if (hasPoints()) {

            int viewRow = pointTable.getSelectedRow();
            if (viewRow >= 0) {
                // selection exists; find it
                int modelRow = pointTable.convertRowIndexToModel(viewRow);

            } else {
                // no selection = go to first unreviewed point
            }



        }
    }

    /**
     * move the camera to the indicated point in LVV and Horta
     */
    private void gotoPoint(double x, double y, double z) {

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
            sampleLocation.setFocusUm(x, y, z);

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
     * pop a file chooser; load and parse a point file; return list of points
     *
     * file format:
     *      -- one point per line = whitespace-delimited x, y, z (preferred)
     *      -- one point per line = [x, y, z] (allowed, matches "copy coord to clipboard" format)
     *      -- blank lines allowed
     *      -- comment lines start with #
     */
    private List<Vec3> readPointFile() {

        List<Vec3> pointList = new ArrayList<>();

        // dialog to get file
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose point file");
        chooser.setMultiSelectionEnabled(false);
        int result = chooser.showOpenDialog(FrameworkImplProvider.getMainFrame());
        if (result == JFileChooser.APPROVE_OPTION) {
            File pointFile = chooser.getSelectedFile();

            List<String> lines = null;
            try {
                lines = Files.readAllLines(pointFile.toPath(), Charset.defaultCharset());
            }
            catch (IOException e) {
                JOptionPane.showMessageDialog(ComponentUtil.getLVVMainWindow(),
                    "Could not read file " + pointFile,
                    "Error reading point file",
                    JOptionPane.ERROR_MESSAGE);
                return pointList;
            }

            int nerrors = 0;
            int npoints = 0;
            for (String line: lines) {
                line = line.trim();

                if (line.length() == 0 || line.startsWith("#")) {
                    // if blank or starts with #, do nothing
                    continue;
                } else {
                    // to allow the [x, y, z] format from "copy coord to clipboard",
                    //  we need only remove the [,] characters and it becomes whitespace delimited
                    line = line.replace("[", "");
                    line = line.replace(",", "");
                    line = line.replace("]", "");

                    String[] items = line.split("\\s+");
                    if (items.length != 3) {
                        nerrors++;
                        continue;
                    }

                    try {
                        Vec3 point = new Vec3(Double.parseDouble(items[0]),
                            Double.parseDouble(items[1]), Double.parseDouble(items[2]));
                        pointList.add(point);
                    }
                    catch (NumberFormatException e) {
                        nerrors++;
                        continue;
                    }
                    npoints++;
                }
            }

            // if any errors, report number of errors and successes
            if (nerrors > 0) {
                JOptionPane.showMessageDialog(ComponentUtil.getLVVMainWindow(),
                    "Not all lines in point file could be parsed; " + npoints + " points parsed with " + nerrors + " errors.",
                    "Errors parsing point file",
                    JOptionPane.ERROR_MESSAGE);
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
}


class PointTableModel extends AbstractTableModel {
    private String[] columnNames = {"x (µm)", "y (µm)", "z (µm)", "reviewed"};

    private List<Vec3> points = new ArrayList<>();
    private List<Boolean> status = new ArrayList<>();

    public void clear() {
        points.clear();
        status.clear();
    }

    public void addPoint(Vec3 point) {
        points.add(point);
        status.add(false);
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
                return points.get(row).getX();
            case 1:
                return points.get(row).getY();
            case 2:
                return points.get(row).getZ();
            case 3:
                return status.get(row);
            default:
                return null;
        }
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
        switch (column) {
            case 0:
                points.get(row).setX((double) value);
            case 1:
                points.get(row).setY((double) value);
            case 2:
                points.get(row).setZ((double) value);
            case 3:
                status.set(row, (Boolean) value);
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
                return Boolean.class;
            default:
                return Object.class;
        }
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return column == 3;
    }

}