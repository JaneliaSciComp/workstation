package org.janelia.it.workstation.browser.gui.support;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.plaf.basic.BasicMenuUI;

import org.apache.commons.lang3.StringUtils;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.gui.support.buttons.DropDownButton;
import org.janelia.it.workstation.browser.model.ResultCategory;
import org.janelia.it.workstation.browser.model.descriptors.ArtifactDescriptor;
import org.janelia.it.workstation.browser.model.descriptors.DescriptorUtils;
import org.janelia.it.workstation.browser.model.descriptors.ResultArtifactDescriptor;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.sample.Sample;
import org.janelia.model.domain.sample.SamplePostProcessingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;

/**
 * Drop-down button for selecting the result to use. Currently it only supports Samples,
 * but it can be easily extended to support other types in the future.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ResultSelectionButton extends DropDownButton {

    private static final Logger log = LoggerFactory.getLogger(ResultSelectionButton.class);
    
    private static final int MAX_TITLE_LENGTH = 30;
    
    private ArtifactDescriptor currResult;
    private boolean showTitle;

    private ButtonGroup group;

    public ResultSelectionButton() {
        this(true);
    }
    
    public ResultSelectionButton(boolean showTitle) {
        this.showTitle = showTitle;
        setIcon(Icons.getIcon("folder_open_page.png"));
        setToolTipText("Select the result to display");
        reset();
    }

    public void reset() {
        setResultDescriptor(ArtifactDescriptor.LATEST);
    }

    public void setResultDescriptor(ArtifactDescriptor descriptor) {
        this.currResult = descriptor == null ? ArtifactDescriptor.LATEST : descriptor;
        if (showTitle) {
            String title = StringUtils.abbreviate(currResult.toString(), MAX_TITLE_LENGTH);
            setText(title);
        }
    }

    public void populate(DomainObject domainObject) {
        populate(Arrays.asList(domainObject));
    }
    
    public synchronized void populate(Collection<DomainObject> domainObjects) {

        // Reset state
        this.group = new ButtonGroup();
        removeAll();
        
        List<DomainObject> samplesOnly = domainObjects.stream()
                .filter((domainObject) -> (domainObject instanceof Sample))
                .collect(Collectors.toList());
        
        Multiset<ArtifactDescriptor> countedArtifacts = DescriptorUtils.getArtifactCounts(samplesOnly);

        Multimap<ResultArtifactDescriptor, ResultArtifactDescriptor> topMenuItems = HashMultimap.create();
        Multiset<ArtifactDescriptor> countedTopArtifacts = LinkedHashMultiset.create();
        for (ArtifactDescriptor ad : countedArtifacts) {
            if (ad instanceof ResultArtifactDescriptor) {
                ResultArtifactDescriptor rad = (ResultArtifactDescriptor)ad;
                ResultArtifactDescriptor withoutObjective = rad.withoutObjective();
                topMenuItems.put(withoutObjective, rad);
                countedTopArtifacts.add(withoutObjective);
                log.info("counting {} -> {}", withoutObjective, rad);
            }
        }
        
        List<ResultArtifactDescriptor> topMenuSortedKeys = topMenuItems.keySet().stream()
            .sorted(new Comparator<ArtifactDescriptor>() {
                @Override
                public int compare(ArtifactDescriptor o1, ArtifactDescriptor o2) {
    
                    ResultArtifactDescriptor r1 = (ResultArtifactDescriptor)o1;
                    ResultArtifactDescriptor r2 = (ResultArtifactDescriptor)o2;
                    boolean r1Post = r1.getResultName()!=null && r1.getResultName().startsWith("Post");
                    boolean r2Post = r2.getResultName()!=null && r2.getResultName().startsWith("Post");
                    
                    return ComparisonChain.start()
                            .compare(r1.getArea(), r2.getArea(), Ordering.natural().nullsFirst())
                            .compare(r1Post, r2Post, Ordering.natural())
                            .compare(r1.toString(), r2.toString(), Ordering.natural())
                            .result();
                }
            })
            .collect(Collectors.toList());
        
        List<ArtifactDescriptor> genericDescriptors = new ArrayList<>();  
        genericDescriptors.add(ArtifactDescriptor.LATEST);      
        genericDescriptors.add(ArtifactDescriptor.LATEST_UNALIGNED);
        genericDescriptors.add(ArtifactDescriptor.LATEST_ALIGNED);

        List<ResultArtifactDescriptor> unalignedDescriptors = new ArrayList<>();  
        List<ResultArtifactDescriptor> postDescriptors = new ArrayList<>();  
        List<ResultArtifactDescriptor> alignedDescriptors = new ArrayList<>();
        for(final ResultArtifactDescriptor descriptor : topMenuSortedKeys) {
            log.info("descriptor={}", descriptor);
            if (descriptor.isAligned()) {
                alignedDescriptors.add(descriptor);
            }
            else {
                if (descriptor.getResultClass().equals(SamplePostProcessingResult.class.getName())) {
                    postDescriptors.add(descriptor);
                }
                else {
                    unalignedDescriptors.add(descriptor);
                }
            }
        }
        
        // Add everything to the menu
        for (ArtifactDescriptor descriptor : genericDescriptors) {
            addMenuItem(createMenuItem(descriptor, descriptor.toString(), null));
        }

        if (!unalignedDescriptors.isEmpty()) {
            addMenuItem(createLabelItem(""));
            addMenuItem(createLabelItem(ResultCategory.PROCESSED.getLabel()));
            for (ResultArtifactDescriptor descriptor : unalignedDescriptors) {
                int count = countedTopArtifacts.count(descriptor);
                JMenu submenu = createSubMenu(descriptor, count);
                submenu.add(createMenuItem(descriptor, "All", count));
                for (ResultArtifactDescriptor objectiveDescriptor : topMenuItems.get(descriptor)) {
                    int subcount = countedArtifacts.count(objectiveDescriptor);
                    submenu.add(createMenuItem(objectiveDescriptor, objectiveDescriptor.getObjective(), subcount));
                }
                addMenuItem(submenu);
            }
        }

        if (!postDescriptors.isEmpty()) {
            addMenuItem(createLabelItem(""));
            addMenuItem(createLabelItem(ResultCategory.POST_PROCESSED.getLabel()));
            for (ResultArtifactDescriptor descriptor : postDescriptors) {
                int count = countedTopArtifacts.count(descriptor);
                JMenu submenu = createSubMenu(descriptor, count);
                submenu.add(createMenuItem(descriptor, "All", count));
                for (ResultArtifactDescriptor objectiveDescriptor : topMenuItems.get(descriptor)) {
                    int subcount = countedArtifacts.count(objectiveDescriptor);
                    submenu.add(createMenuItem(objectiveDescriptor, objectiveDescriptor.getObjective(), subcount));
                }
                addMenuItem(submenu);
            }
        }
        
        if (!alignedDescriptors.isEmpty()) {
            addMenuItem(createLabelItem(""));
            addMenuItem(createLabelItem(ResultCategory.ALIGNED.getLabel()));
            for (ResultArtifactDescriptor descriptor : alignedDescriptors) {
                int count = countedTopArtifacts.count(descriptor);
                JMenu submenu = createSubMenu(descriptor, count);
                submenu.add(createMenuItem(descriptor, "All", count));
                for (ResultArtifactDescriptor objectiveDescriptor : topMenuItems.get(descriptor)) {
                    int subcount = countedArtifacts.count(objectiveDescriptor);
                    submenu.add(createMenuItem(objectiveDescriptor, objectiveDescriptor.getObjective(), subcount));
                }
                addMenuItem(submenu);
            }
        }
        
        // Hide the button if there are no artifacts
        setVisible(!countedArtifacts.isEmpty());
    }
    
    private JMenuItem createLabelItem(String text) {
        JMenuItem menuItem = new JMenuItem(text);
        menuItem.setEnabled(false);
        return menuItem;
    }
    
    private JMenu createSubMenu(final ArtifactDescriptor descriptor, Integer count) {
        String resultName = descriptor.toString();
        if (count != null) resultName += " (" + count + " items)";
        JMenu submenu = new JMenu(resultName);
        return submenu;
    }
    
    private JMenuItem createMenuItem(final ArtifactDescriptor descriptor, String label, Integer count) {

        if (count != null) label += " (" + count + " items)";
        JMenuItem menuItem = new JRadioButtonMenuItem(label, descriptor.equals(currResult));
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
