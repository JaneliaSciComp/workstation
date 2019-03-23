package org.janelia.it.workstation.browser.gui.dialogs.download;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
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
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.gui.support.WrapLayout;
import org.janelia.it.workstation.browser.gui.support.buttons.DropDownButton;
import org.janelia.it.workstation.browser.model.ImageCategory;
import org.janelia.it.workstation.browser.model.ResultCategory;
import org.janelia.it.workstation.browser.model.descriptors.ArtifactDescriptor;
import org.janelia.it.workstation.browser.model.descriptors.LSMArtifactDescriptor;
import org.janelia.it.workstation.browser.model.descriptors.ResultArtifactDescriptor;
import org.janelia.it.workstation.browser.model.descriptors.SelfArtifactDescriptor;
import org.janelia.model.domain.enums.FileType;
import org.janelia.model.domain.sample.SamplePostProcessingResult;
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
    private final DropDownButton objectiveButton = new DropDownButton();
    private final DropDownButton areaButton = new DropDownButton();
    private final DropDownButton resultCategoryButton = new DropDownButton();
    private final DropDownButton imageCategoryButton = new DropDownButton();
    private final HashMap<ArtifactDescriptor, HashMap<FileType, JCheckBox>> fileTypesCheckboxes = new LinkedHashMap<>();
    private JPanel checkboxPanel;
    
    // Inputs
    private ArtifactDescriptor defaultResultDescriptor;
    private Map<ArtifactDescriptor, Multiset<FileType>> artifactFileCounts;
    private String currObjective;
    private String currArea;
    private String currResultCategory;
    private String currImageCategory;
    private List<String> objectives;
    private List<String> areas;
    private List<String> resultCategories;
    private List<String> imageCategories;

    // Outputs
    private List<ArtifactDescriptor> artifactDescriptors;
    
    @Override
    public String getName() {
        return "Result Types";
    }
    
    /**
     * Creates new form DownloadVisualPanel1
     */
    public DownloadVisualPanel1(DownloadWizardPanel1 wizardPanel) {
        this.wizardPanel = wizardPanel;
        setLayout(new BorderLayout());
    }

    private void populateObjectiveButton(List<String> objectives) {
        objectiveButton.setText("Objective: "+currObjective);
        objectiveButton.removeAll();
        ButtonGroup group = new ButtonGroup();
        for (final String objective : objectives) {
            JMenuItem menuItem = new JRadioButtonMenuItem(objective, StringUtils.areEqual(objective, currObjective));
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setObjective(objective);
                    refreshCheckboxPanel();
                }
            });
            group.add(menuItem);
            objectiveButton.addMenuItem(menuItem);
        }
    }
    
    private void setObjective(String objective) {
        this.currObjective = objective;
        objectiveButton.setText("Objective: "+currObjective);
        populateObjectiveButton(objectives);
    }

    private void populateAreaButton(List<String> areas) {
        areaButton.setText("Area: "+currArea);
        areaButton.removeAll();
        ButtonGroup group = new ButtonGroup();
        for (final String area : areas) {
            JMenuItem menuItem = new JRadioButtonMenuItem(area, StringUtils.areEqual(area, currArea));
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setArea(area);
                    refreshCheckboxPanel();
                }
            });
            group.add(menuItem);
            areaButton.addMenuItem(menuItem);
        }
    }
    
    private void setArea(String area) {
        this.currArea = area;
        areaButton.setText("Area: "+currArea);
        populateAreaButton(areas);
    }

    private void populateResultCategoryButton(List<String> resultCategories) {
        resultCategoryButton.setText("Result Category: "+currResultCategory);
        resultCategoryButton.removeAll();
        ButtonGroup group = new ButtonGroup();
        for (final String resultCategory : resultCategories) {
            JMenuItem menuItem = new JRadioButtonMenuItem(resultCategory, StringUtils.areEqual(resultCategory, currResultCategory));
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setResultCategory(resultCategory);
                    refreshCheckboxPanel();
                }
            });
            group.add(menuItem);
            resultCategoryButton.addMenuItem(menuItem);
        }
    }
    
    private void setResultCategory(String resultCategory) {
        if (resultCategories.contains(resultCategory)) {
            this.currResultCategory = resultCategory;
        }
        else {
            this.currResultCategory = ALL_VALUE;
        }
        resultCategoryButton.setText("Result Category: "+currResultCategory);
        populateResultCategoryButton(resultCategories);
    }

    private void populateImageCategoryButton(List<String> imageCategories) {
        imageCategoryButton.setText("Image Category: "+currImageCategory);
        imageCategoryButton.removeAll();
        ButtonGroup group = new ButtonGroup();
        for (final String imageCategory : imageCategories) {
            JMenuItem menuItem = new JRadioButtonMenuItem(imageCategory, StringUtils.areEqual(imageCategory, currImageCategory));
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setImageCategory(imageCategory);
                    refreshCheckboxPanel();
                }
            });
            group.add(menuItem);
            imageCategoryButton.addMenuItem(menuItem);
        }
    }
    
    private void setImageCategory(String resultCategory) {
        if (imageCategories.contains(resultCategory)) {
            this.currImageCategory = resultCategory;
        }
        else {
            this.currImageCategory = ALL_VALUE;
        }
        imageCategoryButton.setText("Image Category: "+currImageCategory);
        populateImageCategoryButton(imageCategories);
    }
    
    public void init(DownloadWizardState state) {
        this.initialState = state;
        this.defaultResultDescriptor = state.getDefaultResultDescriptor();
        this.artifactDescriptors = state.getArtifactDescriptors();
        this.artifactFileCounts = state.getArtifactFileCounts();

        // Init filter values
        buildFilterValueLists();
        
        if (defaultResultDescriptor!=null) {
            log.info("Using defaultResultDescriptor={}", defaultResultDescriptor);
            artifactDescriptors = Arrays.asList(defaultResultDescriptor);
            List<FileType> defaultFileTypes = new ArrayList<>();
            defaultFileTypes.add(FileType.LosslessStack);
            defaultFileTypes.add(FileType.VisuallyLosslessStack);
            defaultResultDescriptor.setSelectedFileTypes(defaultFileTypes);
            setObjective(defaultResultDescriptor.getObjective());
            setArea(defaultResultDescriptor.getArea());
            setResultCategory(defaultResultDescriptor.isAligned() ? ResultCategory.ALIGNED.getLabel() : ResultCategory.PROCESSED.getLabel());
            setImageCategory(ImageCategory.Image3d.getLabel());
        }
        else {
            log.info("Using existing state={}", state);
            // Only set filters if there is no descriptor override
            setObjective(state.getObjective());
            setArea(state.getArea());
            setResultCategory(state.getResultCategory());
            setImageCategory(state.getImageCategory());
        }
        
        JButton resetButton = new JButton();
        resetButton = new JButton("Reset Filters");
        resetButton.setToolTipText("Clear all filters");
        resetButton.setFocusable(false);
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { 
                resetFilters();
            }
        });
        
        this.configPane = new JPanel(new WrapLayout(false, WrapLayout.LEFT, 2, 3));
        configPane.setBorder(BorderFactory.createEmptyBorder(2, 2, 8, 2));
        configPane.add(objectiveButton);
        configPane.add(areaButton);
        configPane.add(resultCategoryButton);
        configPane.add(imageCategoryButton);
        configPane.add(resetButton);
        
        
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.PAGE_AXIS));

        JButton selectAllButton = new JButton();
        selectAllButton = new JButton("Select all");
        selectAllButton.setToolTipText("Clear all selections");
        selectAllButton.setFocusable(false);
        selectAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { 
                for (ArtifactDescriptor artifactDescriptor : artifactFileCounts.keySet()) {
                    HashMap<FileType, JCheckBox> fileTypeMap = fileTypesCheckboxes.get(artifactDescriptor);
                    if (fileTypeMap!=null) {
                        for(FileType fileType : fileTypeMap.keySet()) {
                            JCheckBox fileTypeCheckbox = fileTypeMap.get(fileType);
                            fileTypeCheckbox.setSelected(true);
                        }
                    }
                }
                triggerValidation();
            }
        });
        
        JButton clearAllButton = new JButton();
        clearAllButton = new JButton("Clear all");
        clearAllButton.setToolTipText("Clear all selections");
        clearAllButton.setFocusable(false);
        clearAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { 
                for (ArtifactDescriptor artifactDescriptor : artifactFileCounts.keySet()) {
                    HashMap<FileType, JCheckBox> fileTypeMap = fileTypesCheckboxes.get(artifactDescriptor);
                    if (fileTypeMap!=null) {
                        for(FileType fileType : fileTypeMap.keySet()) {
                            JCheckBox fileTypeCheckbox = fileTypeMap.get(fileType);
                            fileTypeCheckbox.setSelected(false);
                        }
                    }
                }
                triggerValidation();
            }
        });
        
        buttonPane.add(Box.createVerticalGlue()); // Align to bottom
        buttonPane.add(selectAllButton);
        buttonPane.add(clearAllButton);
        
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

    private void resetFilters() {
        setObjective(ALL_VALUE);
        setArea(ALL_VALUE);
        setResultCategory(ALL_VALUE);
        setImageCategory(ALL_VALUE);
        refreshCheckboxPanel();
        configPane.updateUI();
    }
    
    private void buildFilterValueLists() {

        Set<String> objectiveSet = new TreeSet<>();
        Set<String> areaSet = new TreeSet<>();
        for (ArtifactDescriptor artifactDescriptor : artifactFileCounts.keySet()) {
            
            String objective = artifactDescriptor.getObjective();
            if (!StringUtils.isBlank(objective)) {
                objectiveSet.add(objective);
            }
            
            String area = artifactDescriptor.getArea();
            if (!StringUtils.isBlank(area)) {
                areaSet.add(area);
            }
            
        }
        
        objectives = new ArrayList<>(objectiveSet);
        objectives.add(0, ALL_VALUE);

        areas = new ArrayList<>(areaSet);
        areas.add(0, ALL_VALUE);

        resultCategories = new ArrayList<>();
        resultCategories.add(ALL_VALUE);
        for (ResultCategory resultCategory : ResultCategory.values()) {
            resultCategories.add(resultCategory.getLabel());
        }

        imageCategories = new ArrayList<>();
        imageCategories.add(ALL_VALUE);
        for (ImageCategory imageCategory : ImageCategory.values()) {
            imageCategories.add(imageCategory.getLabel());
        }
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
        
        if (fileTypesCheckboxes.isEmpty()) {
            // Nothing selected and this is the init step, so clear the filters and re-run
            checkboxPanel.add(Box.createRigidArea(new Dimension(5,5)));
            checkboxPanel.add(new JLabel("No results. Try changing your filters above."));
        }
    }

    private void addCheckboxes(Collection<DownloadObject> domainObjects, JPanel checkboxPanel) throws Exception {

        fileTypesCheckboxes.clear();
        
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
            
            if (!accept(artifactDescriptor)) {
                continue;
            }
            
            final JPanel subPanel = new JPanel();
            subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.PAGE_AXIS));
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
                checkboxPanel.add(new JLabel(resultName));
                checkboxPanel.add(subPanel);
            }
        }
    }
    
    private boolean accept(ArtifactDescriptor artifactDescriptor) {

        if (artifactDescriptor instanceof SelfArtifactDescriptor) {
            // Always accept the self descriptor
            return true;
        }
    
        // For other descriptors, check the filter values

        if (currObjective!=null && !ALL_VALUE.equals(currObjective) && !currObjective.equals(artifactDescriptor.getObjective())) {
            return false;
        }

        if (currArea!=null && !ALL_VALUE.equals(currArea) && !currArea.equals(artifactDescriptor.getArea())) {
            return false;
        }
        
        ResultCategory resultCategory = ResultCategory.getByLabel(currResultCategory);
        if (ResultCategory.ORIGINAL.equals(resultCategory)) {
            if (!(artifactDescriptor instanceof LSMArtifactDescriptor)) {
                return false;
            }
        }
        else if (ResultCategory.PROCESSED.equals(resultCategory)) {
            if (artifactDescriptor instanceof LSMArtifactDescriptor) {
                return false;
            }
            else if (artifactDescriptor.isAligned()) {
                return false;
            } 
            else if (artifactDescriptor instanceof ResultArtifactDescriptor) {
                ResultArtifactDescriptor resultArtifactDescriptor = (ResultArtifactDescriptor)artifactDescriptor;
                if (resultArtifactDescriptor.getResultClass().equals(SamplePostProcessingResult.class.getName())) {
                    return false;
                }
            }
        }
        else if (ResultCategory.POST_PROCESSED.equals(resultCategory)) {
            if (artifactDescriptor instanceof ResultArtifactDescriptor) {
                ResultArtifactDescriptor resultArtifactDescriptor = (ResultArtifactDescriptor)artifactDescriptor;
                if (!resultArtifactDescriptor.getResultClass().equals(SamplePostProcessingResult.class.getName())) {
                    return false;
                }
            }
            else {
                return false;
            }
        }
        else if (ResultCategory.ALIGNED.equals(resultCategory)) {
            if (!artifactDescriptor.isAligned()) {
                return false;
            }
        }
        
        return true;
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
        for (ArtifactDescriptor artifactDescriptor : artifactFileCounts.keySet()) {
                        
            List<FileType> fileTypes = artifactDescriptor.getSelectedFileTypes();
            fileTypes.clear();
            
            HashMap<FileType, JCheckBox> fileTypeMap = fileTypesCheckboxes.get(artifactDescriptor);
            if (fileTypeMap!=null) {
                for(FileType fileType : fileTypeMap.keySet()) {
                    JCheckBox fileTypeCheckbox = fileTypeMap.get(fileType);
                    if (fileTypeCheckbox.isSelected()) {
                        fileTypes.add(fileType);
                    }
                }
            }
            
            artifactDescriptors.add(artifactDescriptor);
        }
    }
}
