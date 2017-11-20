/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.alignment_board.nodes;

import org.janelia.it.workstation.browser.nodes.AbstractDomainObjectNode;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.gui.alignment_board.AlignmentBoard;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;

/**
 * Netbeans Node definition for alignment board.
 *
 * @author fosterl
 */
public class AlignmentBoardNode extends AbstractDomainObjectNode {

    public AlignmentBoardNode(ChildFactory parentChildFactory, Children children, DomainObject domainObject) {
        super(parentChildFactory, children, domainObject);
    }
    
    public AlignmentBoard getAlignmentBoardItem() {
        return (AlignmentBoard)this.getDomainObject();
    }

}
