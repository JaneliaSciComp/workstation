package org.janelia.it.workstation.gui.browser.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;

import net.miginfocom.swing.MigLayout;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.ShorthandCompletion;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.support.DomainObjectAttribute;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.support.SearchType;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.gui.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.gui.browser.gui.listview.icongrid.IconGridViewerConfiguration;
import org.janelia.it.workstation.gui.browser.gui.support.DropDownButton;
import org.janelia.it.workstation.gui.browser.gui.support.TemplateEditorTextbox;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.janelia.it.workstation.gui.browser.gui.editor.FilterEditorPanel.DEFAULT_SEARCH_CLASS;

/**
 * A dialog for configuring a IconGridViewer.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class IconGridViewerConfigDialog extends ModalDialog {

    private static final Logger log = LoggerFactory.getLogger(IconGridViewerConfigDialog.class);

    private static final String DEFAULT_TITLE_VALUE = "{Name}";
    private static final String DEFAULT_SUBTITLE_VALUE = "";

    public static final int ERROR_OPTION = -1;
    public static final int CANCEL_OPTION = 0;
    public static final int CHOOSE_OPTION = 1;

    private int returnValue = ERROR_OPTION;

    private final DropDownButton typeCriteriaButton;
    private final JPanel attrPanel;
    private final TemplateEditorTextbox titleInputBox;
    private final TemplateEditorTextbox subtitleInputBox;

    private final IconGridViewerConfiguration config;

    private Class<? extends DomainObject> resultClass;

    private static final String PATTERN_HELP =
            "<html><font color='#959595' size='-1'>These templates are used to create the title and subtitle for each item. " +
            "You can use Ctrl-Space to show property suggestions. Properties must be surrounded in braces. " +
            "You can also use the | character to specify default values, for example: {Qi Score|\"None\"}"
                    + "</font></html>";

    public IconGridViewerConfigDialog(Class<? extends DomainObject> defaultResultClass) {

        this.config = IconGridViewerConfiguration.loadConfig();

        setTitle("Table Configuration");

        attrPanel = new JPanel(new MigLayout("wrap 2, ins 20", "[grow 0, growprio 0][grow 100, growprio 100]"));

        this.typeCriteriaButton = new DropDownButton();
        attrPanel.add(typeCriteriaButton,"gap para, span 2");

        attrPanel.add(new JLabel(PATTERN_HELP),"gap para, span 2, width 100:400:600, height 50:100:150, growx, ay top");

        titleInputBox = new TemplateEditorTextbox();
        subtitleInputBox = new TemplateEditorTextbox();
        JLabel titleLabel = new JLabel("Title format: ");
        titleLabel.setLabelFor(titleInputBox);
        attrPanel.add(titleLabel,"gap para");
        attrPanel.add(titleInputBox,"gap para, width 100:400:600, growx");

        JLabel subtitleLabel = new JLabel("Subtitle format: ");
        subtitleLabel.setLabelFor(subtitleInputBox);
        attrPanel.add(subtitleLabel,"gap para");
        attrPanel.add(subtitleInputBox,"gap para, width 100:400:600, growx");

        add(attrPanel, BorderLayout.CENTER);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Close without saving changes");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                returnValue = CANCEL_OPTION;
                setVisible(false);
            }
        });

        JButton okButton = new JButton("OK");
        okButton.setToolTipText("Close and save changes");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveAndClose();
            }
        });

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(okButton);

        add(buttonPane, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                returnValue = CANCEL_OPTION;
            }
        });
        
        // Initialize the drop-down menu
        
        ButtonGroup typeGroup = new ButtonGroup();
        for (final Class<? extends DomainObject> searchClass : DomainUtils.getSearchClasses()) {
            final String label = searchClass.getAnnotation(SearchType.class).label();
            JMenuItem menuItem = new JRadioButtonMenuItem(label, searchClass.equals(DEFAULT_SEARCH_CLASS));
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    addCurrentConfig();
                    setResultType(searchClass);
                }
            });
            typeGroup.add(menuItem);
            typeCriteriaButton.getPopupMenu().add(menuItem);
        }

        setResultType(defaultResultClass);
    }

    private void addCurrentConfig() {
        config.setDomainClassTitle(resultClass.getSimpleName(), titleInputBox.getText());
        config.setDomainClassSubtitle(resultClass.getSimpleName(), subtitleInputBox.getText());
    }

    private void setResultType(Class<? extends DomainObject> resultClass) {
        this.resultClass = resultClass;
        SearchType searchTypeAnnot = resultClass.getAnnotation(SearchType.class);
        typeCriteriaButton.setText("Result Type: " + searchTypeAnnot.label());

        // Install completion providers for the current result type
        CompletionProvider provider = createCompletionProvider(resultClass);
        titleInputBox.setCompletionProvider(provider);
        subtitleInputBox.setCompletionProvider(provider);

        String title = config.getDomainClassTitle(resultClass.getSimpleName());
        if (StringUtils.isEmpty(title)) {
            titleInputBox.setText(DEFAULT_TITLE_VALUE);
        }
        else {
            titleInputBox.setText(title);
        }

        String subtitle = config.getDomainClassSubtitle(resultClass.getSimpleName());
        if (StringUtils.isEmpty(subtitle)) {
            subtitleInputBox.setText(DEFAULT_SUBTITLE_VALUE);
        }
        else {
            subtitleInputBox.setText(subtitle);
        }
    }

    private CompletionProvider createCompletionProvider(Class<? extends DomainObject> resultClass) {
        List<DomainObjectAttribute> attrs = ClientDomainUtils.getUniqueAttributes(resultClass);
        DefaultCompletionProvider provider = new DefaultCompletionProvider();
        for(DomainObjectAttribute attr : attrs) {
            provider.addCompletion(new ShorthandCompletion(provider, attr.getLabel(), "{"+attr.getLabel()+"}", ""));
        }
        return provider;
    }

    public int showDialog(Component parent) throws HeadlessException {
        packAndShow();
        return returnValue;
    }

    private void saveAndClose() {

        Utils.setWaitingCursor(IconGridViewerConfigDialog.this);

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                addCurrentConfig();
                config.save();
            }

            @Override
            protected void hadSuccess() {
                Utils.setDefaultCursor(IconGridViewerConfigDialog.this);
                returnValue = CHOOSE_OPTION;
                setVisible(false);
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
                Utils.setDefaultCursor(IconGridViewerConfigDialog.this);
                returnValue = ERROR_OPTION;
                setVisible(false);
            }
        };

        worker.execute();
    }

    public IconGridViewerConfiguration getConfig() {
        return config;
    }
}
