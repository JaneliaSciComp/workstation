package org.janelia.it.workstation.browser.gui.support;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.interfaces.HasAnatomicalArea;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SampleAlignmentResult;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.sample.SamplePostProcessingResult;
import org.janelia.it.jacs.model.domain.sample.SampleTile;
import org.janelia.it.jacs.model.domain.support.ResultDescriptor;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.model.ResultCategory;
import org.janelia.it.workstation.browser.model.descriptors.ArtifactDescriptor;
import org.janelia.it.workstation.browser.model.descriptors.ResultArtifactDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;

/**
 * Drop-down button for selecting the result to use. Currently it only supports Samples,
 * but it can be easily extended to support other types in the future.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ResultSelectionButton extends ScrollingDropDownButton {

    private static final Logger log = LoggerFactory.getLogger(ResultSelectionButton.class);
    
    private ArtifactDescriptor currResult;
    private boolean showTitle;

    private ButtonGroup group;
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
        setResultDescriptor(ArtifactDescriptor.LATEST);
    }

    public void setResultDescriptor(ArtifactDescriptor currResult) {
        this.currResult = currResult;
        if (showTitle) {
            setText(currResult.toString());
        }
    }

    public void populate(DomainObject domainObject) {
        populate(Arrays.asList(domainObject));
    }
    
    public synchronized void populate(Collection<DomainObject> domainObjects) {

        // Reset state
        this.group = new ButtonGroup();
        getPopupMenu().removeAll();
        
        Multiset<ArtifactDescriptor> countedArtifacts = LinkedHashMultiset.create();
        
        Collection<Sample> samplesOnly = new ArrayList<>();
        for (DomainObject domainObject : domainObjects) {
            if (domainObject instanceof Sample) {
                samplesOnly.add((Sample)domainObject);
            }
        }
        
        for(Sample sample : samplesOnly) {
            for(ObjectiveSample objectiveSample : sample.getObjectiveSamples()) {
                SamplePipelineRun run = objectiveSample.getLatestSuccessfulRun();
                if (run==null || run.getResults()==null) {
                    run = objectiveSample.getLatestRun();
                    if (run==null || run.getResults()==null) continue;
                }
                if (run!=null) {
                    for(PipelineResult result : run.getResults()) {
                        log.trace("  Inspecting pipeline result: {}", result.getName());
                        if (result instanceof SamplePostProcessingResult) {
                            // Add a descriptor for every anatomical area in the sample
                            for (SampleTile sampleTile : objectiveSample.getTiles()) {
                                ResultArtifactDescriptor rad = new ResultArtifactDescriptor(objectiveSample.getObjective(), sampleTile.getAnatomicalArea(), result.getName(), false);
                                log.trace("    Adding result artifact descriptor: {}", rad);
                                countedArtifacts.add(rad);
                            }
                        }
                        else if (result instanceof HasAnatomicalArea){
                            HasAnatomicalArea aaResult = (HasAnatomicalArea)result;
                            ResultArtifactDescriptor rad = new ResultArtifactDescriptor(objectiveSample.getObjective(), aaResult.getAnatomicalArea(), result.getName(), result instanceof SampleAlignmentResult);
                            log.trace("    Adding result artifact descriptor: {}", rad);
                            countedArtifacts.add(rad);
                        }
                        else {
                            log.trace("Cannot handle result '"+result.getName()+"' of type "+result.getClass().getSimpleName());
                        }
                    }
                }
            }
        }
        
        setVisible(!countedArtifacts.isEmpty());
        
        // Sort in alphanumeric order, with Latest first
        List<ArtifactDescriptor> sortedResults = new ArrayList<>(countedArtifacts.elementSet());
        Collections.sort(sortedResults, new Comparator<ArtifactDescriptor>() {
            @Override
            public int compare(ArtifactDescriptor o1, ArtifactDescriptor o2) {
                return o1.toString().compareTo(o2.toString());
            }
        });

        List<ArtifactDescriptor> genericDescriptors = new ArrayList<>();  
        genericDescriptors.add(ArtifactDescriptor.LATEST);      
        genericDescriptors.add(ArtifactDescriptor.LATEST_UNALIGNED);
        genericDescriptors.add(ArtifactDescriptor.LATEST_ALIGNED);

        List<ArtifactDescriptor> unalignedDescriptors = new ArrayList<>();  
        List<ArtifactDescriptor> alignedDescriptors = new ArrayList<>();
        for(final ArtifactDescriptor descriptor : sortedResults) {
            if (descriptor.isAligned()) {
                alignedDescriptors.add(descriptor);
            }
            else {
                unalignedDescriptors.add(descriptor);
            }
        }
        
        // Add everything to the menu
        for (ArtifactDescriptor descriptor : genericDescriptors) {
            getPopupMenu().add(createMenuItem(descriptor, 0));
        }

        if (!unalignedDescriptors.isEmpty()) {
            getPopupMenu().add(createLabelItem(""));
            getPopupMenu().add(createLabelItem(ResultCategory.PreAligned.getLabel()));
            for (ArtifactDescriptor descriptor : unalignedDescriptors) {
                int count = countedArtifacts.count(descriptor);
                getPopupMenu().add(createMenuItem(descriptor, count));
            }
        }

        if (!alignedDescriptors.isEmpty()) {
            getPopupMenu().add(createLabelItem(""));
            getPopupMenu().add(createLabelItem(ResultCategory.PostAligned.getLabel()));
            for (ArtifactDescriptor descriptor : alignedDescriptors) {
                int count = countedArtifacts.count(descriptor);
                getPopupMenu().add(createMenuItem(descriptor, count));
            }
        }
    }
    
    private JMenuItem createLabelItem(String text) {
        JMenuItem menuItem = new JMenuItem(text);
        menuItem.setEnabled(false);
        return menuItem;
    }
    
    private JMenuItem createMenuItem(final ArtifactDescriptor descriptor, int count) {

        String resultName = descriptor.toString();
        if (count > 0) resultName += " (" + count + " items)";
        JMenuItem menuItem = new JRadioButtonMenuItem(resultName, descriptor.equals(currResult));
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setResultDescriptor(descriptor);
                resultChanged(currResult);
                ActivityLogHelper.logUserAction("ResultSelectionButton.resultChanged", descriptor.toString());
            }
        });
        group.add(menuItem);
        return menuItem;
    }
    
    protected void resultChanged(ArtifactDescriptor resultDescriptor) {}

    public ArtifactDescriptor getResultDescriptor() {
        return currResult;
    }
}
