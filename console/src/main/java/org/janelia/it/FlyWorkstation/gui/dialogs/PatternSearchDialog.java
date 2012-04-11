package org.janelia.it.FlyWorkstation.gui.dialogs;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.jacs.shared.annotation.PatternAnnotationDataManager;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 3/13/12
 * Time: 1:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class PatternSearchDialog extends ModalDialog {
    
    private static final String INTENSITY_TYPE="Intensity";
    private static final String DISTRIBUTION_TYPE="Distribution";
    private static final String GLOBAL = "Global";

    DefaultTableModel tableModel;
    
    private static final String[] filterTableColumnNames = {
            "Compartment",
            "Description",
            "Filter Type",
            "Min",
            "Max",
            "Lines"
    };
    
    private static final int FT_INDEX_COMPARTMENT=0;
    private static final int FT_INDEX_DESCRIPTION=1;
    private static final int FT_INDEX_FILTERTYPE=2;
    private static final int FT_INDEX_MIN=3;
    private static final int FT_INDEX_MAX=4;
    private static final int FT_INDEX_LINES=5;

    private final JPanel mainPanel;
    private final JPanel currentSetNamePanel;
    private final MinMaxSelectionRow globalMinMaxPanel;
    private final JTable filterTable;
    private final JScrollPane filterTableScrollPane;
    private final JPanel statusPane;

    private final JLabel statusLabel;
	private final JLabel currentSetNameLabel;
    private final JTextField currentSetTextField;

    private final SimpleWorker quantifierLoaderWorker;
    
    private final Map<String, Map<String, MinMaxModel>> filterSetMap=new HashMap<String, Map<String, MinMaxModel>>();
    private final Map<String, MinMaxSelectionRow> minMaxRowMap=new HashMap<String, MinMaxSelectionRow>();

    static boolean quantifierDataIsLoading=false;

    static protected Map<Long, Map<String,String>> sampleInfoMap=null;
    static protected Map<Long, List<Double>> quantifierInfoMap=null;
    static protected Map<Long, Map<String, Double>> intensityScoreMap=null;
    static protected Map<Long, Map<String, Double>> distributionScoreMap=null;

    final List<String> compartmentAbbreviationList = PatternAnnotationDataManager.getCompartmentListInstance();
    boolean currentSetInitialized=false;
    final List<Boolean> currentListModified = new ArrayList<Boolean>();

    private class MinMaxSelectionRow extends JPanel implements ActionListener {
        String abbreviation;
        String description;
        JRadioButton intensityButton;
        JRadioButton distributionButton;
        Double min=0.0;
        Double max=100.0;
        Long lineCount;
        ButtonGroup buttonGroup;
        JTextField minText;
        JTextField maxText;
        JTextField lineCountText;
        JButton applyButton;

        public MinMaxSelectionRow(String abbreviation, String description) {
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
            minText.setText(min.toString());
            maxText=new JTextField(5);
            maxText.setText(max.toString());
            add(minText);
            add(maxText);
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
            Double globalMin=0.0;
            Double globalMax=100.0;
            try {
                globalMin=new Double(minText.getText());
            } catch (Exception ex) {}
            try {
                globalMax=new Double(maxText.getText());
            } catch (Exception ex) {}
            String globalType=(intensityButton.isSelected()?INTENSITY_TYPE:DISTRIBUTION_TYPE);
            for (int rowIndex=0;rowIndex<compartmentAbbreviationList.size();rowIndex++) {
                String rowKey=compartmentAbbreviationList.get(rowIndex);
                MinMaxSelectionRow compartmentRow=minMaxRowMap.get(rowKey);
                if (!currentListModified.get(rowIndex)) {
                    MinMaxModel state=compartmentRow.getModelState();
                    state.min=globalMin;
                    state.max=globalMax;
                    state.type=globalType;
                    compartmentRow.setModelState(state);
                }
            }
            filterTableScrollPane.update(filterTableScrollPane.getGraphics());
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
        
        public String getDescription() {
            return description;
        }
        
        public String getAbbreviation() {
            return abbreviation;
        }
        
        public void setLineCount(Long lineCount) {
            this.lineCount=lineCount;
            if (lineCountText==null) {
                lineCountText=new JTextField(7);
                add(lineCountText);
            }
            lineCountText.setText(lineCount.toString());
        }
        
        public void setModelState(MinMaxModel model) {
            this.min=model.min;
            this.max=model.max;
            if (model.type.equals(INTENSITY_TYPE)) {
                intensityButton.setSelected(true);
            } else if (model.type.equals(DISTRIBUTION_TYPE)) {
                distributionButton.setSelected(true);
            }
        }
        
        public MinMaxModel getModelState() {
            MinMaxModel model=new MinMaxModel();
            model.min=min;
            model.max=max;
            if (intensityButton.isSelected()) {
                model.type=INTENSITY_TYPE;
            } else if (distributionButton.isSelected()) {
                model.type=DISTRIBUTION_TYPE;
            }
            return model;
        }

    };

    public class MinMaxModel {
        public Double min;
        public Double max;
        public String type;
    }

    public class TypeComboBoxEditor extends DefaultCellEditor {
        public TypeComboBoxEditor(String[] items) {
            super(new JComboBox(items));
        }
    }

    public class ModifyAwareRenderer extends DefaultTableCellRenderer {

        public ModifyAwareRenderer() {
            this.setHorizontalAlignment(JLabel.LEFT);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component cell=super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (currentListModified.get(row)) {
                cell.setBackground(new Color(100,255,100));
                cell.setForeground(filterTable.getForeground());
            } else {
                cell.setForeground(filterTable.getForeground());
                cell.setBackground(filterTable.getBackground());
            }
            return cell;
        }
    }

    public PatternSearchDialog() {

        setTitle("Pattern Annotation Search");

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
        
        globalMinMaxPanel = createGlobalMinMaxPanel();
        mainPanel.add(globalMinMaxPanel, Box.createVerticalGlue());
        
        filterTable = createFilterTable();
        filterTableScrollPane = createFilterTableScrollPane();
        mainPanel.add(filterTableScrollPane, Box.createVerticalGlue());

        Object[] statusObjects = createStatusObjects();
        statusPane=(JPanel)statusObjects[0];
        statusLabel=(JLabel)statusObjects[1];
        mainPanel.add(statusPane, Box.createVerticalGlue());

        add(mainPanel, BorderLayout.NORTH);

        quantifierLoaderWorker=createQuantifierLoaderWorker();

        initializeCurrentListModified();

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
        model.min=0.0;
        model.max=100.0;
        model.type=INTENSITY_TYPE;
        return model;
    }

    private void createOpenFilterSet(String filterSetName) {
        Map<String, MinMaxModel> openFilterSetMap=new HashMap<String, MinMaxModel>();
        MinMaxModel globalModel=getOpenMinMaxModelInstance();
        openFilterSetMap.put(GLOBAL, globalModel);
        List<String> compartmentAbbreviationList= PatternAnnotationDataManager.getCompartmentListInstance();
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
                    minMaxRow=new MinMaxSelectionRow(key, key + " description");
                    minMaxRowMap.put(key, minMaxRow);
                }
                minMaxRow.setModelState(model);
            }
        }
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
                    col==FT_INDEX_FILTERTYPE) {
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
                        return "<lines>";
                    default:
                        return null;
                }
            }

            @Override
            public void setValueAt(Object value, int row, int col) {
                String rowKey=compartmentAbbreviationList.get(row);
                MinMaxSelectionRow compartmentRow=minMaxRowMap.get(rowKey);
                MinMaxModel state=compartmentRow.getModelState();
                if (col==FT_INDEX_MIN) {
                    Double newValue=new Double(value.toString());
                    if (newValue<0.0) {
                        newValue=0.0;
                    }
                    if (newValue>state.max) {
                        newValue=state.max;
                    }
                    state.min=newValue;
                    compartmentRow.setModelState(state);
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
                } else if (col==FT_INDEX_FILTERTYPE) {
                    state.type=(String)value;
                    compartmentRow.setModelState(state);
                }
                if (currentSetInitialized) {
                    if (state.min==0.0 && state.max==100.0 && state.type.equals(INTENSITY_TYPE)) {
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
            }

        };
        filterTable.setModel(tableModel);
        TableColumn typeColumn = filterTable.getColumnModel().getColumn(FT_INDEX_FILTERTYPE);
        typeColumn.setCellEditor(new TypeComboBoxEditor(new String[] { INTENSITY_TYPE, DISTRIBUTION_TYPE }));

        ModifyAwareRenderer modifyAwareRenderer=new ModifyAwareRenderer();
        filterTable.setDefaultRenderer(Object.class, modifyAwareRenderer);
        filterTable.setDefaultRenderer(Double.class, modifyAwareRenderer);
    }

    protected void loadPatternAnnotationQuantifierMapsFromSummary() {
        if (!quantifierDataIsLoading && (sampleInfoMap==null || quantifierInfoMap==null)) {
            quantifierDataIsLoading=true;
            try {
                Long startTime=new Date().getTime();
                System.out.println("PatterSearchDialog loadPatternAnnotationQuantifierMapsFromSummary() start");
                Object[] sampleMaps = ModelMgr.getModelMgr().getPatternAnnotationQuantifierMapsFromSummary();
                sampleInfoMap = (Map<Long, Map<String,String>>)sampleMaps[0];
                quantifierInfoMap = (Map<Long, List<Double>>)sampleMaps[1];
                Long elapsedTime=new Date().getTime() - startTime;
                System.out.println("PatterSearchDialog loadPatternAnnotationQuantifierMapsFromSummary() end - elapsedTime="+elapsedTime);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            quantifierDataIsLoading=false;
        } else {
            System.out.println("PatternSearchDialog loadPatternAnnotationQuantifierMapsFromSummary() - maps already loaded");
        }
    }

    SimpleWorker createQuantifierLoaderWorker() {
        SimpleWorker quantifierLoaderWorker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                setStatusMessage("Loading quantifier maps...");
                loadPatternAnnotationQuantifierMapsFromSummary();
                setStatusMessage("Computing scores...");
                computeScores();
                currentSetInitialized=true;
            }

            @Override
            protected void hadSuccess() {
                setStatusMessage("Ready");
            }

            @Override
            protected void hadError(Throwable error) {
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
                Object[] scores =PatternAnnotationDataManager.getCompartmentScoresByQuantifiers(globalList, compartmentList);
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

}
