package org.janelia.it.workstation.gui.browser.model;

import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;

/**
 * This is just a way to pass a PipelineResult around, and still maintain a link back to the containing sample.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleResult {

    private Sample sample;
    private PipelineResult result;
    
    public SampleResult(Sample sample, PipelineResult result) {
        this.sample = sample;
        this.result = result;
    }
    
    public Sample getSample() {
        return sample;
    }
    
    public PipelineResult getResult() {
        return result;
    }

    public String getName() {
        return result.getName();
    }

    @Override
    public String toString() {
        return "SampleResult [sampleId=" + sample.getId() + ", resultName=" + result.getName() + "]";
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.result == null) ? 0 : this.result.hashCode());
        result = prime * result + ((sample == null) ? 0 : sample.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SampleResult other = (SampleResult) obj;
        if (result == null) {
            if (other.result != null)
                return false;
        } else if (!result.equals(other.result))
            return false;
        if (sample == null) {
            if (other.sample != null)
                return false;
        } else if (!sample.equals(other.sample))
            return false;
        return true;
    }    
}
