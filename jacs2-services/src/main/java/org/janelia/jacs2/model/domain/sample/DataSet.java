package org.janelia.jacs2.model.domain.sample;

import com.google.common.base.Preconditions;
import org.janelia.jacs2.model.domain.AbstractDomainObject;

import java.util.ArrayList;
import java.util.List;

public class DataSet extends AbstractDomainObject {
    private String identifier;
    private String sampleNamePattern;
    private SampleImageType sampleImageType;
    private boolean sageSync;
    private List<String> pipelineProcesses = new ArrayList<>();
    private String sageConfigPath;
    private String sageGrammarPath;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getSampleNamePattern() {
        return sampleNamePattern;
    }

    public void setSampleNamePattern(String sampleNamePattern) {
        this.sampleNamePattern = sampleNamePattern;
    }

    public SampleImageType getSampleImageType() {
        return sampleImageType;
    }

    public void setSampleImageType(SampleImageType sampleImageType) {
        this.sampleImageType = sampleImageType;
    }

    public boolean isSageSync() {
        return sageSync;
    }

    public void setSageSync(boolean sageSync) {
        this.sageSync = sageSync;
    }

    public List<String> getPipelineProcesses() {
        return pipelineProcesses;
    }

    public void setPipelineProcesses(List<String> pipelineProcesses) {
        Preconditions.checkArgument(pipelineProcesses != null, "Pipeline processes cannot be null");
        this.pipelineProcesses = pipelineProcesses;
    }

    public String getSageConfigPath() {
        return sageConfigPath;
    }

    public void setSageConfigPath(String sageConfigPath) {
        this.sageConfigPath = sageConfigPath;
    }

    public String getSageGrammarPath() {
        return sageGrammarPath;
    }

    public void setSageGrammarPath(String sageGrammarPath) {
        this.sageGrammarPath = sageGrammarPath;
    }
}
