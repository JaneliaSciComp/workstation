package org.janelia.workstation.browser.actions;

import java.util.List;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.sample.LSMImage;
import org.janelia.model.domain.sample.Sample;

/**
 * Export the unique line names for the picked results.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ExportPickedLineNames extends AbstractExportPickedAction {

    private boolean vtline;

    public ExportPickedLineNames(List<Reference> refs, boolean vtline) {
        super(refs);
        this.vtline = vtline;
    }

    @Override
    protected String export(DomainObject domainObject) {
        if (domainObject instanceof Sample) {
            Sample sample = (Sample)domainObject;
            return vtline ? sample.getVtLine() : sample.getLine();
        }
        else if (domainObject instanceof LSMImage) {
            LSMImage image = (LSMImage)domainObject;
            return vtline ? image.getVtLine() : image.getLine();
        }
        else {
            return "Item has no associated line: "+domainObject;
        }
    }
}
