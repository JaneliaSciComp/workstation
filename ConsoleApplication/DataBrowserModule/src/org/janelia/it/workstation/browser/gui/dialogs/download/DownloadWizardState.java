package org.janelia.it.workstation.browser.gui.dialogs.download;

import java.util.List;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.workstation.browser.gui.support.DownloadItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multiset;

class DownloadWizardState {
    
    private static final Logger log = LoggerFactory.getLogger(DownloadWizardState.class);
    
    private List<? extends DomainObject> inputObjects;
    private ArtifactDescriptor defaultArtifactDescriptor;
    private List<DownloadObject> downloadObjects;
    private Multiset<ArtifactDescriptor> artifactCounts;
    private List<ArtifactDescriptor> artifactDescriptors;
    private boolean isSplitChannels;
    private String outputFormat;
    private boolean flattenStructure;
    private String filenamePattern;
    private List<DownloadItem> downloadItems;

    public List<? extends DomainObject> getInputObjects() {
        return inputObjects;
    }

    public void setInputObjects(List<? extends DomainObject> inputObjects) {
        this.inputObjects = inputObjects;
    }

    public ArtifactDescriptor getDefaultArtifactDescriptor() {
        return defaultArtifactDescriptor;
    }

    public void setDefaultArtifactDescriptor(ArtifactDescriptor defaultArtifactDescriptor) {
        this.defaultArtifactDescriptor = defaultArtifactDescriptor;
    }

    public List<DownloadObject> getDownloadObjects() {
        return downloadObjects;
    }

    public void setDownloadObjects(List<DownloadObject> downloadObjects) {
        this.downloadObjects = downloadObjects;
    }

    public Multiset<ArtifactDescriptor> getArtifactCounts() {
        return artifactCounts;
    }

    public void setArtifactCounts(Multiset<ArtifactDescriptor> artifactCounts) {
        this.artifactCounts = artifactCounts;
    }

    public List<ArtifactDescriptor> getArtifactDescriptors() {
        return artifactDescriptors;
    }

    public void setArtifactDescriptors(List<ArtifactDescriptor> artifactDescriptors) {
        this.artifactDescriptors = artifactDescriptors;
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

    public List<DownloadItem> getDownloadItems() {
        return downloadItems;
    }

    public void setDownloadItems(List<DownloadItem> downloadItems) {
        this.downloadItems = downloadItems;
    }

    public boolean has3d() {
        if (artifactDescriptors!=null) {
            for (ArtifactDescriptor artifactDescriptor : artifactDescriptors) {
                List<FileType> fileTypes = artifactDescriptor.getFileTypes();
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
        return true; // default to true so that the user can see the potential 3d processing panel
    }
}
