package org.janelia.it.workstation.browser.model;

import java.awt.image.BufferedImage;
import java.util.List;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.integration.framework.domain.DomainObjectHelper;
import org.janelia.it.jacs.integration.framework.domain.ServiceAcceptorHelper;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.gui.listview.icongrid.ImageModel;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.model.descriptors.ArtifactDescriptor;
import org.janelia.it.workstation.browser.model.descriptors.DescriptorUtils;
import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.access.domain.DynamicDomainObjectProxy;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.interfaces.HasFiles;
import org.janelia.model.domain.sample.Sample;

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
            result = DescriptorUtils.getResult(sample, getArtifactDescriptor());
        }
        else if (domainObject instanceof HasFiles) {
            result = (HasFiles)domainObject;
        }
        return result==null? null : DomainUtils.getFilepath(result, getImageTypeName());
    }

    @Override
    public BufferedImage getStaticIcon(DomainObject imageObject) {
        String filename = null;
        DomainObjectHelper provider = ServiceAcceptorHelper.findFirstHelper(imageObject);
        if (provider!=null) {
            filename = provider.getLargeIcon(imageObject);
        }
        return filename==null?null:Icons.getImage(filename);
    }

    @Override
    public DomainObject getImageByUniqueId(Reference id) {
        try {
            return DomainMgr.getDomainMgr().getModel().getDomainObject(id);
        }
        catch (Exception e) {
            FrameworkImplProvider.handleException(e);
            return null;
        }
    }
    
    @Override
    public String getImageTitle(DomainObject domainObject) {
        String titlePattern = getTitlePattern(domainObject.getClass());
        if (StringUtils.isEmpty(titlePattern)) return domainObject.getName();
        DynamicDomainObjectProxy proxy = new DynamicDomainObjectProxy(domainObject);
        return StringUtils.replaceVariablePattern(titlePattern, proxy);
    }

    @Override
    public String getImageSubtitle(DomainObject domainObject) {
        String subtitlePattern = getSubtitlePattern(domainObject.getClass());
        if (StringUtils.isEmpty(subtitlePattern)) return null;
        DynamicDomainObjectProxy proxy = new DynamicDomainObjectProxy(domainObject);
        return StringUtils.replaceVariablePattern(subtitlePattern, proxy);
    }
    
    @Override
    public List<ImageDecorator> getDecorators(DomainObject imageObject) {
        return ClientDomainUtils.getDecorators(imageObject);
    }
    
    protected abstract ArtifactDescriptor getArtifactDescriptor();

    protected abstract String getImageTypeName();
    
    protected abstract String getTitlePattern(Class<? extends DomainObject> clazz);
    
    protected abstract String getSubtitlePattern(Class<? extends DomainObject> clazz);
}
