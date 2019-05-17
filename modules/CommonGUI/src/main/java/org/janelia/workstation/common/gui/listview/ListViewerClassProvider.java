package org.janelia.workstation.common.gui.listview;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Description of a list viewer, with name and class.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, property="class")
public interface ListViewerClassProvider {

    String getName();

    Class<? extends ListViewer<?,?>> getViewerClass();
}
