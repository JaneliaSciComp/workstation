package org.janelia.it.workstation.browser.gui.dialogs.download;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.support.ResultDescriptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Multiset;

/**
 * Holds the entire state of the download wizard, including the initial state, 
 * the user's inputs along the way, and the final calculated file set for download.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
class DownloadWizardState {
    
    // Wizard input
    private List<? extends DomainObject> inputObjects;
    private ResultDescriptor defaultResultDescriptor;
    
    // Calculated from Wizard input
    private List<DownloadObject> downloadObjects;
    private List<ArtifactDescriptor> artifactDescriptors;
    private Map<ArtifactDescriptor,Multiset<FileType>> artifactFileCounts;
    
    // User input from panel 2
    private String objective;
    private String area;
    private String resultCategory;
    private String imageCategory;
    
    // User input from panel 3
    private boolean isSplitChannels;
    private String outputFormat;
    
    // User input from panel 4
    private boolean flattenStructure;
    private String filenamePattern;
    
    // Output of panel 4
    private List<DownloadFileItem> downloadFileItems;

    public List<? extends DomainObject> getInputObjects() {
        return inputObjects;
    }

    public void setInputObjects(List<? extends DomainObject> inputObjects) {
        this.inputObjects = inputObjects;
    }

    public ResultDescriptor getDefaultResultDescriptor() {
        return defaultResultDescriptor;
    }

    public void setDefaultArtifactDescriptor(ResultDescriptor defaultResultDescriptor) {
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
            ObjectMapper mapper = new ObjectMapper();
            
            ArtifactDescriptorList list = new ArtifactDescriptorList();
            list.addAll(artifactDescriptors);
            
            return mapper.writeValueAsString(list);
        }
        catch (Exception e) {
            FrameworkImplProvider.handleExceptionQuietly(e);
            return null;
        }
    }

    public void setArtifactDescriptorString(String artifactDescriptorString) {
        if (StringUtils.isBlank(artifactDescriptorString)) return;
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.artifactDescriptors = mapper.readValue(artifactDescriptorString, ArtifactDescriptorList.class);
        }
        catch (Exception e) {
            this.artifactDescriptors = new ArrayList<>();
            FrameworkImplProvider.handleExceptionQuietly(e);
        }
    }
    
    public boolean isSplitChannels() {
        return isSplitChannels;
    }

    public void setSplitChannels(boolean isSplitChannels) {
        this.isSplitChannels = isSplitChannels;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
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
