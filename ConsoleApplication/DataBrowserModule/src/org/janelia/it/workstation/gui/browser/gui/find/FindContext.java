package org.janelia.it.workstation.gui.browser.gui.find;

import java.util.concurrent.Callable;

import javax.swing.text.Position;

/**
 * Any component that supports a contextual find (Meta-F) should implement this interface so that it can be registered
 * as the context whenever it is focused.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface FindContext {

    public void showFindUI();
    
    public void hideFindUI();

    public void findMatch(String text, Position.Bias bias, boolean skipStartingNode, Callable<Void> success);
    
    public void openMatch();
}
