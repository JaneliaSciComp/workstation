package org.janelia.it.workstation.browser.lifecycle;

import java.util.List;

/**
 * Implement this to provide messages for activity logging.
 *
 * @author fosterl
 */
public interface MessageSource {
    void setMessages(List<String> messages);
    List<String> getMessages();
}
