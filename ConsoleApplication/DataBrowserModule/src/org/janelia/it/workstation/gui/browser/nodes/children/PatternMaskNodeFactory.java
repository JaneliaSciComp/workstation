package org.janelia.it.workstation.gui.browser.nodes.children;

import java.lang.ref.WeakReference;
import java.util.List;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.screen.PatternMask;
import org.janelia.it.jacs.model.domain.screen.ScreenSample;
import org.janelia.it.workstation.gui.browser.api.DomainDAO;
import org.janelia.it.workstation.gui.browser.components.DomainExplorerTopComponent;
import org.janelia.it.workstation.gui.browser.model.PatternMaskSet;
import org.janelia.it.workstation.gui.browser.nodes.PatternMaskNode;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class PatternMaskNodeFactory extends ChildFactory<PatternMask> {

    private final static Logger log = LoggerFactory.getLogger(PatternMaskNodeFactory.class);

    private final WeakReference<ScreenSample> screenSampleRef;
    private final WeakReference<PatternMaskSet> patternMaskSetRef;

    public PatternMaskNodeFactory(ScreenSample screenSample) {
        this.screenSampleRef = new WeakReference<ScreenSample>(screenSample);
        this.patternMaskSetRef = null;
    }

    public PatternMaskNodeFactory(ScreenSample screenSample, PatternMaskSet patternMaskSet) {
        this.screenSampleRef = new WeakReference<ScreenSample>(screenSample);
        this.patternMaskSetRef = new WeakReference<PatternMaskSet>(patternMaskSet);
    }
    
    @Override
    protected boolean createKeys(List<PatternMask> list) {
        
        if (patternMaskSetRef!=null) {
            PatternMaskSet patternMaskSet = patternMaskSetRef.get();
            if (patternMaskSet==null) return false;
            list.addAll(patternMaskSet.getMasks());
            return true;
        }
        
        ScreenSample screenSample = screenSampleRef.get();
        if (screenSample==null) return false;
        
        DomainDAO dao = DomainExplorerTopComponent.getDao();
        if (screenSample.getPatternMasks() != null) {
            List<DomainObject> masks = dao.getDomainObjects(SessionMgr.getSubjectKey(), screenSample.getPatternMasks());
            for(DomainObject mask : masks) {
                if (mask instanceof PatternMask) {
                    list.add((PatternMask)mask);
                }
                else {
                    log.error("Screen sample reverse reference contains non PatternMask: {}",mask.getClass().getName());
                }
            }
        }

        return true;
    }

    @Override
    protected Node createNodeForKey(PatternMask key) {
        ScreenSample screenSample = screenSampleRef.get();
        if (screenSample==null) return null;
        try {
            return new PatternMaskNode(this, key);
        }
        catch (Exception e) {
            log.error("Error creating node for key " + key, e);
        }
        return null;
    }
}
