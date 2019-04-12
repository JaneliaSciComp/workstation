package org.janelia.it.workstation.gui.large_volume_viewer.nodes;

import java.awt.Image;

import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.browser.nodes.AbstractDomainObjectNode;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TmSampleNode extends AbstractDomainObjectNode<TmSample> {

    private final static Logger log = LoggerFactory.getLogger(TmSampleNode.class);

    private final TmSampleChildFactory childFactory;
    
    public TmSampleNode(ChildFactory<?> parentChildFactory, TmSample sample) {
        this(parentChildFactory, new TmSampleChildFactory(sample), sample);
    }

    private TmSampleNode(ChildFactory<?> parentChildFactory, final TmSampleChildFactory childFactory, TmSample sample) {
        super(parentChildFactory, childFactory.hasNodeChildren()?Children.create(childFactory, false):Children.LEAF, sample);
            
        log.trace("Creating node@{} -> {}",System.identityHashCode(this),getDisplayName());

        this.childFactory = childFactory;
        // TODO: implement denormalized "child" count on TmSample
//        if (sample.getNumChildren()>0) {
//            getLookupContents().add(new Index.Support() {
//                @Override
//                public Node[] getNodes() {
//                    return getChildren().getNodes();
//                }
//                @Override
//                public int getNodesCount() {
//                    return getNodes().length;
//                }
//                @Override
//                public void reorder(final int[] order) {
//                    throw new UnsupportedOperationException();
//                }
//            });
//        }
    }

    public final void checkChildren() {
        boolean isLeaf = getChildren()==Children.LEAF;
        boolean hasChildren = childFactory.hasNodeChildren();
        if (isLeaf == hasChildren) {
            log.trace("Node {} changed child-having status",getDisplayName());
            this.setChildren(createChildren());
        }
    }

    public Children createChildren() {
        if (childFactory.hasNodeChildren()) {
            return Children.create(new TmSampleChildFactory(getSample()), false);
        }
        else {
            return Children.LEAF;
        }
    }
    
    @Override
    public void update(TmSample sample) {
        log.debug("Refreshing node@{} -> {}",System.identityHashCode(this),getDisplayName());
        super.update(sample);
        log.debug("Refreshing children for {}", sample.getName());
        //log.debug("Refreshing children for {} (now has {} children)", sample.getName(), sample.getNumChildren());
        childFactory.update(sample);
        refreshChildren();
    }
    
    public void refreshChildren() {
        childFactory.refresh();
        checkChildren();
    }
    
    public TmSample getSample() {
        return (TmSample)getDomainObject();
    }
    
    @Override
    public String getPrimaryLabel() {
        return getSample().getName();
    }
        
    @Override
    public Image getIcon(int type) {
        if (ClientDomainUtils.isOwner(getSample())) {
            return Icons.getIcon("beaker.png").getImage();
        }
        else {
            return Icons.getIcon("beaker.png").getImage();
        }
    }
    
    @Override
    public boolean canDestroy() {
        return true;
    }
}
