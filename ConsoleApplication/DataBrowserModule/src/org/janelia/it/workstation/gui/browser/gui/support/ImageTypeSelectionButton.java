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
    
    private ResultDescriptor currResult;
    private String currImageType;
    private boolean showTitle;

    public ImageTypeSelectionButton() {
        this(false);
    }
    
    public ImageTypeSelectionButton(boolean showTitle) {
    	this.showTitle = showTitle;
        setIcon(Icons.getIcon("page.png"));
        setToolTipText("Select the result type to display");
    }

    public void setResultDescriptor(ResultDescriptor currResult) {
        this.currResult = currResult;
    }
    
    public void setImageType(String currImageType) {
        this.currImageType = currImageType;
        if (showTitle) {
            setText(currImageType);
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
                Multiset<String> typeNames = DomainUtils.get2dTypeNames((HasFileGroups) source);
                log.debug("Source has file groups: {}",typeNames);
                countedTypeNames.addAll(typeNames);
            }
            if (source instanceof HasFiles) {
                Multiset<String> typeNames = DomainUtils.get2dTypeNames((HasFiles) source);
                log.debug("Source has files: {}",typeNames);
                countedTypeNames.addAll(typeNames);
            }
        }
        
        setVisible(!countedTypeNames.isEmpty());
        getPopupMenu().removeAll();
        
        ButtonGroup group = new ButtonGroup();
        
        log.debug("{} domain objects have {} type names",sourceList.size(),countedTypeNames.elementSet().size());
        for(FileType fileType : FileType.values()) {
            final String typeName = fileType.name();
            log.trace("Type {} has count={}",typeName,countedTypeNames.count(typeName));
            if (countedTypeNames.count(typeName)>0) {
                if (currImageType == null || !countedTypeNames.contains(currImageType)) {
                    this.currImageType = typeName;
                }
                JMenuItem menuItem = new JRadioButtonMenuItem(fileType.getLabel(), typeName.equals(currImageType));
                menuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        currImageType = typeName;
                        imageTypeChanged(currImageType);
                    }
                });
                getPopupMenu().add(menuItem);
                group.add(menuItem);
            }
        }
        
        // Default type
        if (currImageType == null) {
            this.currImageType = FileType.SignalMip.name();
            // If there are no signal MIPs, try something else
            if (countedTypeNames.count(currImageType)==0) {
                log.info("No signal MIPs, looking for another default");
                for (FileType fileType : FileType.values()) {
                    if (countedTypeNames.count(fileType)>0) {
                        this.currImageType = fileType.name();
                        log.info("Choosing default image type: {}",currImageType);
                        break;
                    }
                }
            }

        }
    }
    
    protected void imageTypeChanged(String typeName) {}
    
    public String getImageType() {
        return currImageType;
    }

}
