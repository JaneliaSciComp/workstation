package org.janelia.workstation.browser.actions;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.interfaces.HasFiles;
import org.janelia.model.domain.sample.Sample;
import org.janelia.model.domain.sample.SamplePostProcessingResult;
import org.janelia.model.domain.sample.SampleProcessingResult;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.model.descriptors.ArtifactDescriptor;
import org.janelia.workstation.core.model.descriptors.DescriptorUtils;
import org.janelia.workstation.core.model.descriptors.ResultArtifactDescriptor;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleActionUtils {

    public static HasFiles getSingleResult(ViewerContext viewerContext) {
        DomainObject domainObject = viewerContext.getDomainObject();
        ArtifactDescriptor rd = viewerContext.getResultDescriptor();
        HasFiles result = null;
        if (domainObject instanceof Sample) {
            Sample sample = (Sample)domainObject;
            result = DescriptorUtils.getResult(sample, rd);
        }
        else if (domainObject instanceof HasFiles) {
            result = (HasFiles)domainObject;
        }
        return result;
    }

    public static HasFiles getSingle3dResult(ViewerContext viewerContext) {

        DomainObject domainObject = viewerContext.getDomainObject();
        ArtifactDescriptor rd = viewerContext.getResultDescriptor();

        if (rd instanceof ResultArtifactDescriptor) {
            ResultArtifactDescriptor rad = (ResultArtifactDescriptor)rd;
            if ("Post-Processing Result".equals(rad.getResultName())) {
                rd = new ResultArtifactDescriptor(rd.getObjective(), rd.getArea(), SampleProcessingResult.class.getName(), null, false);
            }
            else if (SamplePostProcessingResult.class.getSimpleName().equals(rad.getResultClass())) {
                rd = new ResultArtifactDescriptor(rd.getObjective(), rd.getArea(), SampleProcessingResult.class.getName(), null, false);
            }
        }

        HasFiles result = null;
        if (domainObject instanceof Sample) {
            Sample sample = (Sample)domainObject;
            result = DescriptorUtils.getResult(sample, rd);
        }
        else if (domainObject instanceof HasFiles) {
            result = (HasFiles)domainObject;
        }
        return result;
    }
}
