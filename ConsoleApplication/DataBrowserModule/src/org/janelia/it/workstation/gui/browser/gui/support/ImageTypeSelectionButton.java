package org.janelia.it.workstation.gui.browser.gui.support;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.ButtonGroup;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFileGroups;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.support.ResultDescriptor;
import org.janelia.it.jacs.model.domain.support.SampleUtils;
import org.janelia.it.workstation.gui.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.gui.util.Icons;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Drop-down button for selecting the image type to display. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ImageTypeSelectionButton extends DropDownButton {

    private static final Logger log = LoggerFactory.getLogger(ImageTypeSelectionButton.class);

    private FileType DEFAULT_TYPE = FileType.FirstAvailable2d;

    private ResultDescriptor currResult;
    private FileType currImageType;
    private boolean only2d;
    private boolean showTitle;

    public ImageTypeSelectionButton() {
        this(false, true);
    }
    
    public ImageTypeSelectionButton(boolean showTitle, boolean only2d) {
        this.only2d = only2d;
    	this.showTitle = showTitle;
        setIcon(Icons.getIcon("image.png"));
        setToolTipText("Select the result type to display");
        setImageType(DEFAULT_TYPE);
    }

    public void setResultDescriptor(ResultDescriptor currResult) {
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
            setText(currImageType.getLabel());
        }
    }

    public void populate(DomainObject domainObject) {
        populate(Arrays.asList(domainObject));
    }
    
    public void populate(Collection<? extends Object> sourceList) {
        
        if (currResult == null) {
            this.currResult = ResultDescriptor.LATEST;
        }

        Multiset<String> countedTypeNames = LinkedHashMultiset.create();
            
        for(Object source : sourceList) {
            if (source instanceof Sample) {
                Sample sample = (Sample)source;
                log.debug("Source is sample: {}",sample.getId());
                HasFiles result = SampleUtils.getResult(sample, currResult);
                if (result!=null) {
                    source = result;
                }
            }
            if (source instanceof HasFileGroups) {
                Multiset<String> typeNames = DomainUtils.getTypeNames((HasFileGroups)source, only2d);
                log.debug("Source has file groups: {}",typeNames);
                countedTypeNames.addAll(typeNames);
            }
            if (source instanceof HasFiles) {
                Multiset<String> typeNames = DomainUtils.getTypeNames((HasFiles) source, only2d);
                log.debug("Source has files: {}",typeNames);
                countedTypeNames.addAll(typeNames);
            }
        }
        
        setVisible(!countedTypeNames.isEmpty());
        getPopupMenu().removeAll();
        
        ButtonGroup group = new ButtonGroup();
        
        log.debug("{} domain objects have {} type names",sourceList.size(),countedTypeNames.elementSet().size());
        for(final FileType fileType : FileType.values()) {
            String typeName = fileType.name();
            log.debug("Type {} has count={}",typeName,countedTypeNames.count(typeName));
            int count = countedTypeNames.count(typeName);
            if (count>0 || (only2d && fileType.equals(FileType.FirstAvailable2d)) || (!only2d && fileType.equals(FileType.FirstAvailable3d))) {
                String typeLabel = fileType.getLabel();
                if (count>0) typeLabel += " ("+count+" items)";
                JMenuItem menuItem = new JRadioButtonMenuItem(typeLabel, fileType.equals(currImageType));
                menuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        setImageType(fileType);
                        imageTypeChanged(fileType);
                        ActivityLogHelper.logUserAction("ImageTypeSelectionButton.imageTypeChanged", fileType.getLabel());
                    }
                });
                getPopupMenu().add(menuItem);
                group.add(menuItem);
            }
        }
    }
    
    protected void imageTypeChanged(FileType fileType) {}

}
