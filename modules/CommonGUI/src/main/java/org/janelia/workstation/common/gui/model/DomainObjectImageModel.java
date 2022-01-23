package org.janelia.workstation.common.gui.model;


import org.apache.commons.lang3.StringUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.DynamicDomainObjectProxy;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.interfaces.HasFiles;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.model.ImageModel;
import org.janelia.workstation.core.model.descriptors.ArtifactDescriptor;
import org.janelia.workstation.core.model.descriptors.DescriptorUtils;
import org.janelia.workstation.core.util.StringUtilsExtra;
import org.janelia.workstation.integration.spi.domain.DomainObjectHandler;
import org.janelia.workstation.integration.spi.domain.ServiceAcceptorHelper;
import org.janelia.workstation.integration.util.FrameworkAccess;

import java.awt.image.BufferedImage;

public abstract class DomainObjectImageModel implements ImageModel<DomainObject, Reference> {

    @Override
    public Reference getImageUniqueId(DomainObject domainObject) {
        return Reference.createFor(domainObject);
    }
    
    @Override
    public String getImageFilepath(DomainObject domainObject) {
        HasFiles result = null;
        if (domainObject instanceof Sample) {
            Sample sample = (Sample)domainObject;
            result = DescriptorUtils.getLatestResult(sample, getArtifactDescriptor());
        }
        else if (domainObject instanceof HasFiles) {
            result = (HasFiles)domainObject;
        }
        return result==null? null : DomainUtils.getFilepath(result, getImageTypeName());
    }

    @Override
    public BufferedImage getStaticIcon(DomainObject imageObject) {
        String filename = null;
        DomainObjectHandler provider = ServiceAcceptorHelper.findFirstHelper(imageObject);
        if (provider!=null) {
            filename = provider.getLargeIcon(imageObject);
        }
        return filename==null?null: Icons.getImage(filename);
    }

    @Override
    public DomainObject getImageByUniqueId(Reference id) {
        try {
            return DomainMgr.getDomainMgr().getModel().getDomainObject(id);
        }
        catch (Exception e) {
            FrameworkAccess.handleException(e);
            return null;
        }
    }
    
    @Override
    public String getImageTitle(DomainObject domainObject) {
        String titlePattern = getTitlePattern(domainObject.getClass());
        if (StringUtils.isEmpty(titlePattern)) return domainObject.getName();
        DynamicDomainObjectProxy proxy = new DynamicDomainObjectProxy(domainObject);
        return StringUtilsExtra.replaceVariablePattern(titlePattern, proxy);
    }

    @Override
    public String getImageSubtitle(DomainObject domainObject) {
        String subtitlePattern = getSubtitlePattern(domainObject.getClass());
        if (StringUtils.isEmpty(subtitlePattern)) return null;
        DynamicDomainObjectProxy proxy = new DynamicDomainObjectProxy(domainObject);
        return StringUtilsExtra.replaceVariablePattern(subtitlePattern, proxy);
    }

    protected abstract String getTitlePattern(Class<? extends DomainObject> clazz);

    protected abstract String getSubtitlePattern(Class<? extends DomainObject> clazz);

    public abstract ArtifactDescriptor getArtifactDescriptor();

    public abstract String getImageTypeName();

}
