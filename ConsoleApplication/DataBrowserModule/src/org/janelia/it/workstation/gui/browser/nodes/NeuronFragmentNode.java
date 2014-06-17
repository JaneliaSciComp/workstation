package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.janelia.it.jacs.model.domain.NeuronFragment;
import org.janelia.it.jacs.model.domain.Sample;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.workstation.gui.util.Icons;
import org.openide.nodes.BeanNode;

/**
 *
 * @author rokickik
 */
public class NeuronFragmentNode extends BeanNode<NeuronFragment> {
    
    private Sample sample;
    
    public NeuronFragmentNode(Sample sample, NeuronFragment neuronFragment) throws Exception {
        super(neuronFragment);
        this.sample = sample;
    }
    
    @Override
    public Image getIcon(int type) {
        Entity sample = new Entity();
        sample.setEntityTypeName("Neuron Fragment");
        return Icons.getIcon(sample, false).getImage();
    }
    
    @Override
    public String getHtmlDisplayName() {
        if (getBean() != null) {
            return "<font color='!Label.foreground'>Neuron Fragment " + getBean().getNumber() + "</font>" +
                    " <font color='#957D47'><i>" + getBean().getOwnerKey() + "</i></font>";
        } else {
            return null;
        }
    }
    
    @Override
    public Action[] getActions(boolean context) {
        Action[] result = new Action[]{
            new RefreshAction()
        };
        return result;
    }

    private final class RefreshAction extends AbstractAction {

        public RefreshAction() {
            putValue(Action.NAME, "Refresh");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            //EntityExplorerTopComponent.refreshNode();
        }
    }
    
}
