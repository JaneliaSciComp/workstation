package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.janelia.it.jacs.model.domain.NeuronFragment;
import org.janelia.it.jacs.model.domain.NeuronSeparation;
import org.janelia.it.jacs.model.domain.ObjectiveSample;
import org.janelia.it.jacs.model.domain.PipelineResult;
import org.janelia.it.jacs.model.domain.Sample;
import org.janelia.it.jacs.model.domain.SamplePipelineRun;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.workstation.gui.browser.DomainDAO;
import org.janelia.it.workstation.gui.browser.DomainExplorerTopComponent;
import org.janelia.it.workstation.gui.browser.flavors.DomainObjectFlavor;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;
import org.openide.nodes.BeanNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.datatransfer.ExTransferable;

/**
 *
 * @author rokickik
 */
public class SampleNode extends BeanNode<Sample> {
    
    public SampleNode(Sample sample) throws Exception {
        super(sample);
        if (DomainExplorerTopComponent.isShowNeurons()) {
            setChildren(Children.create(new SampleNode.NeuronNodeFactory(), true));   
        }
        else {  
            setChildren(Children.create(new SampleNode.ObjectiveNodeFactory(), true));   
        }
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
            return "<font color='!Label.foreground'>" + getBean().getName() + "</font>" +
                    " <font color='#957D47'><i>" + getBean().getOwnerKey() + "</i></font>";
        } else {
            return null;
        }
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
    public Transferable clipboardCut() throws IOException {
        Transferable deflt = super.clipboardCut();
        ExTransferable added = ExTransferable.create(deflt);
        added.put(new ExTransferable.Single(DomainObjectFlavor.DOMAIN_OBJECT_FLAVOR) {
            protected Sample getData() {
                //return getLookup().lookup(Sample.class);
                return (Sample)getBean();
            }
        });
        return added;
    }

    @Override
    public boolean canDestroy() {
        return true;
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
    
    public class ObjectiveNodeFactory extends ChildFactory<String> {
    
        @Override
        protected boolean createKeys(List<String> list) {
            for(String objective : getBean().getObjectives().keySet()) {
                list.add(objective);
            }
            Collections.sort(list);
            return true;
        }

        @Override
        protected Node createNodeForKey(String key) {
            try {
                return new ObjectiveNode(getBean(), key);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public class NeuronNodeFactory extends ChildFactory<NeuronFragment> {
    
        @Override
        protected boolean createKeys(List<NeuronFragment> list) {
            DomainDAO dao = DomainExplorerTopComponent.getDao();
            Long separationId = null;
            if (getBean().getObjectives()!=null) {
                List<String> objectives = new ArrayList<String>(getBean().getObjectives().keySet());
                Collections.sort(objectives);
                String objective = objectives.get(0);
                ObjectiveSample objectiveSample = getBean().getObjectives().get(objective);
                SamplePipelineRun run = objectiveSample.getLatestRun();
                if (run!=null && run.getResults()!=null) {
                    for(PipelineResult result : run.getResults()) {
                        NeuronSeparation separation = result.getLatestSeparationResult();
                        if (separation!=null) {
                            separationId = separation.getFragmentsReference().getReferenceId();
                        }    
                    }
                    List<NeuronFragment> fragments = dao.getNeuronFragmentsBySeparationId(SessionMgr.getSubjectKey(), separationId);
                    for(NeuronFragment obj : fragments) {
                        if (obj!=null) {
                            list.add(obj);
                        }
                    }
                }
            }
            return true;
        }

        @Override
        protected Node createNodeForKey(NeuronFragment key) {
            try {
                return new NeuronFragmentNode(getBean(), key);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
