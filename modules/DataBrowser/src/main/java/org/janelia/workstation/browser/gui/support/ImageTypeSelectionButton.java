package org.janelia.workstation.browser.gui.support;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

import org.apache.commons.lang3.StringUtils;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.common.gui.support.buttons.DropDownButton;
import org.janelia.workstation.core.model.descriptors.ArtifactDescriptor;
import org.janelia.workstation.core.model.descriptors.DescriptorUtils;
import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.enums.FileType;
import org.janelia.model.domain.interfaces.HasFileGroups;
import org.janelia.model.domain.interfaces.HasFiles;
import org.janelia.model.domain.sample.NeuronSeparation;
import org.janelia.model.domain.sample.PipelineResult;
import org.janelia.model.domain.sample.Sample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;


/**
 * Drop-down button for selecting the image type to display. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ImageTypeSelectionButton extends DropDownButton {

    private static final Logger log = LoggerFactory.getLogger(ImageTypeSelectionButton.class);

    private static final int MAX_TITLE_LENGTH = 30;
    private static final FileType DEFAULT_TYPE = FileType.FirstAvailable2d;

    private ArtifactDescriptor currResult;
    private FileType currImageType;
    private boolean only2d;
    private boolean showTitle;
    
    public ImageTypeSelectionButton() {
        this(true, true);
    }
    
    public ImageTypeSelectionButton(boolean showTitle, boolean only2d) {
        this.only2d = only2d;
    	this.showTitle = showTitle;
        setIcon(Icons.getIcon("image.png"));
        setToolTipText("Select the result type to display");
        reset();
    }

    public void reset() {
        setImageType(DEFAULT_TYPE);
    }

    public void setResultDescriptor(ArtifactDescriptor currResult) {
        this.currResult = currResult;
    }

    public String getImageTypeName() {
        return currImageType.name();
    }

    public void setImageTypeName(String imageType) {
        setImageType(FileType.valueOf(imageType));
    }

    public void setImageType(FileType imageType) {
        this.currImageType = imageType == null ? DEFAULT_TYPE : imageType;
        if (showTitle) {
            String title = StringUtils.abbreviate(currImageType.getLabel(), MAX_TITLE_LENGTH);
            setText(title);
        }
    }

    public void populate(DomainObject domainObject) {
        populate(Arrays.asList(domainObject));
    }
    
    public synchronized void populate(Collection<? extends Object> sourceList) {
        
        if (currResult == null) {
            this.currResult = ArtifactDescriptor.LATEST;
        }

        Multiset<String> countedTypeNames = LinkedHashMultiset.create();
            
        for(Object source : sourceList) {
            if (source instanceof Sample) {
                Sample sample = (Sample)source;
                log.trace("Source is sample: {}",sample.getId());
                HasFiles result = DescriptorUtils.getResult(sample, currResult);
                if (result!=null) {
                    source = result;
                }
            }
            if (source instanceof HasFileGroups) {
                Multiset<String> typeNames = DomainUtils.getTypeNames((HasFileGroups)source, only2d);
                log.trace("Source has file groups: {}",typeNames);
                countedTypeNames.addAll(typeNames);
            }
            if (source instanceof HasFiles) {
                Multiset<String> typeNames = DomainUtils.getTypeNames((HasFiles) source, only2d);
                log.trace("Source has files: {}",typeNames);
                countedTypeNames.addAll(typeNames);
            }
            log.trace("Source is: {}",source);
            if (source instanceof PipelineResult) {
                PipelineResult result = (PipelineResult)source;
                NeuronSeparation separation = result.getLatestSeparationResult();
                log.trace("Source has separation: {}",separation);
                if (separation!=null) {
                    if (!only2d) {
                        Set<String> typeNames = new HashSet<>();
                        typeNames.add(FileType.NeuronAnnotatorLabel.toString());
                        typeNames.add(FileType.NeuronAnnotatorSignal.toString());
                        typeNames.add(FileType.NeuronAnnotatorReference.toString());
                        log.trace("Adding type names: {}",typeNames);
                        countedTypeNames.addAll(typeNames);
                    }
                }
            }
        }
        
        setVisible(!countedTypeNames.isEmpty());
        removeAll();
        
        ButtonGroup group = new ButtonGroup();
        boolean oneSelected = false;
        
        log.trace("{} domain objects have {} type names",sourceList.size(),countedTypeNames.elementSet().size());
        for(final FileType fileType : FileType.values()) {
            String typeName = fileType.name();
            log.trace("Type {} has count={}",typeName,countedTypeNames.count(typeName));
            int count = countedTypeNames.count(typeName);
            if (count>0 || (only2d && fileType.equals(FileType.FirstAvailable2d)) || (!only2d && fileType.equals(FileType.FirstAvailable3d))) {
                String typeLabel = fileType.getLabel();
                if (count>0) typeLabel += " ("+count+" items)";
                boolean selected = fileType.equals(currImageType);
                if (selected) oneSelected = true;
                JMenuItem menuItem = new JRadioButtonMenuItem(typeLabel, selected);
                menuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        setImageType(fileType);
                        imageTypeChanged(fileType);
                        ActivityLogHelper.logUserAction("ImageTypeSelectionButton.imageTypeChanged", fileType.getLabel());
                    }
                });
                addMenuItem(menuItem);
                group.add(menuItem);
            }
        }
        
        if (!oneSelected) {
            // Last user selection was not found, so reset to default
            reset();
        }
    }
    
    protected void imageTypeChanged(FileType fileType) {}

}
