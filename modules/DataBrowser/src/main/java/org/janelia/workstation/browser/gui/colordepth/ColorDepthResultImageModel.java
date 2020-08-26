package org.janelia.workstation.browser.gui.colordepth;

import org.janelia.model.domain.DomainConstants;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.SampleUtils;
import org.janelia.model.domain.enums.SplitHalfType;
import org.janelia.model.domain.gui.cdmip.ColorDepthImage;
import org.janelia.model.domain.gui.cdmip.ColorDepthMask;
import org.janelia.model.domain.gui.cdmip.ColorDepthMatch;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.model.Decorator;
import org.janelia.workstation.core.model.ImageModel;
import org.janelia.workstation.core.model.SplitTypeInfo;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Image model for color depth search results and their related information. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ColorDepthResultImageModel implements ImageModel<ColorDepthMatch, Reference> {

    private final static Logger log = LoggerFactory.getLogger(ColorDepthResultImageModel.class);

    // Constants
    private static final String SPLIT_ANNOTATION_OWNER = "group:flylight";
    
    // State
    private final ColorDepthMask mask;
    private final Map<Reference, ColorDepthMatch> matchMap;
    private final Map<Reference, ColorDepthImage> imageMap;
    private final Map<Reference, Sample> sampleMap;
    private final Map<String, SplitTypeInfo> splitInfos;
    
    public ColorDepthResultImageModel(
            ColorDepthMask mask,
            Collection<ColorDepthMatch> matches,
            Collection<ColorDepthImage> images,
            Collection<Sample> samples,
            Map<String, SplitTypeInfo> splitInfos) {
        this.mask = mask;
        this.matchMap = new HashMap<>();
        for (ColorDepthMatch match : matches) {
            matchMap.put(match.getImageRef(), match);
        }
        this.imageMap = DomainUtils.getMapByReference(images);
        this.sampleMap = DomainUtils.getMapByReference(samples);
        this.splitInfos = splitInfos;
    }

    public ColorDepthMask getMask() {
        return mask;
    }

    public ColorDepthImage getImage(ColorDepthMatch match) {
        if (match.getImageRef()==null) {
            throw new IllegalStateException("Null image ref: "+match);
        }
        return imageMap.get(match.getImageRef());
    }

    public Sample getSample(ColorDepthMatch match) {
        ColorDepthImage image = getImage(match);
        if (image==null) {
            log.warn("Image does not exist: "+match.getImageRef());
            return null;
        }
        if (image.getSampleRef()==null) {
            return null;
        }
        return sampleMap.get(image.getSampleRef());
    }
    
    public Collection<Sample> getSamples() {
        return sampleMap.values();
    }
    
    @Override
    public Reference getImageUniqueId(ColorDepthMatch match) {
        return match.getImageRef();
    }
    
    @Override
    public String getImageFilepath(ColorDepthMatch match) {
        if (!hasAccess(match)) return null;
        return getImage(match).getFilepath();
    }

    @Override
    public BufferedImage getStaticIcon(ColorDepthMatch match) {
        // Assume anything without an image is locked
        return Icons.getImage("file_lock.png");
    }

    @Override
    public ColorDepthMatch getImageByUniqueId(Reference filepath) {
        return matchMap.get(filepath);
    }
    
    @Override
    public String getImageTitle(ColorDepthMatch match) {
        if (!hasAccess(match)) return "Access denied";
        ColorDepthImage image = getImage(match);
        if (image.getSampleRef()==null) {
            return image.getName();
        }
        else {
            Sample sample = getSample(match);
            if (isShowVtLineNames()) {
                if (sample.getVtLine()!=null) {
                    return sample.getVtLine()+"-"+sample.getSlideCode();
                }
            }
            
            return sample.getName();
        }
    }

    @Override
    public String getImageSubtitle(ColorDepthMatch match) {
        return String.format("Score: %d (%s)", match.getScore(), MaskUtils.getFormattedScorePct(match));
    }
    
    @Override
    public List<Decorator> getDecorators(ColorDepthMatch match) {
        return Collections.emptyList();
    }

    public SplitTypeInfo getSplitTypeInfo(Sample sample) {
        if (sample != null) {
            String frag = SampleUtils.getFragFromLineName(sample.getLine());
            if (frag != null) {
                return splitInfos.get(frag);
            }
        }
        return null;
    }
    
    @Override
    public List<Annotation> getAnnotations(ColorDepthMatch match) {

        List<Annotation> annotations = new ArrayList<>();
        Sample sample = getSample(match);
        if (sample!=null) {
            SplitTypeInfo splitTypeInfo = getSplitTypeInfo(sample);
            if (splitTypeInfo != null) {
                if (splitTypeInfo.hasAD()) {
                    Annotation a = new Annotation();
                    a.setOwnerKey(SPLIT_ANNOTATION_OWNER);
                    a.setName(SplitHalfType.AD.getName());
                    a.setComputational(true);
                    annotations.add(a);
                }
                if (splitTypeInfo.hasDBD()) {
                    Annotation a = new Annotation();
                    a.setOwnerKey(SPLIT_ANNOTATION_OWNER);
                    a.setName(SplitHalfType.DBD.getName());
                    a.setComputational(true);
                    annotations.add(a);
                }
            }
        }
        
        return annotations;
    }
    
    private boolean hasAccess(ColorDepthMatch match) {
        return ClientDomainUtils.hasReadAccess(getImage(match));
    }

    private Boolean isShowVtLineNamesCached;
    
    protected void setShowVtLineNames(boolean showVtLineNames) {
        try {
            this.isShowVtLineNamesCached = showVtLineNames;
            FrameworkAccess.setRemotePreferenceValue(DomainConstants.PREFERENCE_CATEGORY_SHOW_VT_LINE_NAMES, DomainConstants.PREFERENCE_CATEGORY_SHOW_VT_LINE_NAMES, showVtLineNames);
        }
        catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
    }
    
    protected boolean isShowVtLineNames() {
        if (isShowVtLineNamesCached==null) {
            boolean defaultValue = true;
            try {
                isShowVtLineNamesCached = FrameworkAccess.getRemotePreferenceValue(DomainConstants.PREFERENCE_CATEGORY_SHOW_VT_LINE_NAMES, DomainConstants.PREFERENCE_CATEGORY_SHOW_VT_LINE_NAMES, defaultValue);
                log.info("Got preference: "+isShowVtLineNamesCached);
            }
            catch (Exception e) {
                log.error("Error getting preference", e);
                isShowVtLineNamesCached = defaultValue;
            }
        }
        return isShowVtLineNamesCached;
    }
}
