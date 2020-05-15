package org.janelia.workstation.site.jrc.nodes;

import java.awt.Image;
import java.awt.datatransfer.Transferable;
import java.io.IOException;
import java.util.List;

import javax.swing.JOptionPane;

import org.janelia.model.domain.sample.LineRelease;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.common.nodes.AbstractDomainObjectNode;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.NodeTransfer;
import org.openide.util.datatransfer.PasteType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A node which shows the items most recently opened by the user.  
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FlyLineReleaseNode extends AbstractDomainObjectNode<LineRelease> {

    private final static Logger log = LoggerFactory.getLogger(FlyLineReleaseNode.class);

    public FlyLineReleaseNode(ChildFactory<?> parentChildFactory, LineRelease lineRelease) {
        super(parentChildFactory, Children.LEAF, lineRelease);
    }

    public LineRelease getLineRelease() {
        return getDomainObject();
    }

    @Override
    public String getExtraLabel() {
        return "("+getLineRelease().getNumChildren()+")";
    }

    @Override
    public Image getIcon(int type) {
        if (ClientDomainUtils.isOwner(getLineRelease())) {
            return Icons.getIcon("folder.png").getImage();
        }
        else {
            return Icons.getIcon("folder_blue.png").getImage();
        }
    }

    @Override
    public boolean canDestroy() {
        return true;
    }

    @Override
    protected void createPasteTypes(Transferable t, List<PasteType> s) {
        super.createPasteTypes(t, s);
        PasteType dropType = getDropType(t, NodeTransfer.CLIPBOARD_COPY, -1);
        if (dropType!=null) s.add(dropType);
    }

    @Override
    public PasteType getDropType(final Transferable t, int action, final int index) {
        return new PasteType() {
            @Override
            public String getName() {
                return "PasteIntoFlyLineRelease";
            }
            @Override
            public Transferable paste() throws IOException {
                JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                        "To add samples to a release, right-click them and choose 'Stage for Publishing...'");
                return null;
            }
        };
    }
}
