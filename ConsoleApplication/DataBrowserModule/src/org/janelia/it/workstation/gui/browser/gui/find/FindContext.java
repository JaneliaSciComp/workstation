package org.janelia.it.workstation.gui.browser.gui.find;

/**
 * Any component that supports a contextual find (Meta-F) should implement this interface so that it can be registered
 * as the context whenever it is focused.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface FindContext {

    public void showFindUI();
    
    public void hideFindUI();

    public void findPrevMatch(String text, boolean skipStartingNode);

    public void findNextMatch(String text, boolean skipStartingNode);
    
    public void selectMatch();
}
