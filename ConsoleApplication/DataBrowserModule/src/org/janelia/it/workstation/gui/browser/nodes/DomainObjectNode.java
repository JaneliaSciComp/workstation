package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.datatransfer.Transferable;
import java.io.IOException;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.MaterializedView;
import org.janelia.it.workstation.gui.browser.flavors.DomainObjectFlavor;
import org.openide.nodes.BeanNode;
import org.openide.nodes.ChildFactory;
import org.openide.util.datatransfer.ExTransferable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectNode extends BeanNode<DomainObject> {

    private final static Logger log = LoggerFactory.getLogger(DomainObjectNode.class);
    
    private final ChildFactory parentChildFactory;
    
    public DomainObjectNode(ChildFactory parentChildFactory, DomainObject domainObject) throws Exception {
        super(domainObject);
        this.parentChildFactory = parentChildFactory;
    }
    
    @Override
    public boolean canCut() {
        return true;
    }

    @Override
    public boolean canCopy() {
        return true;
    }

    @Override
    public boolean canRename() {
        return true;
    }
    
    @Override
    public boolean canDestroy() {
        if (getBean() instanceof MaterializedView) {
            return false;
        }
        return true;
    }
    
    @Override
    public Transferable clipboardCopy() throws IOException {
        log.info("clipboard COPY "+getBean());
        Transferable deflt = super.clipboardCopy();
        ExTransferable added = ExTransferable.create(deflt);
        added.put(new ExTransferable.Single(DomainObjectFlavor.DOMAIN_OBJECT_FLAVOR) {
            @Override
            protected DomainObject getData() {
                return (DomainObject)getBean();
            }
        });
        return added;
    }
    
    @Override
    public Transferable clipboardCut() throws IOException {
        log.info("clipboard CUT "+getBean());
        Transferable deflt = super.clipboardCut();
        ExTransferable added = ExTransferable.create(deflt);
        added.put(new ExTransferable.Single(DomainObjectFlavor.DOMAIN_OBJECT_FLAVOR) {
            @Override
            protected DomainObject getData() {
                return (DomainObject)getBean();
            }
        });
        return added;
    }
    
}
