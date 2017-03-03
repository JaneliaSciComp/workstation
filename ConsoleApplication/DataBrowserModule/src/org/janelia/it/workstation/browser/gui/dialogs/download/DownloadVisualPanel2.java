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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFileGroups;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;

public final class DownloadVisualPanel2 extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(DownloadVisualPanel2.class);

    private DownloadWizardPanel2 wizardPanel;
    
    // GUI
    private HashMap<ArtifactDescriptor, JCheckBox> artifactCheckboxes = new LinkedHashMap<>();
    private HashMap<ArtifactDescriptor, HashMap<FileType, JCheckBox>> fileTypesCheckboxes = new LinkedHashMap<>();
    
    // Inputs
    private ArtifactDescriptor defaultArtifactDescriptor;
    private Multiset<ArtifactDescriptor> artifacts;

    // Outputs
    private List<ArtifactDescriptor> artifactDescriptors;
    
    @Override
    public String getName() {
        return "Result Types";
    }
    
    /**
     * Creates new form DownloadVisualPanel2
     */
    public DownloadVisualPanel2(DownloadWizardPanel2 wizardPanel) {
        this.wizardPanel = wizardPanel;
        setLayout(new BorderLayout());
    }

    public void init(DownloadWizardState state) {
        this.defaultArtifactDescriptor = state.getDefaultArtifactDescriptor();
        this.artifacts = state.getArtifactCounts();
        this.artifactDescriptors = state.getArtifactDescriptors();
        
        if (artifactDescriptors==null) {
            artifactDescriptors = new ArrayList<>();
            artifactDescriptors.add(defaultArtifactDescriptor);
        }
        
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.PAGE_AXIS));
        
        JButton resetButton = new JButton();

        resetButton = new JButton("Clear selections");
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
        
        buttonPane.add(resetButton);
        
        JPanel outerPanel = new JPanel(new BorderLayout());
        
        JPanel checkboxPanel = new JPanel();
        checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.PAGE_AXIS));
        
        try {
            addCheckboxes(state.getDownloadObjects(), checkboxPanel);
        }
        catch (Exception e) {
            FrameworkImplProvider.handleException(e);
        }

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(checkboxPanel);
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));

        outerPanel.add(scrollPane, BorderLayout.CENTER);
        outerPanel.add(buttonPane, BorderLayout.WEST);
        
        removeAll();
        add(outerPanel, BorderLayout.CENTER);
        
        triggerValidation();
    }

    private void addCheckboxes(Collection<DownloadObject> domainObjects, JPanel checkboxPanel) throws Exception {

        // Sort in alphanumeric order, with Latest first
        List<ArtifactDescriptor> sortedResults = new ArrayList<>(artifacts.elementSet());
        Collections.sort(sortedResults, new Comparator<ArtifactDescriptor>() {
            @Override
            public int compare(ArtifactDescriptor o1, ArtifactDescriptor o2) {
                return o1.toString().compareTo(o2.toString());
            }
        });

        for(final ArtifactDescriptor artifactDescriptor : sortedResults) {
            String resultName = artifactDescriptor.toString();
            
            // Pull out an old version of the descriptor, so that the previously selected file types can be copied out 
            int oldIndex = artifactDescriptors.indexOf(artifactDescriptor);
            boolean selected = oldIndex > -1;
            if (selected) {
                ArtifactDescriptor oldAd = artifactDescriptors.get(oldIndex);
                artifactDescriptor.setFileTypes(oldAd.getFileTypes());
            }
            
            final JPanel subPanel = new JPanel();
            subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.PAGE_AXIS));
            subPanel.setVisible(selected);
            subPanel.setBorder(BorderFactory.createEmptyBorder(0, 30, 5, 0));

            Multiset<FileType> fileTypesCounts = getFileTypeCounts(domainObjects, artifactDescriptor);

            HashMap<FileType, JCheckBox> fileTypeMap = new LinkedHashMap<>();
            fileTypesCheckboxes.put(artifactDescriptor, fileTypeMap);

            fileTypesCounts.add(FileType.FirstAvailable3d);
            fileTypesCounts.add(FileType.FirstAvailable2d);
            
            for(final FileType fileType : FileType.values()) {
                if (fileTypesCounts.contains(fileType)) {
                    String fileTypeLabel = fileType.getLabel();
                    JCheckBox fileTypeCheckbox = new JCheckBox(fileTypeLabel, artifactDescriptor.getFileTypes().contains(fileType));
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
            
            final JCheckBox artifactCheckbox = new JCheckBox(resultName, selected);
            artifactCheckbox.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    subPanel.setVisible(artifactCheckbox.isSelected());
                    triggerValidation();
                }
            });
            
            checkboxPanel.add(artifactCheckbox);
            checkboxPanel.add(subPanel);
            
            artifactCheckboxes.put(artifactDescriptor, artifactCheckbox);
        }        
    }

    public Multiset<FileType> getFileTypeCounts(Collection<DownloadObject> downloadObjects, ArtifactDescriptor artifactDescriptor) throws Exception {

        Multiset<FileType> countedTypeNames = LinkedHashMultiset.create();
        
        Set<Object> sources = new LinkedHashSet<>();
        for(DownloadObject downloadObject : downloadObjects) {
            DomainObject domainObject = downloadObject.getDomainObject();
            sources.addAll(artifactDescriptor.getFileSources(domainObject));
        }
        
        boolean only2d = false;
        for (Object source : sources) {
            log.trace("Inspecting file sources {}", source);
            if (source instanceof HasFileGroups) {
                Multiset<FileType> fileTypes = DomainUtils.getFileTypes((HasFileGroups)source, only2d);
                log.trace("Source has file groups: {}",fileTypes);
                countedTypeNames.addAll(fileTypes);
            }
            if (source instanceof HasFiles) {
                Multiset<FileType> fileTypes = DomainUtils.getFileTypes((HasFiles) source, only2d);
                log.trace("Source has files: {}",fileTypes);
                countedTypeNames.addAll(fileTypes);
            }
            if (source instanceof PipelineResult) {
                PipelineResult result = (PipelineResult)source;
                NeuronSeparation separation = result.getLatestSeparationResult();
                log.trace("Source has separation: {}",separation);
                if (separation!=null) {
                    Set<FileType> typeNames = new HashSet<>();
                    typeNames.add(FileType.NeuronAnnotatorLabel);
                    typeNames.add(FileType.NeuronAnnotatorSignal);
                    typeNames.add(FileType.NeuronAnnotatorReference);
                    log.trace("Adding type names: {}",typeNames);
                    countedTypeNames.addAll(typeNames);
                }
            }
        }
        
        return countedTypeNames;
    }
    
    public List<ArtifactDescriptor> getArtifactDescriptors() {
        return artifactDescriptors;
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
                
                List<FileType> fileTypes = artifactDescriptor.getFileTypes();
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
