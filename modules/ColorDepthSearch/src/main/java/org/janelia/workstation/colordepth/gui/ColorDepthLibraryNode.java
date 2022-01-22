package org.janelia.workstation.colordepth;

import java.awt.Image;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.janelia.model.domain.gui.cdmip.ColorDepthLibrary;
import org.janelia.workstation.browser.model.ColorDepthAlignmentSpace;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.common.nodes.FilterNode;
import org.janelia.workstation.core.util.ColorDepthUtils;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Node representing a single color depth library which is a filter for the images in it.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ColorDepthLibraryNode extends FilterNode<ColorDepthLibrary> {

    private final static Logger log = LoggerFactory.getLogger(ColorDepthLibraryNode.class);
    private final static AtomicLong counter = new AtomicLong(4000);

    ColorDepthLibraryNode(ChildFactory<?> parentChildFactory, ColorDepthLibrary library) {
        this(parentChildFactory, new ColorDepthLibraryFactory(library), library);
    }

    private ColorDepthLibraryNode(ChildFactory<?> parentChildFactory, final ColorDepthLibraryFactory childFactory, ColorDepthLibrary library) {
        super(parentChildFactory, library.getColorDepthCounts().isEmpty() ? Children.LEAF : Children.create(childFactory, false), library);
    }

    public ColorDepthLibrary getLibrary() {
        return getDomainObject();
    }

    @Override
    public String getPrimaryLabel() {
        return getLibrary().getIdentifier();
    }

    @Override
    public String getExtraLabel() {
        return null;
    }

    @Override
    public Image getIcon(int type) {
        return Icons.getIcon("folder_red.png").getImage();
    }

    private static class ColorDepthLibraryFactory extends ChildFactory<ColorDepthAlignmentSpace> {

        private ColorDepthLibrary library;

        ColorDepthLibraryFactory(ColorDepthLibrary library) {
            this.library = library;
        }

        @Override
        protected boolean createKeys(List<ColorDepthAlignmentSpace> list) {
            try {
                log.debug("Creating children keys for ColorDepthLibraryNode");
                int c = 0;
                List<String> alignmentSpaces = new ArrayList<>(library.getColorDepthCounts().keySet());
                alignmentSpaces.sort(String::compareTo);
                for (String alignmentSpace : alignmentSpaces) {
                    if (ColorDepthUtils.isAlignmentSpaceVisible(alignmentSpace)) {
                        long id = counter.getAndIncrement();
                        ColorDepthAlignmentSpace colorDepthAlignmentSpace = new ColorDepthAlignmentSpace(id, library, alignmentSpace, c++);
                        list.add(colorDepthAlignmentSpace);
                    }
                }
            }
            catch (Exception ex) {
                FrameworkAccess.handleException(ex);
            }
            return true;
        }

        @Override
        protected Node createNodeForKey(ColorDepthAlignmentSpace key) {
            try {
                return new ColorDepthAlignmentSpaceNode(key);
            }
            catch (Exception e) {
                log.error("Error creating node for key " + key, e);
            }
            return null;
        }

        public void refresh() {
            log.debug("Refreshing child factory for "+getClass().getSimpleName());
            refresh(true);
        }
    }
}
