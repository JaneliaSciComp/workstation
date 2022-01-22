package org.janelia.workstation.colordepth.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.stream.Collectors;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.Subscribe;

import org.apache.commons.lang3.StringUtils;
import org.janelia.model.domain.DomainConstants;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.SampleUtils;
import org.janelia.model.domain.enums.SplitHalfType;
import org.janelia.model.domain.gui.cdmip.ColorDepthImage;
import org.janelia.model.domain.gui.cdmip.ColorDepthMask;
import org.janelia.model.domain.gui.cdmip.ColorDepthMatch;
import org.janelia.model.domain.gui.cdmip.ColorDepthResult;
import org.janelia.model.domain.gui.cdmip.ColorDepthSearch;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.browser.actions.ExportResultsAction;
import org.janelia.workstation.browser.gui.editor.SelectionButton;
import org.janelia.workstation.browser.gui.editor.SingleSelectionButton;
import org.janelia.workstation.browser.gui.listview.PaginatedResultsPanel;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.common.gui.support.MouseForwarder;
import org.janelia.workstation.common.gui.support.PreferenceSupport;
import org.janelia.workstation.common.gui.support.SearchProvider;
import org.janelia.workstation.common.gui.support.WrapLayout;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.api.web.SageRestClient;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.selection.ChildSelectionModel;
import org.janelia.workstation.core.model.DomainModelViewUtils;
import org.janelia.workstation.core.model.SplitTypeInfo;
import org.janelia.workstation.core.model.search.ResultPage;
import org.janelia.workstation.core.model.search.SearchResults;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ColorDepthResultPanel extends JPanel implements SearchProvider, PreferenceSupport {

    private final static Logger log = LoggerFactory.getLogger(ColorDepthResultPanel.class);

    // Constants
    private static final List<ColorDepthListViewerType> viewerTypes = ImmutableList.of(ColorDepthListViewerType.ColorDepthResultImageViewer, ColorDepthListViewerType.ColorDepthResultTableViewer);
    private static final String PREFERENCE_CATEGORY_CDS_RESULTS_PER_LINE = "CDSResultPerLine";
    private static final String PREFERENCE_CATEGORY_CDS_NEW_RESULTS = "CDSOnlyNewResults";
    private static final String PREFERENCE_CATEGORY_CDS_SPLITHALFTYPES = "CDSSplitHalfTypes";
    private static final int DEFAULT_RESULTS_PER_LINE = 2;
    private static final List<SplitHalfType> ALL_SPLIT_TYPES = Arrays.asList(SplitHalfType.AD, SplitHalfType.DBD);

    private static final String NO_RUN_TEXT = "<html>"
            + "This mask does not have results in the selected search run.<br>"
            + "Execute the search to get results for this mask."
            + "</html>";
    
    private static final String NO_MATCHES_TEXT = "<html>"
            + "No matching lines were found for this mask.<br>"
            + "Try altering your search parameters, or recreating your mask."
            + "</html>";
    
    // UI Components
    private final JPanel topPanel;
    private final SingleSelectionButton<ColorDepthResult> historyButton;
    private final JCheckBox newOnlyCheckbox;
    private final JCheckBox showMatchedImage;
    private final JTextField resultsPerLineField;
    private final PaginatedResultsPanel<ColorDepthMatch,Reference> resultsPanel;
    private final JLabel noRunLabel;
    private final JLabel noMatchesLabel;
    private final SelectionButton<SplitHalfType> splitTypeButton;

    // State
    private ColorDepthSearch search;
    private ColorDepthMask mask;
    /** relevant results for the currently selected mask */
    private List<ColorDepthResult> results = new ArrayList<>();
    private ColorDepthResult currResult;
    private String sortCriteria;
    private ColorDepthSearchResults searchResults;
    private final Set<SplitHalfType> selectedSplitTypes = new HashSet<>();
    
    private final ChildSelectionModel<ColorDepthMatch,Reference> selectionModel = new ChildSelectionModel<ColorDepthMatch,Reference>() {

        @Override
        protected void selectionChanged(List<ColorDepthMatch> objects, boolean select, boolean clearAll, boolean isUserDriven) {
            Events.getInstance().postOnEventBus(new ColorDepthMatchSelectionEvent(getSource(), objects, select, clearAll, isUserDriven));
        }

        @Override
        public Reference getId(ColorDepthMatch match) {
            return match.getImageRef();
        }
    };

    private final ChildSelectionModel<ColorDepthMatch,Reference> editSelectionModel = new ChildSelectionModel<ColorDepthMatch,Reference>() {

        @Override
        protected void selectionChanged(List<ColorDepthMatch> objects, boolean select, boolean clearAll, boolean isUserDriven) {
            Events.getInstance().postOnEventBus(new ColorDepthMatchEditSelectionEvent(getSource(), objects, select, clearAll, isUserDriven));
        }
        
        @Override
        public Reference getId(ColorDepthMatch match) {
            return match.getImageRef();
        }
    };
    
    public ColorDepthResultPanel() {

        noRunLabel = new JLabel(NO_RUN_TEXT);
        noRunLabel.setHorizontalAlignment(SwingConstants.CENTER);

        noMatchesLabel = new JLabel(NO_MATCHES_TEXT);
        noMatchesLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
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
                setPreferenceAsync(PREFERENCE_CATEGORY_CDS_NEW_RESULTS, 
                        newOnlyCheckbox.isSelected()+"")
                        .addListener(() -> refreshView());
            }
        );

        this.showMatchedImage = new JCheckBox("Show matched image");
        this.showMatchedImage.addActionListener(e -> refreshView());

        this.resultsPerLineField = new JTextField(3);
        resultsPerLineField.setHorizontalAlignment(JTextField.RIGHT);
        resultsPerLineField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                try {
                    Integer resultPerLine = new Integer(resultsPerLineField.getText());
                    setPreferenceAsync(PREFERENCE_CATEGORY_CDS_RESULTS_PER_LINE, 
                            resultPerLine+"")
                            .addListener(() -> refreshView());
                }
                catch (NumberFormatException ex) {
                    resultsPerLineField.setText("");
                }
            }
            
        });
        JPanel perLinePanel = new JPanel(new BorderLayout());
        perLinePanel.add(resultsPerLineField, BorderLayout.WEST);
        perLinePanel.add(new JLabel("results per line"), BorderLayout.CENTER);

        splitTypeButton = new SelectionButton<SplitHalfType>("Split Types") {

            @Override
            public Collection<SplitHalfType> getValues() {
                return ALL_SPLIT_TYPES;
            }

            @Override
            public Set<SplitHalfType> getSelectedValues() {
                return selectedSplitTypes;
            }

            @Override
            protected void selectAll() {
                selectedSplitTypes.addAll(ALL_SPLIT_TYPES);

                SimpleWorker worker = new SimpleWorker() {

                    @Override
                    protected void doStuff() throws Exception {
                        for (SplitHalfType value : SplitHalfType.values()) {
                            setPreference(PREFERENCE_CATEGORY_CDS_SPLITHALFTYPES, value.getName(), true);
                        }
                    }

                    @Override
                    protected void hadSuccess() {
                        log.info("Saved split half preference: all");
                        refreshView();
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        FrameworkAccess.handleException(error);
                    }
                };

                worker.execute();
            }
            @Override
            protected void clearSelected() {
                selectedSplitTypes.clear();

                SimpleWorker worker = new SimpleWorker() {

                    @Override
                    protected void doStuff() throws Exception {
                        for (SplitHalfType value : SplitHalfType.values()) {
                            setPreference(PREFERENCE_CATEGORY_CDS_SPLITHALFTYPES, value.getName(), false);
                        }
                    }

                    @Override
                    protected void hadSuccess() {
                        log.info("Saved split half preference: none");
                        refreshView();
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        FrameworkAccess.handleException(error);
                    }
                };

                worker.execute();
            }

            @Override
            protected void updateSelection(SplitHalfType value, boolean selected) {
                if (selected) {
                    selectedSplitTypes.add(value);
                }
                else {
                    selectedSplitTypes.remove(value);
                }
                setPreferenceAsync(PREFERENCE_CATEGORY_CDS_SPLITHALFTYPES, value.getName(), selected).addListener(() -> {
                    log.info("Saved split half preference {}={}", value.getName(), selected);
                    refreshView();
                });
            }
        };

        this.topPanel = new JPanel(new WrapLayout(false, WrapLayout.LEFT, 8, 5));
        topPanel.add(new JLabel("History:"));
        topPanel.add(historyButton);
        topPanel.add(new JSeparator(SwingConstants.VERTICAL));
        topPanel.add(newOnlyCheckbox);
        topPanel.add(showMatchedImage);
        topPanel.add(perLinePanel);
        topPanel.add(splitTypeButton);
        
        this.resultsPanel = new PaginatedResultsPanel<ColorDepthMatch,Reference>(selectionModel, editSelectionModel, this, this, viewerTypes) {
    
            @Override
            protected ResultPage<ColorDepthMatch,Reference> getPage(SearchResults<ColorDepthMatch,Reference> searchResults, int page) throws Exception {
                return searchResults.getPage(page);
            }
            
            @Override
            public Reference getId(ColorDepthMatch match) {
                return match.getImageRef();
            }

            @Override
            public void viewerContextChanged() {
                ColorDepthResultPanel.this.viewerContextChanged();
            }
        };
        resultsPanel.addMouseListener(new MouseForwarder(this, "PaginatedResultsPanel->ColorDepthResultPanel"));
        resultsPanel.getViewer().setEditSelectionModel(editSelectionModel);
        
        setLayout(new BorderLayout());
    }

    protected void viewerContextChanged() {
    }

    @Subscribe
    public void colorDepthMatchSelected(ColorDepthMatchSelectionEvent event) {
        resultsPanel.updateStatusBar();
    }
    
    public void loadSearchResults(ColorDepthSearch search, List<ColorDepthResult> resultList, ColorDepthMask mask, boolean isUserDriven) {

        log.info("loadSearchResults(resultList.size={}, mask={}, isUserDriven={})", resultList.size(), mask.getFilepath(), isUserDriven);
        final StopWatch w = new StopWatch();
        
        showLoadingIndicator();
        
        this.search = search;
        this.mask = mask;

        log.info("Preparing matching results from {} results", resultList.size());

        // Filter down to relevant results for the current mask
        Reference maskRef = Reference.createFor(mask);
        results.clear();
        results.addAll(resultList.stream().filter(
                r -> r.getMaskResults().stream().anyMatch(
                        mr -> mr.getMaskRef().equals(maskRef))
            ).collect(Collectors.toList()));
        
        SimpleWorker worker = new SimpleWorker() {

            String newResultPreference;
            String resultsPerLinePreference;
            
            @Override
            protected void doStuff() throws Exception {

                loadPreferences();
                
                newResultPreference = getPreference(PREFERENCE_CATEGORY_CDS_NEW_RESULTS);
                log.info("Got new result preference: "+newResultPreference);

                resultsPerLinePreference = getPreference(PREFERENCE_CATEGORY_CDS_RESULTS_PER_LINE);
                log.info("Got results per line preference: "+resultsPerLinePreference);
                
                if (getPreference(PREFERENCE_CATEGORY_CDS_SPLITHALFTYPES, SplitHalfType.AD.getName(), false)) {
                    log.info("Got split half filter preference: AD");
                    selectedSplitTypes.add(SplitHalfType.AD);
                }
                
                if (getPreference(PREFERENCE_CATEGORY_CDS_SPLITHALFTYPES, SplitHalfType.DBD.getName(), false)) {
                    log.info("Got split half filter preference: DBD");
                    selectedSplitTypes.add(SplitHalfType.DBD);
                }
            }

            @Override
            protected void hadSuccess() {
                splitTypeButton.update();
                if (newResultPreference != null) {
                    boolean newResults = Boolean.parseBoolean(newResultPreference);
                    newOnlyCheckbox.setSelected(newResults);
                } else {
                    newOnlyCheckbox.setSelected(false);
                }
                
                if (resultsPerLinePreference != null) {
                    resultsPerLineField.setText(resultsPerLinePreference);
                } else {
                    resultsPerLineField.setText(""+DEFAULT_RESULTS_PER_LINE);
                }

                ActivityLogHelper.logElapsed("ColorDepthResultPanel.loadSearchResults", search, w);
                showResults(isUserDriven);
            }

            @Override
            protected void hadError(Throwable error) {
                showNothing();
                FrameworkAccess.handleException(error);
            }
        };

        worker.execute();    
    }
    
    private void showResults(boolean isUserDriven) {
        log.info("showResults(isUserDriven={})", isUserDriven);
        if (!results.isEmpty()) {
            currResult = results.get(results.size()-1);
            selectionModel.setParentObject(currResult);
            historyButton.update();
            showCurrSearchResult(isUserDriven);
        }
        else {
            showNothing();
        }
    }
    
    public void showCurrSearchResult(boolean isUserDriven) {

        if (results.isEmpty()) {
            showNothing();
            return;
        }
        
        log.info("showCurrSearchResult(isUserDriven={})",isUserDriven);

        if (currResult==null) {
            throw new IllegalStateException("No current result to show");
        }
        
        if (!currResult.getParameters().getMasks().contains(Reference.createFor(mask))) {
            showNoRun();
            return;
        }
        
        SimpleWorker worker = new SimpleWorker() {
            
            @Override
            protected void doStuff() throws Exception {
                List<ColorDepthMatch> maskMatches = currResult.getMaskMatches(mask);
                log.info("Found {} matches for {} in {}", maskMatches.size(), mask, search);
                searchResults = prepareResults(maskMatches);
            }

            @Override
            protected void hadSuccess() {
                if (searchResults==null) {
                    showNoMatches();
                }
                else {
                    resultsPanel.showSearchResults(searchResults, isUserDriven, null);
                    showMatches();
                }
            }

            @Override
            protected void hadError(Throwable error) {
                showNothing();
                FrameworkAccess.handleException(error);
            }
        };

        worker.execute(); 
    }

    /**
     * Runs in background thread.
     */
    private ColorDepthSearchResults prepareResults(List<ColorDepthMatch> maskMatches) throws Exception {

        DomainModel model = DomainMgr.getDomainMgr().getModel();

        // Fetch associated images
        Set<Reference> imageRefs;
        if (showMatchedImage.isSelected()) {
            imageRefs = maskMatches.stream()
                    .map(m -> m.getMatchingImageRef() != null ? m.getMatchingImageRef() : m.getImageRef())
                    .collect(Collectors.toSet());
        } else {
            imageRefs = maskMatches.stream()
                    .map(ColorDepthMatch::getImageRef)
                    .collect(Collectors.toSet());
        }

        List<ColorDepthImage> images = model.getDomainObjectsAs(ColorDepthImage.class, new ArrayList<>(imageRefs));

        // Fetch associated samples
        Set<Reference> sampleRefs = new HashSet<>();            
        for (ColorDepthImage image : images) {
            log.trace("Will load {}", image.getSampleRef());
            sampleRefs.add(image.getSampleRef());
        }

        List<Sample> samples = model.getDomainObjectsAs(Sample.class, new ArrayList<>(sampleRefs));
        
        // Fetch split half information

        final SageRestClient sageClient = DomainMgr.getDomainMgr().getSageClient();
        
        Set<String> frags = new HashSet<>();
        for (Sample sample : samples) {
            String frag = SampleUtils.getFragFromLineName(sample.getLine());
            if (frag == null) {
                log.warn("Cannot parse fragment from line: {}", sample.getLine());
            }
            else {
                frags.add(frag);
            }
        }

        Map<String, SplitTypeInfo> splitInfos = new HashMap<>();

        try {
            splitInfos = sageClient.getSplitTypeInfo(frags);
        }
        catch (Exception e) {
            // If split type fails, show an error but keep going
            FrameworkAccess.handleException("Failed to load AB/DBD split half information", e);
        }
        
        // Create and set image model
        ColorDepthResultImageModel imageModel = new ColorDepthResultImageModel(mask, maskMatches, images, samples, splitInfos) {
            @Override
            protected Reference getUsedImageRef(ColorDepthMatch match) {
                return showMatchedImage.isSelected() && match.getMatchingImageRef() != null ? match.getMatchingImageRef() : match.getImageRef();
            }
        };
        resultsPanel.setImageModel(imageModel);

        log.info("selectedSplitTypes: {}",selectedSplitTypes);

        // Filter matches
        maskMatches = maskMatches.stream()
                .filter(match -> showMatch(match, imageModel))
                .filter(match -> {
                    // Filter by split type. If no split types are selected, then assume the user wants to see everything.
                    Sample sample = imageModel.getSample(match);
                    SplitTypeInfo splitTypeInfo = imageModel.getSplitTypeInfo(sample);
                    
                    boolean show = true;
                    
                    if (selectedSplitTypes.contains(SplitHalfType.AD)) {
                        if (splitTypeInfo == null || !splitTypeInfo.hasAD()) {
                            show = false;
                        }
                    }
                    
                    if (selectedSplitTypes.contains(SplitHalfType.DBD)) {
                        if (splitTypeInfo == null || !splitTypeInfo.hasDBD()) {
                            show = false;
                        }
                    }
                    
                    return show;
                })
                .sorted(Comparator.comparing(ColorDepthMatch::getScore).reversed())
                .collect(Collectors.toList());
        
        log.info("Filtered to {} matches which can be displayed", maskMatches.size());

        if (newOnlyCheckbox.isSelected()) {
            
            Set<String> filepaths = new HashSet<>();
            
            // First determine what was a match in previous results
            int currResultIndex = results.indexOf(currResult);
            for (int i=0; i<currResultIndex; i++) {
                for(ColorDepthMatch match : results.get(i).getMaskMatches(mask)) {
                    ColorDepthImage image = imageModel.getImage(match);
                    if (image!=null) {
                        filepaths.add(image.getFilepath());
                    }
                }
            }
            
            // Now filter the current results to show the new matches only
            List<ColorDepthMatch> filteredMatches = new ArrayList<>();
            for(ColorDepthMatch match : maskMatches) {
                ColorDepthImage image = imageModel.getImage(match);
                if (image!=null) {
                    if (!filepaths.contains(image.getFilepath())) {
                        filteredMatches.add(match);
                    }
                }
            }
            
            maskMatches = filteredMatches;
            log.info("Filtered to {} new matches", maskMatches.size());
        }
        
        if (maskMatches.isEmpty()) {
            // No matches for this mask
            return null;
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
            ColorDepthImage image = imageModel.getImage(match);
            Sample sample = imageModel.getSample(match);
            String line = sample==null ? image.getName() : sample.getLine();
            LineMatches lineMatches = lines.get(line);
            if (lineMatches==null) {
                lineMatches = new LineMatches(line);
                lines.put(line, lineMatches);
            }
            lineMatches.addMatch(match);
        }
        
        List<ColorDepthMatch> orderedMatches = new ArrayList<>();
        for (LineMatches lineMatches : lines.values()) {
            for (ColorDepthMatch match : lineMatches.getOrderedFilteredMatches(resultsPerLine, imageModel)) {
                orderedMatches.add(match);
            }
        }

        log.info("Filtered to {} matches, allowing {} results per line, and no duplicate samples", orderedMatches.size(), resultsPerLine);
        return new ColorDepthSearchResults(orderedMatches);
    }
    
    public void showNothing() {
        removeAll();
        updateUI();
    }

    public void showNoRun() {
        removeAll();
        add(topPanel, BorderLayout.NORTH);
        add(noRunLabel, BorderLayout.CENTER);
        updateUI();
    }

    public void showNoMatches() {
        removeAll();
        add(topPanel, BorderLayout.NORTH);
        add(noMatchesLabel, BorderLayout.CENTER);
        updateUI();
    }
    
    public void showMatches() {
        removeAll();
        add(topPanel, BorderLayout.NORTH);
        add(resultsPanel, BorderLayout.CENTER);
        updateUI();
    }

    public void showLoadingIndicator() {
        removeAll();
        add(new JLabel(Icons.getLoadingIcon()), BorderLayout.CENTER);
        updateUI();
    }
    
    private boolean showMatch(ColorDepthMatch match, ColorDepthResultImageModel imageModel) {
        ColorDepthImage image = imageModel.getImage(match);
        if (image==null) {
            log.warn("Image not found for match: "+match.getImageRef());
            return false;
        }

        if (image.getSampleRef()==null) {
            return true; // Match is not bound to a sample
        }

        Sample sample = imageModel.getSample(match);
        // If match is bound to a sample, we need access to it
        boolean sampleRead = sample != null;
        if (!sampleRead) {
            log.warn("User cannot access {}", image.getSampleRef());
            return false;
        }

        return true;
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
        
        public List<ColorDepthMatch> getOrderedFilteredMatches(Integer resultsPerLine, ColorDepthResultImageModel imageModel) {

            log.debug("Getting matches for line {} with {} max results", line, resultsPerLine);
            
            Set<Long> seenSamples = new HashSet<>();
            List<ColorDepthMatch> orderedMatches = new ArrayList<>();
            for (ColorDepthMatch match : matches) {
                ColorDepthImage image = imageModel.getImage(match);
                if (image.getSampleRef() == null) {
                    orderedMatches.add(match);
                    continue;
                }

                String matchStr = String.format("%s@ch%s - %2.2f",
                        image.getSampleRef(), image.getChannelNumber(), match.getScorePercent());

                if (seenSamples.contains(image.getSampleRef().getTargetId())) {
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
                
                seenSamples.add(image.getSampleRef().getTargetId());
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
        savePreferences();
    }
    
    @Override
    public void search() {
    }

    private void loadPreferences() {
        if (search==null || search.getId()==null) return;
        if (mask==null || mask.getId()==null) return;
        try {
            String key = search.getId() + "~" + mask.getId();
            sortCriteria = (String) FrameworkAccess.getRemotePreferenceValue(
                    DomainConstants.PREFERENCE_CATEGORY_SORT_CRITERIA, key, null);
            log.debug("Loaded sort criteria preference: {}",sortCriteria);
        }
        catch (Exception e) {
            log.error("Could not load sort criteria",e);
        }
    }

    private void savePreferences() {
        if (search==null || search.getId()==null) return;
        if (mask==null || mask.getId()==null) return;
        if (StringUtils.isEmpty(sortCriteria)) return;
        try {
            String key = search.getId() + "~" + mask.getId();
            FrameworkAccess.setRemotePreferenceValue(
                    DomainConstants.PREFERENCE_CATEGORY_SORT_CRITERIA, key, sortCriteria);
            log.debug("Saved sort criteria preference: {}",sortCriteria);
        }
        catch (Exception e) {
            log.error("Could not save sort criteria",e);
        }
    }
    
    @Override
    public void export() {
        if (searchResults==null) return;
        if (resultsPanel.getViewer() instanceof ColorDepthResultTableViewer) {
            ColorDepthResultTableViewer viewer = (ColorDepthResultTableViewer)resultsPanel.getViewer();
            ExportResultsAction<ColorDepthMatch,Reference> action = new ExportResultsAction<>(searchResults, viewer);
            action.actionPerformed(null);
        }   
    }

    public ChildSelectionModel<ColorDepthMatch,Reference> getSelectionModel() {
        return selectionModel;
    }

    public ChildSelectionModel<ColorDepthMatch,Reference> getEditSelectionModel() {
        return editSelectionModel;
    }

    public ColorDepthResultImageModel getImageModel() {
        return (ColorDepthResultImageModel)resultsPanel.getImageModel();
    }

    @Override
    public Long getCurrentContextId() {
        if (search == null) return null;
        return search.getId();
    }
    
    void reset() {
        selectionModel.reset();
        this.currResult = results.isEmpty() ? null : results.get(results.size() - 1);
    }

    int getCurrResultIndex() {
        return results.indexOf(currResult);
    }

    public PaginatedResultsPanel<ColorDepthMatch,Reference> getResultPanel() {
        return resultsPanel;
    }

    public boolean isEditMode() {
        return resultsPanel.isEditMode();
    }

    public void refreshView() {
        showCurrSearchResult(true);
    }
    
    /**
     * The split generation website doesn't accept regular line names. For VT lines, we
     * need to strip off the "VT" portion. For regular line names, we just need the plate and well.
     * @param sample 
     * @return split identifier suitable for usage with the split gen website
     */
    private String getSplitIdentifier(Sample sample) {
        String vtLine = sample.getVtLine();
        if (vtLine!=null) {
            return vtLine.replaceFirst("VT", "");
        }
        else {
            return SampleUtils.getPlateWellFromLineName(sample.getLine());
        }
    }
}
