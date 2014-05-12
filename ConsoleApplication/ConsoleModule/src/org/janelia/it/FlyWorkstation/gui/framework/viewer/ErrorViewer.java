
package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * This viewer displays Error entities.
 * @deprecated After the next release, Errors will be simple text files, so we can get rid of this entirely and just use the TextFileViewer
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ErrorViewer extends TextFileViewer {

	public ErrorViewer(ViewerPane viewerPane) {
		super(viewerPane);
	}

	@Override
	public String getText(RootedEntity rootedEntity) throws Exception {
	    String message = rootedEntity.getEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_MESSAGE);
	    if (message==null) {
	        return super.getText(rootedEntity);
	    }
	    return null;
	}
}
