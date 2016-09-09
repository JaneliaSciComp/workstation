
package org.janelia.it.jacs.web.gwt.barcode.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.barcodeDesigner.BarcodeDesignerTask;
import org.janelia.it.jacs.web.gwt.common.client.panel.*;
import org.janelia.it.jacs.web.gwt.common.client.popup.ErrorPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.DataService;
import org.janelia.it.jacs.web.gwt.common.client.service.DataServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.submit.SubmitJob;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ExternalLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.HelpActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FormattedDate;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.StringUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.SystemProps;

import java.util.ArrayList;

/**
 * @author Todd Safford
 *         todo All of these generic panels need to have a common base.  All the listeners could be shared
 */
public class BarCodeDesignerPanel extends Composite {
    public static final String DEFAULT_UPLOAD_MESSAGE = "File upload successful - click Apply to continue.";
    public static final String UPLOAD_SEQUENCE_NAME_PARAM = "uploadSequenceName";

    private TitledBox _mainPanel;
    private TextBox _jobNameTextBox;
    private RoundedButton _submitButton;
    private RoundedButton _clearButton;
    private LoadingLabel _statusMessage;
    private FileChooserPanel _primerFileChooserPanel;
    final FormPanel _uploadPrimerFastaForm = new FormPanel();
    public String _uploadedPrimerFilePath;
    public String _uploadedPrimerFileSequenceType;

    private FileChooserPanel _ampliconFileChooserPanel;
    final FormPanel _uploadAmpliconFastaForm = new FormPanel();
    public String _uploadedAmpliconFilePath;
    public String _uploadedAmpliconFileSequenceType;

    private boolean projectCodeRequired;
    private FinalOutputDestinationPanel _finalOutputPanel = new FinalOutputDestinationPanel();
    private BarcodeDesignerTaskOptionsPanel _optionsPanel;
    private ProjectCodePanel _projectCodePanel;
    private BarcodeDesignerTask _barcodeTask = new BarcodeDesignerTask();
    private org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener _listener;
    private static DataServiceAsync _dataservice = (DataServiceAsync) GWT.create(DataService.class);


    static {
        ((ServiceDefTarget) _dataservice).setServiceEntryPoint("data.srv");
    }

    public BarCodeDesignerPanel(String title, org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener listener) {
        _listener = listener;
        init(title);
    }

    private void init(String title) {
        _mainPanel = new TitledBox(title);
        //Wiki.BarcodeHelp=DAS/Automatic+Bar+Code+Designer+and+Deconvolution
        _mainPanel.addActionLink(new HelpActionLink("help", SystemProps.getString("Wiki.Base", "") +
                SystemProps.getString("Wiki.BarcodeHelp", "")));
        projectCodeRequired = SystemProps.getBoolean("Grid.RequiresProjectCode", false);
        initWidget(_mainPanel);
        popuplateContentPanel();
    }

    private void popuplateContentPanel() {
        VerticalPanel contentPanel = new VerticalPanel();
        _optionsPanel = new BarcodeDesignerTaskOptionsPanel();
        _optionsPanel.setStyleName("AdvancedBlastProgramOptionsPanel");
        _optionsPanel.displayParams(_barcodeTask);


        Grid grid = new Grid((projectCodeRequired) ? 6 : 5, 2);
        grid.setCellSpacing(3);
        int tmpIndex = 0;
        grid.setWidget(tmpIndex, 0, new HTMLPanel("<span class='prompt'>Job Name:</span><span class='requiredInformation'>*</span>"));
        grid.setWidget(tmpIndex, 1, getJobNameWidget());

        grid.setWidget(++tmpIndex, 0, new HTMLPanel("<span class='prompt'>Primer FASTA File:</span><span class='requiredInformation'>*</span>"));
        grid.setWidget(tmpIndex, 1, getPrimerUploadPanel());

        grid.setWidget(++tmpIndex, 0, new HTMLPanel("<span class='prompt'>Amplicon FASTA File:</span><span class='requiredInformation'>*</span>"));
        grid.setWidget(tmpIndex, 1, getAmpliconUploadPanel());

        grid.getCellFormatter().setVerticalAlignment(3, 0, VerticalPanel.ALIGN_TOP);

        grid.setWidget(++tmpIndex, 0, new HTMLPanel("<span class='prompt'>Program:</span>"));
        grid.setWidget(tmpIndex, 1, HtmlUtils.getHtml(BarcodeDesignerTask.DISPLAY_NAME, "prompt"));

        grid.setWidget(++tmpIndex, 0, HtmlUtils.getHtml("Output Destination:", "prompt"));
        grid.setWidget(tmpIndex, 1, _finalOutputPanel);

        if (projectCodeRequired) {
            _projectCodePanel = new ProjectCodePanel();
            grid.setWidget(++tmpIndex, 0, new HTMLPanel("<span class='prompt'>Project Code:</span><span class='requiredInformation'>*</span>"));
            grid.setWidget(tmpIndex, 1, _projectCodePanel);
        }

        createButtons();
        HorizontalPanel externalLinkPanel = new HorizontalPanel();
        externalLinkPanel.add(HtmlUtils.getHtml("Download the Deconvolution Software", "prompt"));
        externalLinkPanel.add(HtmlUtils.getHtml("&nbsp", "smallspacer"));
        externalLinkPanel.add(new ExternalLink("here", "http://sourceforge.net/projects/primerdesigner/"));

        contentPanel.add(externalLinkPanel);
        contentPanel.add(grid);
        contentPanel.add(HtmlUtils.getHtml("&nbsp", "spacer"));
        contentPanel.add(_optionsPanel);
        contentPanel.add(HtmlUtils.getHtml("&nbsp", "spacer"));
        contentPanel.add(getSubmitButtonPanel());
        contentPanel.add(getPrimerStatusMessage());

        _mainPanel.add(contentPanel);
        _submitButton.setEnabled(false);
    }

    private Panel getPrimerUploadPanel() {
        ArrayList<FileChooserPanel.FILE_TYPE> types = new ArrayList<FileChooserPanel.FILE_TYPE>();
        types.add(FileChooserPanel.FILE_TYPE.mpfa);
        types.add(FileChooserPanel.FILE_TYPE.seq);
        types.add(FileChooserPanel.FILE_TYPE.ffn);
        types.add(FileChooserPanel.FILE_TYPE.fa);
        types.add(FileChooserPanel.FILE_TYPE.faa);
        types.add(FileChooserPanel.FILE_TYPE.fna);
        types.add(FileChooserPanel.FILE_TYPE.fsa);
        types.add(FileChooserPanel.FILE_TYPE.fasta);
        _primerFileChooserPanel = new FileChooserPanel(new SelectionListener() {
            public void onSelect(String value) {
                _uploadedPrimerFilePath = value;
                _submitButton.setEnabled(true);
            }

            public void onUnSelect(String value) {
                _uploadedPrimerFilePath = null;
                _submitButton.setEnabled(false);
            }
        }, types);
        return _primerFileChooserPanel;
    }

    private Panel getAmpliconUploadPanel() {
        ArrayList<FileChooserPanel.FILE_TYPE> types = new ArrayList<FileChooserPanel.FILE_TYPE>();
        types.add(FileChooserPanel.FILE_TYPE.mpfa);
        types.add(FileChooserPanel.FILE_TYPE.seq);
        types.add(FileChooserPanel.FILE_TYPE.ffn);
        types.add(FileChooserPanel.FILE_TYPE.fa);
        types.add(FileChooserPanel.FILE_TYPE.faa);
        types.add(FileChooserPanel.FILE_TYPE.fna);
        types.add(FileChooserPanel.FILE_TYPE.fsa);
        types.add(FileChooserPanel.FILE_TYPE.fasta);
        _ampliconFileChooserPanel = new FileChooserPanel(new SelectionListener() {
            public void onSelect(String value) {
                _uploadedAmpliconFilePath = value;
                _submitButton.setEnabled(true);
            }

            public void onUnSelect(String value) {
                _uploadedAmpliconFilePath = null;
                _submitButton.setEnabled(false);
            }
        }, types);
        return _ampliconFileChooserPanel;
    }

    private Widget getPrimerStatusMessage() {
        _statusMessage = new LoadingLabel();
        _statusMessage.setHTML("&nbsp;");
        _statusMessage.addStyleName("AdvancedBlastStatusLabel");

        return new CenteredWidgetHorizontalPanel(_statusMessage);
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

    private Widget getJobNameWidget() {
        _jobNameTextBox = new TextBox();
        _jobNameTextBox.setMaxLength(64);
        _jobNameTextBox.setVisibleLength(64);
        updateJobNameWidget();

        return _jobNameTextBox;
    }

    private void updateJobNameWidget() {
        _jobNameTextBox.setText("My Barcode Design job " + new FormattedDate().toString());
    }

    private Widget getSubmitButtonPanel() {
        HorizontalPanel panel = new HorizontalPanel();
        panel.add(_clearButton);
        panel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        panel.add(_submitButton);

        return new CenteredWidgetHorizontalPanel(panel);
    }

    private void clear() {
        // Clear the Query Sequence PulldownPopup and any selections in the popup
        //_uploadPrimerFastaForm.clear();

        _barcodeTask = new BarcodeDesignerTask();
        _optionsPanel.displayParams(_barcodeTask);
        _primerFileChooserPanel.clear();
        _ampliconFileChooserPanel.clear();
        _submitButton.setEnabled(false);
        // Update all the GUI widgets with the new values
        updateAll();
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
        // submit the job
        _barcodeTask.setJobName(_jobNameTextBox.getText());
        if (projectCodeRequired) {
            _barcodeTask.setParameter(Task.PARAM_project, _projectCodePanel.getProjectCode());
        }
        _barcodeTask.setParameter(BarcodeDesignerTask.PARAM_primerFile, _uploadedPrimerFilePath);
        _barcodeTask.setParameter(BarcodeDesignerTask.PARAM_ampliconsFile, _uploadedAmpliconFilePath);
        if (_finalOutputPanel.overrideFinalOutputPath()) {
            _barcodeTask.setParameter(Task.PARAM_finalOutputDirectory, _finalOutputPanel.getFinalOutputDestination());
        }
        _submitButton.setEnabled(false);
        _statusMessage.showSubmittingMessage();
        new SubmitJob(_barcodeTask, new JobSubmissionListener()).runJob();
    }

    private void updateAll() {
        updateJobNameWidget();
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

}