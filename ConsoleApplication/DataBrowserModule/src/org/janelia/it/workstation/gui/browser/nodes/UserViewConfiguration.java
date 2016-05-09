package org.janelia.it.workstation.gui.browser.nodes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.janelia.it.jacs.model.domain.DomainObject;

/**
 * Configuration which determines what types of domain objects the user view should display.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class UserViewConfiguration {

    private final Set<Class<? extends DomainObject>> visibleClasses = new HashSet<>();
    
    private UserViewConfiguration() {
    }
    
    public Set<Class<? extends DomainObject>> getVisibleClasses() {
        return visibleClasses;
    }

    @SafeVarargs
    public static UserViewConfiguration create(Class<? extends DomainObject>... visible) {
        UserViewConfiguration config = new UserViewConfiguration();
        config.getVisibleClasses().addAll(Arrays.asList(visible));
        return config;
    }
}
