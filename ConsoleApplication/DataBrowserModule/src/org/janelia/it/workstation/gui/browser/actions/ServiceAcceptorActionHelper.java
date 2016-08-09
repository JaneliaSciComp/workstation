package org.janelia.it.workstation.gui.browser.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.workstation.gui.browser.nb_action.DomainObjectAcceptor;
import org.janelia.it.workstation.nb_action.ServiceAcceptorHelper;

/**
 * Helper for creating actions based on the service acceptor framework.
 *
 * @author fosterl
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ServiceAcceptorActionHelper {

    /** Makes the item for showing the object in its own viewer iff the object type is correct. */
    public static Collection<NamedAction> getOpenForContextActions(final DomainObject domainObject) {

        final ServiceAcceptorHelper helper = new ServiceAcceptorHelper();
        Collection<DomainObjectAcceptor> domainObjectAcceptors = helper.findHandler(
                domainObject,
                DomainObjectAcceptor.class,
                DomainObjectAcceptor.DOMAIN_OBJECT_LOOKUP_PATH
        );

        List<NamedAction> actionList = new ArrayList<>();
        for (final DomainObjectAcceptor domainObjectAcceptor: domainObjectAcceptors ) {
            NamedAction action = new NamedAction() {
                @Override
                public String getName() {
                    return domainObjectAcceptor.getActionLabel();
                }

                @Override
                public void doAction() {
                    domainObjectAcceptor.acceptDomainObject(domainObject);
                }
            };
            actionList.add(action);
        }

        // Alphabetical order
        Collections.sort(actionList, new Comparator<NamedAction>() {
            @Override
            public int compare(NamedAction o1, NamedAction o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        return actionList;
    }
}
