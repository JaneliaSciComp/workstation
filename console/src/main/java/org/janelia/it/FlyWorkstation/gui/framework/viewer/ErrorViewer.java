
package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * This viewer displays Error entities.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ErrorViewer extends TextViewer {

	public ErrorViewer(ViewerPane viewerPane) {
		super(viewerPane);
	}

	@Override
	public String getText(RootedEntity rootedEntity) {
	    return rootedEntity.getEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_MESSAGE);
	}
}
