package org.janelia.it.FlyWorkstation.gui.dialogs;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.jacs.shared.annotation.PatternAnnotationDataManager;

import javax.swing.*;
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


    private final JPanel mainPanel;
    private final JPanel currentSetNamePanel;
    private final MinMaxSelectionPanel globalSettingsPanel;
    private final JPanel statusPane;

    private final JLabel statusLabel;
	private final JLabel currentSetNameLabel;
    private final JTextField currentSetTextField;

    private final SimpleWorker quantifierLoaderWorker;
    
    private final Map<String, Map<String, MinMaxModel>> filterSetMap=new HashMap<String, Map<String, MinMaxModel>>();

    static boolean quantifierDataIsLoading=false;
    static protected Map<Long, Map<String,String>> sampleInfoMap=null;
    static protected Map<Long, List<Double>> quantifierInfoMap=null;
    
    private class MinMaxSelectionPanel extends JPanel implements ActionListener {
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

        public MinMaxSelectionPanel(String abbreviation, String description) {
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
        
        globalSettingsPanel = createGlobalSettingsPanel();
        mainPanel.add(globalSettingsPanel, Box.createVerticalGlue());

        Object[] statusObjects = createStatusObjects();
        statusPane=(JPanel)statusObjects[0];
        statusLabel=(JLabel)statusObjects[1];
        mainPanel.add(statusPane, Box.createVerticalGlue());

        add(mainPanel, BorderLayout.NORTH);

        quantifierLoaderWorker=createQuantifierLoaderWorker();

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

    private void createInitialFilterSet() {
        currentSetTextField.setText("Set 1");
        Map<String, MinMaxModel> initialFilterSetMap=new HashMap<String, MinMaxModel>();
        MinMaxModel globalModel=getOpenMinMaxModelInstance();
        initialFilterSetMap.put(GLOBAL, globalModel);
        List<String> compartmentAbbreviationList= PatternAnnotationDataManager.getCompartmentListInstance();
        for (String compartmentName : compartmentAbbreviationList) {
            MinMaxModel compartmentModel=getOpenMinMaxModelInstance();
            initialFilterSetMap.put(compartmentName, compartmentModel);
        }
        filterSetMap.put(currentSetTextField.getText(), initialFilterSetMap);
        setCurrentFilterSetMap(filterSetMap.get(currentSetTextField.getText()));      
    }
    
    private void setCurrentFilterSetMap(Map<String, MinMaxModel> filterMap) {

    }
    
    private MinMaxSelectionPanel createGlobalSettingsPanel() {
        MinMaxSelectionPanel globalSettingsPanel=new MinMaxSelectionPanel("Global", "Settings for all non-modified compartments");
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
        createInitialFilterSet();
        packAndShow();
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
