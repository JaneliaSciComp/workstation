package org.janelia.it.workstation.gui.browser.gui.support;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.rtextarea.RTextArea;

/**
 * Text box for editing templates for string interpolation.
 *
 * Auto-completion provided by https://github.com/bobbylight/AutoComplete
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TemplateEditorTextbox extends RTextArea {

    public TemplateEditorTextbox() {
        super(1, 40);
    }

    public void setCompletionProvider(CompletionProvider provider) {
        new AutoCompletion(provider).install(this);
    }
}
