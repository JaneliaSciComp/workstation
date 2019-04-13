package org.janelia.workstation.browser.gui.dialogs.download;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.janelia.workstation.integration.FrameworkImplProvider;
import org.janelia.workstation.core.model.descriptors.ArtifactDescriptor;
import org.janelia.workstation.core.model.descriptors.DescriptorUtils;
import org.janelia.workstation.core.model.descriptors.ArtifactDescriptorList;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.enums.FileType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Multiset;

/**
 * Holds the entire state of the download wizard, including the initial state, 
 * the user's inputs along the way, and the final calculated file set for download.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
class DownloadWizardState {

    public static final String NATIVE_EXTENSION = "No conversion";
    private static final ObjectMapper mapper = new ObjectMapper();
    
    // Wizard input
    private List<? extends DomainObject> inputObjects;
    private ArtifactDescriptor defaultResultDescriptor;
    
    // Calculated from Wizard input
    private List<DownloadObject> downloadObjects;
    private List<ArtifactDescriptor> artifactDescriptors = new ArrayList<>();
    private Map<ArtifactDescriptor,Multiset<FileType>> artifactFileCounts = new HashMap<>();
    
    // User input from panel 1
    private String objective;
    private String area;
    private String resultCategory;
    private String imageCategory;
    
    // User input from panel 2
    private boolean isSplitChannels;
    private Map<String,String> outputExtensions = new HashMap<>();
    
    // User input from panel 3
    private boolean flattenStructure;
    private String filenamePattern;
    
    // Output of panel 3
    private List<DownloadFileItem> downloadFileItems;

    public List<? extends DomainObject> getInputObjects() {
        return inputObjects;
    }

    public void setInputObjects(List<? extends DomainObject> inputObjects) {
        this.inputObjects = inputObjects;
    }

    public ArtifactDescriptor getDefaultResultDescriptor() {
        return defaultResultDescriptor;
    }

    public void setDefaultArtifactDescriptor(ArtifactDescriptor defaultResultDescriptor) {
        this.defaultResultDescriptor = defaultResultDescriptor;
    }
    
    public List<DownloadObject> getDownloadObjects() {
        return downloadObjects;
    }

    public void setDownloadObjects(List<DownloadObject> downloadObjects) {
        this.downloadObjects = downloadObjects;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getResultCategory() {
        return resultCategory;
    }

    public void setResultCategory(String resultCategory) {
        this.resultCategory = resultCategory;
    }

    public String getImageCategory() {
        return imageCategory;
    }

    public void setImageCategory(String imageCategory) {
        this.imageCategory = imageCategory;
    }

    public List<ArtifactDescriptor> getArtifactDescriptors() {
        return artifactDescriptors;
    }

    public List<ArtifactDescriptor> getSelectedArtifactDescriptors() {
        return artifactDescriptors
                .stream()
                .filter((ad) -> { 
                    return !ad.getSelectedFileTypes().isEmpty(); 
                })
                .collect(Collectors.toList());
    }
    
    public void setArtifactDescriptors(List<ArtifactDescriptor> artifactDescriptors) {
        this.artifactDescriptors = artifactDescriptors;
    }
    
    public Map<ArtifactDescriptor, Multiset<FileType>> getArtifactFileCounts() {
        return artifactFileCounts;
    }

    public void setArtifactFileCounts(Map<ArtifactDescriptor, Multiset<FileType>> artifactFileCounts) {
        this.artifactFileCounts = artifactFileCounts;
    }

    public String getArtifactDescriptorString() {
        try {
            ArtifactDescriptorList list = new ArtifactDescriptorList();
            list.addAll(artifactDescriptors);
            return DescriptorUtils.serializeList(list);
        }
        catch (Exception e) {
            FrameworkImplProvider.handleExceptionQuietly(e);
            return null;
        }
    }

    public void setArtifactDescriptorString(String artifactDescriptorString) throws Exception {
        if (StringUtils.isBlank(artifactDescriptorString)) return;
        try {
            this.artifactDescriptors = DescriptorUtils.deserializeList(artifactDescriptorString);
        }
        catch (Exception e) {
            this.artifactDescriptors = new ArrayList<>();
            throw e;
        }
    }
    
    public boolean isSplitChannels() {
        return isSplitChannels;
    }

    public void setSplitChannels(boolean isSplitChannels) {
        this.isSplitChannels = isSplitChannels;
    }

    public Map<String, String> getOutputExtensions() {
        return outputExtensions;
    }

    public void setOutputExtensions(Map<String, String> outputExtensions) {
        this.outputExtensions = outputExtensions;
    }

    public String getOutputExtensionString() {
        try {
            OutputExtensionMap map = new OutputExtensionMap();
            map.putAll(outputExtensions);
            return mapper.writeValueAsString(map);
        }
        catch (Exception e) {
            FrameworkImplProvider.handleExceptionQuietly(e);
            return null;
        }
    }
    
    public void setOutputExtensionString(String outputExtensionString) throws Exception {
        if (StringUtils.isBlank(outputExtensionString)) return;
        try {
            this.outputExtensions = mapper.readValue(outputExtensionString, OutputExtensionMap.class);
        }
        catch (Exception e) {
            this.outputExtensions = new HashMap<>();
            throw e;
        }
    }
    
    public boolean isFlattenStructure() {
        return flattenStructure;
    }

    public void setFlattenStructure(boolean flattenStructure) {
        this.flattenStructure = flattenStructure;
    }

    public String getFilenamePattern() {
        return filenamePattern;
    }

    public void setFilenamePattern(String filenamePattern) {
        this.filenamePattern = filenamePattern;
    }

    public List<DownloadFileItem> getDownloadItems() {
        return downloadFileItems;
    }

    public void setDownloadItems(List<DownloadFileItem> downloadFileItems) {
        this.downloadFileItems = downloadFileItems;
    }

    public boolean has3d() {
        if (artifactDescriptors!=null) {
            for (ArtifactDescriptor artifactDescriptor : artifactDescriptors) {
                List<FileType> fileTypes = artifactDescriptor.getSelectedFileTypes();
                if (fileTypes!=null) {
                    for (FileType fileType : fileTypes) {
                        if (!fileType.is2dImage()) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
        // default to true so that the user can see the potential 
        // 3d processing panel before they choose any artifacts
        return true;
    }

}
