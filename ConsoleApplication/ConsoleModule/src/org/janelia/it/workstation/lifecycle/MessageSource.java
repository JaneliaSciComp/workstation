/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.lifecycle;

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
