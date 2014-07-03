package org.janelia.it.workstation.gui.browser.api;

import java.util.Collection;
import java.util.Iterator;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainUtils {

    public static boolean hasChild(TreeNode treeNode, DomainObject domainObject) {
        for(Iterator<Reference> i = treeNode.getChildren().iterator(); i.hasNext(); ) {
            Reference iref = i.next();
            if (iref.getTargetId().equals(domainObject.getId())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection==null || collection.isEmpty();
    }
}
