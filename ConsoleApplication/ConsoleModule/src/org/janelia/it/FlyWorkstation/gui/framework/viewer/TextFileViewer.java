package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * This viewer displays text file entities.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TextFileViewer extends TextViewer {

    public TextFileViewer(ViewerPane viewerPane) {
        super(viewerPane);
    }

    @Override
    public String getText(RootedEntity rootedEntity) throws Exception {
        String filepath = rootedEntity.getEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        URL fileURL = SessionMgr.getURL(filepath);
        return IOUtils.toString(fileURL.openStream(), "UTF-8");
    }
}
