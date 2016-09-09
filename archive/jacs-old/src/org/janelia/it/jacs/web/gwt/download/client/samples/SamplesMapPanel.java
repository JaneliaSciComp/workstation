
package org.janelia.it.jacs.web.gwt.download.client.samples;

import com.google.gwt.core.client.GWT;
import com.google.gwt.maps.client.InfoWindowContent;
import com.google.gwt.maps.client.MapType;
import com.google.gwt.maps.client.event.MarkerClickHandler;
import com.google.gwt.maps.client.event.MarkerMouseOutHandler;
import com.google.gwt.maps.client.event.MarkerMouseOverHandler;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.overlay.Marker;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.model.metadata.Sample;
import org.janelia.it.jacs.web.gwt.common.client.model.metadata.Site;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBoxActionLinkUtils;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.PulldownPopup;
import org.janelia.it.jacs.web.gwt.common.client.ui.Span;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.HelpActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.Link;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.DataRetrievedListener;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.StringUtils;
import org.janelia.it.jacs.web.gwt.download.client.DownloadMetaDataService;
import org.janelia.it.jacs.web.gwt.download.client.DownloadMetaDataServiceAsync;
import org.janelia.it.jacs.web.gwt.download.client.model.Project;
import org.janelia.it.jacs.web.gwt.download.client.project.ProjectsSelectedListener;
import org.janelia.it.jacs.web.gwt.download.client.samples.wizard.SampleInfo;
import org.janelia.it.jacs.web.gwt.map.client.GoogleMap;

import java.util.*;

/**
 * @author Michael Press
 */
public class SamplesMapPanel extends Composite implements DataRetrievedListener {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.download.client._samples.SamplesMapPanel");
    private TitledBox _mainPanel;
    private SampleInfo _data;
    private GoogleMap _map;
    private HTML _infoLabel;
    private HTML _detailLabel;
    private Map<String, List<Sample>> _samples;
    private SamplesMapLegendPanel _legendPanel;
    private Map<MyMarker, MarkerInfo> _points;
    private SampleSelectedListener _sampleSelectedListener;
    private PulldownPopup _projectPulldownPopup;
    private ProjectSelectionPopup _projectSelectionPopup;
    private ProjectsSelectedListener _projectsSelectedListener;
    //ProjectSamples.HelpURL=http://www.janelia.org
    public static final String PROJECT_SAMPLES_HELP_LINK_PROP = "ProjectSamples.HelpURL";

    private static DownloadMetaDataServiceAsync downloadService = (DownloadMetaDataServiceAsync) GWT.create(DownloadMetaDataService.class);

    static {
        ((ServiceDefTarget) downloadService).setServiceEntryPoint("download.oas");
    }

    public SamplesMapPanel(SampleSelectedListener sampleSelectedListener, SampleInfo data, ProjectsSelectedListener projectsSelectedListener) {
        _sampleSelectedListener = sampleSelectedListener;
        _data = data;
        _projectsSelectedListener = projectsSelectedListener;
        init();
    }

    public SamplesMapPanel() {
    }

    private void init() {
        _points = new HashMap<MyMarker, MarkerInfo>();

        HorizontalPanel panel = new HorizontalPanel();
        panel.add(getControlPanel());
        panel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        panel.add(getMapPanel());

        _mainPanel = new TitledBox("Project Samples", /*show/hide ActionLink*/ true);
        TitledBoxActionLinkUtils.addHelpActionLink(_mainPanel, new HelpActionLink("help"), PROJECT_SAMPLES_HELP_LINK_PROP);
        _mainPanel.setWidth("250px");  // min for hidden mode
        _mainPanel.add(panel);

        initWidget(_mainPanel);
    }

    private Widget getMapPanel() {
        _infoLabel = HtmlUtils.getHtml("Loading samples...", "loadingMsgText");
        _detailLabel = HtmlUtils.getHtml("&nbsp;", "text");

        VerticalPanel panel = new VerticalPanel();
        panel.add(_infoLabel);
        panel.add(getMap());
        panel.add(_detailLabel);

        return panel;
    }

    public Widget getLegendPanel() {
        _legendPanel = new SamplesMapLegendPanel();
        return _legendPanel;
    }

    private Widget getControlPanel() {
        _projectSelectionPopup = new ProjectSelectionPopup("Select Projects", new ProjectsSelectedListener() {
            public void onSelect(List<String> selectedProjectNames) {
                setSamples(selectedProjectNames);
            }
        });
        _projectPulldownPopup = new PulldownPopup(_projectSelectionPopup);
        _projectPulldownPopup.setText("All");

        SimplePanel promptPanel = new SimplePanel();
        promptPanel.setStyleName("samplesProjectPromptPanel");
        promptPanel.add(HtmlUtils.getHtml("Projects:&nbsp;", "prompt"));

        HorizontalPanel projectPanel = new HorizontalPanel();
        projectPanel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        projectPanel.add(promptPanel);
        projectPanel.add(_projectPulldownPopup);

        VerticalPanel panel = new VerticalPanel();
        panel.add(projectPanel);
        panel.add(getLegendPanel());

        return panel;
    }

    private Widget getMap() {
        _map = new GoogleMap(/*zoom*/1, /*width*/700, /*height*/400);
        _map.setMapType(MapType.getNormalMap());

        SimplePanel mapPanel = new SimplePanel();
        mapPanel.setStyleName("mapPanelWithMap");
        mapPanel.add(_map);

        return mapPanel;
    }

    // from DataRetrievedListener
    public void onSuccess(Object data) {
        _samples = (Map<String, List<Sample>>) data; // map of project name to List<Sample>

        if (_samples == null || _samples.size() == 0) {
            onNoData();
            return;
        }

        // Add a map marker for each sample, color coded by project
        if (_data.getInitialProjectSymbol() != null) // Have to convert project symbol to name
            retrieveProjectSymbols();
        else
            setSamples();
    }

    // Convert project symbol from URL to project name that matches the sample Map
    public void retrieveProjectSymbols() {
        downloadService.getSymbolToProjectMapping(new AsyncCallback() {
            public void onFailure(Throwable throwable) {
                //TODO: notify failure
            }

            public void onSuccess(Object result) {
                Map<String, Project> projects = (Map<String, Project>) result;
                String selectedProject = null;
                for (Map.Entry entry : projects.entrySet()) {
                    if (entry.getKey().equals(_data.getInitialProjectSymbol()))
                        selectedProject = ((Project) entry.getValue()).getProjectName();
                }
                setSamples(selectedProject);
            }
        });
    }

    private void setSamples(String selectedProjectName) {
        List<String> projects = new ArrayList<String>();
        if (selectedProjectName != null)
            projects.add(selectedProjectName);
        setSamples(projects);
        _projectSelectionPopup.setSelectedProjects(projects);
    }

    private void setSamples() {
        setSamples((List<String>) null);
    }

    private void setSamples(List<String> selectedProjects) {
        _map.removeMarkers();
        _legendPanel.clear();

        // Collect the markers for the selected projects
        final Map<String, Set<Marker>> markers = new HashMap<String, Set<Marker>>();
        int numSamples = 0;
        for (String project : _samples.keySet()) {
            // Only add selected projects (or all if selectedProjects is null)
            if (selectedProjects == null || selectedProjects.size() == 0 || selectedProjects.contains(project)) {
                Set<Marker> projMarkers = new HashSet<Marker>();
                for (Sample sample : _samples.get(project)) {
                    if (sample.getSites() != null && sample.getSites().size() > 0) {
                        numSamples += sample.getSites().size();
                        Site site = sample.getSites().iterator().next();
                        projMarkers.add(createMarker(project, sample, site));
                    }
                }
                markers.put(project, projMarkers);
            }
        }

        // update the labels
        int numProjects = ((selectedProjects == null) ? _samples.size() : selectedProjects.size());
        int numSelectedProjects = ((selectedProjects == null) ? 0 : selectedProjects.size());
        updateInfolabel(numSamples + " matching samples from " + numProjects + " project" + ((numProjects == 0 || numProjects > 1) ? "s" : ""),
                "text");
        _projectPulldownPopup.setText(((selectedProjects == null) ? "All" : selectedProjects.size() + "") + " project" +
                ((numSelectedProjects == 1) ? "" : "s") + " selected");

        // Update the legend and the map markers
        DeferredCommand.addCommand(new Command() {
            public void execute() {
                int markerColor = 1;
                for (Map.Entry<String, Set<Marker>> entry : markers.entrySet())
                    _legendPanel.add(entry.getKey(), markerColor++);
            }
        });

        DeferredCommand.addCommand(new Command() {
            public void execute() {
                _map.addMarkersBulk(markers.values(), /*first color*/ 1, /*color markers*/ true);
            }
        });
    }

    private Marker createMarker(String projectName, Sample sample, Site site) {
        try {
            final Marker marker = new Marker(LatLng.newInstance(site.getLatitudeDouble(), site.getLongitudeDouble()));
            final Widget html = getSiteInfoHtml(projectName, sample, site);

            marker.addMarkerMouseOverHandler(new MarkerMouseOverHandler() {
                public void onMouseOver(MarkerMouseOverEvent mouseOverEvent) {
                    highlightSample(marker, true);
                }
            });
            marker.addMarkerMouseOutHandler(new MarkerMouseOutHandler() {
                public void onMouseOut(MarkerMouseOutEvent mouseOverEvent) {
                    highlightSample(marker, false);
                }
            });
            marker.addMarkerClickHandler(new MarkerClickHandler() {
                public void onClick(MarkerClickEvent clickEvent) {
                    clickEvent.getSender().showMapBlowup(new InfoWindowContent(html));
                }
            });
            _points.put(new MyMarker(marker), new MarkerInfo(projectName, sample, site));

            return marker;
        }
        catch (Throwable t) {
            _logger.error("Caught exception creating Marker: " + t.getMessage());
            return null;
        }
    }

    private void highlightSample(Marker marker, boolean highlight) {
        if (!highlight)
            _detailLabel.setHTML("&nbsp;");
        else {
            MarkerInfo markerInfo = _points.get(new MyMarker(marker));
            if (markerInfo != null && highlight) {
                StringBuffer html = new StringBuffer()
                        .append(new Span(markerInfo.getSample().getSampleTitle(), "prompt").toString());
                if (StringUtils.hasValue(markerInfo.getSite().getHabitatType()))
                    html.append(addDetailValue("Habitat", markerInfo.getSite().getHabitatType()));
                if (StringUtils.hasValue(markerInfo.getSite().getWaterDepth()))
                    html.append(addDetailValue("Water Depth", markerInfo.getSite().getWaterDepth()));
                if (StringUtils.hasValue(markerInfo.getSite().getSampleDepth()))
                    html.append(addDetailValue("Sample Depth", markerInfo.getSite().getSampleDepth()));
                _detailLabel.setHTML(html.toString());
                _legendPanel.highlightItem(markerInfo.getProject(), true);
            }
        }
    }

    private String addDetailValue(String prompt, String value) {

        return
                "&nbsp;&nbsp;&nbsp;&nbsp;" +
                        new Span(prompt, "text SamplesDetailPrompt").toString() +
                        ":&nbsp;" +
                        new Span(value, "text").toString();
    }

    public Widget getSiteInfoHtml(String projectName, final Sample sample, Site site) {
        FlexTable grid = new FlexTable();
        final int MAX_ROWS = 3;
        int row = 0;

        setGridRow(row++, grid, "Project", site.getProject());
        if (StringUtils.hasValue(site.getHabitatType()))
            setGridRow(row++, grid, "Habitat", site.getHabitatType());
        if (StringUtils.hasValue(site.getWaterDepth()))
            setGridRow(row++, grid, "Water Depth", site.getWaterDepth());
        if (row < MAX_ROWS && StringUtils.hasValue(site.getSampleDepth()))
            setGridRow(row++, grid, "Sample Depth", site.getSampleDepth());
        if (row < MAX_ROWS && StringUtils.hasValue(site.getTemperature()))
            setGridRow(row++, grid, "Water Temp", site.getTemperature());
        if (row < MAX_ROWS && StringUtils.hasValue(site.getCountry()))
            setGridRow(row++, grid, "Country", site.getCountry());

        VerticalPanel panel = new VerticalPanel();
        panel.add(HtmlUtils.getHtml(sample.getSampleTitle(), "prompt"));
        panel.add(grid);
        panel.add(HtmlUtils.getHtml("&nbsp;", "smallText"));
        panel.add(new Link("More information", new ClickListener() {
            public void onClick(Widget sender) {
                notifyListener(sample);
            }
        }));

        return panel;
    }

    private void notifyListener(Sample sample) {
        if (_sampleSelectedListener != null)
            _sampleSelectedListener.onSelect(sample);
    }

    private void setGridRow(int row, FlexTable grid, String prompt, String value) {
        grid.setWidget(row, 0, HtmlUtils.getHtml(prompt + ":", "infoPrompt"));
        grid.setWidget(row, 1, HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        grid.setWidget(row, 2, HtmlUtils.getHtml(value, "smallText"));
    }

    public void onFailure(Throwable throwable) {
        updateInfolabel("An error occurred retrieving _samples", "text");
    }

    public void onNoData() {
        updateInfolabel("0 samples found", "loadingMsgText");
    }

    private void updateInfolabel(String msg, String styleName) {
        _infoLabel.setText(msg);
        _infoLabel.setStyleName(styleName);
    }

    public class MyMarker {
        private Marker _marker;

        public MyMarker(Marker marker) {
            _marker = marker;
        }

        public boolean equals(Object other) {
            if (this == other)
                return true;
            if (other == null)
                return false;
            MyMarker that = (MyMarker) other;
            return getMarker().getLatLng().isEquals(that.getMarker().getLatLng());
        }

        public int hashCode() {
            return getMarker().getLatLng().hashCode();
        }

        public Marker getMarker() {
            return _marker;
        }
    }

    public class MarkerInfo {
        private String _project;
        private Sample _sample;
        private Site _site;

        public MarkerInfo(String projectName, Sample sample, Site site) {
            _project = projectName;
            _sample = sample;
            _site = site;
        }

        public String getProject() {
            return _project;
        }

        public Sample getSample() {
            return _sample;
        }

        public Site getSite() {
            return _site;
        }
    }
}
