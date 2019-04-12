package org.janelia.it.workstation.browser.gui.find;

import java.awt.Component;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

import org.janelia.workstation.common.gui.util.UIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * You can install this hierarchy listener on a FindContext, so that it properly registers itself with its top component.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FindContextRegistration implements HierarchyListener {

    private final static Logger log = LoggerFactory.getLogger(FindContextRegistration.class);
    
    private FindContext context;
    private Component component;
    
    public FindContextRegistration(FindContext context, Component component) {
        this.context = context;
        this.component = component;
    }
    
    @Override
    public void hierarchyChanged(HierarchyEvent e) {
        if ((e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) == HierarchyEvent.PARENT_CHANGED) {
            log.trace("Changed parents: {}",component.getClass().getName());
            FindContextActivator activator = UIUtils.getAncestorWithType(component, FindContextActivator.class);
            if (activator!=null) {
                activator.setFindContext(context);
            }
        }
    }
}
