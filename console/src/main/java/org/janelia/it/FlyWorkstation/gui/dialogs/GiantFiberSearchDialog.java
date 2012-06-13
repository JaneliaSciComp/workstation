package org.janelia.it.FlyWorkstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.*;
import java.util.concurrent.Callable;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import org.janelia.it.FlyWorkstation.api.entity_model.management.EntitySelectionModel;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.FacadeManager;
import org.janelia.it.FlyWorkstation.gui.framework.outline.EntityOutline;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntity;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.annotation.MaskAnnotationDataManager;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 3/13/12
 * Time: 1:40 PM
 */
public class GiantFiberSearchDialog extends ModalDialog {

    private static final String INTENSITY_TYPE="Intensity";
    private static final String DISTRIBUTION_TYPE="Distribution";
    private static final String GLOBAL = "Global";

    private static final String GIANT_FIBER_FOLDER_NAME="GiantFiber";

    private RootedEntity outputFolder;

    DefaultTableModel tableModel;

    private static final String COMPUTE_TYPE_AND = "And";
    private static final String COMPUTE_TYPE_OR = "Or";

    private String computeType=COMPUTE_TYPE_AND;

    private static final String[] filterTableColumnNames = {
            "Include",
            "Compartment",
            "Description",
            "Filter Type",
            "Min",
            "Max",
            "Lines"
    };

    private static final int FT_INDEX_INCLUDE=0;
    private static final int FT_INDEX_COMPARTMENT=1;
    private static final int FT_INDEX_DESCRIPTION=2;
    private static final int FT_INDEX_FILTERTYPE=3;
    private static final int FT_INDEX_MIN=4;
    private static final int FT_INDEX_MAX=5;
    private static final int FT_INDEX_LINES=6;

    private final JPanel mainPanel;
    private final JPanel currentSetNamePanel;
    private final JPanel computeTypePanel;
    private final MinMaxSelectionRow globalMinMaxPanel;
    private final JTable filterTable;
    private final JScrollPane filterTableScrollPane;
    private final JPanel savePanel;
    private final JPanel statusPane;

    private final JLabel statusLabel;
    private final JLabel currentSetNameLabel;
    private final JTextField currentSetTextField;
    MaskAnnotationDataManager maskManager=new MaskAnnotationDataManager();

    private final SimpleWorker quantifierLoaderWorker;

    private final Map<String, Map<String, MinMaxModel>> filterSetMap=new HashMap<String, Map<String, MinMaxModel>>();
    private final Map<String, MinMaxSelectionRow> minMaxRowMap=new HashMap<String, MinMaxSelectionRow>();

    static boolean quantifierDataIsLoading=false;

    static protected Map<Long, Map<String,String>> sampleInfoMap=null;
    static protected Map<Long, List<Double>> quantifierInfoMap=null;

    static protected Map<Long, Map<String, Double>> intensityScoreMap=null;
    static protected Map<Long, Map<String, Double>> distributionScoreMap=null;

    static protected Map<Long, Map<String, Double>> intensityPercentileMap=null;
    static protected Map<Long, Map<String, Double>> distributionPercentileMap=null;

    List<String> compartmentAbbreviationList;
    boolean currentSetInitialized=false;
    final List<Boolean> currentListModified = new ArrayList<Boolean>();
    Set<Long> membershipSampleSet;

    public class PercentileScore implements Comparable {

        public Long sampleId;
        public Double score;

        @Override
        public int compareTo(Object o) {
            PercentileScore other=(PercentileScore)o;
            if (score > other.score) {
                return 1;
            } else if (score < other.score) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    private class MinMaxSelectionRow extends JPanel implements ActionListener {
        Boolean include;
        String abbreviation;
        String description;
        JRadioButton intensityButton;
        JRadioButton distributionButton;
        ButtonGroup buttonGroup;
        JTextField minText;
        JTextField maxText;
        JTextField lineCountText;
        JButton applyButton;

        public MinMaxSelectionRow(String abbreviation, String description) {
            include=true;
            this.abbreviation=abbreviation;
            this.description=description;
            JLabel abbreviationLabel=new JLabel();
            abbreviationLabel.setText(abbreviation);
            JLabel descriptionLabel=new JLabel();
            descriptionLabel.setText(description);
            add(abbreviationLabel);
            add(descriptionLabel);
            intensityButton = new JRadioButton(INTENSITY_TYPE);
            intensityButton.setActionCommand(INTENSITY_TYPE);
            distributionButton = new JRadioButton(DISTRIBUTION_TYPE);
            distributionButton.setActionCommand(DISTRIBUTION_TYPE);
            buttonGroup = new ButtonGroup();
            buttonGroup.add(intensityButton);
            buttonGroup.add(distributionButton);
            intensityButton.setSelected(true);
            intensityButton.addActionListener(this);
            distributionButton.addActionListener(this);
            add(intensityButton);
            add(distributionButton);
            minText=new JTextField(5);
            minText.setText(new Double(0.0).toString());
            maxText=new JTextField(5);
            maxText.setText(new Double(100.0).toString());
            add(minText);
            add(maxText);
            lineCountText=new JTextField(7);
            if (!abbreviation.equals(GLOBAL)) {
                add(lineCountText);
            }
            applyButton=new JButton();
            applyButton.setText("Apply");
            applyButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    applyGlobalSettings();
                }
            });
            add(applyButton);
        }

        public void applyGlobalSettings() {
            MinMaxModel globalState = getModelState();
            for (int rowIndex=0;rowIndex<compartmentAbbreviationList.size();rowIndex++) {
                String rowKey=compartmentAbbreviationList.get(rowIndex);
                MinMaxSelectionRow compartmentRow=minMaxRowMap.get(rowKey);
                MinMaxModel state=compartmentRow.getModelState();
                if (currentListModified.get(rowIndex)) {
                    // Check to see if change reverts to unmodified
                    if (state.equals(globalState)) {
                        currentListModified.set(rowIndex, false);
                    }
                } else {
                    state.min=globalState.min;
                    state.max=globalState.max;
                    state.type=globalState.type;
                    compartmentRow.setModelState(state);
                }
            }
            refreshCompartmentTable();
        }

        public void actionPerformed(ActionEvent e) {
            String actionString=e.getActionCommand();
            if (actionString.equals(INTENSITY_TYPE)) {
                setStatusMessage(abbreviation+": INTENSITY type selected");
            } else if (actionString.equals(DISTRIBUTION_TYPE)) {
                setStatusMessage(abbreviation+": DISTRIBUTION type selected");
            }
            applyGlobalSettings();
            return;
        }

        public Boolean getInclude() {
            return include;
        }

        public String getDescription() {
            return description;
        }

        public String getAbbreviation() {
            return abbreviation;
        }

        public void setLineCount(Long lineCount) {
            lineCountText.setText(lineCount.toString());
        }

        public void setModelState(MinMaxModel model) {
            minText.setText(model.min.toString());
            maxText.setText(model.max.toString());
            if (model.type.equals(INTENSITY_TYPE)) {
                intensityButton.setSelected(true);
            } else if (model.type.equals(DISTRIBUTION_TYPE)) {
                distributionButton.setSelected(true);
            }
            include=model.include;
            //updateLineCount();
        }

        public MinMaxModel getModelState() {
            MinMaxModel model=new MinMaxModel();
            if (minText==null || minText.getText()==null || minText.getText().trim().length()==0) {
                model.min=0.0;
            } else {
                model.min=new Double(minText.getText().toString());
            }
            if (maxText==null || maxText.getText()==null || maxText.getText().trim().length()==0) {
                model.max=0.0;
            } else {
                model.max=new Double(maxText.getText().toString());
            }
            if (intensityButton.isSelected()) {
                model.type=INTENSITY_TYPE;
            } else if (distributionButton.isSelected()) {
                model.type=DISTRIBUTION_TYPE;
            }
            model.include=include;
            return model;
        }

    };

    public class MinMaxModel {
        public Boolean include;
        public Double min;
        public Double max;
        public String type;

        @Override
        public boolean equals(Object o) {
            MinMaxModel other = (MinMaxModel)o;
            if (min.equals(other.min) && max.equals(other.max) && type.equals(other.type)) {
                return true;
            } else {
                return false;
            }
        }
    }

    public class TypeComboBoxEditor extends DefaultCellEditor {
        public TypeComboBoxEditor(String[] items) {
            super(new JComboBox(items));
        }
    }

    public class TypeCheckBoxEditor extends DefaultCellEditor {
        public TypeCheckBoxEditor() {
            super(new JCheckBox());
        }
    }

    public class ModifyAwareRenderer extends DefaultTableCellRenderer {

        public ModifyAwareRenderer() {
            this.setHorizontalAlignment(JLabel.LEFT);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component cell=null;
            if (column==FT_INDEX_INCLUDE) {
                Boolean bv=(Boolean)value;
                JCheckBox checkBox=new JCheckBox();
                if (bv) {
                    checkBox.setSelected(true);
                } else {
                    checkBox.setSelected(false);
                }
                cell=checkBox;
            } else {
                cell=super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
            if (currentListModified.get(row)) {
                cell.setBackground(new Color(100,255,100));
                cell.setForeground(filterTable.getForeground());
            } else {
                cell.setForeground(filterTable.getForeground());
                cell.setBackground(filterTable.getBackground());
            }
            if (column==FT_INDEX_INCLUDE && computeType.equals(COMPUTE_TYPE_AND)) {
                cell.setBackground(new Color(200,200,200));
                cell.setForeground(new Color(200,200,200));
                cell.setEnabled(false);
            }
            return cell;
        }
    }

    public File getGiantFiberResourceDir() throws Exception {
        File jacsBase=new File(FacadeManager.getOsSpecificRootPath());
        String resourceDirString= SystemConfigurationProperties.getString("MaskSampleAnnotation.ResourceDir");
        String[] resourceComponents=resourceDirString.split("jacsData");
        if (resourceComponents.length!=2) {
            throw new Exception("Could not parse mask resource path="+resourceDirString);
        }
        File maskResourceDir=new File(jacsBase.getAbsolutePath()+resourceComponents[1]);
        File gfDir=new File(maskResourceDir.getAbsolutePath(), GIANT_FIBER_FOLDER_NAME);
        if (!gfDir.exists()) {
            throw new Exception("Could not locate Giant Fiber resource dir="+gfDir.getAbsolutePath());
        }
        return gfDir;
    }

    public File getMaskSummaryFile() throws Exception {
        String quantifierSummaryFilename= SystemConfigurationProperties.getString("FlyScreen.PatternAnnotationQuantifierSummaryFile");
        File gfDir=getGiantFiberResourceDir();
        File summaryFile=new File(gfDir.getAbsolutePath(), quantifierSummaryFilename);
        return summaryFile;
    }

    public File getMaskNameIndexFile() throws Exception {
        File gfDir=getGiantFiberResourceDir();
        File nameFile = new File(gfDir, "maskNameIndex.txt");
        return nameFile;
    }

    public GiantFiberSearchDialog() throws Exception {

        setTitle("Giant Fiber Compartment Search");
        File maskSummaryFile = getMaskSummaryFile();
        File maskNameIndexFile = getMaskNameIndexFile();
        maskManager.loadMaskCompartmentList(maskNameIndexFile);
        maskManager.loadMaskSummaryFile(maskSummaryFile);
        compartmentAbbreviationList = maskManager.getCompartmentListInstance();


        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        currentSetNamePanel = new JPanel();
        currentSetNameLabel = new JLabel("Name of set: ");
        currentSetTextField = new JTextField(50);
        currentSetTextField.setText("");
        currentSetNamePanel.add(currentSetNameLabel);
        currentSetNamePanel.add(currentSetTextField);
        mainPanel.add(currentSetNamePanel, Box.createVerticalGlue());

        computeTypePanel = createComputeTypePanel();
        mainPanel.add(computeTypePanel, Box.createVerticalGlue());

        globalMinMaxPanel = createGlobalMinMaxPanel();
        mainPanel.add(globalMinMaxPanel, Box.createVerticalGlue());

        filterTable = createFilterTable();
        filterTableScrollPane = createFilterTableScrollPane();
        mainPanel.add(filterTableScrollPane, Box.createVerticalGlue());

        Object[] statusObjects = createStatusObjects();
        statusPane=(JPanel)statusObjects[0];
        statusLabel=(JLabel)statusObjects[1];
        savePanel=createSavePanel();
        mainPanel.add(savePanel, Box.createVerticalGlue());
        mainPanel.add(statusPane, Box.createVerticalGlue());

        add(mainPanel, BorderLayout.NORTH);

        quantifierLoaderWorker=createQuantifierLoaderWorker();

        initializeCurrentListModified();

    }

    private JPanel createSavePanel() {
        JPanel savePanel=new JPanel();
        JLabel instructionLabel=new JLabel();
        instructionLabel.setText("Save search result");
        savePanel.add(instructionLabel);
        JButton saveButton=new JButton();
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                saveCurrentSet();
            }
        });
        saveButton.setText("Save");
        savePanel.add(saveButton);
        return savePanel;
    }

    private void initializeCurrentListModified() {
        for (String abbreviation : compartmentAbbreviationList) {
            currentListModified.add(false);
        }
    }

    private JTable createFilterTable() {
        JTable filterTable=new JTable();
        return filterTable;
    }

    private JScrollPane createFilterTableScrollPane() {
        JScrollPane scrollPane=new JScrollPane(filterTable);
        return scrollPane;
    }

    private int getNextFilterSetIndex() {
        return filterSetMap.size()+1;
    }

    private MinMaxModel getOpenMinMaxModelInstance() {
        MinMaxModel model=new MinMaxModel();
        model.include=true;
        model.min=0.0;
        model.max=100.0;
        model.type=INTENSITY_TYPE;
        return model;
    }

    private void createOpenFilterSet(String filterSetName) {
        Map<String, MinMaxModel> openFilterSetMap=new HashMap<String, MinMaxModel>();
        MinMaxModel globalModel=getOpenMinMaxModelInstance();
        openFilterSetMap.put(GLOBAL, globalModel);
        List<String> compartmentAbbreviationList= maskManager.getCompartmentListInstance();
        for (String compartmentName : compartmentAbbreviationList) {
            MinMaxModel compartmentModel=getOpenMinMaxModelInstance();
            openFilterSetMap.put(compartmentName, compartmentModel);
        }
        filterSetMap.put(filterSetName, openFilterSetMap);
    }

    private void setCurrentFilterModel(Map<String, MinMaxModel> filterMap) {
        for (String key : filterMap.keySet()) {
            MinMaxModel model=filterMap.get(key);
            if (key.equals(GLOBAL)) {
                globalMinMaxPanel.setModelState(model);
            } else {
                MinMaxSelectionRow minMaxRow=minMaxRowMap.get(key);
                if (minMaxRow==null) {
                    minMaxRow=new MinMaxSelectionRow(key, maskManager.getCompartmentDescription(key));
                    minMaxRowMap.put(key, minMaxRow);
                }
                minMaxRow.setModelState(model);
            }
        }
    }

    private JPanel createComputeTypePanel() {
        JPanel computeTypePanel=new JPanel();
        JLabel searchTypeLabel=new JLabel();
        searchTypeLabel.setText("Search type");
        computeTypePanel.add(searchTypeLabel);

        JRadioButton andButton=new JRadioButton(COMPUTE_TYPE_AND);
        JRadioButton orButton=new JRadioButton(COMPUTE_TYPE_OR);
        ButtonGroup computeTypeButtonGroup=new ButtonGroup();
        computeTypeButtonGroup.add(andButton);
        computeTypeButtonGroup.add(orButton);

        andButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                computeType=COMPUTE_TYPE_AND;
                for (String key : minMaxRowMap.keySet()) {
                    MinMaxSelectionRow row=minMaxRowMap.get(key);
                    if (!key.equals(GLOBAL) && row!=null) {
                        MinMaxModel model=row.getModelState();
                        model.include=true;
                        row.setModelState(model);
                    }
                }
                refreshCompartmentTable();
            }
        });

        orButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                computeType=COMPUTE_TYPE_OR;
                for (String key : minMaxRowMap.keySet()) {
                    MinMaxSelectionRow row=minMaxRowMap.get(key);
                    if (!key.equals(GLOBAL) && row!=null) {
                        MinMaxModel model=row.getModelState();
                        model.include=false;
                        row.setModelState(model);
                    }
                }
                refreshCompartmentTable();
            }
        });

        andButton.setSelected(true);

        computeTypePanel.add(andButton);
        computeTypePanel.add(orButton);
        return computeTypePanel;
    }

    private MinMaxSelectionRow createGlobalMinMaxPanel() {
        MinMaxSelectionRow globalSettingsPanel=new MinMaxSelectionRow("Global", "Settings for all non-modified compartments");
        return globalSettingsPanel;
    }

    private Object[] createStatusObjects() {
        JPanel statusPane = new JPanel();
        JLabel statusLabel = new JLabel("");
        statusPane.add(statusLabel);
        Object[] statusObjects=new Object[2];
        statusObjects[0]=statusPane;
        statusObjects[1]=statusLabel;
        return statusObjects;
    }

    private void setStatusMessage(String message) {
        statusLabel.setText(message);
    }

    public void showDialog() {
        showDialog(null);
    }

    public void showDialog(RootedEntity outputFolder) {
        this.outputFolder = outputFolder;
        init();
    }

    private void init() {
        quantifierLoaderWorker.execute();
        String initialFilterName="Set "+getNextFilterSetIndex();
        currentSetTextField.setText(initialFilterName);
        createOpenFilterSet(initialFilterName);
        setCurrentFilterModel(filterSetMap.get(initialFilterName));
        setupFilterTable();
        packAndShow();
    }

    private void setupFilterTable() {
        tableModel = new DefaultTableModel(filterTableColumnNames, filterTableColumnNames.length) {
            @Override
            public int getRowCount() {
                return compartmentAbbreviationList.size();
            }

            @Override
            public int getColumnCount() {
                return filterTableColumnNames.length;
            }

            @Override
            public boolean isCellEditable(int row, int col) {
                if (col==FT_INDEX_MIN ||
                    col==FT_INDEX_MAX ||
                    col==FT_INDEX_FILTERTYPE ||
                    col==FT_INDEX_INCLUDE) {
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public Class getColumnClass(int c) {
                return getValueAt(0,c).getClass();
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                String rowKey=compartmentAbbreviationList.get(rowIndex);
                MinMaxSelectionRow compartmentRow=minMaxRowMap.get(rowKey);
                switch (columnIndex) {
                    case FT_INDEX_INCLUDE:
                        Boolean include=compartmentRow.getInclude();
                        return include;
                    case FT_INDEX_COMPARTMENT:
                        String compartmentAbbreviation=compartmentRow.getAbbreviation();
                        return compartmentAbbreviation;
                    case FT_INDEX_DESCRIPTION:
                        String description=compartmentRow.getDescription();
                        return description;
                    case FT_INDEX_FILTERTYPE:
                        String filterType=compartmentRow.getModelState().type;
                        return filterType;
                    case FT_INDEX_MIN:
                        Double min=compartmentRow.getModelState().min;
                        return min;
                    case FT_INDEX_MAX:
                        Double max=compartmentRow.getModelState().max;
                        return max;
                    case FT_INDEX_LINES:
                        Long lineCount=0L;
                        if (compartmentRow.lineCountText!=null) {
                            String lineText=compartmentRow.lineCountText.getText();
                            if (lineText!=null && lineText.trim().length()>0) {
                                lineCount=new Long(lineText);
                            }
                        }
                        return lineCount.toString();
                    default:
                        return null;
                }
            }

            @Override
            public void setValueAt(Object value, int row, int col) {
                String rowKey=compartmentAbbreviationList.get(row);
                MinMaxSelectionRow compartmentRow=minMaxRowMap.get(rowKey);
                MinMaxModel state=compartmentRow.getModelState();
                if (col==FT_INDEX_INCLUDE) {
                    Boolean includeValue=(Boolean)value;
                    if (computeType.equals(COMPUTE_TYPE_AND)) {
                        includeValue=true;
                    }
                    state.include=includeValue;
                    compartmentRow.setModelState(state);
                    updateRowImpactOnCounts(row);
                } else if (col==FT_INDEX_MIN) {
                    Double newValue=new Double(value.toString());
                    if (newValue<0.0) {
                        newValue=0.0;
                    }
                    if (newValue>state.max) {
                        newValue=state.max;
                    }
                    state.min=newValue;
                    compartmentRow.setModelState(state);
                    updateRowImpactOnCounts(row);
                } else if (col==FT_INDEX_MAX) {
                    Double newValue=new Double(value.toString());
                    if (newValue>100.0) {
                        newValue=100.0;
                    }
                    if (newValue<state.min) {
                        newValue=state.min;
                    }
                    state.max=newValue;
                    compartmentRow.setModelState(state);
                    updateRowImpactOnCounts(row);
                } else if (col==FT_INDEX_FILTERTYPE) {
                    state.type=(String)value;
                    compartmentRow.setModelState(state);
                    updateRowImpactOnCounts(row);
                }
                if (currentSetInitialized) {
                    MinMaxModel globalState=globalMinMaxPanel.getModelState();
                    if (state.equals(globalState)) {
                        if (currentListModified.get(row)) {
                            currentListModified.set(row, false);
                        }
                    } else {
                        if (!currentListModified.get(row)) {
                            currentListModified.set(row, true);
                        }
                    }
                }
                fireTableCellUpdated(row, col);
                refreshCompartmentTable();
            }

        };
        filterTable.setModel(tableModel);

        TableColumn typeColumn = filterTable.getColumnModel().getColumn(FT_INDEX_FILTERTYPE);
        typeColumn.setCellEditor(new TypeComboBoxEditor(new String[] { INTENSITY_TYPE, DISTRIBUTION_TYPE }));

        TableColumn includeColumn = filterTable.getColumnModel().getColumn(FT_INDEX_INCLUDE);
        includeColumn.setCellEditor(new TypeCheckBoxEditor());

        ModifyAwareRenderer modifyAwareRenderer=new ModifyAwareRenderer();
        filterTable.setDefaultRenderer(Object.class, modifyAwareRenderer);
        filterTable.setDefaultRenderer(Double.class, modifyAwareRenderer);
        filterTable.setDefaultRenderer(Boolean.class, modifyAwareRenderer);
    }

    protected void loadPatternAnnotationQuantifierMapsFromSummary() {
        if (!quantifierDataIsLoading && (sampleInfoMap==null || quantifierInfoMap==null)) {
            quantifierDataIsLoading=true;
            try {
                Long startTime=new Date().getTime();
                System.out.println("GiantFiberSearchDialog getMaskQuantifierMapsFromSummary() start");
                Object[] sampleMaps = ModelMgr.getModelMgr().getMaskQuantifierMapsFromSummary(GIANT_FIBER_FOLDER_NAME);
                sampleInfoMap = (Map<Long, Map<String,String>>)sampleMaps[0];
                quantifierInfoMap = (Map<Long, List<Double>>)sampleMaps[1];
                Long elapsedTime=new Date().getTime() - startTime;
                System.out.println("GiantFiberSearchDialog getMaskQuantifierMapsFromSummary() end - elapsedTime="+elapsedTime);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            quantifierDataIsLoading=false;
        } else {
            System.out.println("GiantFiberSearchDialog getMaskQuantifierMapsFromSummary() - maps already loaded");
        }
    }

    SimpleWorker createQuantifierLoaderWorker() {
        SimpleWorker quantifierLoaderWorker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                Utils.setWaitingCursor(GiantFiberSearchDialog.this);
                setStatusMessage("Loading quantifier maps...");
                loadPatternAnnotationQuantifierMapsFromSummary();
                setStatusMessage("Computing scores...");
                computeScores();
                setStatusMessage("Computing percentiles...");
                computePercentiles();
                refreshCompartmentTable();
                currentSetInitialized=true;
            }

            @Override
            protected void hadSuccess() {
                resetSearchState();
            }

            @Override
            protected void hadError(Throwable error) {
                Utils.setDefaultCursor(GiantFiberSearchDialog.this);
                SessionMgr.getSessionMgr().handleException(error);
                setStatusMessage("Error during quantifier load");
            }
        };
        return quantifierLoaderWorker;
    }

    protected void computeScores() {
        long totalComputeCount=0;
        if (intensityScoreMap==null) {
            intensityScoreMap=new HashMap<Long, Map<String, Double>>();
        }
        if (distributionScoreMap==null) {
            distributionScoreMap=new HashMap<Long, Map<String, Double>>();
        }
        for (Long sampleId : quantifierInfoMap.keySet()) {
            List<Double> quantifierList = quantifierInfoMap.get(sampleId);
            Map<String, Double> intensityMap = new HashMap<String, Double>();
            Map<String, Double> distributionMap = new HashMap<String, Double>();
            List<Double> globalList = new ArrayList<Double>();
            List<Double> compartmentList = new ArrayList<Double>();
            // We assume the compartment list here matches the order of the quantifierList
            final int GLOBAL_LIST_SIZE=9;
            for (int g=0;g<GLOBAL_LIST_SIZE;g++) {
                globalList.add(quantifierList.get(g));
            }
            int compartmentCount=0;
            for (String compartmentAbbreviation : compartmentAbbreviationList) {
                compartmentList.clear();
                int startPosition=GLOBAL_LIST_SIZE + compartmentCount*10;
                int endPosition=startPosition+10;
                for (int c=startPosition;c<endPosition;c++) {
                    compartmentList.add(quantifierList.get(c));
                }
                Object[] scores =maskManager.getCompartmentScoresByQuantifiers(globalList, compartmentList);
                totalComputeCount++;
                intensityMap.put(compartmentAbbreviation, (Double)scores[0]);
                distributionMap.put(compartmentAbbreviation, (Double)scores[1]);
                compartmentCount++;
            }
            intensityScoreMap.put(sampleId, intensityMap);
            distributionScoreMap.put(sampleId, distributionMap);
        }
        System.out.println("Total calls to getCompartmentScoresByQuantifiers() = "+totalComputeCount);
    }

    protected void computePercentiles() {
        if (intensityPercentileMap==null) {
            intensityPercentileMap=new HashMap<Long, Map<String, Double>>();
        }
        if (distributionPercentileMap==null) {
            distributionPercentileMap=new HashMap<Long, Map<String, Double>>();
        }
        List<PercentileScore> intensityList = new ArrayList<PercentileScore>();
        List<PercentileScore> distributionList = new ArrayList<PercentileScore>();
        Map<Long, Double> percIntensityMap=new HashMap<Long, Double>();
        Map<Long, Double> percDistMap=new HashMap<Long, Double>();
        for (String compartmentAbbreviation : compartmentAbbreviationList) {
            intensityList.clear();
            distributionList.clear();
            for (Long sampleId : intensityScoreMap.keySet()) {
                Map<String, Double> intensityMap = intensityScoreMap.get(sampleId);
                PercentileScore ps=new PercentileScore();
                ps.sampleId=sampleId;
                ps.score=intensityMap.get(compartmentAbbreviation);
                intensityList.add(ps);
            }
            for (Long sampleId : distributionScoreMap.keySet()) {
                Map<String, Double> distributionMap = distributionScoreMap.get(sampleId);
                PercentileScore ps=new PercentileScore();
                ps.sampleId=sampleId;
                ps.score=distributionMap.get(compartmentAbbreviation);
                distributionList.add(ps);
            }
            Collections.sort(intensityList);
            Collections.sort(distributionList);
            double listLength=intensityList.size()-1.0;

            percIntensityMap.clear();
            percDistMap.clear();

            double index=0.0;
            for (PercentileScore ps : intensityList) {
                ps.score = index / listLength;
                percIntensityMap.put(ps.sampleId, ps.score);
                index+=1.0;
            }
            index=0.0;
            for (PercentileScore ps : distributionList) {
                ps.score = index / listLength;
                percDistMap.put(ps.sampleId, ps.score);
                index+=1.0;
            }

            for (Long sampleId : intensityScoreMap.keySet()) {
                Map<String, Double> piMap=intensityPercentileMap.get(sampleId);
                if (piMap==null) {
                    piMap=new HashMap<String, Double>();
                    intensityPercentileMap.put(sampleId, piMap);
                }
                piMap.put(compartmentAbbreviation, percIntensityMap.get(sampleId));
            }
            for (Long sampleId : distributionScoreMap.keySet()) {
                Map<String, Double> pdMap=distributionPercentileMap.get(sampleId);
                if (pdMap==null) {
                    pdMap=new HashMap<String, Double>();
                    distributionPercentileMap.put(sampleId, pdMap);
                }
                pdMap.put(compartmentAbbreviation, percDistMap.get(sampleId));
            }
        }
    }

    protected Long computeLineCountForCompartment(int rowIndex) {
        String rowKey=compartmentAbbreviationList.get(rowIndex);
        return computeLineCountForCompartment(rowKey);
    }

    protected Long computeLineCountForCompartment(String compartmentAbbreviation) {
        if (quantifierDataIsLoading) {
            return 0L;
        } else {
            List<Long> validSamples=getValidSamplesForCompartment(compartmentAbbreviation);
            return new Long(validSamples.size());
        }
    }

    List<Long> getValidSamplesForCompartment(String compartmentAbbreviation) {
        List<Long> validSamples=new ArrayList<Long>();
        if (quantifierDataIsLoading) {
            return validSamples;
        } else {
            MinMaxSelectionRow compartmentRow=minMaxRowMap.get(compartmentAbbreviation);
            MinMaxModel state=compartmentRow.getModelState();
            if (!state.include) {
                validSamples.clear();
                return validSamples;
            }
            Double min=state.min / 100.0;
            Double max=state.max / 100.0;
            if (state.type.equals(INTENSITY_TYPE)) {
                for (Long sampleId : intensityPercentileMap.keySet()) {
                    Map<String, Double> map=intensityPercentileMap.get(sampleId);
                    Double value=map.get(compartmentAbbreviation);
                    if (value>=min && value <=max) {
                        validSamples.add(sampleId);
                    }
                }
            } else if (state.type.equals(DISTRIBUTION_TYPE)) {
                for (Long sampleId : distributionPercentileMap.keySet()) {
                    Map<String, Double> map=distributionPercentileMap.get(sampleId);
                    if (map==null) {
                        System.err.println("distributionPercentileMap is null for sampleId="+sampleId);
                    }
                    Double value=map.get(compartmentAbbreviation);
                    if (value==null) {
                        System.err.println("distribution perc value is null for compartmentAbbr="+compartmentAbbreviation);
                    }
                    if (value>=min && value <=max) {
                        validSamples.add(sampleId);
                    }
                }
            }
        }
        return validSamples;
    }

    protected void refreshCompartmentTable() {
        List<Integer> rowUpdateList=new ArrayList<Integer>();
        for (int rowIndex=0;rowIndex<compartmentAbbreviationList.size();rowIndex++) {
            rowUpdateList.add(rowIndex);
        }
        updateRowImpactOnCounts(rowUpdateList);
        filterTableScrollPane.update(filterTableScrollPane.getGraphics());
    }

    protected Set<Long> generateMembershipListForCurrentSet() {
        setStatusMessage("Computing result membership");
        Long compartmentListSize=new Long(compartmentAbbreviationList.size());
        Set<Long> sampleSet=new HashSet<Long>();
        Map<Long, Long> sampleCompartmentCountMap=new HashMap<Long, Long>();
        for (String compartment : compartmentAbbreviationList) {
            List<Long> samples=getValidSamplesForCompartment(compartment);
            for (Long sampleId : samples) {
                Long sampleCount=sampleCompartmentCountMap.get(sampleId);
                if (sampleCount==null) {
                    sampleCount=new Long(0L);
                }
                sampleCount++;
                sampleCompartmentCountMap.put(sampleId, sampleCount);
            }
        }
        for (Long sampleId : sampleCompartmentCountMap.keySet()) {
            Long count=sampleCompartmentCountMap.get(sampleId);
            if (computeType.equals(COMPUTE_TYPE_AND)) {
                if (count.equals(compartmentListSize)) {
                    sampleSet.add(sampleId);
                }
            } else if (computeType.equals(COMPUTE_TYPE_OR)) {
                if (count>0) {
                    sampleSet.add(sampleId);
                }
            }
        }
        setStatusMessage("Result has " + sampleSet.size() + " members");
        return sampleSet;
    }

    protected void updateRowImpactOnCounts(int rowIndex) {
        List<Integer> rowList=new ArrayList<Integer>();
        rowList.add(rowIndex);
        updateRowImpactOnCounts(rowList);
    }

    protected void updateRowImpactOnCounts(List<Integer> rowList) {
        for (int rowIndex : rowList) {
            String rowKey=compartmentAbbreviationList.get(rowIndex);
            MinMaxSelectionRow compartmentRow=minMaxRowMap.get(rowKey);
            Long updatedLineCount=computeLineCountForCompartment(rowIndex);
            compartmentRow.lineCountText.setText(updatedLineCount.toString());
        }
        membershipSampleSet=generateMembershipListForCurrentSet();
    }

    protected synchronized void saveCurrentSet() {

        SimpleWorker worker = new SimpleWorker() {

            private RootedEntity newRootedFolder;

            @Override
            protected void doStuff() throws Exception {
                Entity newFolder = ModelMgr.getModelMgr().createEntity(EntityConstants.TYPE_FOLDER, currentSetTextField.getText());

                if (outputFolder!=null) {
                    newFolder = ModelMgr.getModelMgr().saveOrUpdateEntity(newFolder);
                    EntityData childEd = ModelMgr.getModelMgr().addEntityToParent(outputFolder.getEntity(), newFolder, outputFolder.getEntity().getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
                    newRootedFolder = outputFolder.getChild(childEd);
                }
                else {
                    newFolder.addAttributeAsTag(EntityConstants.ATTRIBUTE_COMMON_ROOT);
                    newFolder = ModelMgr.getModelMgr().saveOrUpdateEntity(newFolder);
                    newRootedFolder = new RootedEntity(newFolder);
                }

                ModelMgr.getModelMgr().addChildren(newFolder.getId(),
                        new ArrayList<Long>(membershipSampleSet), EntityConstants.ATTRIBUTE_ENTITY);
            }

            @Override
            protected void hadSuccess() {
                final EntityOutline entityOutline = SessionMgr.getSessionMgr().getActiveBrowser().getEntityOutline();
                entityOutline.refresh(true, new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(EntitySelectionModel.CATEGORY_OUTLINE, newRootedFolder.getUniqueId(), true);
                        Utils.setDefaultCursor(GiantFiberSearchDialog.this);
                        setVisible(false);
                        resetSearchState();
                        return null;
                    }
                });
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
                Utils.setDefaultCursor(GiantFiberSearchDialog.this);
                resetSearchState();
            }
        };

        Utils.setWaitingCursor(GiantFiberSearchDialog.this);
        worker.execute();
    }

    protected void resetSearchState() {
        membershipSampleSet.clear();
        MinMaxModel globalModel=globalMinMaxPanel.getModelState();
        globalModel.min=0.0;
        globalModel.max=100.0;
        globalModel.type=INTENSITY_TYPE;
        globalMinMaxPanel.setModelState(globalModel);
        for (int row=0;row<currentListModified.size();row++) {
            currentListModified.set(row, false);
        }
        globalMinMaxPanel.applyGlobalSettings();
        Utils.setDefaultCursor(GiantFiberSearchDialog.this);
        setStatusMessage("Ready");
    }


}
