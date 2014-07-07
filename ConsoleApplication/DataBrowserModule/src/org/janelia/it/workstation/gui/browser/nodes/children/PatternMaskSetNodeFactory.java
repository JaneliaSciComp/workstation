package org.janelia.it.workstation.gui.browser.nodes.children;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.screen.PatternMask;
import org.janelia.it.jacs.model.domain.screen.ScreenSample;
import org.janelia.it.workstation.gui.browser.api.DomainDAO;
import org.janelia.it.workstation.gui.browser.components.DomainExplorerTopComponent;
import org.janelia.it.workstation.gui.browser.model.PatternMaskSet;
import org.janelia.it.workstation.gui.browser.nodes.PatternMaskSetNode;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class PatternMaskSetNodeFactory extends ChildFactory<PatternMaskSet> {

    private final static Logger log = LoggerFactory.getLogger(PatternMaskSetNodeFactory.class);

    private final WeakReference<ScreenSample> screenSampleRef;
    

    public PatternMaskSetNodeFactory(ScreenSample screenSample) {
        this.screenSampleRef = new WeakReference<ScreenSample>(screenSample);
    }

    @Override
    protected boolean createKeys(List<PatternMaskSet> list) {
        ScreenSample screenSample = screenSampleRef.get();
        if (screenSample==null) return false;
        
        DomainDAO dao = DomainExplorerTopComponent.getDao();
        if (screenSample.getMasks() != null) {
            
            List<DomainObject> masks = dao.getDomainObjects(SessionMgr.getSubjectKey(), screenSample.getMasks());
            
            Map<String,PatternMaskSet> groups = new LinkedHashMap<String,PatternMaskSet>();
            
            for(DomainObject obj : masks) {
                if (obj instanceof PatternMask) {
                    PatternMask mask = (PatternMask)obj;
                    PatternMaskSet set = groups.get(mask.getMaskSetName());
                    if (set==null) {
                        set = new PatternMaskSet();
                        set.setName(mask.getMaskSetName());
                        set.setMasks(new ArrayList<PatternMask>());
                        groups.put(set.getName(),set);
                    }
                    set.getMasks().add(mask);
                }
                else {
                    log.error("Screen sample reverse reference contains non PatternMask: {}",obj.getClass().getName());
                }
            }
            
            list.addAll(groups.values());
        }

        return true;
    }

    @Override
    protected Node createNodeForKey(PatternMaskSet key) {
        ScreenSample screenSample = screenSampleRef.get();
        if (screenSample==null) return null;
        try {
            return new PatternMaskSetNode(this, screenSample, key);
        }
        catch (Exception e) {
            log.error("Error creating node for key " + key, e);
        }
        return null;
    }
}
