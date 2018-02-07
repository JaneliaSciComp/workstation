package org.janelia.it.workstation.browser.gui.colordepth;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.events.Events;
import org.janelia.it.workstation.browser.events.selection.ChildSelectionModel;
import org.janelia.it.workstation.browser.gui.editor.SingleSelectionButton;
import org.janelia.it.workstation.browser.gui.listview.ListViewerType;
import org.janelia.it.workstation.browser.gui.listview.PaginatedResultsPanel;
import org.janelia.it.workstation.browser.gui.support.PreferenceSupport;
import org.janelia.it.workstation.browser.gui.support.SearchProvider;
import org.janelia.it.workstation.browser.gui.support.WrapLayout;
import org.janelia.it.workstation.browser.model.DomainModelViewUtils;
import org.janelia.it.workstation.browser.model.search.ResultPage;
import org.janelia.it.workstation.browser.model.search.SearchResults;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.gui.colordepth.ColorDepthMask;
import org.janelia.model.domain.gui.colordepth.ColorDepthMatch;
import org.janelia.model.domain.gui.colordepth.ColorDepthResult;
import org.janelia.model.domain.gui.colordepth.ColorDepthSearch;
import org.janelia.model.domain.sample.DataSet;
import org.janelia.model.domain.sample.Sample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

public class ColorDepthResultPanel extends JPanel implements SearchProvider, PreferenceSupport {

    private final static Logger log = LoggerFactory.getLogger(ColorDepthResultPanel.class);

    // Constants
    private static final List<ListViewerType> viewerTypes = ImmutableList.of(ListViewerType.ColorDepthResultViewer);
    private static final String PREFERENCE_CATEGORY_CDS_RESULTS_PER_LINE = "CDSResultPerLine";
    private static final String PREFERENCE_CATEGORY_CDS_NEW_RESULTS = "CDSOnlyNewResults";
    private static final int DEFAULT_RESULTS_PER_LINE = 2;
    
    // UI Components
    private JPanel topPanel;
    private SingleSelectionButton<ColorDepthResult> historyButton;
    private JCheckBox newOnlyCheckbox;
    private JTextField resultsPerLineField;
    private final PaginatedResultsPanel<ColorDepthMatch, String> resultsPanel;

    // State
    private ColorDepthSearch search;
    private ColorDepthMask mask;
    /** relevant results for the currently selected mask */
    private List<ColorDepthResult> results = new ArrayList<>();
    private ColorDepthResult currResult;
    private Map<Reference, Sample> sampleMap = new HashMap<>();
    private Map<String, ColorDepthMatch> matchMap = new HashMap<>();
    private String sortCriteria;
    
    private final ChildSelectionModel<ColorDepthMatch,String> selectionModel = new ChildSelectionModel<ColorDepthMatch,String>() {

        @Override
        protected void selectionChanged(List<ColorDepthMatch> objects, boolean select, boolean clearAll, boolean isUserDriven) {
            Events.getInstance().postOnEventBus(new ColorDepthMatchSelectionEvent(getSource(), objects, select, clearAll, isUserDriven));
        }

        @Override
        public String getId(ColorDepthMatch match) {
            return match.getFilepath();
        }
    };
    
    public ColorDepthResultPanel() {

        historyButton = new SingleSelectionButton<ColorDepthResult>("Search Results") {

            @Override
            public Collection<ColorDepthResult> getValues() {
                return results;
            }

            @Override
            public ColorDepthResult getSelectedValue() {
                return currResult;
            }
            
            @Override
            public String getLabel(ColorDepthResult result) {
                return DomainModelViewUtils.getDateString(result.getCreationDate());
            }
            
            @Override
            protected void updateSelection(ColorDepthResult result) {
                currResult = result;
                showCurrSearchResult(true);
            }            
        };
          
        this.newOnlyCheckbox = new JCheckBox("Only new results");
        newOnlyCheckbox.addActionListener((ActionEvent e) -> {
                setPreferenceAsync(PREFERENCE_CATEGORY_CDS_NEW_RESULTS, newOnlyCheckbox.isSelected()+"");
            }
        );
        
        this.resultsPerLineField = new JTextField(3);
        resultsPerLineField.setHorizontalAlignment(JTextField.RIGHT);
        resultsPerLineField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                try {
                    Integer resultPerLine = new Integer(resultsPerLineField.getText());
                    setPreferenceAsync(PREFERENCE_CATEGORY_CDS_RESULTS_PER_LINE, resultPerLine+"");
                }
                catch (NumberFormatException ex) {
                    resultsPerLineField.setText("");
                }
            }
            
        });
        JPanel perLinePanel = new JPanel(new BorderLayout());
        perLinePanel.add(resultsPerLineField, BorderLayout.WEST);
        perLinePanel.add(new JLabel("results per line"), BorderLayout.CENTER);
        
        this.topPanel = new JPanel(new WrapLayout(false, WrapLayout.LEFT, 8, 5));
        topPanel.add(new JLabel("History:"));
        topPanel.add(historyButton);
        topPanel.add(new JSeparator(SwingConstants.VERTICAL));
        topPanel.add(newOnlyCheckbox);
        topPanel.add(new JSeparator(SwingConstants.VERTICAL));
        topPanel.add(perLinePanel);
        
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
    
    public void loadSearchResults(ColorDepthSearch search, List<ColorDepthResult> resultList, ColorDepthMask mask, boolean isUserDriven) {

        log.info("loadSearchResults(resultList.size={}, mask={}, isUserDriven={})", resultList.size(), mask.getFilepath(), isUserDriven);
        this.search = search;
        this.mask = mask;
        sampleMap.clear();
        matchMap.clear();

        
        SimpleWorker worker = new SimpleWorker() {

            String newResultPreference;
            String resultsPerLinePreference;
            
            @Override
            protected void doStuff() throws Exception {
                
                newResultPreference = getPreference(PREFERENCE_CATEGORY_CDS_NEW_RESULTS);
                log.info("Got new result preference: "+newResultPreference);

                resultsPerLinePreference = getPreference(PREFERENCE_CATEGORY_CDS_RESULTS_PER_LINE);
                log.info("Got results per line preference: "+resultsPerLinePreference);
                
                prepareResults(resultList);
            }

            @Override
            protected void hadSuccess() {
                if (newResultPreference != null) {
                    boolean newResults = Boolean.parseBoolean(newResultPreference);
                    newOnlyCheckbox.setSelected(newResults);
                }
                else {
                    newOnlyCheckbox.setSelected(false);
                }
                
                if (resultsPerLinePreference != null) {
                    resultsPerLineField.setText(resultsPerLinePreference);
                }
                else {
                    resultsPerLineField.setText(""+DEFAULT_RESULTS_PER_LINE);
                }
                
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
    
    /**
     * Runs in background thread.
     */
    private void prepareResults(List<ColorDepthResult> resultList) throws Exception {

        log.info("Preparing matching results from {} results", resultList.size());
        
        results.clear();
        for(ColorDepthResult result : resultList) {
            List<ColorDepthMatch> matches = result.getMaskMatches(mask);
            if (matches!=null && !matches.isEmpty()) {
                results.add(result);
            }
        }

        DomainModel model = DomainMgr.getDomainMgr().getModel();
        
        Set<Reference> sampleRefs = new HashSet<>();
        for (ColorDepthResult result : resultList) {
            List<ColorDepthMatch> maskMatches = result.getMaskMatches(mask);
            
            // Populate maps
            for (ColorDepthMatch match : maskMatches) {
                if (!sampleMap.containsKey(match.getSample())) {
                    log.trace("Will load {}", match.getSample());
                    sampleRefs.add(match.getSample());
                }
            }
        }

        sampleMap.putAll(DomainUtils.getMapByReference(model.getDomainObjectsAs(Sample.class, new ArrayList<>(sampleRefs))));
        
        log.info("Found {} results for {}", results.size(), mask);
    }
    
    private void showResults(boolean isUserDriven) {
        if (!results.isEmpty()) {
            currResult = results.get(results.size()-1);
            historyButton.update();
            topPanel.setVisible(true);
            showCurrSearchResult(isUserDriven);
        }
        else {
            log.debug("No results for mask");
            showNothing();
        }
    }
    
    public void showCurrSearchResult(boolean isUserDriven) {

        if (results.isEmpty()) return;
        
        log.debug("showCurrSearchResult(isUserDriven={})",isUserDriven);

        if (currResult==null) {
            throw new IllegalStateException("No current result to show");
        }
        
        int currResultIndex = results.indexOf(currResult);
        selectionModel.setParentObject(currResult);
        
        List<ColorDepthMatch> maskMatches = currResult.getMaskMatches(mask).stream()
                .filter(match -> showMatch(match))
                .sorted(Comparator.comparing(ColorDepthMatch::getScore))
                .collect(Collectors.toList());
        
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
        
        Integer resultsPerLine = null;
        try {
            resultsPerLine = new Integer(resultsPerLineField.getText());
        }
        catch (NumberFormatException e) {
            log.warn("Illegal results per line value: "+resultsPerLineField.getText());
        }
        
        // Group matches by line
        Map<String,LineMatches> lines = new LinkedHashMap<>();
        for (ColorDepthMatch match : maskMatches) {
            Sample sample = sampleMap.get(match.getSample());
            String line = sample==null ? ""+match.getSample() : sample.getLine();
            LineMatches lineMatches = lines.get(line);
            if (lineMatches==null) {
                lineMatches = new LineMatches(line);
                lines.put(line, lineMatches);
            }
            lineMatches.addMatch(match);
        }
        
        List<ColorDepthMatch> orderedMatches = new ArrayList<>();
        for (LineMatches lineMatches : lines.values()) {
            for (ColorDepthMatch match : lineMatches.getOrderedFilteredMatches(resultsPerLine)) {
                orderedMatches.add(match);
            }
        }
        
        ColorDepthSearchResults searchResults = new ColorDepthSearchResults(orderedMatches);
        resultsPanel.showSearchResults(searchResults, isUserDriven, null);
    }
    
    private boolean showMatch(ColorDepthMatch match) {
        if (match.getSample()==null) return true; // Match is not bound to a sample
        Sample sample = sampleMap.get(match.getSample());
        // If match is bound to a sample, we need access to it
        return sample != null;
    }

    private class LineMatches {
        
        private String line;
        private List<ColorDepthMatch> matches = new ArrayList<>();
        
        LineMatches(String line) {
            this.line = line;
        }
        
        public void addMatch(ColorDepthMatch match) {
            matches.add(match);
        }
        
        public List<ColorDepthMatch> getOrderedFilteredMatches(Integer resultsPerLine) {

            log.debug("Getting matches for line {} with {} max results", line, resultsPerLine);
            
            Set<Long> seenSamples = new HashSet<>();
            List<ColorDepthMatch> orderedMatches = new ArrayList<>();
            for (ColorDepthMatch match : matches) {
                
                if (match.getSample() == null) {
                    orderedMatches.add(match);
                    continue;
                }
                
                String matchStr = String.format("%s@ch%s - %2.2f", 
                        match.getSample(), match.getChannelNumber(), match.getScorePercent());

                if (seenSamples.contains(match.getSample().getTargetId())) {
                    // Only show top hit for each sample
                    log.debug("  Skipping duplicate sample: {}", matchStr);
                    continue;
                }

                if (resultsPerLine!=null && orderedMatches.size() >= resultsPerLine) {
                    // Got enough matches
                    log.debug("  Skipping line: {}", matchStr);
                    continue;
                }
                
                log.debug("  Adding match: {}", matchStr);
                orderedMatches.add(match);
                
                seenSamples.add(match.getSample().getTargetId());
            }
            
            return orderedMatches;   
        }
    }
    
    @Override
    public String getSortField() {
        return sortCriteria;
    }

    @Override
    public void setSortField(final String sortCriteria) {
        this.sortCriteria = sortCriteria;
    }
    
    @Override
    public void search() {
    }
    
    @Override
    public void export() {
    }

    public ChildSelectionModel<ColorDepthMatch, String> getSelectionModel() {
        return selectionModel;
    }

    void reset() {
        selectionModel.reset();
        this.currResult = results.isEmpty() ? null : results.get(results.size() - 1);
    }

    int getCurrResultIndex() {
        return results.indexOf(currResult);
    }

    public PaginatedResultsPanel<ColorDepthMatch, String> getResultPanel() {
        return resultsPanel;
    }

    public void showNothing() {
        topPanel.setVisible(false);
        resultsPanel.showNothing();
    }

    @Override
    public Long getCurrentParentId() {
        return search.getId();
    }
    
    @Override
    public void refreshView() {
        showCurrSearchResult(true);
    }
}
