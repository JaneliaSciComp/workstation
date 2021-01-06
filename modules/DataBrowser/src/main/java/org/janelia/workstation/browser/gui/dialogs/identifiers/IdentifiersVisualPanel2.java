package org.janelia.workstation.browser.gui.dialogs.identifiers;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.gui.search.Filter;
import org.janelia.model.domain.gui.search.criteria.FacetCriteria;
import org.janelia.model.domain.sample.LSMImage;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.core.model.search.SearchConfiguration;
import org.janelia.workstation.core.model.search.SolrSearchResults;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class IdentifiersVisualPanel2 extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(IdentifiersVisualPanel2.class);

    // Controller
    private IdentifiersWizardPanel2 wizardPanel;

    // GUI
    private JTextArea textArea;
    private JPanel notFoundPanel;

    // State
    private boolean isSearching;
    private List<Reference> results;

    @Override
    public String getName() {
        return "Search Results";
    }

    /**
     * Creates new form DownloadVisualPanel1
     */
    public IdentifiersVisualPanel2(IdentifiersWizardPanel2 wizardPanel) {
        this.wizardPanel = wizardPanel;
        setLayout(new BorderLayout());

        this.textArea = new JTextArea();
        textArea.setEditable(false);
        
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(textArea);
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // Increase scroll speed

        notFoundPanel = new JPanel(new BorderLayout());
        notFoundPanel.add(new JLabel("The following identifiers could not be matched:"), BorderLayout.NORTH);
        notFoundPanel.add(scrollPane, BorderLayout.CENTER);
    }
    
    public void init(IdentifiersWizardState state) {

        removeAll();
        add(new JLabel(Icons.getLoadingIcon()), BorderLayout.CENTER);

        importIdentifiers(state.getSearchClass(), state.getText());

        triggerValidation();
    }

    private void importIdentifiers(Class<? extends DomainObject> searchClass, String text) {

        this.isSearching = true;

        List<String> lines = new ArrayList<>();
        for (String line : text.split("\\n")) {
            String trimmed = line.trim();
            if (!StringUtils.isBlank(trimmed)) {
                lines.add(trimmed);
            }
        }

        SimpleWorker worker = new SimpleWorker() {

            Set<Reference> refs = new LinkedHashSet<>();
            StringBuilder notFound = new StringBuilder();

            @Override
            protected void doStuff() throws Exception {
                int i=0;
                for (String line : lines) {
                    boolean found = false;
                    if (line.contains("#")) {
                        refs.add(Reference.createFor(line));
                        found = true;
                    }
                    else {
                        Set<Reference> lineResults = search(searchClass, line);
                        if (!lineResults.isEmpty()) {
                            refs.addAll(lineResults);
                            found = true;
                        }
                    }
                    if (!found) {
                        if (notFound.length()>0) notFound.append('\n');
                        notFound.append(line);
                    }
                    // TODO: display progress in the UI
                    setProgress(i++, lines.size());
                }
            }

            @Override
            protected void hadSuccess() {

                // Update state
                results = new ArrayList<>(refs);
                isSearching = false;
                triggerValidation();

                // Display results
                removeAll();

                JLabel results = new JLabel("Found "+refs.size()+" matches for "+lines.size()+" identifiers. Click the Finish button below to view them.");
                results.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

                if (notFound.length()>0) {
                    textArea.setText(notFound.toString());
                    JPanel outerPanel = new JPanel(new BorderLayout());
                    outerPanel.add(results, BorderLayout.NORTH);
                    outerPanel.add(notFoundPanel, BorderLayout.CENTER);
                    add(outerPanel, BorderLayout.CENTER);
                }
                else {
                    add(results, BorderLayout.NORTH);
                }

                updateUI();
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
            }
        };

        worker.execute();

    }

    private Set<Reference> search(Class<? extends DomainObject> searchClass, String searchString) throws Exception {

        Filter filter = createUnsavedFilter(searchClass, "Bulk Search");
        filter.setSearchString(searchString);
        SearchConfiguration config = new SearchConfiguration(filter, 100);
        config.setFetchAnnotations(false);
        SolrSearchResults searchResults = config.performSearch();

        Set<Reference> refs = new LinkedHashSet<>();
        // This only fetches a page of results. This seems reasonable if we're searching for identifiers.
        for (DomainObject object : searchResults.getPage(0).getObjects()) {
            refs.add(Reference.createFor(object));
        }

        log.debug("Found {} matching objects", refs.size());
        return refs;
    }

    // Copied from FilterEditorPanel... may want to consolidate this in the future
    public static Filter createUnsavedFilter(Class<?> searchClass, String name) {
        Filter filter = new Filter();
        filter.setSearchClass(searchClass.getName());
        filter.setName(name);
        if (Sample.class.equals(searchClass) || LSMImage.class.equals(searchClass)) {
            FacetCriteria facet = new FacetCriteria();
            facet.setAttributeName("sageSynced");
            facet.setValues(Sets.newHashSet("true"));
            filter.addCriteria(facet);
        }
        return filter;
    }

    public List<Reference> getResults() {
        return results;
    }

    public boolean isSearching() {
        return isSearching;
    }

    private void triggerValidation() {
        wizardPanel.fireChangeEvent();
    }
}
