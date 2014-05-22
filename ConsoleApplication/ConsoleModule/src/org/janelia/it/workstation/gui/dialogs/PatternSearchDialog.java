package org.janelia.it.workstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import org.janelia.it.workstation.model.utils.FolderUtils;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.shared.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 3/13/12
 * Time: 1:40 PM
 */
public class PatternSearchDialog extends ModalDialog {
    
	private static final Logger log = LoggerFactory.getLogger(PatternSearchDialog.class);
	
    private static final String INTENSITY_TYPE="Intensity";
    private static final String DISTRIBUTION_TYPE="Distribution";
    private static final String GLOBAL = "Global";

    private static final Long MAX_LOADING_WAIT_MS = 120000L; // 2 minutes

    final Color DARK_GREEN = new Color(0,120,0);

    private org.janelia.it.workstation.model.entity.RootedEntity outputFolder;
    
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

    private static final int MAX_ENTITIES_IN_FOLDER = 2000;

    private final MinMaxSelectionRow globalMinMaxPanel;
    private final JTable filterTable;
    private final JScrollPane filterTableScrollPane;

    private final JLabel statusLabel;
    private final JTextField currentSetTextField;

    private final SimpleWorker quantifierLoaderWorker;
    
    private final Map<String, Map<String, MinMaxModel>> filterSetMap=new HashMap<String, Map<String, MinMaxModel>>();
    private final Map<String, MinMaxSelectionRow> minMaxRowMap=new HashMap<String, MinMaxSelectionRow>();

    static boolean quantifierDataIsLoading=true; // we want to initialize this to true until loading is done

    Map<String, List<DataDescriptor>> managerDescriptorMap=new HashMap<String, List<DataDescriptor>>();

    List<String> compartmentAbbreviationList=new ArrayList<String>();

    boolean currentSetInitialized=false;
    final List<Boolean> currentListModified = new ArrayList<Boolean>();
    FilterResult filterResult;
    
    private org.janelia.it.workstation.model.entity.RootedEntity saveFolder;
	private boolean returnInsteadOfSaving = false;
	private boolean saveClicked = false;

    private class MinMaxSelectionRow extends JPanel implements ActionListener {
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
            minText.setText(Double.toString(0.0));
            maxText=new JTextField(5);
            maxText.setText(Double.toString(100.0));
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
            try {
                refreshCompartmentTable();
            } catch (Exception ex) {}
        }

        public void actionPerformed(ActionEvent e) {
            String actionString=e.getActionCommand();
            if (actionString.equals(INTENSITY_TYPE)) {
                setStatusMessage(abbreviation+": INTENSITY type selected", Color.WHITE);
            } else if (actionString.equals(DISTRIBUTION_TYPE)) {
                setStatusMessage(abbreviation+": DISTRIBUTION type selected", Color.WHITE);
            }
            applyGlobalSettings();
        }
        
        public String getDescription() {
            return description;
        }
        
        public String getAbbreviation() {
            return abbreviation;
        }

        public void setModelState(MinMaxModel model) {
            minText.setText(model.min.toString());
            maxText.setText(model.max.toString());
            if (model.type.equals(INTENSITY_TYPE)) {
                intensityButton.setSelected(true);
            } else if (model.type.equals(DISTRIBUTION_TYPE)) {
                distributionButton.setSelected(true);
            }
        }
        
        public MinMaxModel getModelState() {
            MinMaxModel model=new MinMaxModel();
            if (minText==null || minText.getText()==null || minText.getText().trim().length()==0) {
                model.min=0.0;
            } else {
                model.min=getValueSafely(minText.getText());
            }
            if (maxText==null || maxText.getText()==null || maxText.getText().trim().length()==0) {
                model.max=0.0;
            } else {
                model.max=getValueSafely(maxText.getText());
            }
            if (intensityButton.isSelected()) {
                model.type=INTENSITY_TYPE;
            } else if (distributionButton.isSelected()) {
                model.type=DISTRIBUTION_TYPE;
            }
            return model;
        }

    }

    public class MinMaxModel {
        public Double min;
        public Double max;
        public String type;

        @Override
        public boolean equals(Object o) {
            MinMaxModel other = (MinMaxModel)o;
            return min.equals(other.min) && max.equals(other.max) && type.equals(other.type);
        }
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
                cell.setBackground(DARK_GREEN);
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

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel currentSetNamePanel = new JPanel();
        JLabel currentSetNameLabel = new JLabel("Name of set: ");
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
        JPanel statusPane = (JPanel) statusObjects[0];
        statusLabel=(JLabel)statusObjects[1];
        JPanel savePanel = createSavePanel();
        mainPanel.add(savePanel, Box.createVerticalGlue());
        mainPanel.add(statusPane, Box.createVerticalGlue());

        add(mainPanel, BorderLayout.NORTH);

        quantifierLoaderWorker=createQuantifierLoaderWorker();
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
                saveResults();
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
        return new JTable();
    }
    
    private JScrollPane createFilterTableScrollPane() {
        return new JScrollPane(filterTable);
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
        Map<String, MinMaxModel> openFilterSetMap = new HashMap<String, MinMaxModel>();
        MinMaxModel globalModel = getOpenMinMaxModelInstance();
        openFilterSetMap.put(GLOBAL, globalModel);
        if (compartmentAbbreviationList != null) {
            for (String compartmentName : compartmentAbbreviationList) {
                MinMaxModel compartmentModel = getOpenMinMaxModelInstance();
                openFilterSetMap.put(compartmentName, compartmentModel);
            }
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
                    minMaxRow=new MinMaxSelectionRow(key, PatternAnnotationDataManager.getCompartmentDescription(key));
                    minMaxRowMap.put(key, minMaxRow);
                }
                minMaxRow.setModelState(model);
            }
        }
    }
    
    private MinMaxSelectionRow createGlobalMinMaxPanel() {
        return new MinMaxSelectionRow("Global", "Settings for all non-modified compartments");
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
    
    private void setStatusMessage(String message, Color color) {
        statusLabel.setForeground(color);
        statusLabel.setText(message);
    }

    public void showDialog() {
		this.returnInsteadOfSaving = false;
    	showDialog(null);
    }

    public org.janelia.it.workstation.model.entity.RootedEntity showDialog(org.janelia.it.workstation.model.entity.RootedEntity outputFolder) {
    	this.outputFolder = outputFolder;
    	this.saveFolder = null;
		this.returnInsteadOfSaving = false;
        quantifierLoaderWorker.execute();
        packAndShow();
    	return saveFolder;
    }
	
	public List<Long> showDialog(boolean returnInsteadOfSaving) {
		this.outputFolder = null;
		this.saveFolder = null;
		this.returnInsteadOfSaving = true;
        quantifierLoaderWorker.execute();
		packAndShow();
		try {
			List<Long> results = new ArrayList<Long>();
			if (saveClicked) {
    			if (filterResult!=null) {
    				List<Long> allResults = filterResult.getSampleList();
    				if (allResults!=null) {
    					results.addAll(new LinkedHashSet<Long>(allResults));
    				}
    			}
			}
			return results;
		}
		catch (Exception e) {
			org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().handleException(e);
			return new ArrayList<Long>();
		}
	}

	public String getSaveFolderName() {
		return currentSetTextField.getText();
	}
	
    private void initFilters() throws Exception {
        String initialFilterName = "Set " + getNextFilterSetIndex();
        currentSetTextField.setText(initialFilterName);
        createOpenFilterSet(initialFilterName);
        setCurrentFilterModel(filterSetMap.get(initialFilterName));
        setupFilterTable();
    }
    
    /** Convenience method to avoid exceptions when user mis-enters values. */
    private Double getValueSafely(String text) {
        Double rtnVal = 0.0;
        try {
            rtnVal = new Double(text);
        } catch (NumberFormatException nfe) {
            rtnVal = 0.0;
        }
        return rtnVal;
    }

    private Double getValueSafely(Object obj) {
        Double rtnVal = 0.0;
        try {
            String text = obj.toString();
            rtnVal = new Double(text);
        } catch (Exception nfe) {
            rtnVal = 0.0;
        }
        return rtnVal;
    }

    private void setupFilterTable() throws Exception {
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
                return col == FT_INDEX_MIN ||
                       col == FT_INDEX_MAX ||
                       col == FT_INDEX_FILTERTYPE;
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
                        return compartmentRow.getAbbreviation();
                    case FT_INDEX_DESCRIPTION:
                        return compartmentRow.getDescription();
                    case FT_INDEX_FILTERTYPE:
                        return compartmentRow.getModelState().type;
                    case FT_INDEX_MIN:
                        return compartmentRow.getModelState().min;
                    case FT_INDEX_MAX:
                        return compartmentRow.getModelState().max;
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
                if (col==FT_INDEX_MIN) {
                    Double newValue=getValueSafely(value);
                    if (newValue<0.0) {
                        newValue=0.0;
                    }
                    if (newValue>state.max) {
                        newValue=state.max;
                    }
                    state.min=newValue;
                    compartmentRow.setModelState(state);
                    try {
                        updateRowImpactOnCounts(row);
                    } catch (Exception ex) {}
                } else if (col==FT_INDEX_MAX) {
                    Double newValue=getValueSafely(value);
                    if (newValue>100.0) {
                        newValue=100.0;
                    }
                    if (newValue<state.min) {
                        newValue=state.min;
                    }
                    state.max=newValue;
                    compartmentRow.setModelState(state);
                    try {
                        updateRowImpactOnCounts(row);
                    } catch (Exception ex) {}
                } else if (col==FT_INDEX_FILTERTYPE) {
                    state.type=(String)value;
                    compartmentRow.setModelState(state);
                    try {
                        updateRowImpactOnCounts(row);
                    } catch (Exception ex) {}
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
        if (!quantifierDataIsLoading) {
            quantifierDataIsLoading = true;
        }
        try {
            Long startTime = new Date().getTime();
            int loadingState = org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().patternSearchGetState();
            while (loadingState == PatternAnnotationDataManager.STATE_LOADING && (new Date().getTime() - startTime) < MAX_LOADING_WAIT_MS) {
                Thread.sleep(1000);
                loadingState = org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().patternSearchGetState();
            }
            if (loadingState != PatternAnnotationDataManager.STATE_READY) {
                throw new Exception(("Pattern Annotation loading timeout"));
            }
            Long endTime = new Date().getTime();
            Long loadingTime = endTime - startTime;
            log.info("PatterSearchDialog : Pattern Annotation loading time=" + loadingTime + " ms");
            compartmentAbbreviationList = org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().patternSearchGetCompartmentList(RelativePatternAnnotationDataManager.RELATIVE_TYPE);
            List<DataDescriptor> relativeDescriptorList = org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().patternSearchGetDataDescriptors(RelativePatternAnnotationDataManager.RELATIVE_TYPE);
            managerDescriptorMap.put(RelativePatternAnnotationDataManager.RELATIVE_TYPE, relativeDescriptorList);
            initializeCurrentListModified();
            initFilters();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        quantifierDataIsLoading = false;
    }

    SimpleWorker createQuantifierLoaderWorker() {
        return new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                Utils.setWaitingCursor(PatternSearchDialog.this);
                setStatusMessage("Loading quantifier maps...", Color.RED);
                loadPatternAnnotationQuantifierMapsFromSummary();
                refreshCompartmentTable();
                currentSetInitialized=true;
            }

            @Override
            protected void hadSuccess() {
                resetSearchState();
            }

            @Override
            protected void hadError(Throwable error) {
                Utils.setDefaultCursor(PatternSearchDialog.this);
                org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().handleException(error);
                setStatusMessage("Error during quantifier load", Color.RED);
            }
        };
    }

    protected Long computeLineCountForCompartment(int rowIndex) {
        String rowKey=compartmentAbbreviationList.get(rowIndex);
        return computeLineCountForCompartment(rowKey);
    }

    protected Long computeLineCountForCompartment(String compartmentAbbreviation) {
        if (quantifierDataIsLoading) {
            return 0L;
        } else {
            Map<String, Long> countMap=filterResult.getCountMap();
            Long countMapValue=countMap.get(compartmentAbbreviation);
            if (countMapValue==null) {
                // We assume this is benign because the requested compartment was not in the filter criteria, so we
                // simply return the total number of entries as the unfiltered result
                return filterResult.getTotalSampleCount();
            } else {
                return countMapValue;
            }
        }
    }

    protected void refreshCompartmentTable() throws Exception {
        List<Integer> rowUpdateList=new ArrayList<Integer>();
        for (int rowIndex=0;rowIndex<compartmentAbbreviationList.size();rowIndex++) {
            rowUpdateList.add(rowIndex);
        }
        updateRowImpactOnCounts(rowUpdateList);
        filterTableScrollPane.update(filterTableScrollPane.getGraphics());
    }

    protected FilterResult generateMembershipListForCurrentSet() throws Exception {

        FilterResult emptyFilterResult=new FilterResult();
        // Check if we are still loading
        if (quantifierDataIsLoading) {
            log.info("returning emptyFilterResult because quantifierDataIsLoading");
            return emptyFilterResult; // just return empty set
        }

        // New code - we need to construct a list of DataFilters which describe, for both Intensity and
        // Distribution, the min/max settings from the gui. The trick with the DataFilter set is that
        // we don't need to include one for compartments in which the settings are wide-open.
        Map<String, Set<DataFilter>> filterMap=new HashMap<String, Set<DataFilter>>();

        for (DataDescriptor dataDescriptor : managerDescriptorMap.get(RelativePatternAnnotationDataManager.RELATIVE_TYPE)) {
            Float dMin=dataDescriptor.getMin();
            Float dMax=dataDescriptor.getMax();
            Set<DataFilter> filterSet=new HashSet<DataFilter>();
            for (String compartment : compartmentAbbreviationList) {
                MinMaxSelectionRow row=minMaxRowMap.get(compartment);
                MinMaxModel rowState=row.getModelState();
                String rowType=rowState.type;
                Float rowMin=rowState.min.floatValue();
                Float rowMax=rowState.max.floatValue();
                if (dataDescriptor.getName().equals(rowType)) {
                    if (rowMin>dMin || rowMax<dMax) {
                        DataFilter filter=new DataFilter(compartment, rowMin, rowMax);
                        filterSet.add(filter);
                    }
                }
            }
            filterMap.put(dataDescriptor.getName(), filterSet);
        }

        FilterResult filterResult= org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().patternSearchGetFilteredResults(RelativePatternAnnotationDataManager.RELATIVE_TYPE, filterMap);
        setStatusMessage("Result has " + filterResult.getSampleList().size()+" members", Color.GREEN);

        return filterResult;
    }

    protected void updateRowImpactOnCounts(int rowIndex) throws Exception {
        List<Integer> rowList=new ArrayList<Integer>();
        rowList.add(rowIndex);
        updateRowImpactOnCounts(rowList);
    }

    protected void updateRowImpactOnCounts(List<Integer> rowList) throws Exception {
        filterResult=generateMembershipListForCurrentSet();
        for (int rowIndex : rowList) {
            String rowKey=compartmentAbbreviationList.get(rowIndex);
            MinMaxSelectionRow compartmentRow=minMaxRowMap.get(rowKey);
            Long updatedLineCount=computeLineCountForCompartment(rowIndex);
            compartmentRow.lineCountText.setText(updatedLineCount.toString());
        }
    }

    protected synchronized void saveResults() {

		if (returnInsteadOfSaving) {
		    this.saveClicked = true;
            setVisible(false);
			return;
		}

		if (filterResult.getSampleList().size()>MAX_ENTITIES_IN_FOLDER) {
            JOptionPane.showMessageDialog(PatternSearchDialog.this, "You can save a maximum of "+MAX_ENTITIES_IN_FOLDER+" results into a single folder. Please adjust your search criteria.", 
                    "Result set has too many members", JOptionPane.ERROR_MESSAGE);
            return;
		}
		
        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
            	List<Long> samples = new ArrayList<Long>(new LinkedHashSet<Long>(filterResult.getSampleList()));
				saveFolder = FolderUtils.saveEntitiesToFolder(outputFolder==null?null:outputFolder, 
						currentSetTextField.getText(), samples);
            }

            @Override
            protected void hadSuccess() {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(org.janelia.it.workstation.api.entity_model.management.EntitySelectionModel.CATEGORY_OUTLINE, saveFolder.getUniqueId(), true);
                        setVisible(false);
                        resetSearchState();
                    }
                });
            }

            @Override
            protected void hadError(Throwable error) {
                org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().handleException(error);
                Utils.setDefaultCursor(PatternSearchDialog.this);
                resetSearchState();
            }
        };

        Utils.setWaitingCursor(PatternSearchDialog.this);
        worker.execute();
    }

    protected void resetSearchState() {
        filterResult.clear();
        MinMaxModel globalModel=globalMinMaxPanel.getModelState();
        globalModel.min=0.0;
        globalModel.max=100.0;
        globalModel.type=INTENSITY_TYPE;
        globalMinMaxPanel.setModelState(globalModel);
        for (int row=0;row<currentListModified.size();row++) {
            currentListModified.set(row, false);
        }
        globalMinMaxPanel.applyGlobalSettings();
        Utils.setDefaultCursor(PatternSearchDialog.this);
        setStatusMessage("Ready", Color.GREEN);
    }


}
