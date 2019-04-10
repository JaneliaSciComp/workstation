package org.janelia.it.workstation.browser.gui.listview;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Description of a list viewer, with name and class.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public interface ListViewerClassProvider {

    String getName();

    Class<? extends ListViewer<?,?>> getViewerClass();
}
