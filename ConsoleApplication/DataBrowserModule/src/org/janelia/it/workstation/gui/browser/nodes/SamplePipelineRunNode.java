package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.janelia.it.jacs.model.domain.PipelineResult;
import org.janelia.it.jacs.model.domain.Sample;
import org.janelia.it.jacs.model.domain.SamplePipelineRun;
import org.janelia.it.workstation.gui.util.Icons;
import org.openide.nodes.BeanNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

/**
 *
 * @author rokickik
 */
public class SamplePipelineRunNode extends BeanNode<SamplePipelineRun> {
    
    private Sample sample;
    
    public SamplePipelineRunNode(Sample sample, SamplePipelineRun run) throws Exception {
        super(run);
        this.sample = sample;
        setChildren(Children.create(new MyChildFactory(), true));   
    }
    
    @Override
    public Image getIcon(int type) {
        return Icons.getIcon("folder_image.png").getImage();
    }
    
    @Override
    public String getHtmlDisplayName() {
        if (getBean() != null) {
            return "<font color='!Label.foreground'>" + getBean().getName() + "</font>" +
                    " <font color='#957D47'><i>" + getBean().getCreationDate() + "</i></font>";
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
    
    public class MyChildFactory extends ChildFactory<PipelineResult> {
    
        @Override
        protected boolean createKeys(List<PipelineResult> list) {
            if (getBean().getResults()!=null) {
                list.addAll(getBean().getResults());
            }
            return true;
        }

        @Override
        protected Node createNodeForKey(PipelineResult key) {
            try {
                return new PipelineResultNode(sample, key);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

    }
}
