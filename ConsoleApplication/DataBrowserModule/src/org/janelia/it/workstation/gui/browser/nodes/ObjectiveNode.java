package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.janelia.it.jacs.model.domain.ObjectiveSample;
import org.janelia.it.jacs.model.domain.Sample;
import org.janelia.it.jacs.model.domain.SamplePipelineRun;
import org.janelia.it.workstation.gui.util.Icons;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

/**
 *
 * @author rokickik
 */
public class ObjectiveNode extends AbstractNode {
    
    private String objective;
    private Sample sample;
    
    public ObjectiveNode(Sample sample, String objective) throws Exception {
        super(Children.LEAF);
        this.sample = sample;
        this.objective = objective;
        setChildren(Children.create(new ObjectiveNode.MyChildFactory(), true));
    }
    
    @Override
    public Image getIcon(int type) {
        return Icons.getIcon("page_white.png").getImage();
    }
    
    @Override
    public String getHtmlDisplayName() {
        if (objective != null) {
            return "<font color='!Label.foreground'>Objective " + objective + "</font>";
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
    
    public class MyChildFactory extends ChildFactory<SamplePipelineRun> {
    
        @Override
        protected boolean createKeys(List<SamplePipelineRun> list) {
            ObjectiveSample objectiveSample = sample.getObjectives().get(objective);
            if (objectiveSample.getPipelineRuns()!=null) {
                for(SamplePipelineRun run : objectiveSample.getPipelineRuns()) {
                    list.add(run);
                }
            }
            return true;
        }

        @Override
        protected Node createNodeForKey(SamplePipelineRun key) {
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
