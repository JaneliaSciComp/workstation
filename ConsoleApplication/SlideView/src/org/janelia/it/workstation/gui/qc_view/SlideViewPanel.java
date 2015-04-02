package org.janelia.it.workstation.gui.qc_view;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import org.janelia.it.workstation.api.entity_model.management.EntitySelectionModel;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.framework.outline.Refreshable;
import org.janelia.it.workstation.gui.framework.viewer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.janelia.it.jacs.model.TimebasedIdentifierGenerator;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.solr.EntityDocument;
import org.janelia.it.jacs.shared.solr.SolrQueryBuilder;
import org.janelia.it.jacs.shared.solr.SolrResults;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.workstation.gui.dialogs.search.ResultPage;
import org.janelia.it.workstation.gui.dialogs.search.ResultTreeMapping;
import org.janelia.it.workstation.gui.dialogs.search.SearchConfiguration;
import org.janelia.it.workstation.gui.dialogs.search.SearchParametersPanel;
import org.janelia.it.workstation.gui.dialogs.search.SearchResults;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.model.entity.RootedEntity;
import org.janelia.it.workstation.shared.util.ConcurrentUtils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;

/**
 * A panel for allowing technicians to quickly search for and review images.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SlideViewPanel extends JPanel implements Refreshable {

    private static final Logger log = LoggerFactory.getLogger(SlideViewPanel.class);
    
    private static final Color DISABLED_COLOR = (Color)UIManager.get("Label.disabledForeground");
    protected static final int PAGE_SIZE = 50;
    
    private final String disabledColorHex;
    private final SearchParametersPanel searchParamsPanel;
    private final ViewerPane viewerPane;
    private final IconDemoPanel imageViewer;
    private final Map<String, Set<String>> filters = new HashMap<>();
    private final SearchResults searchResults = new SearchResults();
    private final Action autoScaleAction;
    private final JToggleButton autoScaleButton;
    
    private int numImagesPerRow = 1;
    
    public SlideViewPanel() {
        
        if (DISABLED_COLOR!=null) {
            String rgb = Integer.toHexString(DISABLED_COLOR.getRGB());
            disabledColorHex = "#"+rgb.substring(2, rgb.length());
        }
        else {
            disabledColorHex = null;
        }

        Set<String> entityTypes = new HashSet<>();
        entityTypes.add(EntityConstants.TYPE_LSM_STACK);
        filters.put("entity_type",entityTypes);
        
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder((Color) UIManager.get("windowBorder")));
        
        this.searchParamsPanel = new SearchParametersPanel() {
            @Override
            public void performSearch(boolean clear) {
                performSlideSearch(0, true);
            }
        };
        
        SearchConfiguration searchConfig = new SearchConfiguration();
        searchConfig.load();
        searchConfig.addConfigurationChangeListener(searchParamsPanel);
        searchParamsPanel.init(searchConfig);
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));

        viewerPane = new ViewerPane(null, EntitySelectionModel.CATEGORY_MAIN_VIEW, false);
        imageViewer = new IconDemoPanel(viewerPane) {

            @Override
            protected void buttonDrillDown(AnnotatedImageButton button) {
                // Do nothing, this panel does not support drill down
            }

            @Override
            public void setAsActive() {
                // This viewer cannot be activated
            }
            
            @Override
            public void refresh(final boolean invalidateCache, final Callable<Void> successCallback) {
                try {
                    performSlideSearch(0, true);
                    ConcurrentUtils.invoke(successCallback);
                }
                catch (Exception e) {
                    SessionMgr.getSessionMgr().handleException(e);
                }
                
            }
            
            @Override
            public boolean isLabelSizeLimitedByImageSize() {
                return false;
            }
        };
        viewerPane.setViewer(imageViewer);
        
        // Customize toolbar
        IconDemoToolbar toolbar = imageViewer.getToolbar();
        toolbar.getPrevButton().setVisible(false);
        toolbar.getNextButton().setVisible(false);
        toolbar.getPathButton().setVisible(false);
        
        this.autoScaleAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (autoScaleButton.isSelected()) {
                    imageViewer.getImagesPanel().recalculateGrid(numImagesPerRow);
                }
            }
        };
                        
        autoScaleButton = new JToggleButton("Auto-size");
        autoScaleButton.setIcon(Icons.getIcon("table.png"));
        autoScaleButton.setFocusable(false);
        autoScaleButton.setSelected(true);
        autoScaleButton.setToolTipText("Auto size the rows to fit one slide code per row.");
        autoScaleButton.addActionListener(autoScaleAction);
        toolbar.getToolbar().add(autoScaleButton);
                
        add(searchParamsPanel, BorderLayout.NORTH);
        add(viewerPane, BorderLayout.CENTER);

        ModelMgr.getModelMgr().registerOnEventBus(this);
    }

    public synchronized void performSlideSearch(final int pageNum, final boolean showLoading) {
        performSlideSearch(pageNum, showLoading, null);
    }

    public synchronized void performSlideSearch(final int pageNum, final boolean showLoading, final Callable<Void> success) {

        log.debug("performSlideSearch(pageNum={},showLoading={})", pageNum, showLoading);

        SimpleWorker worker = new SimpleWorker() {

            private RootedEntity tempSearchRE;

            @Override
            protected void doStuff() throws Exception {
                this.tempSearchRE = performSlideSearch(pageNum);
            }

            @Override
            protected void hadSuccess() {
                try {
                    if (autoScaleButton.isSelected()) {
                        autoScaleAction.actionPerformed(null);
                    }
                    viewerPane.loadEntity(tempSearchRE, new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            if (showLoading) {
                                imageViewer.showImagePanel();
                            }
                            ConcurrentUtils.invoke(success);
                            return null;
                        }
                    });
                }
                catch (Exception e) {
                    SessionMgr.getSessionMgr().handleException(e);
                }
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
                if (showLoading) {
                    imageViewer.loadEntity(null);
                }
            }
        };

        if (showLoading) {
            imageViewer.showLoadingIndicator();
        }
        worker.execute();
    }
    
    /**
     * Perform the search and organize the results. 
     * TODO: implement paging functionality. Currently, only the first page of 
     * results is considered relevant. 
     * @param pageNum
     * @return
     * @throws Exception 
     */
    private RootedEntity performSlideSearch(final int pageNum) throws Exception {
        
        if (SwingUtilities.isEventDispatchThread()) {
            throw new RuntimeException("SlideViewPanel.performSlideSearch called in the EDT");
        }
        
        // Build the basic query from what the user has input
        final SolrQueryBuilder builder = new SolrQueryBuilder();
        searchParamsPanel.getQueryBuilder(builder);
        if (!builder.hasQuery()) return null;
        
        // Only looking for LSMs which have a denormalized SAGE id gives us the sub-sample LSMs we're looking for, 
        // without any parent-sample LSMs that we're not interested in.
        String aux = builder.getAuxString()==null?"":builder.getAuxString()+" ";
        builder.setAuxString(aux+"+sage_id_txt:*");
        // Sort by slide code so that we get a consistent set of data (we can't sort by slide_code_txt because it's a multivalued field)
        builder.setSortField("sage_light_imagery_slide_code_t");
        // Filter to get LSMs only 
        builder.getFilters().putAll(filters);

        log.info("Search for "+builder.getSearchString());
        
        ResultPage resultPage = new ResultPage(performSearch(builder, pageNum, PAGE_SIZE));
        log.info("Adding result page ({} results)", resultPage.getResults().size());
        searchResults.clear();
        searchResults.addPage(resultPage);

        // Map LSMs to samples
        List<String> upMapping = new ArrayList<>();
        List<String> downMapping = new ArrayList<>();
        upMapping.add(EntityConstants.TYPE_IMAGE_TILE);
        upMapping.add(EntityConstants.TYPE_SUPPORTING_DATA);
        upMapping.add(EntityConstants.TYPE_SAMPLE);
        ResultTreeMapping projection = new ResultTreeMapping(upMapping, downMapping);
        searchResults.setResultTreeMapping(projection);
        searchResults.projectResultPages();
        
        return getSlideResults(resultPage);
    }
    
    /**
     * Given a page of LSM results mapped to their parent samples, walk all of 
     * the samples and build a result set consisting of all the LSMs in all the
     * samples. Build a set of virtual entities with information that should be
     * displayed in the slide view. 
     * @param resultPage a single page of LSM results mapped to parent samples
     * @return RootedEntity containing the search results as children, 
     *         for display in an IconPanel
     * @throws Exception 
     */
    private RootedEntity getSlideResults(ResultPage resultPage) throws Exception {
        
        // Figure out which LSMs go with which Sample
        final Map<Long,String> lsmIdToGenotype = new HashMap<>();
        final Map<Long,String> sampleIdToDataset = new HashMap<>();
        final Map<Long,Entity> sampleMap = new HashMap<>();
        final Multimap<Long,EntityDocument> sampleToLsm = ArrayListMultimap.<Long,EntityDocument>create();
        SolrResults pageResults = resultPage.getSolrResults();
        for (EntityDocument entityDoc : pageResults.getEntityDocuments()) {
            Entity lsmEntity = entityDoc.getEntity();

            List<Entity> mappedEntities = resultPage.getMappedEntities(lsmEntity.getId());
            if (mappedEntities.isEmpty()) {
                log.info("No sample for entity "+lsmEntity.getId());
                continue;
            }
            if (mappedEntities.size()>1) {
                log.warn("More than one sample for LSM "+lsmEntity.getName());
            }
            Entity sample = mappedEntities.get(0);

            String lsmDataSet = (String)entityDoc.getDocument().getFieldValue("sage_light_imagery_data_set_t");
            sampleIdToDataset.put(sample.getId(), lsmDataSet);

            String lsmGenotype = (String)entityDoc.getDocument().getFieldValue("sage_line_genotype_t");
            lsmIdToGenotype.put(lsmEntity.getId(), lsmGenotype);
            
            sampleMap.put(sample.getId(), sample);
            sampleToLsm.put(sample.getId(), entityDoc);

            // If the sample does not have it's own Data Set, borrow it from the LSM
            if (sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER)==null) {
                sample.setValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER, lsmDataSet);
            }   
        }

        List<Long> sortedSampleIds = new ArrayList<>(sampleToLsm.keySet());
        Collections.sort(sortedSampleIds, new Comparator<Long>() {
            @Override
            public int compare(Long id1, Long id2) {
                Entity e1 = sampleMap.get(id1);
                Entity e2 = sampleMap.get(id2);
                ComparisonChain chain = ComparisonChain.start()
                    .compare(e1.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER), e2.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER), Ordering.natural().nullsLast())
                    .compare(e1.getValueByAttributeName(EntityConstants.ATTRIBUTE_SLIDE_CODE), e2.getValueByAttributeName(EntityConstants.ATTRIBUTE_SLIDE_CODE), Ordering.natural().nullsLast())
                    .compare(e1.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE), e2.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE), Ordering.natural().nullsFirst());
                return chain.result();
            }
        });
        
        Entity searchResultsEntity = getPlaceholderEntity(null);
        Map<String,Map<String,EntityData>> slideCodeToLsmMap = new LinkedHashMap<>();
        Set<String> tilePattern = new HashSet<>();

        // First pass to determine if any tiles were merged
        boolean someMergedTiles = false;
        All: for (Long sampleId : sortedSampleIds) {
            Entity sample = sampleMap.get(sampleId);
            ModelMgr.getModelMgr().loadLazyEntity(sample, false);
            Entity supportingData = EntityUtils.getSupportingData(sample);
            ModelMgr.getModelMgr().loadLazyEntity(supportingData, true);
            for(Entity tileEntity : EntityUtils.getChildrenOfType(supportingData, EntityConstants.TYPE_IMAGE_TILE)) {
                List<Entity> lsmEntities = EntityUtils.getChildrenOfType(tileEntity, EntityConstants.TYPE_LSM_STACK);
                if (lsmEntities.size()>1) {
                    someMergedTiles = true;
                    break All;
                }
            }
        }
        
        // Second pass to determine overall tile pattern
        Map<String,EntityData> slideCodeEds = null;
        String dataSetSlideCode = null;

        for (Long sampleId : sortedSampleIds) {
            Entity sample = sampleMap.get(sampleId);
            String dataSet = sampleIdToDataset.get(sample.getId());
            dataSet = dataSet==null?"":dataSet;
            ModelMgr.getModelMgr().loadLazyEntity(sample, false);
            Entity supportingData = EntityUtils.getSupportingData(sample);
            ModelMgr.getModelMgr().loadLazyEntity(supportingData, true);

            for(Entity tileEntity : EntityUtils.getChildrenOfType(supportingData, EntityConstants.TYPE_IMAGE_TILE)) {

                // Really cheating here. The model is the view. But this is the only way to get things done without rewriting the IconDemoPanel.
                Set<EntityData> mips = new HashSet<>();
                EntityData ed1 = tileEntity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
                EntityData ed2 = tileEntity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE);
                EntityData ed3 = tileEntity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE);
                if (ed1!=null) mips.add(ed1);
                if (ed2!=null) mips.add(ed2);
                if (ed3!=null) mips.add(ed3);

                List<Entity> lsmEntities = EntityUtils.getChildrenOfType(tileEntity, EntityConstants.TYPE_LSM_STACK);
                
                int i = 1;
                String patternCode = null;
                for(Entity lsmEntity : lsmEntities) {

                    Set<EntityData> lmips = new HashSet<>();
                    EntityData led1 = lsmEntity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
                    EntityData led2 = lsmEntity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE);
                    EntityData led3 = lsmEntity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE);
                    if (led1!=null) mips.add(led1);
                    if (led2!=null) mips.add(led2);
                    if (led3!=null) mips.add(led3);

                    String lsmSlideCode = lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SLIDE_CODE);

                    String key = dataSet+"~"+lsmSlideCode;
                    if (dataSetSlideCode==null || !dataSetSlideCode.equals(key)) {
                        slideCodeEds = new LinkedHashMap<>();
                        dataSetSlideCode = key;
                        log.debug("Starting "+dataSetSlideCode);
                    }

                    String objective = lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);
                    String qiScore = lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_QI_SCORE);
                    String nccScore = lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_NCC_SCORE);
                    String genotype = lsmIdToGenotype.get(lsmEntity.getId());
                    
                    lsmSlideCode = lsmSlideCode==null?"":lsmSlideCode;
                    objective = objective==null?"":objective;
                    genotype = genotype==null?"":genotype;

                    String tileKey = tileEntity.getName();
                    if (someMergedTiles) {
                        tileKey += " #"+i;
                    }
                    
                    patternCode = objective;
                    if (!StringUtils.isEmpty(patternCode)) patternCode += " ";
                    patternCode += tileKey;
                    
                    StringBuilder title = new StringBuilder("<html><b>");
                    title.append(lsmSlideCode).append("</b> - ");
                    
                    title.append(objective).append(" ").append(tileKey);
                                        
                    title.append("<br>").append(StringUtils.abbreviate(dataSet, 30));
                    title.append("<br>").append(StringUtils.abbreviate(genotype, 20));
                    
                    if (qiScore!=null) {
                        title.append(", Qi=").append(qiScore);
                    }
                    
                    if (nccScore!=null) {
                        title.append(", Ncc=").append(nccScore);
                    }
                    
                    title.append("&nbsp;</html>");
                    
                    Entity virtualLsm = getVirtualEntity(title.toString(), lsmEntity);
                    
                    // Use the LSM's MIPs if available, otherwise fallback on the tile's MIPs
                    if (lmips.isEmpty()) {
                        virtualLsm.getEntityData().addAll(mips);
                    }
                    else {
                        virtualLsm.getEntityData().addAll(lmips);
                    }

                    EntityData ed = new EntityData();
                    ed.setEntityAttrName(EntityConstants.ATTRIBUTE_ENTITY);
                    ed.setOwnerKey(SessionMgr.getSubjectKey());
                    ed.setParentEntity(searchResultsEntity);
                    ed.setChildEntity(virtualLsm);
                    log.debug("   Adding "+patternCode);
                    slideCodeEds.put(patternCode, ed);

                    tilePattern.add(patternCode);
                    
                    i++;
                }
                
                if (lsmEntities.size()==1 && someMergedTiles && patternCode!=null) {
                    // Add a dummy merged tile
                    tilePattern.add(patternCode.replaceFirst("#1", "#2"));
                }
            }

            slideCodeToLsmMap.put(dataSetSlideCode, slideCodeEds);
        }
        
        List<String> sortedTilePattern = new ArrayList<>(tilePattern);
        Collections.sort(sortedTilePattern, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                // Sort by LSM# first, so that merged LSMs can be placed on two rows
                ComparisonChain chain = ComparisonChain.start();
                int h1 = o1.indexOf('#');
                int h2 = o2.indexOf('#');
                if (h1>0 && h2>0) {
                    Integer i1 = Integer.parseInt(o1.charAt(h1+1)+"");
                    Integer i2 = Integer.parseInt(o2.charAt(h2+1)+"");
                    chain = chain.compare(i1, i2, Ordering.natural().nullsFirst());
                }
                chain = chain.compare(o1, o2, Ordering.natural().nullsFirst());
                return chain.result();
            }
        });

        numImagesPerRow = sortedTilePattern.size();
        
        // Ensure there is an even number of tiles per slide code
        if (numImagesPerRow % 2 != 0) {
            sortedTilePattern.add("Empty");
            numImagesPerRow++;
        }
        
        log.debug("numImagesPerSlide="+numImagesPerRow);
        
        if (someMergedTiles) {
            if (numImagesPerRow>2) {
                log.debug("Some tiles were merged, so dividing by 2");
                numImagesPerRow /= 2;    
            }
        }
        
        while (numImagesPerRow>6) {
            log.debug("numImagesPerRow={}>5 , so dividing by 2",numImagesPerRow);
            numImagesPerRow /= 2;    
        }

        log.debug("numImagesPerRow="+numImagesPerRow);
        if (log.isDebugEnabled()) {
            for(String patternCode : sortedTilePattern) {
                log.debug("  "+patternCode);    
            }
        }
        
        List<EntityData> eds = new ArrayList<>();

        // Walk through each slide code and find all the necessary LSMs to fill
        // out the display. If an LSM for a given objective/tile is not found, 
        // fill it in with a placeholder. 
        
        int i = 0;
        for(String key : slideCodeToLsmMap.keySet()) {
            Map<String,EntityData> slideCodeMap = slideCodeToLsmMap.get(key);
            if (log.isDebugEnabled()) {
                log.debug("slideCodeToLsmMap[{}] = {} items:",key,slideCodeMap.size());
                for(String d : slideCodeMap.keySet()) {
                    log.debug("  "+d);
                }
            }
            String[] dataSetSlideCodeArr = key.split("~");
            String slideCode = dataSetSlideCodeArr[1];
            for(String patternCode : sortedTilePattern) {
                EntityData ed = slideCodeMap.get(patternCode);    
                if (ed==null) {
                    log.debug("   "+patternCode+" not found");

                    // Build disable title
                    StringBuilder sb = new StringBuilder();
                    sb.append("<html>");
                    if (disabledColorHex!=null) {
                        sb.append("<font color=").append(disabledColorHex).append(">");
                    }
                    sb.append("<b>").append(slideCode).append("</b> - ").append(patternCode);
                    if (disabledColorHex!=null) {
                        sb.append("</font>");
                    }
                    sb.append("</html>");

                    Entity placeholder = getPlaceholderEntity(sb.toString());
                    ed = new EntityData();
                    ed.setEntityAttrName(EntityConstants.ATTRIBUTE_ENTITY);
                    ed.setParentEntity(searchResultsEntity);
                    ed.setChildEntity(placeholder);
                }
                else {
                    log.debug("   "+patternCode+" found");
                }
                eds.add(ed);
                ed.setOrderIndex(i++);
            }
        }

        log.debug("Setting "+eds.size()+" children");
        String title = eds.isEmpty()?"No results found":"Search Results";
        searchResultsEntity.setEntityData(new HashSet<>(eds));
        searchResultsEntity.setValueByAttributeName(EntityConstants.IN_MEMORY_ATTRIBUTE_TITLE, title);
        return new UnrootedEntity(searchResultsEntity);
    }
    
    /**
     * Actually execute the query. This method must be called from a worker thread.
     */
    private SolrResults performSearch(SolrQueryBuilder builder, int page, int pageSize) throws Exception {

        if (SwingUtilities.isEventDispatchThread()) {
            throw new RuntimeException("GeneralSearchDialog.search called in the EDT");
        }

        SolrQuery query = builder.getQuery();
        query.setStart(pageSize * page);
        query.setRows(pageSize);

        log.info("Searching SOLR: " + query.getQuery());
        return ModelMgr.getModelMgr().searchSolr(query);
    }
    
    private Entity getPlaceholderEntity(String title) {
        Entity virtualEntity = new Entity();
        virtualEntity.setId(TimebasedIdentifierGenerator.generateIdList(1).get(0));
        virtualEntity.setName("Placeholder");
        virtualEntity.setEntityTypeName(EntityConstants.IN_MEMORY_TYPE_PLACEHOLDER_ENTITY);
        virtualEntity.setOwnerKey(SessionMgr.getSubjectKey());
        if (title!=null) {
            virtualEntity.setValueByAttributeName(EntityConstants.IN_MEMORY_ATTRIBUTE_TITLE, title);
        }
        return virtualEntity;
    }
    
    private Entity getVirtualEntity(String title, Entity template) {
        Entity virtualEntity = new Entity();
        virtualEntity.setId(template.getId());
        virtualEntity.setName(template.getName());
        virtualEntity.setEntityTypeName(EntityConstants.IN_MEMORY_TYPE_VIRTUAL_ENTITY);
        virtualEntity.setOwnerKey(template.getOwnerKey());
        virtualEntity.setCreationDate(template.getCreationDate());
        virtualEntity.setUpdatedDate(template.getUpdatedDate());
        virtualEntity.setValueByAttributeName(EntityConstants.IN_MEMORY_ATTRIBUTE_TITLE, title);
        for(EntityData ed : template.getEntityData()) {
            if (ed.getValue()!=null) {
                virtualEntity.setValueByAttributeName(ed.getEntityAttrName(), ed.getValue());
            }   
        }
        return virtualEntity;
    }
    
    @Override
    public void refresh() {
        performSlideSearch(0, true);
    }

    @Override
    public void totalRefresh() {
        performSlideSearch(0, true);
    }
    
    private class UnrootedEntity extends RootedEntity {

        public UnrootedEntity(Entity entity) {
            super(entity);
        }

        @Override
        public RootedEntity getChild(EntityData childEd) {
            if (childEd == null) {
                return null;
            }
            return new RootedEntity(""+childEd.getChildEntity().getId(), childEd);
        }
    }
}
