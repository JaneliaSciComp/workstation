package org.janelia.it.workstation.api.facade.facade_mgr;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 7/22/11
 * Time: 2:57 PM
 */
public interface DataSourceSelector {

    void selectDataSource(FacadeManagerBase facade);

    void setDataSource(FacadeManagerBase facade, Object dataSource);

}