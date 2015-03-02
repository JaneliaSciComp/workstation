package org.janelia.it.workstation.gui.qc_view;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import java.awt.BorderLayout;
import java.awt.Color;
import org.janelia.it.workstation.api.entity_model.management.EntitySelectionModel;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.framework.outline.Refreshable;
import org.janelia.it.workstation.gui.framework.viewer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.apache.solr.client.solrj.SolrQuery;
import org.janelia.it.jacs.compute.api.support.SolrQueryBuilder;
import org.janelia.it.jacs.model.TimebasedIdentifierGenerator;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.solr.EntityDocument;
import org.janelia.it.jacs.shared.solr.SolrResults;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.workstation.gui.dialogs.search.ResultPage;
import org.janelia.it.workstation.gui.dialogs.search.ResultTreeMapping;
import org.janelia.it.workstation.gui.dialogs.search.SearchConfiguration;
import org.janelia.it.workstation.gui.dialogs.search.SearchParametersPanel;
import org.janelia.it.workstation.gui.dialogs.search.SearchResults;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.model.entity.RootedEntity;
import org.janelia.it.workstation.shared.util.ConcurrentUtils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;

/**
 * A panel for allowing technicians to quickly search for and review images.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class QCViewPanel extends JPanel implements Refreshable {

    private static final Logger log = LoggerFactory.getLogger(QCViewPanel.class);

    protected static final int PAGE_SIZE = 50;
    
    private final SearchParametersPanel searchParamsPanel;
    private final ViewerPane viewerPane;
    private final IconDemoPanel imageViewer;
    private final Map<String, Set<String>> filters = new HashMap<String, Set<String>>();
    private final SearchResults searchResults = new SearchResults();
    
    public QCViewPanel() {

        log.info("Init QCViewPanel");
        
        Set<String> entityTypes = new HashSet<String>();
        entityTypes.add(EntityConstants.TYPE_LSM_STACK);
        filters.put("entity_type",entityTypes);
        
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder((Color) UIManager.get("windowBorder")));

        this.searchParamsPanel = new SearchParametersPanel() {
            @Override
            public String getSearchString() {
                String s = (String)getInputFieldValue();
                if (!s.contains(" ") && !s.endsWith("*")) {
                    return s+"*";
                }
                return s;
            }
        };
        
        SearchConfiguration searchConfig = new SearchConfiguration();
        searchConfig.load();
        searchConfig.addConfigurationChangeListener(searchParamsPanel);
        searchParamsPanel.init(searchConfig);
        
        AbstractAction searchAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performSearch(0, true);
            }
        };
        
        searchParamsPanel.getSearchButton().addActionListener(searchAction);

        searchParamsPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0,true),"enterAction");
        searchParamsPanel.getActionMap().put("enterAction", searchAction);
        
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
        };
        viewerPane.setViewer(imageViewer);
        
        searchParamsPanel.setInputFieldValue("20121204_32_A2");
        
        add(searchParamsPanel, BorderLayout.NORTH);
        add(viewerPane, BorderLayout.CENTER);

        ModelMgr.getModelMgr().registerOnEventBus(this);
    }

    public synchronized void performSearch(final int pageNum, final boolean showLoading) {
        performSearch(pageNum, showLoading, null);
    }

    public synchronized void performSearch(final int pageNum, final boolean showLoading, final Callable<Void> success) {

        log.debug("performSearch(pageNum={},showLoading={})", pageNum, showLoading);

        final SolrQueryBuilder builder = searchParamsPanel.getQueryBuilder();
        if (!builder.hasQuery()) {
            return;
        }

        builder.getFilters().putAll(filters);

        log.info("Search for "+builder.getSearchString());

        SimpleWorker worker = new SimpleWorker() {

            private ResultPage resultPage;
            private RootedEntity tempSearchRE;

            @Override
            protected void doStuff() throws Exception {
                resultPage = new ResultPage(performSearch(builder, pageNum, PAGE_SIZE));
                log.info("Adding result page ({} results)", resultPage.getResults().size());
                searchResults.clear();
                searchResults.addPage(resultPage);

                // Map LSMs to samples
                List<String> upMapping = new ArrayList<String>();
                List<String> downMapping = new ArrayList<String>();
                upMapping.add(EntityConstants.TYPE_IMAGE_TILE);
                upMapping.add(EntityConstants.TYPE_SUPPORTING_DATA);
                upMapping.add(EntityConstants.TYPE_SAMPLE);
                ResultTreeMapping projection = new ResultTreeMapping(upMapping, downMapping);
                searchResults.setResultTreeMapping(projection);
                searchResults.projectResultPages();
                                
                // Figure out which LSMs go with which Sample
                final Map<Long,String> lsmIdToDataset = new HashMap<Long,String>();
                final Map<Long,Entity> sampleMap = new HashMap<Long,Entity>();
                final Set<String> parentSampleNames = new HashSet<String>();
                final Multimap<Long,EntityDocument> sampleToLsm = ArrayListMultimap.<Long,EntityDocument>create();
                SolrResults pageResults = resultPage.getSolrResults();
                for (EntityDocument entityDoc : pageResults.getEntityDocuments()) {
                    Entity lsmEntity = entityDoc.getEntity();
                    
                    String lsmDataSet = (String)entityDoc.getDocument().getFirstValue("sage_light_imagery_data_set_t");
                    lsmIdToDataset.put(lsmEntity.getId(), lsmDataSet);
                    
                    List<Entity> mappedEntities = resultPage.getMappedEntities(lsmEntity.getId());
                    if (mappedEntities.isEmpty()) {
                        log.info("No sample for entity "+lsmEntity.getId());
                        continue;
                    }
                    if (mappedEntities.size()>1) {
                        log.warn("More than one sample for LSM "+lsmEntity.getName());
                    }
                    Entity sample = mappedEntities.get(0);
                    sampleMap.put(sample.getId(), sample);
                    sampleToLsm.put(sample.getId(), entityDoc);
                    if (sample.getName().contains("~")) {
                        String sampleName = sample.getName().replaceFirst("~\\d+x$", "");
                        parentSampleNames.add(sampleName);
                    }
                    
                    // If the sample does not have it's own Data Set, borrow it from the LSM
                    if (sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER)==null) {
                        sample.setValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER, lsmDataSet);
                    }   
                }
                
                // Remove redundant parent samples
                List<Long> toDelete = new ArrayList<Long>();
                for(Long sampleId : sampleToLsm.keySet()) {
                    Entity sample = sampleMap.get(sampleId);
                    if (parentSampleNames.contains(sample.getName())) {
                        toDelete.add(sampleId);
                    }
                }
                for(Long sampleId : toDelete) {
                    sampleToLsm.removeAll(sampleId);
                }
                
                List<Long> sortedSampleIds = new ArrayList<Long>(sampleToLsm.keySet());
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
                
                Entity searchResultsEntity = new Entity();
                searchResultsEntity.setId(TimebasedIdentifierGenerator.generateIdList(1).get(0));
                searchResultsEntity.setName("Search Results");
                searchResultsEntity.setOwnerKey(SessionMgr.getSubjectKey());
                searchResultsEntity.setEntityTypeName(EntityConstants.TYPE_FOLDER);

                List<EntityData> eds = new ArrayList<EntityData>();
                for (Long sampleId : sortedSampleIds) {
                    Entity sample = sampleMap.get(sampleId);
                    
                    List<EntityData> sampleEds = new ArrayList<EntityData>();
                
                    ModelMgr.getModelMgr().loadLazyEntity(sample, false);
                    Entity supportingData = EntityUtils.getSupportingData(sample);
                    
                    ModelMgr.getModelMgr().loadLazyEntity(supportingData, true);
                    
                    for(Entity tileEntity : EntityUtils.getChildrenOfType(supportingData, EntityConstants.TYPE_IMAGE_TILE)) {

                        String tile = tileEntity.getName();
                        
                        // Really cheating here. The model is the view. But this is the only way to get things done without rewriting the IconDemoPanel.
                        Set<EntityData> mips = new HashSet<EntityData>();
                        EntityData ed1 = tileEntity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
                        EntityData ed2 = tileEntity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE);
                        EntityData ed3 = tileEntity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE);
                        if (ed1!=null) mips.add(ed1);
                        if (ed2!=null) mips.add(ed2);
                        if (ed3!=null) mips.add(ed3);

                        for(Entity lsmEntity : EntityUtils.getChildrenOfType(tileEntity, EntityConstants.TYPE_LSM_STACK)) {
                            
                            String dataSet = lsmIdToDataset.get(lsmEntity.getId());
                            String objective = lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);
                            String slideCode = lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SLIDE_CODE);
                            String qiScore = lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_QI_SCORE);

                            objective = objective==null?"":objective;
                            dataSet = dataSet==null?"":dataSet;
                            slideCode = slideCode==null?"":slideCode;
                            qiScore = qiScore==null?"":qiScore;

                            lsmEntity.setName("<html><b>"+slideCode+"</b> - "+objective+" "+tile+"<br>"+dataSet+"<br>"+qiScore+"</html>");
                            lsmEntity.getEntityData().addAll(mips);
                            EntityData ed = new EntityData();
                            ed.setEntityAttrName(EntityConstants.ATTRIBUTE_ENTITY);
                            ed.setParentEntity(searchResultsEntity);
                            ed.setChildEntity(lsmEntity);
                            sampleEds.add(ed);
                        }
                    }
                
                    eds.addAll(sampleEds);
                }

                int i = 0;
                for(EntityData ed : eds) {
                    ed.setOrderIndex(i++);
                }
                
                log.info("Setting "+eds.size()+" children");
                searchResultsEntity.setEntityData(new HashSet<EntityData>(eds));
                this.tempSearchRE = new UnrootedEntity(searchResultsEntity);
            }

            @Override
            protected void hadSuccess() {
                try {
                    viewerPane.loadEntity(tempSearchRE);
                    if (showLoading) {
                        imageViewer.showImagePanel();
                    }
                    ConcurrentUtils.invoke(success);
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
     * Actually execute the query. This method must be called from a worker thread.
     */
    public SolrResults performSearch(SolrQueryBuilder builder, int page, int pageSize) throws Exception {

        if (SwingUtilities.isEventDispatchThread()) {
            throw new RuntimeException("GeneralSearchDialog.search called in the EDT");
        }

        SolrQuery query = builder.getQuery();
        query.setStart(pageSize * page);
        query.setRows(pageSize);

        log.info("Searching SOLR: " + query.getQuery());
        return ModelMgr.getModelMgr().searchSolr(query);
    }

    protected void populateResultView(final ResultPage resultPage) {

        if (resultPage == null) {
            return;
        }
//        long numResults = pageResults.getResponse().getResults().getNumFound();

        if (searchResults.getNumLoadedPages() == 1) {
            // First page, so clear the previous results
//            resultsTable.removeAllRows();
        }

        

//        int numLoadedResults = resultsTable.getRows().size();
//        statusLabel.setText(numResults + " results found for '" + fullQueryString.trim() + "', " + numLoadedResults + " results loaded.");
//        statusLabel.setToolTipText("Query took " + pageResults.getResponse().getElapsedTime() + " milliseconds");

    }
    
    @Override
    public void refresh() {
        performSearch(0, true);
    }

    @Override
    public void totalRefresh() {
        // TODO: clear cache?
        performSearch(0, true);
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
