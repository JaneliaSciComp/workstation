package org.janelia.workstation.browser.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;

import javax.swing.Action;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.neuron.NeuronMergeTask;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.sample.NeuronFragment;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.common.actions.ViewerContextAction;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.StateMgr;
import org.janelia.workstation.core.workers.BackgroundWorker;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.core.workers.TaskMonitoringWorker;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.janelia.workstation.integration.spi.domain.ContextualActionUtils;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.util.lookup.ServiceProvider;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position=560)
public class NeuronMergeBuilder implements ContextualActionBuilder {

    private static ViewDataSetSettingsAction action = new ViewDataSetSettingsAction();

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof NeuronFragment;
    }

    @Override
    public Action getAction(Object obj) {
        return action;
    }

    public static class ViewDataSetSettingsAction extends ViewerContextAction {

        private HashSet<NeuronFragment> fragments;

        @Override
        public String getName() {
            return "Merge " + fragments.size() + " Selected Neurons";
        }

        @Override
        public void setup() {
            ViewerContext viewerContext = getViewerContext();
            this.fragments = new LinkedHashSet<>();
            for (Object obj : viewerContext.getSelectedObjects()) {
                if (obj instanceof NeuronFragment) {
                    fragments.add((NeuronFragment)obj);
                }
            }
            ContextualActionUtils.setVisible(this, fragments.size()>=2);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ActivityLogHelper.logUserAction("DomainObjectContextMenu.mergeSelectedNeurons");
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
                    return new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {

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
                        }
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

            Collections.sort(fragmentList, new Comparator<NeuronFragment>() {
                @Override
                public int compare(NeuronFragment o1, NeuronFragment o2) {
                    Integer o1n = o1.getNumber();
                    Integer o2n = o2.getNumber();
                    return o1n.compareTo(o2n);
                }
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
}