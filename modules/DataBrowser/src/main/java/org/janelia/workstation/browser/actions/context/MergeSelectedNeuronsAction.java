package org.janelia.workstation.browser.actions.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.neuron.NeuronMergeTask;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.sample.NeuronFragment;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.StateMgr;
import org.janelia.workstation.core.workers.BackgroundWorker;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.core.workers.TaskMonitoringWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "Actions",
        id = "MergeSelectedNeuronsAction"
)
@ActionRegistration(
        displayName = "#CTL_MergeSelectedNeuronsAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions/Sample", position = 560)
})
@NbBundle.Messages("CTL_MergeSelectedNeuronsAction=Merge Selected Neurons")
public class MergeSelectedNeuronsAction extends BaseContextualNodeAction {

    private Set<NeuronFragment> fragments = new HashSet<>();

    @Override
    public String getName() {
        if (fragments.isEmpty()) return super.getName();
        return "Merge " + fragments.size() + " Selected Neurons";
    }

    @Override
    protected void processContext() {
        fragments.clear();
        if (getNodeContext().isOnlyObjectsOfType(NeuronFragment.class)) {
            fragments.addAll(getNodeContext().getOnlyObjectsOfType(NeuronFragment.class));
            setEnabledAndVisible(fragments.size()>=2);
        }
        else {
            setEnabledAndVisible(false);
        }
    }

    @Override
    public void performAction() {
        Set<NeuronFragment> fragments = new HashSet<>(this.fragments);

        ActivityLogHelper.logUserAction("MergeSelectedNeuronsAction.performAction");
        BackgroundWorker executeWorker = new TaskMonitoringWorker() {

            @Override
            public String getName() {
                return "Merge Neuron Fragments ";
            }

            @Override
            protected void doStuff() throws Exception {

                setStatus("Submitting task");
                long taskId = startMergeTask();
                setTaskId(taskId);
                setStatus("Grid execution");

                // Wait until task is finished
                super.doStuff();

                if (isCancelled()) throw new CancellationException();
                setStatus("Done merging");
            }

            @Override
            public Callable<Void> getSuccessCallback() {
                return () -> {

                    // Domain model must be called from a background thread
                    SimpleWorker simpleWorker = new SimpleWorker() {
                        @Override
                        protected void doStuff() throws Exception {
                            NeuronFragment fragment = fragments.iterator().next();
                            Reference sampleRef = fragment.getSample();
                            Sample sample = DomainMgr.getDomainMgr().getModel().getDomainObject(sampleRef);
                            DomainMgr.getDomainMgr().getModel().invalidate(sample);
                        }

                        @Override
                        protected void hadSuccess() {
                        }

                        @Override
                        protected void hadError(Throwable error) {
                            FrameworkAccess.handleException(error);
                        }
                    };

                    simpleWorker.execute();
                    return null;
                };
            }
        };

        executeWorker.executeWithEvents();
    }

    private long startMergeTask() throws Exception {
        Long parentId = null;
        List<NeuronFragment> fragmentList = new ArrayList<>();
        for (NeuronFragment fragment : fragments) {
            Long resultId = fragment.getSeparationId();
            if (parentId == null) {
                parentId = resultId;
            } else if (resultId == null || !parentId.equals(resultId)) {
                throw new IllegalStateException(
                        "The selected neuron fragments are not part of the same neuron separation result: parentId="
                                + parentId + " resultId=" + resultId);
            }
            fragmentList.add(fragment);
        }

        Collections.sort(fragmentList, (o1, o2) -> {
            Integer o1n = o1.getNumber();
            Integer o2n = o2.getNumber();
            return o1n.compareTo(o2n);
        });

        HashSet<String> fragmentIds = new LinkedHashSet<>();
        for (NeuronFragment fragment : fragmentList) {
            fragmentIds.add(fragment.getId().toString());
        }

        HashSet<TaskParameter> taskParameters = new HashSet<>();
        taskParameters.add(new TaskParameter(NeuronMergeTask.PARAM_separationEntityId, parentId.toString(), null));
        taskParameters.add(new TaskParameter(NeuronMergeTask.PARAM_commaSeparatedNeuronFragmentList, Task.csvStringFromCollection(fragmentIds), null));
        Task mergeTask = StateMgr.getStateMgr().submitJob("NeuronMerge", "Neuron Merge Task", taskParameters);
        return mergeTask.getObjectId();
    }

}