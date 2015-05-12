package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;
import java.lang.ref.WeakReference;
import java.util.List;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.sample.LSMImage;

import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SampleTile;
import org.janelia.it.workstation.gui.browser.api.DomainDAO;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjectiveTileNode extends InternalNode<SampleTile> {
    
    private final static Logger log = LoggerFactory.getLogger(ObjectiveTileNode.class);
    
    private final WeakReference<Sample> sampleRef;
    
    public ObjectiveTileNode(Sample sample, SampleTile sampleTile) throws Exception {
        super(Children.create(new ObjectiveTileNode.MyChildFactory(sample, sampleTile), true), sampleTile);
        this.sampleRef = new WeakReference<>(sample);
    }
    
    public SampleTile getSampleTile() {
        return (SampleTile)getObject();
    }
    
    @Override
    public String getPrimaryLabel() {
        return getSampleTile().getName();
    }
    
    @Override
    public Image getIcon(int type) {
        return Icons.getIcon("plugin.png").getImage();
    }
    
    static class MyChildFactory extends ChildFactory<LSMImage> {
    
        private final WeakReference<Sample> sampleRef;
        private final WeakReference<SampleTile> sampleTileRef;
        
        public MyChildFactory(Sample sample, SampleTile sampleTile) {
            this.sampleRef = new WeakReference<>(sample);
            this.sampleTileRef = new WeakReference<>(sampleTile);
        }
        
        @Override
        protected boolean createKeys(List<LSMImage> list) {
            SampleTile sampleTile = sampleTileRef.get();
            if (sampleTile==null) return false;
            
            DomainDAO dao = DomainMgr.getDomainMgr().getDao();
            for(DomainObject obj : dao.getDomainObjects(SessionMgr.getSubjectKey(), sampleTile.getLsmReferences())) {
                LSMImage image = (LSMImage)obj;
                list.add(image);
            }
            
            return true;
        }

        @Override
        protected Node createNodeForKey(LSMImage key) {
            try {
                return new LSMImageNode(this, key);
            }
            catch (Exception e) {
                log.error("Error creating node for key " + key, e);
            }
            return null;
        }

    }
}
