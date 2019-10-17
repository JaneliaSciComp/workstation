package org.janelia.workstation.browser.gui.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.ButtonGroup;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import org.apache.commons.lang3.StringUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.sample.Sample;
import org.janelia.model.domain.sample.SamplePostProcessingResult;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.common.gui.support.buttons.DropDownButton;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.model.ResultCategory;
import org.janelia.workstation.core.model.descriptors.ArtifactDescriptor;
import org.janelia.workstation.core.model.descriptors.DescriptorUtils;
import org.janelia.workstation.core.model.descriptors.ResultArtifactDescriptor;

/**
 * Drop-down button for selecting the result to use. Currently it only supports Samples,
 * but it can be easily extended to support other types in the future.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ResultSelectionButton extends DropDownButton {
    
    private static final int MAX_TITLE_LENGTH = 30;

    private final List<ArtifactDescriptor> descriptors = new ArrayList<>();
    private ArtifactDescriptor currResult;
    private boolean showTitle;
    private boolean showCounts;
    private boolean showOnly3dItems;

    private ButtonGroup group;

    public ResultSelectionButton() {
        this(true);
    }

    public ResultSelectionButton(boolean showTitle) {
        this(showTitle, true, true, false);
    }

    public ResultSelectionButton(boolean showTitle, boolean showIcon, boolean showCounts, boolean showOnly3dItems) {
        this.showTitle = showTitle;
        this.showCounts = showCounts;
        this.showOnly3dItems = showOnly3dItems;
        setToolTipText("Select the result to display");
        if (showIcon) {
            setIcon(Icons.getIcon("folder_open_page.png"));
        }
        reset();
    }

    public void reset() {
        ArtifactDescriptor defaultDescriptor = descriptors.isEmpty() ? null : descriptors.get(0);
        setResultDescriptor(defaultDescriptor);
    }

    public void setResultDescriptor(ArtifactDescriptor descriptor) {
        this.currResult = descriptor;
        if (currResult!=null && showTitle) {
            String title = StringUtils.abbreviate(currResult.toString(), MAX_TITLE_LENGTH);
            setText(title);
        }
    }

    public void populate(DomainObject domainObject) {
        populate(Collections.singletonList(domainObject));
    }
    
    public synchronized void populate(Collection<DomainObject> domainObjects) {

        // Reset state
        this.descriptors.clear();
        this.group = new ButtonGroup();
        removeAll();
        
        List<DomainObject> samplesOnly = domainObjects.stream()
                .filter((domainObject) -> (domainObject instanceof Sample))
                .collect(Collectors.toList());
        
        Multiset<ArtifactDescriptor> countedArtifacts = DescriptorUtils.getArtifactCounts(samplesOnly);
                
        // Sorted list of ResultArtifactDescriptor
        List<ResultArtifactDescriptor> sortedResults = countedArtifacts.elementSet().stream()
                .filter(artifact -> artifact instanceof ResultArtifactDescriptor)
                .map(artifact -> (ResultArtifactDescriptor)artifact)
                .sorted((Comparator<ArtifactDescriptor>) (o1, o2) -> {

                    ResultArtifactDescriptor r1 = (ResultArtifactDescriptor)o1;
                    ResultArtifactDescriptor r2 = (ResultArtifactDescriptor)o2;
                    boolean r1Post = r1.getResultName()!=null && r1.getResultName().startsWith("Post");
                    boolean r2Post = r2.getResultName()!=null && r2.getResultName().startsWith("Post");

                    return ComparisonChain.start()
                            .compare(r1.getObjective(), r2.getObjective(), Ordering.natural().nullsLast())
                            .compare(r1.getArea(), r2.getArea(), Ordering.natural().nullsFirst())
                            .compare(r1Post, r2Post, Ordering.natural())
                            .compare(r1.toString(), r2.toString(), Ordering.natural())
                            .result();
                })
                .collect(Collectors.toList());
        
        List<ArtifactDescriptor> genericDescriptors = new ArrayList<>();
        if (!showOnly3dItems) {
            genericDescriptors.add(ArtifactDescriptor.LATEST);
            genericDescriptors.add(ArtifactDescriptor.LATEST_UNALIGNED);
            genericDescriptors.add(ArtifactDescriptor.LATEST_ALIGNED);
        }

        List<ArtifactDescriptor> unalignedDescriptors = new ArrayList<>();  
        List<ArtifactDescriptor> postDescriptors = new ArrayList<>();  
        List<ArtifactDescriptor> alignedDescriptors = new ArrayList<>();
        for(final ResultArtifactDescriptor descriptor : sortedResults) {
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
            addMenuItem(createMenuItem(descriptor, 0));
        }

        if (!unalignedDescriptors.isEmpty()) {
            if (!genericDescriptors.isEmpty()) {
                // If there are items above, we need a space first
                addMenuItem(createLabelItem(""));
            }
            addMenuItem(createLabelItem(ResultCategory.PROCESSED.getLabel()));
            for (ArtifactDescriptor descriptor : unalignedDescriptors) {
                int count = countedArtifacts.count(descriptor);
                addMenuItem(createMenuItem(descriptor, count));
            }
        }

        if (!showOnly3dItems) {
            if (!postDescriptors.isEmpty()) {
                addMenuItem(createLabelItem(""));
                addMenuItem(createLabelItem(ResultCategory.POST_PROCESSED.getLabel()));
                for (ArtifactDescriptor descriptor : postDescriptors) {
                    int count = countedArtifacts.count(descriptor);
                    addMenuItem(createMenuItem(descriptor, count));
                }
            }
        }

        if (!alignedDescriptors.isEmpty()) {
            addMenuItem(createLabelItem(""));
            addMenuItem(createLabelItem(ResultCategory.ALIGNED.getLabel()));
            for (ArtifactDescriptor descriptor : alignedDescriptors) {
                int count = countedArtifacts.count(descriptor);
                addMenuItem(createMenuItem(descriptor, count));
            }
        }
        
        // Hide the button if there are no artifacts
        setVisible(!countedArtifacts.isEmpty());
        if (currResult==null) {
            reset();
        }
    }

    protected JMenuItem createLabelItem(String text) {
        JMenuItem menuItem = new JMenuItem(text);
        menuItem.setEnabled(false);
        return menuItem;
    }

    private JMenuItem createMenuItem(final ArtifactDescriptor descriptor, int count) {

        descriptors.add(descriptor);

        String resultName = descriptor.toString();
        if (showCounts && count > 0) resultName += " (" + count + " items)";
        JMenuItem menuItem = createMenuItem(resultName, descriptor.equals(currResult));
        menuItem.addActionListener(e -> {
            setResultDescriptor(descriptor);
            resultChanged(descriptor);
            ActivityLogHelper.logUserAction("ResultSelectionButton.resultChanged", descriptor.toString());
        });
        group.add(menuItem);
        return menuItem;
    }

    protected JMenuItem createMenuItem(String text, boolean selected) {
        return new JRadioButtonMenuItem(text, selected);
    }
    
    protected void resultChanged(ArtifactDescriptor resultDescriptor) {}

    public ArtifactDescriptor getResultDescriptor() {
        return currResult;
    }
}
