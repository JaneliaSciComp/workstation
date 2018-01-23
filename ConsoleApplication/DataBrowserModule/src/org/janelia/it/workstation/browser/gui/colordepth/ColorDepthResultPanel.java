package org.janelia.it.workstation.browser.gui.colordepth;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.events.selection.ChildSelectionModel;
import org.janelia.it.workstation.browser.gui.listview.ListViewerType;
import org.janelia.it.workstation.browser.gui.listview.PaginatedResultsPanel;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.gui.support.SearchProvider;
import org.janelia.it.workstation.browser.gui.support.WrapLayout;
import org.janelia.it.workstation.browser.model.DomainModelViewUtils;
import org.janelia.it.workstation.browser.model.search.ResultPage;
import org.janelia.it.workstation.browser.model.search.SearchResults;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.domain.DomainConstants;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.gui.colordepth.ColorDepthMask;
import org.janelia.model.domain.gui.colordepth.ColorDepthMatch;
import org.janelia.model.domain.gui.colordepth.ColorDepthResult;
import org.janelia.model.domain.sample.Sample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

public class ColorDepthResultPanel extends JPanel implements SearchProvider {

    private final static Logger log = LoggerFactory.getLogger(ColorDepthResultPanel.class);

    // Constants
    private static final String PREFERENCE_KEY = "ColorDepthResultPanel";
    private static final List<ListViewerType> viewerTypes = ImmutableList.of(ListViewerType.ColorDepthResultViewer);
    
    // UI Components
    private JPanel topPanel;
    private JButton prevResultButton;
    private JLabel resultLabel;
    private JButton nextResultButton;
    private JCheckBox newOnlyCheckbox;
    private final PaginatedResultsPanel<ColorDepthMatch, String> resultsPanel;

    // State
    private ColorDepthMask mask;
    /** relevant results for the currently selected mask */
    private List<ColorDepthResult> results = new ArrayList<>();
    private int currResultIndex;
    private Map<Reference, Sample> sampleMap = new HashMap<>();
    private Map<String, ColorDepthMatch> matchMap = new HashMap<>();
    private String sortCriteria;
    
    private final ChildSelectionModel<ColorDepthMatch,String> selectionModel = new ChildSelectionModel<ColorDepthMatch,String>() {

        @Override
        protected void selectionChanged(List<ColorDepthMatch> objects, boolean select, boolean clearAll, boolean isUserDriven) {
        }

        @Override
        public String getId(ColorDepthMatch match) {
            return match.getFilepath();
        }
    };
    
    public ColorDepthResultPanel() {

        this.prevResultButton = new JButton(Icons.getIcon("resultset_previous.png"));
        prevResultButton.setToolTipText("View the previous search result");
        prevResultButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                goPrevResult();
            }
        });

        this.resultLabel = new JLabel("");
        
        this.nextResultButton = new JButton(Icons.getIcon("resultset_next.png"));
        nextResultButton.setToolTipText("View the next search result");
        nextResultButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                goNextResult();
            }
        });
        
        this.newOnlyCheckbox = new JCheckBox("Only new results");
        newOnlyCheckbox.addActionListener((ActionEvent e) -> {
                showCurrSearchResult(true);
            }
        );
        
        this.topPanel = new JPanel(new WrapLayout(false, WrapLayout.LEFT, 8, 5));
        topPanel.add(new JLabel("Results:"));
        topPanel.add(prevResultButton);
        topPanel.add(resultLabel);
        topPanel.add(nextResultButton);
        topPanel.add(new JSeparator(SwingConstants.VERTICAL));
        topPanel.add(newOnlyCheckbox);
        topPanel.add(new JSeparator(SwingConstants.VERTICAL));
        
        this.resultsPanel = new PaginatedResultsPanel<ColorDepthMatch,String>(selectionModel, this, viewerTypes) {
    
            @Override
            protected ResultPage<ColorDepthMatch, String> getPage(SearchResults<ColorDepthMatch, String> searchResults, int page) throws Exception {
                return searchResults.getPage(page);
            }
            @Override
            public String getId(ColorDepthMatch object) {
                return object.getFilepath();
            }
        };
        
        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(resultsPanel, BorderLayout.CENTER);
    }
    
    public void loadSearchResults(List<ColorDepthResult> resultList, ColorDepthMask mask, boolean isUserDriven) {

        log.info("loadSearchResults(resultList.size={}, mask={}, isUserDriven={})", resultList.size(), mask.getFilepath(), isUserDriven);
        this.mask = mask;
        sampleMap.clear();
        matchMap.clear();

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                loadPreferences();
                prepareResults(resultList);
            }

            @Override
            protected void hadSuccess() {
                showResults(isUserDriven);
            }

            @Override
            protected void hadError(Throwable error) {
                resultsPanel.showNothing();
                ConsoleApp.handleException(error);
            }
        };

        worker.execute();    
    }
    
    private void prepareResults(List<ColorDepthResult> resultList) {

        log.info("Preparing matching results from {} results", resultList.size());
        
        results.clear();
        for(ColorDepthResult result : resultList) {
            List<ColorDepthMatch> matches = result.getMaskMatches(mask);
            if (matches!=null && !matches.isEmpty()) {
                results.add(result);
            }
        }
        
        log.info("Found {} results for {}", results.size(), mask);
    }
    
    private void showResults(boolean isUserDriven) {
        if (!results.isEmpty()) {
            currResultIndex = results.size()-1;
            showCurrSearchResult(isUserDriven);
        }
        else {
            log.info("No results for mask");
            resultsPanel.showNothing();
        }
    }
    
    public void showCurrSearchResult(boolean isUserDriven) {

        log.debug("showCurrSearchResult(isUserDriven={})",isUserDriven);

        updatePagingStatus();
        
        if (currResultIndex < 0 || currResultIndex >= results.size()) {
            throw new IllegalStateException("Cannot show search result index outside of result list size");
        }
        
        ColorDepthResult result = results.get(currResultIndex);
        
        selectionModel.setParentObject(result);
        resultLabel.setText(DomainModelViewUtils.getDateString(result.getCreationDate()));
        
        List<ColorDepthMatch> maskMatches = result.getMaskMatches(mask);
        
        if (newOnlyCheckbox.isSelected()) {
            
            Set<String> filepaths = new HashSet<>();
            
            // First determine what was a match in previous results
            for (int i=0; i<currResultIndex; i++) {
                for(ColorDepthMatch match : results.get(i).getMaskMatches(mask)) {
                    filepaths.add(match.getFilepath());
                }
            }
            
            // Now filter the current results to show the new matches only
            List<ColorDepthMatch> filteredMatches = new ArrayList<>();
            for(ColorDepthMatch match : maskMatches) {
                if (!filepaths.contains(match.getFilepath())) {
                    filteredMatches.add(match);
                }
            }
            
            maskMatches = filteredMatches;
        }
        
        ColorDepthSearchResults searchResults = new ColorDepthSearchResults(maskMatches);
        resultsPanel.showSearchResults(searchResults, isUserDriven, null);
    }

    private synchronized void goPrevResult() {
        this.currResultIndex -= 1;
        if (currResultIndex < 0) {
            currResultIndex = 0;
        }
        showCurrSearchResult(true);
    }

    private synchronized void goNextResult() {
        this.currResultIndex += 1;
        if (currResultIndex >= results.size()) {
            currResultIndex = results.size()-1;
        }
        showCurrSearchResult(true);
    }

    protected void updatePagingStatus() {
        int numResults = results.size();
        prevResultButton.setEnabled(numResults>0 && currResultIndex > 0);
        nextResultButton.setEnabled(numResults>0 && currResultIndex < numResults - 1);
    }
    
    @Override
    public String getSortField() {
        return sortCriteria;
    }

    @Override
    public void setSortField(final String sortCriteria) {
        this.sortCriteria = sortCriteria;
        savePreferences();
    }
    
    @Override
    public void search() {

//        SimpleWorker worker = new SimpleWorker() {
//
//            @Override
//            protected void doStuff() throws Exception {
//                loadPreferences();
//                prepareLsmResults();
//            }
//
//            @Override
//            protected void hadSuccess() {
//                showResults(true);
//            }
//
//            @Override
//            protected void hadError(Throwable error) {
//                showNothing();
//                ConsoleApp.handleException(error);
//            }
//        };
//
//        worker.execute();
    }
    
    @Override
    public void export() {
//        DomainObjectTableViewer viewer = null;
//        if (lsmPanel.getViewer() instanceof DomainObjectTableViewer) {
//            viewer = (DomainObjectTableViewer)lsmPanel.getViewer();
//        }
//        ExportResultsAction<DomainObject> action = new ExportResultsAction<>(lsmSearchResults, viewer);
//        action.actionPerformed(null);
    }

    private void loadPreferences() {
//        if (search.getId()==null) return;
        try {
            sortCriteria = FrameworkImplProvider.getRemotePreferenceValue(DomainConstants.PREFERENCE_CATEGORY_SORT_CRITERIA, PREFERENCE_KEY, null);
        }
        catch (Exception e) {
            log.error("Could not load sort criteria",e);
        }
    }

    private void savePreferences() {
        if (StringUtils.isEmpty(sortCriteria)) return;
        try {
            FrameworkImplProvider.setRemotePreferenceValue(DomainConstants.PREFERENCE_CATEGORY_SORT_CRITERIA, PREFERENCE_KEY, sortCriteria);
        }
        catch (Exception e) {
            log.error("Could not save sort criteria",e);
        }
    }

    public ChildSelectionModel<ColorDepthMatch, String> getSelectionModel() {
        return selectionModel;
    }

    void reset() {
        selectionModel.reset();
        this.currResultIndex = results.size() - 1;
    }

    int getCurrResultIndex() {
        return currResultIndex;
    }

    public PaginatedResultsPanel<ColorDepthMatch, String> getResultPanel() {
        return resultsPanel;
    }

}
