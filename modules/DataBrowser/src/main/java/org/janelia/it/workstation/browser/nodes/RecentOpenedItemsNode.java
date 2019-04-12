package org.janelia.it.workstation.browser.nodes;

import java.awt.Image;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.integration.framework.domain.DomainObjectHelper;
import org.janelia.it.jacs.integration.framework.domain.ServiceAcceptorHelper;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.api.StateMgr;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.interfaces.HasIdentifier;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A node which shows the items most recently opened by the user.  
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RecentOpenedItemsNode extends AbstractNode implements HasIdentifier {
        
    private final static Logger log = LoggerFactory.getLogger(RecentOpenedItemsNode.class);
    
    private static final long RECENTLY_OPENED_ID = 2L; // This magic number means nothing, it just needs to be unique and different from GUID space.
    
    private final DomainObjectNodeChildFactory childFactory;

    public RecentOpenedItemsNode() {
        this(new DomainObjectNodeChildFactory());
    }
    
    private RecentOpenedItemsNode(DomainObjectNodeChildFactory childFactory) {
        super(Children.create(childFactory, false));
        this.childFactory = childFactory;
    }

    @Override
    public Long getId() {
        return RECENTLY_OPENED_ID;
    }

    @Override
    public String getDisplayName() {
        return "Recently Opened";
    }

    @Override
    public String getHtmlDisplayName() {
        String primary = getDisplayName();
        StringBuilder sb = new StringBuilder();
        if (primary!=null) {
            sb.append("<font color='!Label.foreground'>");
            sb.append(primary);
            sb.append("</font>");
        }
        return sb.toString();
    }
    
    @Override
    public Image getIcon(int type) {
        return Icons.getIcon("drop-folder-white-icon.png").getImage();
    }

    @Override
    public Image getOpenedIcon(int type) {
        return getIcon(type);
    }

    @Override
    public boolean canDestroy() {
        return false;
    }

    public void refreshChildren() {
        childFactory.refresh();
    }   
    
    private static boolean isSupportedAsChild(Class<? extends DomainObject> clazz) {
        try {
            // TODO: this should use the other isCompatible() method which takes a class, 
            // instead of constructing a dummy object
            DomainObject dummyChild = (DomainObject)clazz.newInstance();
            DomainObjectHelper provider = ServiceAcceptorHelper.findFirstHelper(dummyChild);
            if (provider!=null) {
                return true;
            }
        }
        catch (InstantiationException | IllegalAccessException e) {
            log.error("Error instantiating purported domain class "+clazz, e);
        }
        return false;
    }
    
    private static class DomainObjectNodeChildFactory extends ChildFactory<DomainObject> {
        
        @Override
        protected boolean createKeys(List<DomainObject> list) {
            try {
                log.debug("Creating children keys for RecentOpenedItemsNode");

                List<String> strRefs =  StateMgr.getStateMgr().getRecentlyOpenedHistory();
                List<Reference> refs = new ArrayList<>();
                for(String strRef : strRefs) {
                    refs.add(Reference.createFor(strRef));
                }
                
                DomainModel model = DomainMgr.getDomainMgr().getModel();
                List<DomainObject> children = model.getDomainObjects(refs);
                if (children.size()!=strRefs.size()) {
                    log.info("Got {} children but expected {}",children.size(),strRefs.size());
                }
                log.debug("Got children: {}",children);

                Map<Long,DomainObject> map = new HashMap<>();
                for (DomainObject obj : children) {
                    map.put(obj.getId(), obj);
                }

                List<DomainObject> temp = new ArrayList<>();
                if (!refs.isEmpty()) {
                    for(Reference reference : refs) {
                        if (reference==null) continue;
                        DomainObject obj = map.get(reference.getTargetId());
                        log.trace(reference.getTargetClassName()+"#"+reference.getTargetId()+" -> "+obj);
                        if (obj!=null) {
                            if (isSupportedAsChild(obj.getClass())) {
                                temp.add(obj);
                            }
                        }
                        else {
                            log.warn("Dead reference detected: "+reference);
                        }
                    }
                }

                list.addAll(temp);
            } 
            catch (Exception ex) {
                FrameworkImplProvider.handleException(ex);
            }
            return true;
        }

        @Override
        protected Node createNodeForKey(DomainObject key) {
            try {
                DomainObjectHelper provider = ServiceAcceptorHelper.findFirstHelper(key);
                if (provider!=null) {
                    return provider.getNode(key, this);
                }
                return null;
            }
            catch (Exception e) {
                log.error("Error creating node for key " + key, e);
            }
            return null;
        }
        
        public void refresh() {
            log.debug("Refreshing child factory for "+getClass().getSimpleName());
            refresh(true);
        }
    }
}
