
package org.janelia.it.jacs.web.gwt.prokAnnot.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.tasks.prokAnnotation.ProkAnnotationDirectoryUpdateTask;
import org.janelia.it.jacs.model.tasks.prokAnnotation.ProkAnnotationLocalDirectoryImportTask;
import org.janelia.it.jacs.model.tasks.prokAnnotation.ProkaryoticAnnotationTask;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener;
import org.janelia.it.jacs.web.gwt.common.client.panel.CenteredWidgetHorizontalPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.popup.ErrorPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.DataService;
import org.janelia.it.jacs.web.gwt.common.client.service.DataServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.service.StatusService;
import org.janelia.it.jacs.web.gwt.common.client.service.StatusServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.HelpActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.suggest.BaseSuggestOracle;
import org.janelia.it.jacs.web.gwt.common.client.ui.suggest.MatchesAnywhereSuggestOracle;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.SystemProps;
import org.janelia.it.jacs.web.gwt.prokAnnot.client.panel.ProkGenomeStatusPanel;
import org.janelia.it.jacs.web.gwt.prokAnnot.client.popup.CustomProkCommandPopup;
import org.janelia.it.jacs.web.gwt.prokAnnot.client.popup.LoadAnnotationServiceGenomePopup;
import org.janelia.it.jacs.web.gwt.prokAnnot.client.popup.NCBILoadGenomePopup;
import org.janelia.it.jacs.web.gwt.prokAnnot.client.popup.ProkAnnotationPopup;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Todd Safford
 */
public class ProkAnnotationPanel extends Composite {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.prokAnnot.client.ProkAnnotationPanel");
    public static final String DIRECTORY_REFRESH = "Refresh Directory List";

    private static final String ACTION_SELECT = "-- Select An Action --";
    private static final String ACTION_DIVIDER = "----------------------";
    private static final String ACTION_LOAD_NCBI = "Load Completed Genome Data From NCBI";
    private static final String ACTION_LOAD_NCBI_BULK = "Bulk Load Completed Genome Data From NCBI";
    private static final String ACTION_RUN_NCBI_GENOME = "Run NCBI Genome Pipeline";
    private static final String ACTION_RUN_NCBI_GENOME_BULK = "Bulk Run NCBI Genome Pipeline";
    private static final String ACTION_RUN_AS_GENOME = "Run Annotation Service Genome Pipeline";
    private static final String ACTION_REQUEST_NEW_DB = "Request New Genome Database and Drive Location";
    private static final String ACTION_RUN_JCVI = "Run JCVI Genome Pipeline";
    private static final String ACTION_RUN_JCVI_BULK = "Bulk Run JCVI Genome Pipeline";
    private static final String ACTION_LOAD_ANNOTATION_SERVICE_DATA = "Load Annotation Service Data";
    private static final String ACTION_RUN_SPECIFIC = "Run Specific Pipeline Actions";
    private static final String ACTION_RUN_CUSTOM_COMMAND = "Run Specific Perl Script Command Line";
//    private static final String ACTION_CLEAR = "Clear Genome Database";

    private RoundedButton _getStatusButton;
    private RoundedButton _refreshDirectoryButton;
    private ListBox _actionListBox;
    private JobSubmissionListener _listener;
    private SuggestBox _suggestBox;
    private BaseSuggestOracle _suggestOracle = new MatchesAnywhereSuggestOracle();
    private ProkGenomeStatusPanel _genomeStatusPanel;
    private static StatusServiceAsync _statusservice = (StatusServiceAsync) GWT.create(StatusService.class);
    private static DataServiceAsync _dataservice = (DataServiceAsync) GWT.create(DataService.class);

    static {
        ((ServiceDefTarget) _statusservice).setServiceEntryPoint("status.srv");
        ((ServiceDefTarget) _dataservice).setServiceEntryPoint("data.srv");
    }

    public ProkAnnotationPanel(String title, JobSubmissionListener listener) {
        this._listener = listener;
        TitledBox _mainPanel = new TitledBox(title);
        _mainPanel.removeActionLinks();
        //Wiki.ProkPipelineHelp=VISW/Prokaryotic+Pipeline
        _mainPanel.addActionLink(new HelpActionLink("help", SystemProps.getString("Wiki.Base", "") +
                SystemProps.getString("Wiki.ProkPipelineHelp", "")));
        //Jira.Base=http://issuetracker/
        //Jira.ProkPipeline=browse/PROK
        _mainPanel.addActionLink(new HelpActionLink("report issues/requests", SystemProps.getString("Jira.Base", "") +
                SystemProps.getString("Jira.ProkPipeline", "")));
        initWidget(_mainPanel);

        // will populateContentPanel() after loading data
        _mainPanel.add(popuplateContentPanel());
    }

    private Panel popuplateContentPanel() {
        VerticalPanel contentPanel = new VerticalPanel();
        _genomeStatusPanel = new ProkGenomeStatusPanel("");

        _actionListBox = new ListBox();
        _actionListBox.addItem(ACTION_SELECT);
        _actionListBox.addItem(ACTION_LOAD_NCBI);
        _actionListBox.addItem(ACTION_RUN_NCBI_GENOME);
        _actionListBox.addItem(ACTION_LOAD_NCBI_BULK);
        _actionListBox.addItem(ACTION_RUN_NCBI_GENOME_BULK);
        _actionListBox.addItem(ACTION_DIVIDER);
        _actionListBox.addItem(ACTION_LOAD_ANNOTATION_SERVICE_DATA);
        _actionListBox.addItem(ACTION_RUN_AS_GENOME);
        _actionListBox.addItem(ACTION_DIVIDER);
        _actionListBox.addItem(ACTION_RUN_JCVI);
        _actionListBox.addItem(ACTION_RUN_JCVI_BULK);
        _actionListBox.addItem(ACTION_DIVIDER);
        _actionListBox.addItem(ACTION_REQUEST_NEW_DB);
        _actionListBox.addItem(ACTION_RUN_SPECIFIC);
        _actionListBox.addItem(ACTION_RUN_CUSTOM_COMMAND);
//        _actionListBox.addItem(ACTION_CLEAR);
        _actionListBox.setItemSelected(0, true);
        _actionListBox.addChangeListener(new ChangeListener() {
            public void onChange(Widget sender) {
                String selection = _actionListBox.getItemText(_actionListBox.getSelectedIndex());
                if (!_suggestOracle.hasItem(_suggestBox.getText()) &&
                        !(ACTION_REQUEST_NEW_DB.equals(selection) ||
                                ACTION_DIVIDER.equals(selection) ||
                                ACTION_LOAD_NCBI_BULK.equals(selection) ||
                                ACTION_RUN_NCBI_GENOME_BULK.equals(selection) ||
                                ACTION_RUN_JCVI_BULK.equals(selection))) {
                    new PopupCenteredLauncher(new ErrorPopupPanel("A valid Organism Directory must be supplied."), 250).showPopup(_getStatusButton);
                    _actionListBox.setSelectedIndex(0);
                    return;
                }
                if (ACTION_SELECT.equals(selection) || ACTION_DIVIDER.equals(selection)) {/* Do nothing */}
                else if (ACTION_LOAD_NCBI.equals(selection)) {
                    new PopupCenteredLauncher(new NCBILoadGenomePopup(_suggestBox.getText(), false,
                            _listener), 250).showPopup(_actionListBox);
                }
                else if (ACTION_LOAD_NCBI_BULK.equals(selection)) {
                    new PopupCenteredLauncher(new NCBILoadGenomePopup(_suggestBox.getText(), true,
                            _listener), 250).showPopup(_actionListBox);
                }
                else if (ACTION_LOAD_ANNOTATION_SERVICE_DATA.equals(selection)) {
                    new PopupCenteredLauncher(new LoadAnnotationServiceGenomePopup(_suggestBox.getText(),
                            _listener), 250).showPopup(_actionListBox);
                }
                else if (ACTION_RUN_NCBI_GENOME.equals(selection)) {
                    // run the pipeline with the completed settings
                    new PopupCenteredLauncher(new ProkAnnotationPopup(_suggestBox.getText(),
                            ProkaryoticAnnotationTask.ncbiSectionFlags, false,
                            _listener, ProkaryoticAnnotationTask.MODE_CMR_GENOME), 250).showPopup(_actionListBox);
                }
                else if (ACTION_RUN_NCBI_GENOME_BULK.equals(selection)) {
                    // run the pipeline with the completed settings
                    new PopupCenteredLauncher(new ProkAnnotationPopup(_suggestBox.getText(),
                            ProkaryoticAnnotationTask.ncbiSectionFlags, true,
                            _listener, ProkaryoticAnnotationTask.MODE_CMR_GENOME), 250).showPopup(_actionListBox);
                }
                else if (ACTION_RUN_AS_GENOME.equals(selection)) {
                    // run the pipeline with the completed settings
                    new PopupCenteredLauncher(new ProkAnnotationPopup(_suggestBox.getText(),
                            ProkaryoticAnnotationTask.annotationServiceSectionFlags, false,
                            _listener, ProkaryoticAnnotationTask.MODE_ANNOTATION_SERVICE), 250).showPopup(_actionListBox);
                }
                else if (ACTION_RUN_JCVI.equals(selection)) {
                    // run the pipeline with the completed settings
                    new PopupCenteredLauncher(new ProkAnnotationPopup(_suggestBox.getText(),
                            ProkaryoticAnnotationTask.jcviGenomeSectionFlags, false,
                            _listener, ProkaryoticAnnotationTask.MODE_JCVI_GENOME), 250).showPopup(_actionListBox);
                }
                else if (ACTION_RUN_JCVI_BULK.equals(selection)) {
                    // run the pipeline with the completed settings
                    new PopupCenteredLauncher(new ProkAnnotationPopup(_suggestBox.getText(),
                            ProkaryoticAnnotationTask.jcviGenomeSectionFlags, true,
                            _listener, ProkaryoticAnnotationTask.MODE_JCVI_GENOME), 250).showPopup(_actionListBox);
                }
                else if (ACTION_RUN_SPECIFIC.equals(selection)) {
                    // run the pipeline with the completed settings
                    new PopupCenteredLauncher(new ProkAnnotationPopup(_suggestBox.getText(),
                            new ArrayList<String>(), false, _listener, ProkaryoticAnnotationTask.MODE_ANNOTATION_SERVICE), 250)
                            .showPopup(_actionListBox);
                }
                else if (ACTION_RUN_CUSTOM_COMMAND.equals(selection)) {
                    // run the pipeline with the completed settings
                    new PopupCenteredLauncher(new CustomProkCommandPopup(_suggestBox.getText(), _listener), 250)
                            .showPopup(_actionListBox);
                }
                else if (ACTION_REQUEST_NEW_DB.equals(selection)) {
                    Window.open(SystemProps.getString("ProkAnnotation.NewDatabaseRequestURL", ""), "_other", "");
                }
                else {
                    Window.alert("Action \"" + selection + "\" not currently defined. \nSorry.");
                }
                _actionListBox.setSelectedIndex(0);
            }
        });
        _actionListBox.setVisible(true);
        _getStatusButton = new RoundedButton("Get Status >", new ClickListener() {
            public void onClick(Widget widget) {
                //Pull up organism info here.
                _actionListBox.setEnabled(true);
                _genomeStatusPanel.setGenome(_suggestBox.getText().trim());
            }
        });
        _refreshDirectoryButton = new RoundedButton(DIRECTORY_REFRESH, new ClickListener() {
            public void onClick(Widget widget) {
                _refreshDirectoryButton.setText("Refreshing...");
                _refreshDirectoryButton.setEnabled(false);
                ProkAnnotationDirectoryUpdateTask updateTask = new ProkAnnotationDirectoryUpdateTask();
                _dataservice.submitJob(updateTask, new AsyncCallback() {
                    public void onFailure(Throwable throwable) {
                        _refreshDirectoryButton.setText(DIRECTORY_REFRESH);
                        _refreshDirectoryButton.setEnabled(true);
                        new PopupCenteredLauncher(new ErrorPopupPanel("There was a problem updating the directories."), 250).showPopup(_refreshDirectoryButton);
                    }

                    public void onSuccess(Object o) {
                        _refreshDirectoryButton.setText(DIRECTORY_REFRESH);
                        _refreshDirectoryButton.setEnabled(true);
                        getSuggestionBox();
                    }
                });
            }
        });
        Grid grid = new Grid(2, 2);
        grid.setCellSpacing(3);
        HorizontalPanel tmpPanel = new HorizontalPanel();
        tmpPanel.add(getSuggestionBox());
        tmpPanel.add(HtmlUtils.getHtml("&nbsp;", "text"));
        tmpPanel.add(_getStatusButton);
        tmpPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        tmpPanel.add(_refreshDirectoryButton);

        grid.setWidget(0, 0, HtmlUtils.getHtml("Organism Directory:", "prompt"));
        grid.setWidget(0, 1, tmpPanel);

        grid.setWidget(1, 0, HtmlUtils.getHtml("Action:", "prompt"));
        grid.setWidget(1, 1, _actionListBox);

        contentPanel.add(HtmlUtils.getHtml("Please provide the following system information.", "prompt"));
        contentPanel.add(grid);
        contentPanel.add(getStatusMessage());
        contentPanel.add(_genomeStatusPanel);
        return contentPanel;
    }

    private Widget getStatusMessage() {
        LoadingLabel _statusMessage = new LoadingLabel();
        _statusMessage.setHTML("&nbsp;");
        _statusMessage.addStyleName("AdvancedBlastStatusLabel");

        return new CenteredWidgetHorizontalPanel(_statusMessage);
    }

    private Widget getSuggestionBox() {
        if (null == _suggestBox) {
            _suggestBox = new SuggestBox(_suggestOracle);
        }
        _statusservice.getSystemTaskNamesByClass(ProkAnnotationLocalDirectoryImportTask.class.getName(), new AsyncCallback() {
            public void onFailure(Throwable caught) {
                _logger.error("Error retrieving system ProkAnnotationImportDataTask names: " + ((caught == null) ? "" : caught.getMessage()));
            }

            public void onSuccess(Object result) {
                _suggestBox.setText("");
                _suggestOracle.removeAll();
                _suggestOracle.addAll((List) result);
            }
        });

        return _suggestBox;
    }

}