package org.janelia.workstation.browser.api.services;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.janelia.model.domain.Reference;
import org.janelia.workstation.browser.api.state.DataBrowserMgr;
import org.janelia.workstation.browser.gui.components.DomainExplorerTopComponent;
import org.janelia.workstation.integration.api.BrowsingController;
import org.openide.util.lookup.ServiceProvider;

/**
 * Implements the browser history.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = BrowsingController.class, position=100)
public class ConsoleBrowsingController implements BrowsingController {

    @Override
    public void refreshExplorer() {
        refreshExplorer(null);
    }

    @Override
    public void refreshExplorer(final Callable<Void> success) {
        DomainExplorerTopComponent.getInstance().refresh(true, true, success);
    }

    /**
     * Retrieve the history of recently opened domain objects.
     * @return list of references to domain objects
     */
    public List<Reference> getRecentlyOpenedHistory() {
        List<String> strRefs =  DataBrowserMgr.getDataBrowserMgr().getRecentlyOpenedHistory();
        List<Reference> refs = new ArrayList<>();
        for(String strRef : strRefs) {
            refs.add(Reference.createFor(strRef));
        }
        return refs;
    }

    /**
     * Update the history to add the given object reference to the top of the stack.
     * @param ref reference to domain object
     */
    public void updateRecentlyOpenedHistory(Reference ref) {
        if (ref==null) return;
        DataBrowserMgr.getDataBrowserMgr().updateRecentlyOpenedHistory(ref.toString());
    }
}
