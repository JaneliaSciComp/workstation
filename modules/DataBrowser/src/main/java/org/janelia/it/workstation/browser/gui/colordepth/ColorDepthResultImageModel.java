package org.janelia.it.workstation.browser.gui.colordepth;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.workstation.browser.gui.model.ImageDecorator;
import org.janelia.it.workstation.browser.gui.model.ImageModel;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.model.SplitTypeInfo;
import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.access.domain.SampleUtils;
import org.janelia.model.domain.DomainConstants;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.enums.SplitHalfType;
import org.janelia.model.domain.gui.colordepth.ColorDepthMatch;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.sample.Sample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Image model for color depth search results and their related information. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ColorDepthResultImageModel implements ImageModel<ColorDepthMatch, String> {

    private final static Logger log = LoggerFactory.getLogger(ColorDepthResultImageModel.class);

    // Constants
    private static final String SPLIT_ANNOTATION_OWNER = "group:flylight";
    
    // State
    private Map<Reference, Sample> sampleMap = new HashMap<>();
    private Map<String, ColorDepthMatch> matchMap = new HashMap<>();
    private Map<String, SplitTypeInfo> splitInfos = new HashMap<>();
    
    public ColorDepthResultImageModel(
            Collection<ColorDepthMatch> matches, 
            Collection<Sample> samples,
            Map<String, SplitTypeInfo> splitInfos) {
        
        for (ColorDepthMatch match : matches) {
            matchMap.put(match.getFilepath(), match);
        }
        this.sampleMap = DomainUtils.getMapByReference(samples);
        this.splitInfos = splitInfos;
    }
    
    public Sample getSample(ColorDepthMatch match) {
        return sampleMap.get(match.getSample());
    }
    
    public Collection<Sample> getSamples() {
        return sampleMap.values();
    }
    
    @Override
    public String getImageUniqueId(ColorDepthMatch match) {
        return match.getFilepath();
    }
    
    @Override
    public String getImageFilepath(ColorDepthMatch match) {
        if (!hasAccess(match)) return null;
        return match.getFilepath();
    }

    @Override
    public BufferedImage getStaticIcon(ColorDepthMatch match) {
        // Assume anything without an image is locked
        return Icons.getImage("file_lock.png");
    }

    @Override
    public ColorDepthMatch getImageByUniqueId(String filepath) {
        return matchMap.get(filepath);
    }
    
    @Override
    public String getImageTitle(ColorDepthMatch match) {
        if (!hasAccess(match)) return "Access denied";
        if (match.getSample()==null) {
            return match.getFile().getName();
        }
        else {
            Sample sample = sampleMap.get(match.getSample());
            
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
    public List<ImageDecorator> getDecorators(ColorDepthMatch match) {
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
        if (match.getSample()!=null) {
            Sample sample = sampleMap.get(match.getSample());
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
        if (match.getSample()!=null) {
            Sample sample = sampleMap.get(match.getSample());
            if (sample == null) {
                // The result maps to a sample, but the user has no access to see it
                // TODO: check access to data set?
                return false;
            }
        }
        return true;
    }

    private Boolean isShowVtLineNamesCached;
    
    protected void setShowVtLineNames(boolean showVtLineNames) {
        try {
            this.isShowVtLineNamesCached = showVtLineNames;
            FrameworkImplProvider.setRemotePreferenceValue(DomainConstants.PREFERENCE_CATEGORY_SHOW_VT_LINE_NAMES, DomainConstants.PREFERENCE_CATEGORY_SHOW_VT_LINE_NAMES, showVtLineNames);
        }
        catch (Exception e) {
            FrameworkImplProvider.handleException(e);
        }
    }
    
    protected boolean isShowVtLineNames() {
        if (isShowVtLineNamesCached==null) {
            boolean defaultValue = true;
            try {
                isShowVtLineNamesCached = FrameworkImplProvider.getRemotePreferenceValue(DomainConstants.PREFERENCE_CATEGORY_SHOW_VT_LINE_NAMES, DomainConstants.PREFERENCE_CATEGORY_SHOW_VT_LINE_NAMES, defaultValue);
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
