
package org.janelia.it.jacs.server.access.hibernate;

import org.apache.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.SQLQuery;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.search.SearchHit;
import org.janelia.it.jacs.model.tasks.search.SearchTask;
import org.janelia.it.jacs.web.gwt.common.shared.data.ChartData;
import org.janelia.it.jacs.web.gwt.common.shared.data.ChartDataEntry;
import org.janelia.it.jacs.web.gwt.common.shared.data.ImageModel;
import org.janelia.it.jacs.web.gwt.search.client.model.SampleResult;
import org.janelia.it.jacs.web.gwt.search.client.model.SiteDescription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: cgoina
 * Implementation of sample specific search DAO
 */
public class SampleSearchDAOImpl extends SearchDAOImpl {
    private static Logger _logger = Logger.getLogger(SampleSearchDAOImpl.class);


    public SampleSearchDAOImpl() {
    }

    public int executeSearchTask(SearchTask searchTask)
            throws DaoException {
        return 0;
        //return populateSearchResult(searchTask,SearchTask.TOPIC_SAMPLE);
    }

    public int getNumSearchHits(String searchString, int matchFlags)
            throws DaoException {
        return countSearchHits(searchString, SearchTask.TOPIC_SAMPLE, matchFlags);
    }

    public List<SearchHit> search(String searchString,
                                  int matchFlags,
                                  int startIndex,
                                  int numRows,
                                  SortArgument[] sortArgs)
            throws DaoException {
        return performGenericSearch(searchString, SearchTask.TOPIC_SAMPLE, matchFlags, startIndex, numRows, sortArgs);
    }

    public List<SampleResult> getPagedCategoryResultsByNodeId(Long nodeId,
                                                              int startIndex,
                                                              int numRows,
                                                              SortArgument[] sortArgs) throws DaoException {
        String sql = "select " +
                "sd.project, " +
                "sd.sample_acc, " +
                "sd.sample_name, " +
                "sd.filter_size, " +
                "sd.read_count, " +
                "sd.sample_region, " +
                "sd.sample_location, " +
                "sd.sample_country, " +
                "sd.sample_habitat, " +
                "sd.sample_host_organism," +
                "sd.sample_depth, " +
                "sd.site_location, " +
                "sd.site_longitude, " +
                "sd.site_latitude, " +
                "nt.rank " +
                "from (select hit_id, rank from sample_ts_result where node_id=" + nodeId + " order by rank desc) nt " +
                "inner join sample_detail sd on sd.sample_id=nt.hit_id";
        sql = addOrderClauseToSql(sql, sortArgs);
        _logger.info("Executing sample search sql=" + sql);
        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
        // NOTE: for this query, we are handling the start and number manually since we do not expect a large
        // number of rows returned. This gives us flexibility in generating the site meta-data.
        List<Object[]> results = sqlQuery.list();
        _logger.info("Sample search yielded result count=" + results.size());
        List<SampleResult> samples = new ArrayList<SampleResult>();
        Map<String, SampleResult> sampleMap = new HashMap<String, SampleResult>();
        for (Object[] res : results) {
            String accession = (String) res[1];
            if (!sampleMap.keySet().contains(accession)) {
/*
                "ss.project, " +
                "ss.sample_acc, " +
                "ss.sample_name, " +
                "coalesce(ss.filter_size,''), " +
                #reads
                "ss.region, "+
                "ss.location, " +
                "ss.country, "+
                "ss.habitat_type, "+
                "case when ss.host_organism>'' then ss.host_organism||coalesce(' - '||ss.host_details,'') else '' end as host_organism," +
                "ss.depth as sample_depth, " +
                "ss.longitude, " +
                "ss.latitude , " +
*/
                SampleResult sampleResult = new SampleResult();
                sampleResult.setProject((String) res[0]);
                sampleResult.setAccession(accession);
                sampleResult.setName((String) res[2]);
                sampleResult.setFilterSize((String) res[3]);
                sampleResult.setReadCount((Integer) res[4]);
                sampleResult.setRegion((String) res[5]);
                sampleResult.setLocation((String) res[6]);
                sampleResult.setCountry((String) res[7]);
                sampleResult.setHabitat(((String) res[8]));
                sampleResult.setHostOrganism((String) res[9]);
                sampleResult.setDepth((String) res[10]);
                List<SiteDescription> sites = new ArrayList<SiteDescription>();
                SiteDescription site = new SiteDescription();
                site.setLocation((String) res[11]);
                site.setLongitude((String) res[12]);
                site.setLatitude((String) res[13]);
                sites.add(site);
                sampleResult.setSites(sites);
                sampleResult.setRank((Float) res[14]);
                sampleMap.put(accession, sampleResult);
                samples.add(sampleResult);
            }
            else {
                SampleResult sampleResult = sampleMap.get(accession);
                List<SiteDescription> sites = sampleResult.getSites();
                SiteDescription site = new SiteDescription();
                site.setLocation((String) res[11]);
                site.setLongitude((String) res[12]);
                site.setLatitude((String) res[13]);
                sites.add(site);
            }
        }
        List<SampleResult> finalSampleList = new ArrayList<SampleResult>();
        if (startIndex >= 0 || numRows > 0) {
            // I added the samples.size condition when
            // I tried to change the paginator to retrieve the
            // count or rows and the rows themselves simultaneously
            int maxSamplesIndex;
            if (numRows == 0) {
                maxSamplesIndex = samples.size();
            }
            else {
                maxSamplesIndex = startIndex + numRows;
                if (maxSamplesIndex > samples.size()) {
                    maxSamplesIndex = samples.size();
                }
            }
            for (int i = startIndex; i < maxSamplesIndex; i++) {
                finalSampleList.add(samples.get(i));
            }
        }
        else {
            finalSampleList = samples;
        }
//        addDocumentsToCategoryResultsByNodeId(nodeId, finalSampleList);
        _logger.info("Returning sample result of size=" + finalSampleList.size());
        return finalSampleList;
    }

    public int getNumCategoryResultsByNodeId(Long nodeId) throws DaoException {
        String sql =
                "select cast(count(1) as Integer)" +
                        "from sample_ts_result nt " +
                        "where node_id=" + nodeId;
        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
        int count = ((Integer) sqlQuery.uniqueResult()).intValue();
        return count;
    }

    public List<ImageModel> getSearchResultCharts(Long searchId, String resultBaseDirectory)
            throws DaoException {
        List<ImageModel> samplesSearchResultCharts = new ArrayList<ImageModel>();
        samplesSearchResultCharts.add(getSamplesByProjectChart(searchId, resultBaseDirectory));
        samplesSearchResultCharts.add(getReadsPerSampleChart(searchId, resultBaseDirectory));
        return samplesSearchResultCharts;
    }

//    public Set<Site> getMapInfoForSearchResultsBySearchId(Long searchId, String category)
//            throws DaoException {
//        String sql =
//                "select distinct " +
//                        "  site.sample_name as siteId, " +
//                        "  site.location as location, " +
//                        "  site.latitude as latitude, " +
//                        "  site.longitude as longitude " +
//                        "from sample_ts_result nt " +
//                        "inner join sample_site site on site.sample_id = nt.hit_id " +
//                        "where nt.node_id = (select node_id from node where task_id = :searchId) ";
//        _logger.info("Reads per sample sql=" + sql);
//        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
//        sqlQuery.setLong("searchId", searchId);
//        sqlQuery.addScalar("siteId", Hibernate.STRING);
//        sqlQuery.addScalar("location", Hibernate.STRING);
//        sqlQuery.addScalar("latitude", Hibernate.STRING);
//        sqlQuery.addScalar("longitude", Hibernate.STRING);
//        List<Object[]> results = sqlQuery.list();
//        int i = 0;
//        Set<Site> sites = new HashSet<Site>();
//        for (Object[] result : results) {
//            GeoPoint geoPoint = new GeoPoint();
//            try {
////                geoPoint.setSiteId((Long)result[0]);
////                geoPoint.setLocation((String)result[1]);
//                geoPoint.setLatitude((String) result[2]);
//                geoPoint.setLongitude((String) result[3]);
//                Site s = new Site();
//                s.setSiteId((String) result[0]);
//                s.setSampleLocation((String) result[1]);
//                s.setLatitude(geoPoint.getLatitude());
//                s.setLatitudeDouble(geoPoint.getLatitudeAsDouble());
//                s.setLongitude(geoPoint.getLongitude());
//                s.setLongitudeDouble(geoPoint.getLongitudeAsDouble());
//                sites.add(s);
//            }
//            catch (Exception e) {
//                _logger.error("Exception encountered processing site id:" + geoPoint.getSiteId() + " location:" + geoPoint.getLocation() + " latitude:" + geoPoint.getLatitude() + " longitude:" + geoPoint.getLongitude(), e);
//            }
//        }
//        return sites;
//    }

    private ImageModel getSamplesByProjectChart(Long searchId, String resultBaseDirectory)
            throws DaoException {
        String chartTitle = "Samples By Project";
        ChartData dataset = new ChartData();
        retrieveSamplesByProject(searchId, dataset);
        if (dataset == null || dataset.getTotal() == null) {
            return null;
        }
        /*
        * Generate the pie chart.
        */
        try {
            return chartTool.createPieChart(chartTitle,
                    dataset,
                    PIE_CHART_WIDTH,  // width
                    PIE_CHART_HEIGHT, // height

                    resultBaseDirectory, getResultChartDirectory(searchId));
        }
        catch (Exception e) {
            _logger.error("Error creating the samples by project chart", e);
            throw new DaoException(e, "SampleSearchDAOImpl.getSamplesByProjectChart.createPieChart");
        }
    }

    private ImageModel getReadsPerSampleChart(Long searchId, String resultBaseDirectory)
            throws DaoException {
        String chartTitle = "Reads Per Sample";
        ChartData dataset = new ChartData();
        retrieveReadsPerSample(searchId, dataset);
        if (dataset == null || dataset.getTotal() == null) {
            return null;
        }
        /*
        * Generate the pie chart.
        */
        try {
            return chartTool.createPieChart(chartTitle,
                    dataset,
                    PIE_CHART_WIDTH,  // width
                    PIE_CHART_HEIGHT, // height
                    resultBaseDirectory, getResultChartDirectory(searchId));
        }
        catch (Exception e) {
            _logger.error("Error creating the samples by project chart", e);
            throw new DaoException(e, "SampleSearchDAOImpl.getSamplesByProjectChart.createPieChart");
        }
    }

    private void retrieveReadsPerSample(Long searchId, ChartData dataset)
            throws DaoException {
        String sql =
                "select " +
                        "  bioSample.sample_acc as sampleAcc, " +
                        "  bioSample.sample_title as sampleName, " +
                        "  bioSample.read_count as nReadsPerSample " +
                        "from sample_ts_result sampleResult " +
                        "inner join sample_detail bioSample on sampleResult.hit_id = bioSample.sample_id " +
                        "where sampleResult.node_id = (select node_id from node where task_id = :searchId) " +
                        "and bioSample.read_count>0 " +
                        "order by nReadsPerSample desc";
        _logger.info("Reads per sample sql=" + sql);
        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
        sqlQuery.setLong("searchId", searchId);
        sqlQuery.addScalar("sampleAcc", Hibernate.STRING);
        sqlQuery.addScalar("sampleName", Hibernate.STRING);
        sqlQuery.addScalar("nReadsPerSample", Hibernate.LONG);
        List<Object[]> results = sqlQuery.list();
        int i = 0;
        for (Object[] result : results) {
            String sampleName = (String) result[1];
            Long n = (Long) result[2];
            dataset.addChartDataEntry(new ChartDataEntry(sampleName, n));
        }
    }

    private void retrieveSamplesByProject(Long searchId, ChartData dataset)
            throws DaoException {
        String sql =
                "select " +
                        "  p.symbol as projectAcc, p.name as projectName, count(distinct s.sample_id) as nSamplesPerProject " +
                        "from sample_ts_result sampleResult " +
                        "inner join sample_detail s on s.sample_id = sampleResult.hit_id " +
                        "inner join project p on p.symbol = 'CAM_PROJ_'||s.project " +
                        "where sampleResult.node_id = (select node_id from node where task_id = :searchId) " +
                        "group by projectAcc, projectName " +
                        "order by nSamplesPerProject desc";
        _logger.info("Samples by project partition sql=" + sql);
        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
        sqlQuery.setLong("searchId", searchId);
        sqlQuery.addScalar("projectAcc", Hibernate.STRING);
        sqlQuery.addScalar("projectName", Hibernate.STRING);
        sqlQuery.addScalar("nSamplesPerProject", Hibernate.LONG);
        List<Object[]> results = sqlQuery.list();
        int i = 0;
        for (Object[] result : results) {
            String projectName = (String) result[1];
            Long n = (Long) result[2];
            dataset.addChartDataEntry(new ChartDataEntry(projectName, n));
        }
    }

}
