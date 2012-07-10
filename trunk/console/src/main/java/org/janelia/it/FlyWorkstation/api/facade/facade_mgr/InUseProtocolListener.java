package org.janelia.it.FlyWorkstation.api.facade.facade_mgr;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 7/22/11
 * Time: 4:28 PM
 */
public interface InUseProtocolListener {

    void protocolAddedToInUseList(String protocol);

    void protocolRemovedFromInUseList(String protocol);

}
