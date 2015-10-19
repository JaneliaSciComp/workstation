
package org.janelia.it.jacs.compute.service.export.model;

import org.janelia.it.jacs.compute.service.export.util.CSVDataConversionHelper;
import org.janelia.it.jacs.model.genomics.Library;
import org.janelia.it.jacs.model.genomics.Sample;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jul 29, 2008
 * Time: 3:58:53 PM
 */
public class SampleColumnFormatter extends ColumnFormatter {
    public static final String NOT_AVAILABLE = "n/a";
    public static final Map<SampleHeader, String> headerMap = new HashMap<SampleHeader, String>();

    public static enum SampleHeader {
        SAMPLE_NAME,
        SAMPLE_ID,
        NUM_READS,
        FILTER_SIZE,
        SITE,
        LAT_LONG,
        SMPL_POS,
        GEO_POS,
        NATION,
        TIME,
        DATE,
        HABITAT,
        SMPL_DEPTH,
        WATER_DEPTH,
        TEMPERATURE,
        SALINITY,
        CHLOROPHYLL_DENS,
        SAMPLE_MONTH_CHLOROPHYLL_DENS,
        ANNUAL_CHLOROPHYLL_DENS
    }

    static {
        headerMap.put(SampleHeader.SAMPLE_NAME, "Sample");
        headerMap.put(SampleHeader.SAMPLE_ID, "Sample ID");
        headerMap.put(SampleHeader.NUM_READS, "Smpl Num Reads");
        headerMap.put(SampleHeader.FILTER_SIZE, "Filter Size (\u00B5m)");
        headerMap.put(SampleHeader.SITE, "Site");
        headerMap.put(SampleHeader.LAT_LONG, "Lat/Long");
        headerMap.put(SampleHeader.SMPL_POS, "Sample Pos");
        headerMap.put(SampleHeader.GEO_POS, "Geo Pos");
        headerMap.put(SampleHeader.NATION, "Nation");
        headerMap.put(SampleHeader.TIME, "Time");
        headerMap.put(SampleHeader.DATE, "Date");
        headerMap.put(SampleHeader.HABITAT, "Habitat");
        headerMap.put(SampleHeader.SMPL_DEPTH, "Sample Depth");
        headerMap.put(SampleHeader.WATER_DEPTH, "Water Depth");
        headerMap.put(SampleHeader.TEMPERATURE, "Temperature");
        headerMap.put(SampleHeader.SALINITY, "Salinity");
        headerMap.put(SampleHeader.CHLOROPHYLL_DENS, "Chlorophyll Dens");
        headerMap.put(SampleHeader.SAMPLE_MONTH_CHLOROPHYLL_DENS, "Smpl Month Chloro Dens");
        headerMap.put(SampleHeader.ANNUAL_CHLOROPHYLL_DENS, ("Annual Chloro Dens"));
    }

    public static List<String> getHeaderList() {
        List<String> headerList = new ArrayList<String>();
        for (SampleHeader h : SampleHeader.values()) {
            headerList.add(headerMap.get(h));
        }
        return headerList;
    }

    public static List<String> formatColumns(Sample sample) {
        BioMaterial bm = getBioMaterial(sample);
        CollectionSite site = null;
        if (bm != null)
            site = bm.getCollectionSite();
        List<String> pl = new ArrayList<String>();
        add(pl, (sample == null ? NOT_AVAILABLE : sample.getSampleName())); //SAMPLE_NAME,
        add(pl, (sample == null ? NOT_AVAILABLE : sample.getSampleAcc())); //SAMPLE_ID,
        add(pl, getNumReads(sample)); // NUM_READS,
        add(pl, getFilterSize(sample)); //FILTER_SIZE,
        add(pl, (site == null ? NOT_AVAILABLE : site.getSiteDescription())); //SITE,
        add(pl, getLatLong(site));                  //LAT_LONG,
        add(pl, (site == null ? NOT_AVAILABLE : site.getLocation())); //SMPL_POS,
        add(pl, (site == null ? NOT_AVAILABLE : site.getRegion())); //GEO_POS,
        add(pl, getCountry(site));                  //NATION,
        add(pl, (bm == null ? NOT_AVAILABLE : CSVDataConversionHelper.n2sTime(bm.getCollectionStartTime()))); //TIME,
        add(pl, (bm == null ? NOT_AVAILABLE : CSVDataConversionHelper.n2sDate(bm.getCollectionStartTime()))); //DATE,
        add(pl, (site == null ? NOT_AVAILABLE : site.getSiteDescription())); //HABITAT,
        add(pl, getSampleDepth(site));              //SMPL_DEPTH,
        add(pl, (bm == null ? NOT_AVAILABLE : bm.getObservationAsString("water depth"))); //WATER_DEPTH,
        add(pl, (bm == null ? NOT_AVAILABLE : bm.getObservationAsString("temperature"))); //TEMPERATURE,
        add(pl, (bm == null ? NOT_AVAILABLE : bm.getObservationAsString("salinity"))); //SALINITY,
        add(pl, (bm == null ? NOT_AVAILABLE : bm.getObservationAsString("chlorophyll density"))); //CHLOROPHYLL_DENS,
        add(pl, (bm == null ? NOT_AVAILABLE : bm.getObservationAsString("chlorophyll density/sample month"))); //SAMPLE_MONTH_CHLOROPHYLL_DENS,
        add(pl, (bm == null ? NOT_AVAILABLE : bm.getObservationAsString("chlorophyll density/annual"))); //ANNUAL_CHLOROPHYLL_DENS
        return pl;
    }

    private static BioMaterial getBioMaterial(Sample s) {
        if (s == null)
            return null;
        Set<BioMaterial> bms = s.getBioMaterials();
        if (bms == null) {
            return null;
        }
        else if (bms.size() == 0) {
            return null;
        }
        else {
            return bms.iterator().next(); // For csv, get first biomaterial
        }
    }

    protected static String getLatLong(CollectionSite site) {
        if (site instanceof GeoPoint) {
            GeoPoint gp = (GeoPoint) site;
            return gp.getLatitude() + " ; " + gp.getLongitude();
        }
        else {
            return NOT_AVAILABLE;
        }
    }

    protected static String getCountry(CollectionSite site) {
        if (site instanceof GeoPoint) {
            GeoPoint gp = (GeoPoint) site;
            return gp.getCountry();
        }
        else {
            return NOT_AVAILABLE;
        }
    }

    protected static String getSampleDepth(CollectionSite site) {
        if (site instanceof GeoPoint) {
            GeoPoint gp = (GeoPoint) site;
            return gp.getDepth();
        }
        else {
            return NOT_AVAILABLE;
        }
    }

    protected static String getFilterSize(Sample sample) {
        if (sample == null) {
            return NOT_AVAILABLE;
        }
        else {
            String min = (sample.getFilterMin() == null ? "?" : sample.getFilterMin().toString());
            String max = (sample.getFilterMax() == null ? "?" : sample.getFilterMax().toString());
            return min + " - " + max;
        }
    }

    protected static String getNumReads(Sample sample) {
        if (sample == null) {
            return NOT_AVAILABLE;
        }
        else {
            Set<Library> libSet = sample.getLibraries();
            Integer total = 0;
            for (Library lib : libSet) {
                total += lib.getNumberOfReads();
            }
            if (total == 0) {
                return NOT_AVAILABLE;
            }
            else {
                return total.toString();
            }
        }
    }

}
