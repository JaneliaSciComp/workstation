package org.janelia.it.workstation.ab2;

import org.janelia.it.jacs.integration.framework.domain.ObjectOpenAcceptor;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.model.SampleImage;
import org.janelia.model.domain.sample.SampleAlignmentResult;
import org.janelia.model.domain.sample.SampleProcessingResult;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = ObjectOpenAcceptor.class, path=ObjectOpenAcceptor.LOOKUP_PATH)
public class AB2OpenAcceptor implements ObjectOpenAcceptor  {

    private static final int MENU_ORDER = 200;

    @Override
    public String getActionLabel() {
        return "  Open In AB2";
    }

    @Override
    public boolean isCompatible(Object obj) {
        // For now, restrict access to admins only
        if (!AccessManager.getAccessManager().isAdmin()) {
            return false;
        }
        if (obj instanceof SampleImage) {
            SampleImage sampleImage = (SampleImage)obj;
            if (sampleImage.getResult() instanceof SampleProcessingResult || sampleImage.getResult() instanceof SampleAlignmentResult) {
                return true;    
            }
        }
        return false;
    }

    @Override
    public boolean isEnabled(Object obj) {
        if (obj instanceof SampleImage) {
            SampleImage sampleImage = (SampleImage)obj;
            Boolean purged = sampleImage.getResult().getPurged();
            if (purged!=null && purged) {
                // Disable for purged samples
                return false;
            }
        }
        return true;
    }

    @Override
    public void acceptObject(Object obj) {
        AB2TopComponent ab2TopComponent = AB2TopComponent.findComp();
        if (ab2TopComponent != null) {
            if (!ab2TopComponent.isOpened()) {
                ab2TopComponent.open();
            }
            if (ab2TopComponent.isOpened()) {
                ab2TopComponent.requestActive();
            }
            
            if (obj instanceof SampleImage) {
                ab2TopComponent.loadSampleImage((SampleImage)obj, true);
            }
            else {
                throw new IllegalArgumentException("This viewer only accepts SampleImage objects");
            }
        }
    }

    @Override
    public Integer getOrder() {
        return MENU_ORDER;
    }

    @Override
    public boolean isPrecededBySeparator() {
        return false;
    }

    @Override
    public boolean isSucceededBySeparator() {
        return false;
    }

}