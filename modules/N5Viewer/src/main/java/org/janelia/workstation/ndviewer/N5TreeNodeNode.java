package org.janelia.workstation.ndviewer;

import org.apache.commons.lang.StringEscapeUtils;
import org.janelia.model.domain.files.N5Container;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5TreeNode;
import org.janelia.saalfeldlab.n5.metadata.MultiscaleMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5DatasetMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.metadata.N5ViewerMultichannelMetadata;
import org.janelia.saalfeldlab.n5.metadata.canonical.CanonicalMultichannelMetadata;
import org.janelia.workstation.browser.gui.components.ViewerUtils;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.core.nodes.UserObjectNode;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Node representing an N5TreeNode from the N5 API.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class N5TreeNodeNode extends UserObjectNode<N5TreeNode> {

    private final static Logger log = LoggerFactory.getLogger(N5TreeNodeNode.class);

    protected static final String dimDelimeter = "x";

    private static long idCounter = 100011;

    private final long id;

    private final InstanceContent lookupContents;

    public N5TreeNodeNode(N5Container n5Container, N5TreeNode n5TreeNode) {
        this(new InstanceContent(),
                new N5TreeNodeChildFactory(n5Container, n5TreeNode),
                n5Container,
                n5TreeNode);
    }

    private N5TreeNodeNode(InstanceContent lookupContents, final N5TreeNodeChildFactory childFactory,
                           N5Container n5Container, N5TreeNode n5TreeNode) {
        super(n5TreeNode.childrenList().isEmpty() ?
                        Children.LEAF :
                        Children.create(childFactory, true),
                new AbstractLookup(lookupContents));
        this.lookupContents = lookupContents;
        lookupContents.add(n5Container);
        lookupContents.add(n5TreeNode);
        this.id = ++idCounter;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public N5TreeNode getObject() {
        return getLookup().lookup(N5TreeNode.class);
    }

    public N5TreeNode getN5TreeNode() {
        return getObject();
    }

    public N5Container getN5Container() {
        return getLookup().lookup(N5Container.class);
    }

    public String getPrimaryLabel() {
        return getN5TreeNode().getNodeName();
    }

    public String getSecondaryLabel() {
        return null;
    }

    public String getExtraLabel() {
        // Adapted from n5-viewer's N5ViewerTreeCellRenderer
        N5TreeNode node = getN5TreeNode();
        N5Metadata metadata = node.getMetadata();
        if (metadata != null) {

            final String multiChannelString;
            if( metadata instanceof N5ViewerMultichannelMetadata
                    || metadata instanceof CanonicalMultichannelMetadata)
                multiChannelString = "multichannel";
            else
                multiChannelString = "";

            final String multiscaleString;
            if( metadata instanceof MultiscaleMetadata)
                multiscaleString = "multiscale";
            else
                multiscaleString = "";

            String parameterString = getParameterString(node);
            return String.join("", new String[]{
                    " (",
                    parameterString,
                    multiChannelString,
                    multiscaleString,
                    ")"});
        }
        return null;
    }

    public String getParameterString( final N5TreeNode node ) {

        N5Metadata meta = node.getMetadata();
        if (!(meta instanceof N5DatasetMetadata))
            return "";

        final DatasetAttributes attributes = ((N5DatasetMetadata)node.getMetadata()).getAttributes();
        final String dimString = String.join(dimDelimeter,
                Arrays.stream(attributes.getDimensions())
                        .mapToObj(d -> Long.toString(d))
                        .collect(Collectors.toList()));

        return dimString + ", " + attributes.getDataType();
    }

    @Override
    public String getDisplayName() {
        String displayName = getPrimaryLabel();
        String secondary = getSecondaryLabel();
        if (secondary!=null) {
            displayName += " "+secondary;
        }
        String extra = getExtraLabel();
        if (extra!=null) {
            displayName += " "+extra;
        }
        return displayName;
    }

    @Override
    public String getHtmlDisplayName() {
        String primary = getPrimaryLabel();
        String secondary = getSecondaryLabel();
        String extra = getExtraLabel();
        StringBuilder sb = new StringBuilder();
        if (primary!=null) {
            sb.append("<font color='!Tree.textForeground'>");
            sb.append(StringEscapeUtils.escapeHtml(primary));
            sb.append("</font>");
        }
        if (secondary!=null) {
            sb.append(" <font color='!ws.TreeSecondaryLabel'>");
            sb.append(StringEscapeUtils.escapeHtml(secondary));
            sb.append("</font>");
        }
        if (extra!=null) {
            sb.append(" <font color='!ws.TreeExtraLabel'>");
            sb.append(StringEscapeUtils.escapeHtml(extra));
            sb.append("</font>");
        }
        return sb.toString();
    }

    @Override
    public Image getIcon(int type) {
        return Icons.getIcon("brick_grey.png").getImage();
    }

    @Override
    public void update(N5TreeNode refreshed) {
        if (refreshed==null) throw new IllegalStateException("Cannot update with null object");
        String oldName = getName();
        String oldDisplayName = getDisplayName();
        log.debug("Updating node with: {}",refreshed.getNodeName());
        lookupContents.remove(getObject());
        lookupContents.add(refreshed);
        fireCookieChange();
        fireNameChange(oldName, getName());
        log.debug("Display name changed {} -> {}",oldDisplayName, getDisplayName());
        fireDisplayNameChange(oldDisplayName, getDisplayName());
    }

    @Override
    public Action[] getActions(boolean context) {
        Collection<Action> actions = new ArrayList<>();
        actions.add(new PopupLabelAction());
        actions.add(new OpenInNewN5ViewerAction());
        actions.add(new OpenInN5ViewerAction());
        return actions.toArray(new Action[0]);
    }

    protected final class PopupLabelAction extends AbstractAction {

        PopupLabelAction() {
            putValue(NAME, getDisplayName());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
        }

        @Override
        public boolean isEnabled() {
            return false;
        }
    }

    protected final class OpenInNewN5ViewerAction extends AbstractAction {

        OpenInNewN5ViewerAction() {
            putValue(NAME, "Open in New N5 Viewer");
        }

        @Override
        public void actionPerformed(ActionEvent e) {

            BigDataViewerTopComponent viewer = ViewerUtils.createNewViewer(BigDataViewerManager.getInstance(), "editor");
            viewer.loadData(getN5Container(), getN5TreeNode());
        }
    }

    protected final class OpenInN5ViewerAction extends AbstractAction {

        OpenInN5ViewerAction() {
            putValue(NAME, "Add to N5 Viewer");
        }

        @Override
        public void actionPerformed(ActionEvent e) {

            BigDataViewerTopComponent viewer = ViewerUtils.provisionViewer(BigDataViewerManager.getInstance(), "editor");
            viewer.addData(getN5Container(), getN5TreeNode());
        }
    }

    @Override
    public boolean canDestroy() {
        return false;
    }

    private static class N5TreeNodeChildFactory extends ChildFactory<N5TreeNode> {

        private N5Container n5Container;
        private N5TreeNode n5TreeNode;

        N5TreeNodeChildFactory(N5Container n5Container, N5TreeNode n5TreeNode) {
            this.n5Container = n5Container;
            this.n5TreeNode = n5TreeNode;
        }

        @Override
        protected boolean createKeys(List<N5TreeNode> list) {
            try {
                log.debug("Creating children keys for N5ContainerNode");

                for (N5TreeNode n5ChildNode : n5TreeNode.childrenList()) {
                    list.add(n5ChildNode);
                }
            }
            catch (Exception ex) {
                FrameworkAccess.handleException(ex);
            }
            return true;
        }

        @Override
        protected Node createNodeForKey(N5TreeNode key) {
            try {
                return new N5TreeNodeNode(n5Container, key);
            }
            catch (Exception e) {
                log.error("Error creating node for key " + key, e);
            }
            return null;
        }
    }
}
