package org.janelia.it.workstation.gui.browser.gui.support;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.interfaces.HasFileGroups;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.workstation.gui.browser.model.ResultDescriptor;
import org.janelia.it.workstation.gui.util.Icons;

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;

/**
 * Drop-down button for selecting the result to use. Currently it only supports Samples,
 * but it can be easily extended to support other types in the future.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ResultSelectionButton extends DropDownButton {

    private ResultDescriptor currResult;
    private boolean showTitle;
    
    public ResultSelectionButton() {
        this(false);
    }
    
    public ResultSelectionButton(boolean showTitle) {
        this.showTitle = showTitle;
        setIcon(Icons.getIcon("folder_open_page.png"));
        setFocusable(false);
        setToolTipText("Select the result to display");
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
        
        if (currResult == null) {
            this.currResult = ResultDescriptor.LATEST;
        }
        
        Multiset<String> countedResultNames = LinkedHashMultiset.create();
            
        for(DomainObject domainObject : domainObjects) {
            if (domainObject instanceof Sample) {
                Sample sample = (Sample)domainObject;
                for(String objective : sample.getOrderedObjectives()) {
                    ObjectiveSample objectiveSample = sample.getObjectiveSample(objective);
                    SamplePipelineRun run = objectiveSample.getLatestSuccessfulRun();
                    if (run==null || run.getResults()==null) continue;
                    for(PipelineResult result : run.getResults()) {
                        if (result instanceof HasFileGroups) {
                            HasFileGroups hasGroups = (HasFileGroups)result;
                            for(String groupKey : hasGroups.getGroupKeys()) {
                                String name = objective+" "+result.getName()+" ("+groupKey+")";
                                countedResultNames.add(name);
                            }
                        }
                        else {
                            String name = objective+" "+result.getName();
                            countedResultNames.add(name);
                        }
                    }
                }
            }
        }
        
        setVisible(!countedResultNames.isEmpty());
        getPopupMenu().removeAll();
        
        // Sort in alphanumeric order, with Latest first
        List<String> sortedResultNames = new ArrayList<>(countedResultNames.elementSet());
        Collections.sort(sortedResultNames);
        
        List<ResultDescriptor> sortedResults = new ArrayList<>();
        for(String resultName : sortedResultNames)  {
            if (countedResultNames.count(resultName)>1 || domainObjects.size()==1) {
                sortedResults.add(new ResultDescriptor(resultName));
            }
        }

        sortedResults.add(0, ResultDescriptor.LATEST);
        
        ButtonGroup group = new ButtonGroup();
        
        for(final ResultDescriptor resultDescriptor : sortedResults) {
            String resultName = resultDescriptor.getResultKey();
            JMenuItem menuItem = new JRadioButtonMenuItem(resultName, resultName.equals(currResult.getResultKey()));
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setResultDescriptor(resultDescriptor);
                    resultChanged(currResult);
                }
            });
            getPopupMenu().add(menuItem);
            group.add(menuItem);
        }
    }
    
    protected void resultChanged(ResultDescriptor resultDescriptor) {}

    public ResultDescriptor getResultDescriptor() {
        return currResult;
    }
}
