package org.janelia.it.workstation.gui.browser.gui.support;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

import com.google.common.base.Supplier;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Multiset;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.interfaces.HasFileGroups;
import org.janelia.it.jacs.model.domain.sample.LSMSummaryResult;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.support.ResultDescriptor;
import org.janelia.it.workstation.gui.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.gui.util.Icons;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drop-down button for selecting the result to use. Currently it only supports Samples,
 * but it can be easily extended to support other types in the future.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ResultSelectionButton extends DropDownButton {

    private static final Logger log = LoggerFactory.getLogger(ResultSelectionButton.class);

    private ResultDescriptor currResult;
    private boolean showTitle;

    private JPopupMenu popupMenu;

    public ResultSelectionButton() {
        this(false);
    }
    
    public ResultSelectionButton(boolean showTitle) {

        // We don't use the default DropDownButton menu because it's a JYPopupmenu that
        // doesn't play nicely with the scrollable popup menus we need
        popupMenu = new JPopupMenu();
        popupMenu.setLightWeightPopupEnabled(true);
        setPopupMenu(popupMenu);

        this.showTitle = showTitle;
        setIcon(Icons.getIcon("folder_open_page.png"));
        setToolTipText("Select the result to display");
        reset();
    }

    public void reset() {
        setResultDescriptor(ResultDescriptor.LATEST);
    }

    public void setResultDescriptor(ResultDescriptor currResult) {
        this.currResult = currResult;
        if (showTitle) {
            setText(currResult.toString());
        }
    }

    public void populate(DomainObject domainObject) {
        populate(Arrays.asList(domainObject));
    }
    
    public void populate(Collection<DomainObject> domainObjects) {

        Multiset<ResultDescriptor> countedResultNames = LinkedHashMultiset.create();
            
        for(DomainObject domainObject : domainObjects) {
            if (domainObject instanceof Sample) {
                Sample sample = (Sample)domainObject;
                for(ObjectiveSample objectiveSample : sample.getObjectiveSamples()) {
                    SamplePipelineRun run = objectiveSample.getLatestSuccessfulRun();
                    if (run==null || run.getResults()==null) {
                        run = objectiveSample.getLatestRun();
                        if (run==null || run.getResults()==null) continue;
                    }
                    for(PipelineResult result : run.getResults()) {
                        if (result instanceof HasFileGroups && !(result instanceof LSMSummaryResult)) {
                            HasFileGroups hasGroups = (HasFileGroups)result;
                            for(String groupKey : hasGroups.getGroupKeys()) {
                                ResultDescriptor rd = ResultDescriptor.create().setObjective(objectiveSample.getObjective()).setResultName(result.getName()).setGroupName(groupKey);
                                countedResultNames.add(rd);
                            }
                        }
                        if (!DomainUtils.get2dTypeNames(result).isEmpty()) {
                            ResultDescriptor rd = ResultDescriptor.create().setObjective(objectiveSample.getObjective()).setResultName(result.getName());
                            countedResultNames.add(rd);
                        }
                    }
                }
            }
        }
        
        setVisible(!countedResultNames.isEmpty());
        getPopupMenu().removeAll();
        
        // Sort in alphanumeric order, with Latest first
        List<ResultDescriptor> sortedResults = new ArrayList<>(countedResultNames.elementSet());
        Collections.sort(sortedResults, new Comparator<ResultDescriptor>() {
            @Override
            public int compare(ResultDescriptor o1, ResultDescriptor o2) {
                return o1.toString().compareTo(o2.toString());
            }
        });

        sortedResults.add(0, ResultDescriptor.LATEST_ALIGNED);
        sortedResults.add(0, ResultDescriptor.LATEST_UNALIGNED);
        sortedResults.add(0, ResultDescriptor.LATEST);
        
        ButtonGroup group = new ButtonGroup();

        // Need linked hash map to maintain sorted keys
        Multimap<String,ResultDescriptor> groupedResults = Multimaps.newListMultimap(
                new LinkedHashMap<String,Collection<ResultDescriptor>>(),
                new Supplier<List<ResultDescriptor>>() {
                    @Override
                    public List<ResultDescriptor> get() {
                        return Lists.newArrayList();
                    }
                });

        for(final ResultDescriptor resultDescriptor : sortedResults) {
            groupedResults.put(getGroupLessLabel(resultDescriptor), resultDescriptor);
        }

        for(final String groupLessLabel : groupedResults.keySet()) {

            Collection<ResultDescriptor> resultDescriptors = groupedResults.get(groupLessLabel);
            if (resultDescriptors.size()==1) {
                final ResultDescriptor resultDescriptor = resultDescriptors.iterator().next();
                int count = countedResultNames.count(resultDescriptor);
                String resultName = resultDescriptor.toString();
                if (count > 0) resultName += " (" + count + " items)";
                JMenuItem menuItem = new JRadioButtonMenuItem(resultName, resultDescriptor.equals(currResult));
                menuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        setResultDescriptor(resultDescriptor);
                        resultChanged(currResult);
                        ActivityLogHelper.logUserAction("ResultSelectionButton.resultChanged", resultDescriptor.toString());
                    }
                });
                getPopupMenu().add(menuItem);
                group.add(menuItem);
            }
            else {
                JScrollMenu subMenu = new JScrollMenu(groupLessLabel);
                for(final ResultDescriptor resultDescriptor : resultDescriptors) {
                    int count = countedResultNames.count(resultDescriptor);
                    String resultName = resultDescriptor.getGroupName();
                    if (resultName==null) resultName = "Default";
                    if (count > 0) resultName += " (" + count + " items) ";
                    JMenuItem menuItem = new JRadioButtonMenuItem(resultName, resultDescriptor.equals(currResult));
                    menuItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            setResultDescriptor(resultDescriptor);
                            resultChanged(currResult);
                            ActivityLogHelper.logUserAction("ResultSelectionButton.resultChanged", resultDescriptor.toString());
                        }
                    });
                    subMenu.add(menuItem);
                    group.add(menuItem);
                }
                getPopupMenu().add(subMenu);
            }
        }
    }
    
    protected void resultChanged(ResultDescriptor resultDescriptor) {}

    public ResultDescriptor getResultDescriptor() {
        return currResult;
    }

    public String getGroupLessLabel(ResultDescriptor resultDescriptor) {
        String objective = resultDescriptor.getObjective();
        String resultName = resultDescriptor.getResultName();
        Boolean aligned = resultDescriptor.isAligned();
        StringBuilder sb =  new StringBuilder();
        if (objective!=null) {
            sb.append(objective).append(" ");
        }
        if (resultName!=null) {
            sb.append(resultName).append(" ");
        }
        else {
            sb.append("Latest ");
        }
        if (aligned!=null) {
            String alignment = aligned ? "Aligned" : "Unaligned";
            if (resultName==null) {
                sb.append(alignment).append(" ");
            }
            else {
                sb.append("(").append(alignment).append(") ");
            }
        }
        if (sb.length()>0 && sb.charAt(sb.length()-1)==' ') {
            sb.deleteCharAt(sb.length()-1);
        }
        return sb.toString();
    }
}
