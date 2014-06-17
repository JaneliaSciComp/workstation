package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.NeuronFragment;
import org.janelia.it.jacs.model.domain.NeuronSeparation;
import org.janelia.it.jacs.model.domain.PipelineResult;
import org.janelia.it.jacs.model.domain.Sample;
import org.janelia.it.jacs.model.domain.SampleAlignmentResult;
import org.janelia.it.jacs.model.domain.SampleProcessingResult;
import org.janelia.it.workstation.gui.browser.DomainDAO;
import org.janelia.it.workstation.gui.browser.DomainExplorerTopComponent;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;
import org.openide.nodes.BeanNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author rokickik
 */
public class PipelineResultNode extends BeanNode<PipelineResult> {
    
    private Logger log = LoggerFactory.getLogger(NeuronFragmentNode.class);
    
    private Sample sample;
    
    public PipelineResultNode(Sample sample, PipelineResult result) throws Exception {
        super(result);
        this.sample = sample;
        if (getBean() instanceof NeuronSeparation) {
            setChildren(Children.create(new NeuronChildFactory(), true));   
        }
        else {
            setChildren(Children.create(new ResultChildFactory(), true));       
        } 
    }
    
    @Override
    public Image getIcon(int type) {
        return Icons.getIcon("folder_image.png").getImage();
    }
    
    @Override
    public String getHtmlDisplayName() {
        PipelineResult result = getBean();
        if (getBean() != null) {
            String name = "Result";
            if (result instanceof NeuronSeparation) {
                 name = "Neuron Separation";
            }
            else if (result instanceof SampleProcessingResult) {
                name = "Sample Processing";
            }
            else if (result instanceof SampleAlignmentResult) {
                SampleAlignmentResult alignment = (SampleAlignmentResult)result;
                name = "Sample Alignment";
            }
            
            return "<font color='!textText'>" + name + "</font>" +
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
    
    public class ResultChildFactory extends ChildFactory<PipelineResult> {
    
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
    
    public class NeuronChildFactory extends ChildFactory<NeuronFragment> {
    
        @Override
        protected boolean createKeys(List<NeuronFragment> list) {
            DomainDAO dao = DomainExplorerTopComponent.getDao();
            NeuronSeparation separation = (NeuronSeparation)getBean();
            if (separation.getFragmentsReference()!=null) {
                for(DomainObject object : dao.getDomainObjects(SessionMgr.getSubjectKey(), separation.getFragmentsReference())) {
                    if (object instanceof NeuronFragment) {
                        list.add((NeuronFragment)object);
                    }
                    else {
                        log.warn("Fragments reference contains non-fragment: "+object.getId());
                    }
                }
            }
            return true;
        }

        @Override
        protected Node createNodeForKey(NeuronFragment key) {
            try {
                return new NeuronFragmentNode(sample, key);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
