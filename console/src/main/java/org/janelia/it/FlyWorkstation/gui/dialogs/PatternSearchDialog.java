package org.janelia.it.FlyWorkstation.gui.dialogs;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.jacs.shared.annotation.PatternAnnotationDataManager;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
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
    
    private static final String PEAK_TYPE="Peak";
    private static final String TOTAL_TYPE="Total";
    private static final String GLOBAL = "Global";
    
    private static final String[] filterTableColumnNames = {
            "Compartment",
            "Description",
            "Filter Type",
            "Min",
            "Max",
            "Graph",
            "Lines"
    };
    
    private static final int FT_INDEX_COMPARTMENT=0;
    private static final int FT_INDEX_DESCRIPTION=1;
    private static final int FT_INDEX_FILTERTYPE=2;
    private static final int FT_INDEX_MIN=3;
    private static final int FT_INDEX_MAX=4;
    private static final int FT_INDEX_GRAPH=5;
    private static final int FT_INDEX_LINES=6;

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
    //private final Map<String, MinMaxModel> currentMinMaxModelMap=new HashMap<String, MinMaxModel>();
    private final Map<String, MinMaxSelectionRow> minMaxRowMap=new HashMap<String, MinMaxSelectionRow>();

    static boolean quantifierDataIsLoading=false;
    static protected Map<Long, Map<String,String>> sampleInfoMap=null;
    static protected Map<Long, List<Double>> quantifierInfoMap=null;
    
    private class MinMaxSelectionRow extends JPanel implements ActionListener {
        String abbreviation;
        String description;
        JRadioButton peakButton;
        JRadioButton totalButton;
        Double min=0.0;
        Double max=100.0;
        Long lineCount;
        ButtonGroup buttonGroup;
        JTextField minText;
        JTextField maxText;
        JTextField lineCountText;

        public MinMaxSelectionRow(String abbreviation, String description) {
            this.abbreviation=abbreviation;
            this.description=description;
            JLabel abbreviationLabel=new JLabel();
            abbreviationLabel.setText(abbreviation);
            JLabel descriptionLabel=new JLabel();
            descriptionLabel.setText(description);
            add(abbreviationLabel);
            add(descriptionLabel);
            peakButton = new JRadioButton(PEAK_TYPE);
            peakButton.setActionCommand(PEAK_TYPE);
            totalButton = new JRadioButton(TOTAL_TYPE);
            totalButton.setActionCommand(TOTAL_TYPE);
            buttonGroup = new ButtonGroup();
            buttonGroup.add(peakButton);
            buttonGroup.add(totalButton);
            peakButton.setSelected(true);
            peakButton.addActionListener(this);
            totalButton.addActionListener(this);
            add(peakButton);
            add(totalButton);
            minText=new JTextField(5);
            minText.setText(min.toString());
            maxText=new JTextField(5);
            maxText.setText(max.toString());
            add(minText);
            add(maxText);
        }

        public void actionPerformed(ActionEvent e) {
            String actionString=e.getActionCommand();
            if (actionString.equals(PEAK_TYPE)) {
                setStatusMessage(abbreviation+": Peak type selected");
            } else if (actionString.equals(TOTAL_TYPE)) {
                setStatusMessage(abbreviation+": Total type selected");
            }
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
            if (model.type.equals(PEAK_TYPE)) {
                peakButton.setSelected(true);
            } else if (model.type.equals(TOTAL_TYPE)) {
                totalButton.setSelected(true);
            }
        }
        
        public MinMaxModel getModelState() {
            MinMaxModel model=new MinMaxModel();
            model.min=min;
            model.max=max;
            if (peakButton.isSelected()) {
                model.type=PEAK_TYPE;
            } else if (totalButton.isSelected()) {
                model.type=TOTAL_TYPE;
            }
            return model;
        }

        public JTextField getMinText() {
            return minText;
        }

    };
    
    public class MinMaxModel {
        public Double min;
        public Double max;
        public String type;
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
        model.type=PEAK_TYPE;
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
        final List<String> compartmentAbbreviationList = PatternAnnotationDataManager.getCompartmentListInstance();
        TableModel tableModel = new DefaultTableModel() {
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
                if (col==FT_INDEX_MIN) {
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
                    case FT_INDEX_GRAPH:
                        return "<graph>";
                    case FT_INDEX_LINES:
                        return "<lines>";
                    default:
                        return null;
                }
            }

            @Override
            public void setValueAt(Object value, int row, int col) {
                System.out.println("setValueAt value="+value.toString()+" row="+row+" col="+col);
                if (col==FT_INDEX_MIN) {
                    System.out.println("col==FT_INDEX_MIN");
                    String rowKey=compartmentAbbreviationList.get(row);
                    MinMaxSelectionRow compartmentRow=minMaxRowMap.get(rowKey);
                    MinMaxModel state=compartmentRow.getModelState();
                    Double newValue=new Double(value.toString());
                    System.out.println("Setting new value="+newValue);
                    state.min=newValue;
                    compartmentRow.setModelState(state);
                    // Check
                    MinMaxSelectionRow checkRow=minMaxRowMap.get(rowKey);
                    MinMaxModel checkState=checkRow.getModelState();
                    Double checkValue=checkState.min;
                    System.out.println("Check value="+checkValue);
                }
                System.out.println("fireTableCellUpdated row="+row+" col="+col);
                fireTableCellUpdated(row, col);
            }

        };
        filterTable.setModel(tableModel);
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

}
