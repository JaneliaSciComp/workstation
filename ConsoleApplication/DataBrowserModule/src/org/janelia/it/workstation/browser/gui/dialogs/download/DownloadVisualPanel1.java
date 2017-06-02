package org.janelia.it.workstation.browser.gui.dialogs.download;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.support.ResultDescriptor;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.gui.support.DropDownButton;
import org.janelia.it.workstation.browser.gui.support.WrapLayout;
import org.janelia.it.workstation.browser.model.ImageCategory;
import org.janelia.it.workstation.browser.model.ResultCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multiset;

public final class DownloadVisualPanel1 extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(DownloadVisualPanel1.class);
    
    // Constants
    private final static String ALL_VALUE = "all";

    // Controller
    private DownloadWizardState initialState;
    private DownloadWizardPanel1 wizardPanel;
    
    // GUI
    private JPanel configPane;
    private DropDownButton objectiveButton;
    private DropDownButton areaButton;
    private DropDownButton resultCategoryButton;
    private DropDownButton imageCategoryButton;
    private HashMap<ArtifactDescriptor, JCheckBox> artifactCheckboxes = new LinkedHashMap<>();
    private HashMap<ArtifactDescriptor, HashMap<FileType, JCheckBox>> fileTypesCheckboxes = new LinkedHashMap<>();
    
    // Inputs
    private ResultDescriptor defaultResultDescriptor;
    private Map<ArtifactDescriptor, Multiset<FileType>> artifactFileCounts;
    private String currObjective;
    private String currArea;
    private String currResultCategory;
    private String currImageCategory;

    // Outputs
    private List<ArtifactDescriptor> artifactDescriptors;

    private JPanel checkboxPanel;

    
    @Override
    public String getName() {
        return "Result Types";
    }
    
    /**
     * Creates new form DownloadVisualPanel2
     */
    public DownloadVisualPanel1(DownloadWizardPanel1 wizardPanel) {
        this.wizardPanel = wizardPanel;
        setLayout(new BorderLayout());
    }

    private void populateObjectiveButton(List<String> objectives) {
        objectiveButton.setText("Objective: "+currObjective);
        JPopupMenu popupMenu = objectiveButton.getPopupMenu();
        popupMenu.removeAll();
        ButtonGroup group = new ButtonGroup();
        for (final String objective : objectives) {
            JMenuItem menuItem = new JRadioButtonMenuItem(objective, StringUtils.areEqual(objective, currObjective));
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setObjective(objective);
                }
            });
            group.add(menuItem);
            popupMenu.add(menuItem);
        }
    }
    
    private void setObjective(String objective) {
        this.currObjective = objective;
        objectiveButton.setText("Objective: "+currObjective);
        refreshCheckboxPanel();
    }

    private void populateAreaButton(List<String> areas) {
        areaButton.setText("Area: "+currArea);
        JPopupMenu popupMenu = areaButton.getPopupMenu();
        popupMenu.removeAll();
        ButtonGroup group = new ButtonGroup();
        for (final String area : areas) {
            JMenuItem menuItem = new JRadioButtonMenuItem(area, StringUtils.areEqual(area, currArea));
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setArea(area);
                }
            });
            group.add(menuItem);
            popupMenu.add(menuItem);
        }
    }
    
    private void setArea(String area) {
        this.currArea = area;
        areaButton.setText("Area: "+currArea);
        refreshCheckboxPanel();
    }

    private void populateResultCategoryButton(List<String> resultCategories) {
        resultCategoryButton.setText("Result Category: "+currResultCategory);
        JPopupMenu popupMenu = resultCategoryButton.getPopupMenu();
        popupMenu.removeAll();
        ButtonGroup group = new ButtonGroup();
        for (final String resultCategory : resultCategories) {
            JMenuItem menuItem = new JRadioButtonMenuItem(resultCategory, StringUtils.areEqual(resultCategory, currResultCategory));
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setResultCategory(resultCategory);
                }
            });
            group.add(menuItem);
            popupMenu.add(menuItem);
        }
    }
    
    private void setResultCategory(String resultCategory) {
        this.currResultCategory = resultCategory;
        resultCategoryButton.setText("Result Category: "+currResultCategory);
        refreshCheckboxPanel();
    }

    private void populateImageCategoryButton(List<String> imageCategories) {
        imageCategoryButton.setText("Image Category: "+currImageCategory);
        JPopupMenu popupMenu = imageCategoryButton.getPopupMenu();
        popupMenu.removeAll();
        ButtonGroup group = new ButtonGroup();
        for (final String imageCategory : imageCategories) {
            JMenuItem menuItem = new JRadioButtonMenuItem(imageCategory, StringUtils.areEqual(imageCategory, currImageCategory));
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setImageCategory(imageCategory);
                }
            });
            group.add(menuItem);
            popupMenu.add(menuItem);
        }
    }
    
    private void setImageCategory(String resultCategory) {
        this.currImageCategory = resultCategory;
        imageCategoryButton.setText("Image Category: "+currImageCategory);
        refreshCheckboxPanel();
    }
    
    public void init(DownloadWizardState state) {
        this.initialState = state;
        this.defaultResultDescriptor = state.getDefaultResultDescriptor();
        this.artifactDescriptors = state.getArtifactDescriptors();
        this.artifactFileCounts = state.getArtifactFileCounts();
        
        // TODO: select the artifact descriptors matching the default result descriptor
//        if (artifactDescriptors==null) {
//            artifactDescriptors = new ArrayList<>();
//            artifactDescriptors.add(defaultResultDescriptor);
//        }

        this.currObjective = state.getObjective();
        this.currArea = state.getArea();
        this.currResultCategory = state.getResultCategory();
        this.currImageCategory = state.getImageCategory();
        
        if (currObjective==null) {
            this.currObjective = ALL_VALUE;
        }

        if (currArea==null) {
            this.currArea = ALL_VALUE;
        }

        if (currResultCategory==null) {
            this.currResultCategory = ALL_VALUE;
        }

        if (currImageCategory==null) {
            this.currImageCategory = ALL_VALUE;
        }
        
        this.objectiveButton = new DropDownButton("Objective: "+currObjective);
        this.areaButton = new DropDownButton("Area: "+currArea);
        this.resultCategoryButton = new DropDownButton("Result Category: "+currResultCategory);
        this.imageCategoryButton = new DropDownButton("Image Category: "+currImageCategory);

        Set<String> objectiveSet = new TreeSet<>();
        Set<String> areaSet = new TreeSet<>();
        for (ArtifactDescriptor artifactDescriptor : artifactFileCounts.keySet()) {
            String objective = artifactDescriptor.getObjective();
            if (objective!=null) objectiveSet.add(objective);
            String area = artifactDescriptor.getArea();
            if (area!=null) areaSet.add(area);
        }
        
        List<String> objectives = new ArrayList<>(objectiveSet);
        objectives.add(0, ALL_VALUE);
        populateObjectiveButton(objectives);

        List<String> areas = new ArrayList<>(areaSet);
        areas.add(0, ALL_VALUE);
        populateAreaButton(areas);

        List<String> resultCategories = new ArrayList<>();
        resultCategories.add(ALL_VALUE);
        for (ResultCategory resultCategory : ResultCategory.values()) {
            resultCategories.add(resultCategory.getLabel());
        }
        populateResultCategoryButton(resultCategories);

        List<String> imageCategories = new ArrayList<>();
        imageCategories.add(ALL_VALUE);
        for (ImageCategory imageCategory : ImageCategory.values()) {
            imageCategories.add(imageCategory.getLabel());
        }
        populateImageCategoryButton(imageCategories);
        
        this.configPane = new JPanel(new WrapLayout(false, WrapLayout.LEFT, 2, 3));
        configPane.setBorder(BorderFactory.createEmptyBorder(2, 2, 8, 2));
        configPane.add(objectiveButton);
        configPane.add(areaButton);
        configPane.add(resultCategoryButton);
        configPane.add(imageCategoryButton);
        
        
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.PAGE_AXIS));

        JButton selectAllButton = new JButton();
        selectAllButton = new JButton("Select all");
        selectAllButton.setToolTipText("Clear all selections");
        selectAllButton.setFocusable(false);
        selectAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { 
                for (ArtifactDescriptor artifactDescriptor : artifactCheckboxes.keySet()) {
                    JCheckBox artifactCheckbox = artifactCheckboxes.get(artifactDescriptor);
                    artifactCheckbox.setSelected(true);
                    HashMap<FileType, JCheckBox> fileTypeMap = fileTypesCheckboxes.get(artifactDescriptor);
                    for(FileType fileType : fileTypeMap.keySet()) {
                        JCheckBox fileTypeCheckbox = fileTypeMap.get(fileType);
                        fileTypeCheckbox.setSelected(true);
                    }
                }
                triggerValidation();
            }
        });
        
        JButton resetButton = new JButton();
        resetButton = new JButton("Clear all");
        resetButton.setToolTipText("Clear all selections");
        resetButton.setFocusable(false);
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { 
                for (ArtifactDescriptor artifactDescriptor : artifactCheckboxes.keySet()) {
                    JCheckBox artifactCheckbox = artifactCheckboxes.get(artifactDescriptor);
                    artifactCheckbox.setSelected(false);
                    HashMap<FileType, JCheckBox> fileTypeMap = fileTypesCheckboxes.get(artifactDescriptor);
                    for(FileType fileType : fileTypeMap.keySet()) {
                        JCheckBox fileTypeCheckbox = fileTypeMap.get(fileType);
                        fileTypeCheckbox.setSelected(false);
                    }
                }
                triggerValidation();
            }
        });
        
        buttonPane.add(Box.createVerticalGlue()); // Align to bottom
        buttonPane.add(selectAllButton);
        buttonPane.add(resetButton);
        
        JPanel outerPanel = new JPanel(new BorderLayout());
        
        checkboxPanel = new JPanel();
        checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.PAGE_AXIS));
        refreshCheckboxPanel();

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(checkboxPanel);
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // Increase scroll speed

        outerPanel.add(configPane, BorderLayout.NORTH);
        outerPanel.add(scrollPane, BorderLayout.CENTER);
        outerPanel.add(buttonPane, BorderLayout.WEST);
        
        removeAll();
        add(outerPanel, BorderLayout.CENTER);
        
        triggerValidation();
    }
    
    private void refreshCheckboxPanel() {
        checkboxPanel.removeAll();
        try {
            addCheckboxes(initialState.getDownloadObjects(), checkboxPanel);
            checkboxPanel.updateUI();
        }
        catch (Exception e) {
            FrameworkImplProvider.handleException(e);
        }
    }

    private void addCheckboxes(Collection<DownloadObject> domainObjects, JPanel checkboxPanel) throws Exception {

        fileTypesCheckboxes.clear();
        artifactCheckboxes.clear();
        
        // Sort in alphanumeric order, with Latest first
        List<ArtifactDescriptor> sortedResults = new ArrayList<>(artifactFileCounts.keySet());
        Collections.sort(sortedResults, new Comparator<ArtifactDescriptor>() {
            @Override
            public int compare(ArtifactDescriptor o1, ArtifactDescriptor o2) {
                return o1.toString().compareTo(o2.toString());
            }
        });

        for(final ArtifactDescriptor artifactDescriptor : sortedResults) {
            String resultName = artifactDescriptor.toString();
            log.debug("Considering: "+resultName);
            
            // Pull out an old version of the descriptor, so that the previously selected file types can be copied out 
            int oldIndex = artifactDescriptors.indexOf(artifactDescriptor);
            boolean selected = oldIndex > -1;
            if (selected) {
                ArtifactDescriptor oldAd = artifactDescriptors.get(oldIndex);
                artifactDescriptor.setSelectedFileTypes(oldAd.getSelectedFileTypes());
                log.debug("Adding previously selected file types: "+oldAd.getSelectedFileTypes());
            }
            
            if (currObjective!=null && !ALL_VALUE.equals(currObjective) && !currObjective.equals(artifactDescriptor.getObjective())) {
                continue;
            }

            if (currArea!=null && !ALL_VALUE.equals(currArea) && !currArea.equals(artifactDescriptor.getArea())) {
                continue;
            }
            
            ResultCategory resultCategory = ResultCategory.getByLabel(currResultCategory);
            if (ResultCategory.OriginalLSM.equals(resultCategory) && !(artifactDescriptor instanceof LSMArtifactDescriptor)) {
                continue;
            }
            else if (ResultCategory.PreAligned.equals(resultCategory) && (artifactDescriptor.isAligned() || (artifactDescriptor instanceof LSMArtifactDescriptor))) {
                continue;
            }
            else if (ResultCategory.PostAligned.equals(resultCategory) && !artifactDescriptor.isAligned()) {
                continue;
            }
            
            final JPanel subPanel = new JPanel();
            subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.PAGE_AXIS));
            subPanel.setVisible(selected);
            subPanel.setBorder(BorderFactory.createEmptyBorder(0, 30, 5, 0));

            Multiset<FileType> fileTypesCounts = artifactFileCounts.get(artifactDescriptor);
            
            HashMap<FileType, JCheckBox> fileTypeMap = new LinkedHashMap<>();
            
            for(final FileType fileType : FileType.values()) {

                ImageCategory imageCategory = ImageCategory.getByLabel(currImageCategory);
                if (ImageCategory.Image2d.equals(imageCategory) && !fileType.is2dImage()) {
                    continue;
                }
                else if (ImageCategory.Image3d.equals(imageCategory) && !fileType.is3dImage()) {
                    continue;
                }
                
                if (fileTypesCounts.contains(fileType)) {
                    
                    String fileTypeLabel = fileType.getLabel();
                    JCheckBox fileTypeCheckbox = new JCheckBox(fileTypeLabel, artifactDescriptor.getSelectedFileTypes().contains(fileType));
                    fileTypeCheckbox.addItemListener(new ItemListener() {
                        @Override
                        public void itemStateChanged(ItemEvent e) {
                            triggerValidation();
                        }
                    });
                    subPanel.add(fileTypeCheckbox);
                    fileTypeMap.put(fileType, fileTypeCheckbox);
                }
            }
            
            if (!fileTypeMap.isEmpty()) {
                log.trace("Adding descriptor: {}", artifactDescriptor);
                fileTypesCheckboxes.put(artifactDescriptor, fileTypeMap);

                final JCheckBox artifactCheckbox = new JCheckBox(resultName, selected);
                artifactCheckbox.addItemListener(new ItemListener() {
                    @Override
                    public void itemStateChanged(ItemEvent e) {
                        subPanel.setVisible(artifactCheckbox.isSelected());
                        triggerValidation();
                    }
                });
                
                artifactCheckboxes.put(artifactDescriptor, artifactCheckbox);
                checkboxPanel.add(artifactCheckbox);
                checkboxPanel.add(subPanel);
            }
        }        
    }
    
    public List<ArtifactDescriptor> getArtifactDescriptors() {
        return artifactDescriptors;
    }
    
    public String getCurrObjective() {
        return currObjective;
    }

    public String getCurrArea() {
        return currArea;
    }

    public String getCurrResultCategory() {
        return currResultCategory;
    }

    public String getCurrImageCategory() {
        return currImageCategory;
    }

    private void triggerValidation() {
        updateArtifactDescriptors();
        wizardPanel.fireChangeEvent();
    }

    private void updateArtifactDescriptors() {
        artifactDescriptors = new ArrayList<>();
        for (ArtifactDescriptor artifactDescriptor : artifactCheckboxes.keySet()) {
            JCheckBox artifactCheckbox = artifactCheckboxes.get(artifactDescriptor);
            if (artifactCheckbox.isSelected()) {
                
                List<FileType> fileTypes = artifactDescriptor.getSelectedFileTypes();
                fileTypes.clear();
                
                HashMap<FileType, JCheckBox> fileTypeMap = fileTypesCheckboxes.get(artifactDescriptor);
                for(FileType fileType : fileTypeMap.keySet()) {
                    JCheckBox fileTypeCheckbox = fileTypeMap.get(fileType);
                    if (fileTypeCheckbox.isSelected()) {
                        fileTypes.add(fileType);
                    }
                }
                
                artifactDescriptors.add(artifactDescriptor);
            }
        }
    }
}
