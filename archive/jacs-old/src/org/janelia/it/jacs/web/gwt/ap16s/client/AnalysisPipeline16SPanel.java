
package org.janelia.it.jacs.web.gwt.ap16s.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.common.AP16QualityThresholds;
import org.janelia.it.jacs.model.genomics.PrimerPair;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.ap16s.AnalysisPipeline16sTask;
import org.janelia.it.jacs.web.gwt.common.client.panel.*;
import org.janelia.it.jacs.web.gwt.common.client.popup.ErrorPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.DataService;
import org.janelia.it.jacs.web.gwt.common.client.service.DataServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.submit.SubmitJob;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.HelpActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.renderers.DoubleParameterRenderer;
import org.janelia.it.jacs.web.gwt.common.client.ui.renderers.ParameterRendererFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FormattedDate;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.StringUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.SystemProps;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Todd Safford
 */
public class AnalysisPipeline16SPanel extends Composite {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.ap16s.client.AnalysisPipeline16SPanel");

    public static final String DEFAULT_UPLOAD_MESSAGE = "File upload successful - click Apply to continue.";
    public static final String UPLOAD_SEQUENCE_NAME_PARAM = "uploadSequenceName";

    private TitledBox _mainPanel;
    private TextBox _jobNameTextBox;
    private org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener _listener;

    private ListBox _referenceDatasetsListBox;

    private RoundedButton _submitButton;
    private RoundedButton _clearButton;
    private LoadingLabel _statusMessage;
    public String _uploadedFilePath;
    public String _uploadedFileSequenceType;
    public String _qualFilePath;
    private ListBox _primerListBox;
    private ListBox _sequencerListBox;
    private TextBox _ampliconStatsTextBox;
    private TextBox _primer1DeflineTextBox;
    private TextBox _primer2DeflineTextBox;
    private TextBox _primer1SequenceTextBox;
    private TextBox _primer2SequenceTextBox;
    private Label _intendedProjectStats;
    private Label _targetOrganism;
    private HorizontalPanel _primerPanel;
    private HorizontalPanel _primerStatsPanel;
    private HorizontalPanel _qualitySettingsPanel;
    private ProjectCodePanel _projectCodePanel;
    private AnalysisPipeline16sTask ap16Task = new AnalysisPipeline16sTask();
    private static DataServiceAsync _dataservice = (DataServiceAsync) GWT.create(DataService.class);
    private String CUSTOM_PRIMERS = "Custom Primers";
    private boolean projectCodeRequired;
    private DoubleParameterRenderer _readLengthMinWidget;
    private DoubleParameterRenderer _minAvQWidget;
    private DoubleParameterRenderer _maxNCountWidget;
    private DoubleParameterRenderer _minIdentCountWidget;
    private FinalOutputDestinationPanel _finalOutputPanel = new FinalOutputDestinationPanel();


    static {
        ((ServiceDefTarget) _dataservice).setServiceEntryPoint("data.srv");
    }

    public AnalysisPipeline16SPanel(String title, org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener listener) {
        _listener = listener;
        init(title);
    }

    private void init(String title) {
        _mainPanel = new TitledBox(title);
        //Wiki.AHP16SHelp=DAS/Small+Sub+Unit+Analysis+Pipeline
        _mainPanel.addActionLink(new HelpActionLink("help", SystemProps.getString("Wiki.Base", "") + SystemProps.getString("Wiki.AHP16SHelp", "")));
        projectCodeRequired = SystemProps.getBoolean("Grid.RequiresProjectCode", false);
        initWidget(_mainPanel);

        popuplateContentPanel();
    }

    private void popuplateContentPanel() {
        _primerPanel = new HorizontalPanel();
        _primerStatsPanel = new HorizontalPanel();
        _qualitySettingsPanel = new HorizontalPanel();
        _primerListBox = new ListBox();
        _sequencerListBox = new ListBox();
        _ampliconStatsTextBox = new TextBox();
        _primer1DeflineTextBox = new TextBox();
        _primer2DeflineTextBox = new TextBox();
        _primer1SequenceTextBox = new TextBox();
        _primer2SequenceTextBox = new TextBox();
        _intendedProjectStats = new Label();
        _targetOrganism = new Label();
        try {
            _readLengthMinWidget = (DoubleParameterRenderer) ParameterRendererFactory.getParameterRenderer(
                    AnalysisPipeline16sTask.PARAM_readLengthMinimum,
                    ap16Task.getParameterVO(AnalysisPipeline16sTask.PARAM_readLengthMinimum), ap16Task);
            _minAvQWidget = (DoubleParameterRenderer) ParameterRendererFactory.getParameterRenderer(
                    AnalysisPipeline16sTask.PARAM_readLengthMinimum,
                    ap16Task.getParameterVO(AnalysisPipeline16sTask.PARAM_readLengthMinimum), ap16Task);
            _maxNCountWidget = (DoubleParameterRenderer) ParameterRendererFactory.getParameterRenderer(
                    AnalysisPipeline16sTask.PARAM_readLengthMinimum,
                    ap16Task.getParameterVO(AnalysisPipeline16sTask.PARAM_readLengthMinimum), ap16Task);
            _minIdentCountWidget = (DoubleParameterRenderer) ParameterRendererFactory.getParameterRenderer(
                    AnalysisPipeline16sTask.PARAM_readLengthMinimum,
                    ap16Task.getParameterVO(AnalysisPipeline16sTask.PARAM_readLengthMinimum), ap16Task);
        }
        catch (Exception ex) {
            _logger.error("Unable to render the AHP 16S gui");
        }
        VerticalPanel contentPanel = new VerticalPanel();
        AnalysisPipeline16STaskOptionsPanel _optionsPanel = new AnalysisPipeline16STaskOptionsPanel();
        _optionsPanel.setStyleName("AdvancedBlastProgramOptionsPanel");
        _optionsPanel.displayParams(ap16Task);

        Grid grid = new Grid((projectCodeRequired) ? 8 : 7, 2);
        grid.setCellSpacing(3);
        int tmpIndex = 0;
        grid.setWidget(tmpIndex, 0, new HTMLPanel("<span class='prompt'>Job Name:</span><span class='requiredInformation'>*</span>"));
        grid.setWidget(tmpIndex, 1, getJobNameWidget());

        grid.setWidget(++tmpIndex, 0, new HTMLPanel("<span class='prompt'>FASTA/Fragment File:</span><span class='requiredInformation'>*</span>"));
        grid.setWidget(tmpIndex, 1, getUploadPanel());

        grid.setWidget(++tmpIndex, 0, HtmlUtils.getHtml("File of QV's (Qual File):", "prompt"));
        grid.setWidget(tmpIndex, 1, getQualFilePanel());

        grid.setWidget(++tmpIndex, 0, new HTMLPanel("<span class='prompt'>Reference Dataset:</span><span class='requiredInformation'>*</span>"));
        grid.setWidget(tmpIndex, 1, getReferenceDatasetWidget());

        grid.setWidget(++tmpIndex, 0, getPrimerLabelWidget());
        grid.setWidget(tmpIndex, 1, getPrimerSequenceWidget());
        grid.getCellFormatter().setVerticalAlignment(tmpIndex, 0, VerticalPanel.ALIGN_TOP);

        grid.setWidget(++tmpIndex, 0, getSequencerLabelWidget());
        grid.setWidget(tmpIndex, 1, getSequencerWidget());
        grid.getCellFormatter().setVerticalAlignment(tmpIndex, 0, VerticalPanel.ALIGN_TOP);

        grid.setWidget(++tmpIndex, 0, HtmlUtils.getHtml("Output Destination:", "prompt"));
        grid.setWidget(tmpIndex, 1, _finalOutputPanel);

        if (projectCodeRequired) {
            _projectCodePanel = new ProjectCodePanel();
            grid.setWidget(++tmpIndex, 0, new HTMLPanel("<span class='prompt'>Project Code:</span><span class='requiredInformation'>*</span>"));
            grid.setWidget(tmpIndex, 1, _projectCodePanel);
        }

        createButtons();
        contentPanel.add(grid);
        contentPanel.add(HtmlUtils.getHtml("&nbsp", "spacer"));
        contentPanel.add(_optionsPanel);
        contentPanel.add(HtmlUtils.getHtml("&nbsp", "spacer"));
        contentPanel.add(getSubmitButtonPanel());
        contentPanel.add(getStatusMessage());

        _mainPanel.add(contentPanel);
        _primerListBox.setItemSelected(0, true);
        setPrimerValues();
        _sequencerListBox.setItemSelected(0, true);
        setQualityValues();
        _submitButton.setEnabled(false);
    }

    private Widget getJobNameWidget() {
        _jobNameTextBox = new TextBox();
        _jobNameTextBox.setMaxLength(64);
        _jobNameTextBox.setVisibleLength(64);
        updateJobNameWidget();

        return _jobNameTextBox;
    }

    private Panel getUploadPanel() {
        ArrayList<FileChooserPanel.FILE_TYPE> types = new ArrayList<FileChooserPanel.FILE_TYPE>();
        types.add(FileChooserPanel.FILE_TYPE.mpfa);
        types.add(FileChooserPanel.FILE_TYPE.seq);
        types.add(FileChooserPanel.FILE_TYPE.ffn);
        types.add(FileChooserPanel.FILE_TYPE.fa);
        types.add(FileChooserPanel.FILE_TYPE.faa);
        types.add(FileChooserPanel.FILE_TYPE.fna);
        types.add(FileChooserPanel.FILE_TYPE.fsa);
        types.add(FileChooserPanel.FILE_TYPE.fasta);
        types.add(FileChooserPanel.FILE_TYPE.frg);
        return new FileChooserPanel(new SelectionListener() {
            public void onSelect(String value) {
                _uploadedFilePath = value;
                _submitButton.setEnabled(true);
            }

            public void onUnSelect(String value) {
                _uploadedFilePath = null;
                _submitButton.setEnabled(false);
            }
        }, types);
    }

    private Panel getQualFilePanel() {
        ArrayList<FileChooserPanel.FILE_TYPE> types = new ArrayList<FileChooserPanel.FILE_TYPE>();
        types.add(FileChooserPanel.FILE_TYPE.qual);
        types.add(FileChooserPanel.FILE_TYPE.qv);
        return new FileChooserPanel(new SelectionListener() {
            public void onSelect(String value) {
                _qualFilePath = value;
            }

            public void onUnSelect(String value) {
                _qualFilePath = null;
            }
        }, types);
    }

    private Widget getReferenceDatasetWidget() {
        _referenceDatasetsListBox = new ListBox();
        for (String tmpDataset : ap16Task.getSubjectDatabaseList()) {
            _referenceDatasetsListBox.addItem(tmpDataset);
        }

        final HorizontalPanel panel = new HorizontalPanel();
        panel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        panel.add(_referenceDatasetsListBox);
        panel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));

        return panel;
    }

    private void updateJobNameWidget() {
        _jobNameTextBox.setText("My AHP Analysis Pipeline job " + new FormattedDate().toString());
    }

    private void createButtons() {
        _clearButton = new RoundedButton("Clear", new ClickListener() {
            public void onClick(Widget sender) {
                clear();
            }
        });

        _submitButton = new RoundedButton("Submit Job", new ClickListener() {
            public void onClick(Widget sender) {
                submitJob();
            }
        });
        _submitButton.setEnabled(false);
    }

    private Widget getSubmitButtonPanel() {
        HorizontalPanel panel = new HorizontalPanel();
        panel.add(_clearButton);
        panel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        panel.add(_submitButton);

        return new CenteredWidgetHorizontalPanel(panel);
    }

    private void clear() {
        _mainPanel.clear();
        ap16Task = new AnalysisPipeline16sTask();
        popuplateContentPanel();
    }

    private void submitJob() {
        // Validate job name
        if (!StringUtils.hasValue(_jobNameTextBox.getText())) {
            new PopupCenteredLauncher(new ErrorPopupPanel("A job name is required.")).showPopup(null);
            return;
        }
        if (projectCodeRequired) {
            if (!_projectCodePanel.isCurrentProjectCodeValid()) {
                new PopupCenteredLauncher(new ErrorPopupPanel("A valid project code is required.")).showPopup(null);
                return;
            }
        }
        ap16Task.setParameter(AnalysisPipeline16sTask.PARAM_subjectDatabase,
                _referenceDatasetsListBox.getItemText(_referenceDatasetsListBox.getSelectedIndex()));
        // submit the job - to be safer might need to instantiate a fresh task object here
        ap16Task.setJobName(_jobNameTextBox.getText());
        if (projectCodeRequired) {
            String projectCode = _projectCodePanel.getProjectCode();
            if (!_projectCodePanel.isCurrentProjectCodeValid()) {
                new PopupCenteredLauncher(new ErrorPopupPanel("A valid project code is required.")).showPopup(null);
                return;
            }
            ap16Task.setParameter(Task.PARAM_project, projectCode);
        }
        ap16Task.setParameter(AnalysisPipeline16sTask.PARAM_fragmentFiles, _uploadedFilePath);
        ap16Task.setParameter(AnalysisPipeline16sTask.PARAM_qualFile, _qualFilePath);
        // Get the primers
        // if custom, set the values accordingly
        if (CUSTOM_PRIMERS.equals(_primerListBox.getItemText(_primerListBox.getSelectedIndex()))) {
            ap16Task.setParameter(AnalysisPipeline16sTask.PARAM_ampliconSize, _ampliconStatsTextBox.getText());
            ap16Task.setParameter(AnalysisPipeline16sTask.PARAM_primer1Defline, _primer1DeflineTextBox.getText());
            ap16Task.setParameter(AnalysisPipeline16sTask.PARAM_primer1Sequence, _primer1SequenceTextBox.getText());
            ap16Task.setParameter(AnalysisPipeline16sTask.PARAM_primer2Defline, _primer2DeflineTextBox.getText());
            ap16Task.setParameter(AnalysisPipeline16sTask.PARAM_primer2Sequence, _primer2SequenceTextBox.getText());
        }
        else {
            PrimerPair tmpPair = AnalysisPipeline16sTask.getPrimerList().get(_primerListBox.getSelectedIndex());
            ap16Task.setParameter(AnalysisPipeline16sTask.PARAM_ampliconSize, tmpPair.getAmpliconSize());
            ap16Task.setParameter(AnalysisPipeline16sTask.PARAM_primer1Defline, tmpPair.getPrimer1Id());
            ap16Task.setParameter(AnalysisPipeline16sTask.PARAM_primer1Sequence, tmpPair.getPrimer1Sequence5to3());
            ap16Task.setParameter(AnalysisPipeline16sTask.PARAM_primer2Defline, tmpPair.getPrimer2Id());
            ap16Task.setParameter(AnalysisPipeline16sTask.PARAM_primer2Sequence, tmpPair.getPrimer2Sequence5to3());
        }
        ap16Task.setParameter(AnalysisPipeline16sTask.PARAM_readLengthMinimum, _readLengthMinWidget.getValueObject().getStringValue());
        ap16Task.setParameter(AnalysisPipeline16sTask.PARAM_minAvgQV, _minAvQWidget.getValueObject().getStringValue());
        ap16Task.setParameter(AnalysisPipeline16sTask.PARAM_maxNCount, _maxNCountWidget.getValueObject().getStringValue());
        ap16Task.setParameter(AnalysisPipeline16sTask.PARAM_minIdentCount, _minIdentCountWidget.getValueObject().getStringValue());
        Long ampliconSize = Long.valueOf(ap16Task.getParameter(AnalysisPipeline16sTask.PARAM_ampliconSize));
        // If the amplicons are small then force the cd-hit-est clustering
        if (300 > ampliconSize) {
            ap16Task.setParameter(AnalysisPipeline16sTask.PARAM_iterateCdHitESTClustering, Boolean.TRUE.toString());
        }
        if (_finalOutputPanel.overrideFinalOutputPath()) {
            ap16Task.setParameter(Task.PARAM_finalOutputDirectory, _finalOutputPanel.getFinalOutputDestination());
        }
        _submitButton.setEnabled(false);
        _statusMessage.showSubmittingMessage();
        new SubmitJob(ap16Task, new JobSubmissionListener()).runJob();
        clear();
    }

    public VerticalPanel getPrimerLabelWidget() {
        VerticalPanel tmpPanel = new VerticalPanel();
        tmpPanel.setHeight("100%");
        tmpPanel.setVerticalAlignment(VerticalPanel.ALIGN_TOP);
        tmpPanel.add(new HTMLPanel("<span class='prompt'>Primer Sequences For Trimming:</span><span class='requiredInformation'>*</span>"));
        return tmpPanel;
    }

    public Widget getPrimerSequenceWidget() {
        VerticalPanel panel = new VerticalPanel();
        _ampliconStatsTextBox.setVisibleLength(15);
        _primer1DeflineTextBox.setVisibleLength(15);
        _primer2DeflineTextBox.setVisibleLength(15);
        _primer1SequenceTextBox.setVisibleLength(40);
        _primer2SequenceTextBox.setVisibleLength(40);
        _intendedProjectStats.setVisible(true);
        _intendedProjectStats.setStyleName("prompt");
        _targetOrganism.setVisible(true);
        _targetOrganism.setStyleName("prompt");

        Grid grid = new Grid(4, 4);
        grid.setCellSpacing(3);

        grid.setWidget(0, 0, HtmlUtils.getHtml("Target Organism:", "prompt"));
        grid.setWidget(0, 1, _targetOrganism);

        grid.setWidget(0, 2, HtmlUtils.getHtml("Intended Project:", "prompt"));
        grid.setWidget(0, 3, _intendedProjectStats);

        grid.setWidget(1, 0, HtmlUtils.getHtml("Amplicon Size:", "prompt"));
        grid.setWidget(1, 1, _ampliconStatsTextBox);

        grid.setWidget(2, 0, HtmlUtils.getHtml("Primer 1 Defline:", "prompt"));
        grid.setWidget(2, 1, _primer1DeflineTextBox);

        grid.setWidget(3, 0, HtmlUtils.getHtml("Primer 1 Sequence:", "prompt"));
        grid.setWidget(3, 1, _primer1SequenceTextBox);

        grid.setWidget(2, 2, HtmlUtils.getHtml("Primer 2 Defline:", "prompt"));
        grid.setWidget(2, 3, _primer2DeflineTextBox);

        grid.setWidget(3, 2, HtmlUtils.getHtml("Primer 2 Sequence:", "prompt"));
        grid.setWidget(3, 3, _primer2SequenceTextBox);
        _primerPanel.add(grid);

        _primerListBox.addChangeListener(new ChangeListener() {
            public void onChange(Widget sender) {
                setPrimerValues();
            }
        });

        // Populate the list box values
        List<PrimerPair> primerMap = AnalysisPipeline16sTask.getPrimerList();
        for (PrimerPair pair : primerMap) {
            _primerListBox.addItem(pair.getName());
        }
        _primerListBox.addItem(CUSTOM_PRIMERS);

        // Build the panel
        panel.add(_primerListBox);
        panel.add(_primerStatsPanel);
        panel.add(_primerPanel);
        return panel;
    }

    private void setPrimerValues() {
        if (CUSTOM_PRIMERS.equals(_primerListBox.getItemText(_primerListBox.getSelectedIndex()))) {
            _ampliconStatsTextBox.setEnabled(true);
            _primer1DeflineTextBox.setEnabled(true);
            _primer2DeflineTextBox.setEnabled(true);
            _primer1SequenceTextBox.setEnabled(true);
            _primer2SequenceTextBox.setEnabled(true);
            _targetOrganism.setText("--");
            _ampliconStatsTextBox.setText("");
            _intendedProjectStats.setText("--");
            _primer1DeflineTextBox.setText("1f Custom Primer");
            _primer2DeflineTextBox.setText("2r Custom Primer");
            _primer1SequenceTextBox.setText("");
            _primer2SequenceTextBox.setText("");
        }
        else {
            PrimerPair tmpPair = AnalysisPipeline16sTask.getPrimerList().get(_primerListBox.getSelectedIndex());
            _ampliconStatsTextBox.setEnabled(false);
            _primer1DeflineTextBox.setEnabled(false);
            _primer2DeflineTextBox.setEnabled(false);
            _primer1SequenceTextBox.setEnabled(false);
            _primer2SequenceTextBox.setEnabled(false);
            _targetOrganism.setText(tmpPair.getOrganismType());
            _ampliconStatsTextBox.setText(tmpPair.getAmpliconSize());
            _intendedProjectStats.setText(tmpPair.getIntendedProject());
            _primer1DeflineTextBox.setText(tmpPair.getPrimer1Id());
            _primer2DeflineTextBox.setText(tmpPair.getPrimer2Id());
            _primer1SequenceTextBox.setText(tmpPair.getPrimer1Sequence5to3());
            _primer2SequenceTextBox.setText(tmpPair.getPrimer2Sequence5to3());
        }
    }

    public VerticalPanel getSequencerLabelWidget() {
        VerticalPanel tmpPanel = new VerticalPanel();
        tmpPanel.setHeight("100%");
        tmpPanel.setVerticalAlignment(VerticalPanel.ALIGN_TOP);
        tmpPanel.add(new HTMLPanel("<span class='prompt'>Quality Settings:</span><span class='requiredInformation'>*</span>"));
        return tmpPanel;
    }

    public Widget getSequencerWidget() {
        VerticalPanel panel = new VerticalPanel();
        Grid grid = new Grid(2, 4);
        grid.setCellSpacing(3);

        grid.setWidget(0, 0, HtmlUtils.getHtml("Read Length Minimum:", "prompt"));
        grid.setWidget(0, 1, _readLengthMinWidget);

        grid.setWidget(1, 0, HtmlUtils.getHtml("Minimum Average Quality Value:", "prompt"));
        grid.setWidget(1, 1, _minAvQWidget);

        grid.setWidget(0, 2, HtmlUtils.getHtml("Maximum N-count In A Read:", "prompt"));
        grid.setWidget(0, 3, _maxNCountWidget);

        grid.setWidget(1, 2, HtmlUtils.getHtml("Minimum Identity Count In 16S Hit:", "prompt"));
        grid.setWidget(1, 3, _minIdentCountWidget);

        _qualitySettingsPanel.add(grid);

        _sequencerListBox.addChangeListener(new ChangeListener() {
            public void onChange(Widget sender) {
                setQualityValues();
            }
        });

        // Populate the list box values
        List<AP16QualityThresholds> sequencerMap = AnalysisPipeline16sTask.getSequencerList();
        for (AP16QualityThresholds setting : sequencerMap) {
            _sequencerListBox.addItem(setting.getType());
        }

        // Build the panel
        panel.add(_sequencerListBox);
        panel.add(_qualitySettingsPanel);
        return panel;
    }

    private void setQualityValues() {
        AP16QualityThresholds tmpSetting = AnalysisPipeline16sTask.getSequencerList().get(_sequencerListBox.getSelectedIndex());
        _readLengthMinWidget.setValue(tmpSetting.getReadLengthMinimum());
        _minAvQWidget.setValue(tmpSetting.getMinAvgQV());
        _maxNCountWidget.setValue(tmpSetting.getMaxNCount());
        _minIdentCountWidget.setValue(tmpSetting.getMinIdentCount());
    }

    private class JobSubmissionListener implements org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener {
        public void onFailure(Throwable caught) {
            _submitButton.setEnabled(true);
            _statusMessage.showFailureMessage();
        }

        public void onSuccess(String jobId) {
            _submitButton.setEnabled(true);
            _statusMessage.showSuccessMessage();
            _listener.onSuccess(jobId);
        }
    }

    private Widget getStatusMessage() {
        _statusMessage = new LoadingLabel();
        _statusMessage.setHTML("&nbsp;");
        _statusMessage.addStyleName("AdvancedBlastStatusLabel");

        return new CenteredWidgetHorizontalPanel(_statusMessage);
    }

}