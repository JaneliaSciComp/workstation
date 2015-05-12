package org.janelia.it.workstation.gui.browser.nodes.children;

import java.lang.ref.WeakReference;
import java.util.List;

import org.janelia.it.jacs.model.domain.screen.FlyLine;
import org.janelia.it.jacs.model.domain.screen.ScreenSample;
import org.janelia.it.workstation.gui.browser.api.DomainDAO;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.nodes.ScreenSampleNode;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ScreenSampleNodeFactory extends ChildFactory<ScreenSample> {

    private final static Logger log = LoggerFactory.getLogger(ScreenSampleNodeFactory.class);

    private final WeakReference<FlyLine> flyLineRef;

    public ScreenSampleNodeFactory(FlyLine flyLine) {
        this.flyLineRef = new WeakReference<>(flyLine);
    }

    @Override
    protected boolean createKeys(List<ScreenSample> list) {
        FlyLine flyLine = flyLineRef.get();
        if (flyLine==null) return false;
        
        DomainDAO dao = DomainMgr.getDomainMgr().getDao();
        
        for(ScreenSample screenSample : dao.getScreenSampleByFlyLine(SessionMgr.getSubjectKey(), flyLine.getName())) {
            list.add(screenSample);
        }
        return true;
    }

    @Override
    protected Node createNodeForKey(ScreenSample key) {
        FlyLine flyLine = flyLineRef.get();
        if (flyLine==null) return null;
        try {
            return new ScreenSampleNode(this, key);
        }
        catch (Exception e) {
            log.error("Error creating node for key " + key, e);
        }
        return null;
    }
}
