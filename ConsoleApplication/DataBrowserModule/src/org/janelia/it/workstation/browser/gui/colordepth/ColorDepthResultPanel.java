package org.janelia.it.workstation.browser.gui.colordepth;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.events.selection.ChildSelectionModel;
import org.janelia.it.workstation.browser.gui.listview.ListViewerType;
import org.janelia.it.workstation.browser.gui.listview.PaginatedResultsPanel;
import org.janelia.it.workstation.browser.gui.support.SearchProvider;
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
    private final PaginatedResultsPanel<ColorDepthMatch, String> resultPanel;

    // State
    private ColorDepthMask mask;
    /** relevant results for the currently selected mask */
    private List<ColorDepthResult> results = new ArrayList<>();
    private ColorDepthResult selectedResult;
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
        resultPanel = new PaginatedResultsPanel<ColorDepthMatch,String>(selectionModel, this, viewerTypes) {
    
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
        add(resultPanel, BorderLayout.CENTER);
    }
    
    public void loadSearchResults(List<ColorDepthResult> resultList, ColorDepthMask mask, boolean isUserDriven) {

        log.info("loadSearchResults(resultList.size={}, mask={}, isUserDriven={})", resultList.size(), mask.getFilepath(), isUserDriven);
        this.mask = mask;
        selectionModel.setParentObject(mask);
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
                if (!results.isEmpty()) {
                    ColorDepthResult latestResult = results.get(results.size()-1);
                    showSearchResult(latestResult, isUserDriven);
                }
            }

            @Override
            protected void hadError(Throwable error) {
                resultPanel.showNothing();
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
    }
    
    public void showSearchResult(ColorDepthResult result, boolean isUserDriven) {
        log.info("showSearchResult({}, isUserDriven={})", result.getId(), isUserDriven);
        this.selectedResult = result;
        List<ColorDepthMatch> maskMatches = selectedResult.getMaskMatches(mask);
        ColorDepthSearchResults searchResults = new ColorDepthSearchResults(maskMatches);
        resultPanel.showSearchResults(searchResults, isUserDriven, null);
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

}
