package org.janelia.workstation.browser.gui.dialogs.identifiers;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;

import java.util.List;

public class IdentifiersWizardState {

    private Class<? extends DomainObject> searchClass;
    private String text;
    private List<Reference> results;

    public Class<? extends DomainObject> getSearchClass() {
        return searchClass;
    }

    public void setSearchClass(Class<? extends DomainObject> searchClass) {
        this.searchClass = searchClass;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<Reference> getResults() {
        return results;
    }

    public void setResults(List<Reference> results) {
        this.results = results;
    }
}
