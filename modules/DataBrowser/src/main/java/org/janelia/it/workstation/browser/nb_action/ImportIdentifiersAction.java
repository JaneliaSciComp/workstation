package org.janelia.it.workstation.browser.nb_action;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ProgressMonitor;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.shared.solr.SolrJsonResults;
import org.janelia.it.jacs.shared.solr.SolrParams;
import org.janelia.it.jacs.shared.solr.SolrQueryBuilder;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.components.DomainListViewManager;
import org.janelia.it.workstation.browser.components.DomainListViewTopComponent;
import org.janelia.it.workstation.browser.components.ViewerUtils;
import org.janelia.it.workstation.browser.gui.dialogs.identifiers.IdentifiersWizardIterator;
import org.janelia.it.workstation.browser.gui.dialogs.identifiers.IdentifiersWizardState;
import org.janelia.it.workstation.browser.model.search.SearchConfiguration;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.sample.Sample;
import org.janelia.model.domain.support.SearchType;
import org.janelia.model.domain.workspace.TreeNode;
import org.openide.DialogDisplayer;
import org.openide.WizardDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.CallableSystemAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * Action which brings up the Import Identifiers wizard 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "Services",
        id = "org.janelia.it.workstation.browser.nb_action.ImportIdentifiersAction"
)
@ActionRegistration(
        displayName = "#CTL_ImportIdentifiersAction"
)
@ActionReferences({
    @ActionReference(path = "Menu/Services", position = 20),
    @ActionReference(path = "Shortcuts", name = "A-U")
})
@Messages("CTL_ImportIdentifiersAction=Batch Search")
public final class ImportIdentifiersAction extends CallableSystemAction {

    private static final Logger log = LoggerFactory.getLogger(ImportIdentifiersAction.class);
    
    @Override
    public String getName() {
        return "Batch Search";
    }

    @Override
    protected String iconResource() {
        return "images/search-white-icon.png";
    }
    
    @Override
    public HelpCtx getHelpCtx() {
        return null;
    }

    @Override
    protected boolean asynchronous() {
        return false;
    }
    
    @Override
    public void performAction() {

        // Hide the default wizard image, which does not look good on our dark background
        UIDefaults uiDefaults = UIManager.getDefaults();
        uiDefaults.put("nb.wizard.hideimage", Boolean.TRUE); 
        
        // Create wizard
        IdentifiersWizardIterator iterator = new IdentifiersWizardIterator();
        WizardDescriptor wiz = new WizardDescriptor(iterator);
        iterator.initialize(wiz); 

        // {0} will be replaced by WizardDescriptor.Panel.getComponent().getName()
        // {1} will be replaced by WizardDescriptor.Iterator.name()
        wiz.setTitleFormat(new MessageFormat("{0} ({1})"));
        wiz.setTitle(getName());

        // Install the state
        wiz.putProperty(IdentifiersWizardIterator.PROP_WIZARD_STATE, new IdentifiersWizardState());
        
        // Show the wizard
        if (DialogDisplayer.getDefault().notify(wiz) == WizardDescriptor.FINISH_OPTION) {
            // Start import 
            IdentifiersWizardState endState = (IdentifiersWizardState) wiz.getProperty(IdentifiersWizardIterator.PROP_WIZARD_STATE);
            importIdentifiers(endState.getText());
        }
    }
    
    private void importIdentifiers(String text) {

        List<String> lines = new ArrayList<>();
        for (String line : text.split("\\n")) {
            lines.add(line.trim());
        }

        SimpleWorker worker = new SimpleWorker() {
            
            List<Reference> refs = new ArrayList<>();
            
            @Override
            protected void doStuff() throws Exception {
                int i=0;
                for (String line : lines) {
                    if (line.contains("#")) {
                        refs.add(Reference.createFor(line));
                    }
                    else {
                        refs.addAll(search(line));
                    }
                    setProgress(i++, lines.size());
                }
            }

            @Override
            protected void hadSuccess() {
                
                TreeNode node = new TreeNode();
                node.setId(1L);
                node.setName("Bulk Search Results");
                node.setChildren(refs);

                DomainListViewTopComponent targetViewer = ViewerUtils.provisionViewer(DomainListViewManager.getInstance(), "editor");
                if (targetViewer!=null) {
                    log.info("Loading into "+targetViewer);
                    // If we are reacting to a selection event in another viewer, then this load is not user driven.
                    targetViewer.loadDomainObject(node, true);
                }
                
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }
        };

        worker.setProgressMonitor(new ProgressMonitor(FrameworkImplProvider.getMainFrame(), "Searching for identifiers...", "", 0, 100));
        worker.execute();
        
        
    }
    
    private List<Reference> search(String searchString) throws Exception {

        SolrQueryBuilder builder = new SolrQueryBuilder();

        for (String subjectKey : AccessManager.getReaderSet()) {
            log.trace("Adding query owner key: {}",subjectKey);
            builder.addOwnerKey(subjectKey);
        }

        builder.setSearchString(searchString);

        final Map<String, Set<String>> filters = new HashMap<>();
        SearchType searchTypeAnnot = Sample.class.getAnnotation(SearchType.class);
        String searchType = searchTypeAnnot.key();
        filters.put(SearchConfiguration.SOLR_TYPE_FIELD,Sets.newHashSet(searchType));
        
        SolrQuery query = builder.getQuery();
        DomainModel model = DomainMgr.getDomainMgr().getModel();
        SolrParams queryParams = SolrQueryBuilder.serializeSolrQuery(query);
        SolrJsonResults results = model.search(queryParams);

        List<Reference> refs = new ArrayList<>();

        if (results != null) {
            for (SolrDocument doc : results.getResults()) {
                Long id = new Long(doc.get("id").toString());
                String type = (String) doc.getFieldValue(SearchConfiguration.SOLR_TYPE_FIELD);
                String className = DomainUtils.getClassNameForSearchType(type);
                if (className != null) {
                    refs.add(Reference.createFor(className, id));
                } else {
                    log.warn("Unrecognized type has no collection mapping: " + type);
                }
            }
        }
        
        log.info("Found {} matching objects", refs.size());
        return refs;
    }
}
