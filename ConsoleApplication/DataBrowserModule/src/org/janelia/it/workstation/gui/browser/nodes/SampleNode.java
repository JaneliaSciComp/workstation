package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;
import javax.swing.Action;
import org.janelia.it.jacs.model.domain.Sample;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.workstation.gui.browser.DomainExplorerTopComponent;
import org.janelia.it.workstation.gui.browser.children.NeuronNodeFactory;
import org.janelia.it.workstation.gui.browser.children.ObjectiveNodeFactory;
import org.janelia.it.workstation.gui.browser.children.TreeNodeChildFactory;
import org.janelia.it.workstation.gui.util.Icons;
import org.openide.actions.CopyAction;
import org.openide.actions.CutAction;
import org.openide.actions.DeleteAction;
import org.openide.actions.MoveDownAction;
import org.openide.actions.MoveUpAction;
import org.openide.actions.PasteAction;
import org.openide.actions.RenameAction;
import org.openide.nodes.Children;

public class SampleNode extends DomainObjectNode {
    
    public SampleNode(TreeNodeChildFactory parentChildFactory, Sample sample) throws Exception {
        super(parentChildFactory, sample);
        if (DomainExplorerTopComponent.isShowNeurons()) {
            setChildren(Children.create(new NeuronNodeFactory(sample), true));   
        }
        else {  
            setChildren(Children.create(new ObjectiveNodeFactory(sample), true));   
        }
    }
    
    private Sample getSample() {
        return (Sample)getBean();
    }
    
    @Override
    public Image getIcon(int type) {
        Entity sample = new Entity();
        sample.setEntityTypeName("Sample");
        return Icons.getIcon(sample, false).getImage();
    }
    
    @Override
    public String getHtmlDisplayName() {
        if (getBean() != null) {
            return "<font color='!Label.foreground'>" + getSample().getName() + "</font>" +
                    " <font color='#957D47'><i>" + getSample().getOwnerKey() + "</i></font>";
        } else {
            return null;
        }
    }
    
    @Override
    public Action[] getActions(boolean context) {
        Action[] result = new Action[]{
                RenameAction.get(RenameAction.class),
                CutAction.get(CutAction.class),
                CopyAction.get(CopyAction.class),
                PasteAction.get(PasteAction.class),
                DeleteAction.get(DeleteAction.class),
                MoveUpAction.get(MoveUpAction.class),
                MoveDownAction.get(MoveDownAction.class)
        };
        return result;
    }
    
}
