package org.janelia.workstation.ndviewer;

import org.janelia.jacsstorage.newclient.JadeStorageService;
import org.janelia.jacsstorage.newclient.StorageService;
import org.janelia.model.domain.files.N5Container;
import org.janelia.saalfeldlab.n5.N5DatasetDiscoverer;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5TreeNode;
import org.janelia.saalfeldlab.n5.metadata.*;
import org.janelia.saalfeldlab.n5.metadata.canonical.CanonicalMetadataParser;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.common.nodes.AbstractDomainObjectNode;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Node representing a single N5Container.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class N5ContainerNode extends AbstractDomainObjectNode<N5Container> {

    private final static Logger log = LoggerFactory.getLogger(N5ContainerNode.class);

    public static final N5MetadataParser<?>[] n5vGroupParsers = new N5MetadataParser[]{
            new N5CosemMultiScaleMetadata.CosemMultiScaleParser(),
            new N5ViewerMultiscaleMetadataParser(),
            new CanonicalMetadataParser(),
            new N5ViewerMultichannelMetadata.N5ViewerMultichannelMetadataParser()
    };

    public static final N5MetadataParser<?>[] n5vParsers = new N5MetadataParser[] {
            new N5CosemMetadataParser(),
            new N5SingleScaleMetadataParser(),
            new CanonicalMetadataParser(),
            new N5GenericSingleScaleMetadataParser()
    };

    public N5ContainerNode(N5Container n5Container) {
        this(null, new N5ChildFactory(n5Container), n5Container);
    }

    private N5ContainerNode(ChildFactory<?> parentChildFactory, final N5ChildFactory childFactory, N5Container n5Container) {
        super(parentChildFactory, Children.create(childFactory, true), n5Container);
    }

    public N5Container getN5Container() {
        return getDomainObject();
    }

    @Override
    public String getPrimaryLabel() {
        return getN5Container().getName();
    }

    @Override
    public Image getIcon(int type) {
        return Icons.getIcon("brick_grey.png").getImage();
    }

    @Override
    public boolean canDestroy() {
        return true;
    }

    private static class N5ChildFactory extends ChildFactory<N5TreeNode> {

        private N5Container n5Container;

        N5ChildFactory(N5Container n5Container) {
            this.n5Container = n5Container;
        }

        @Override
        protected boolean createKeys(List<N5TreeNode> list) {
            try {
                log.debug("Creating children keys for N5ContainerNode");

                String remoteStorageUrl = ConsoleProperties.getInstance().getProperty("jadestorage.rest.url");
                StorageService storageService = new StorageService(remoteStorageUrl, null);
                JadeStorageService jadeStorage = new JadeStorageService(storageService,
                        AccessManager.getSubjectKey(), AccessManager.getAccessManager().getToken());

                // TODO: use this after we implement server side discovery
//                StorageLocation storageLocation = jadeStorage.getStorageObjectByPath(n5Container.getFilepath());
//                String relativePath = storageLocation.getRelativePath(n5Container.getFilepath());
//                N5TreeNode n5RootNode = jadeStorage.getN5Tree(storageLocation, relativePath);

                N5Reader n5Reader = new N5JadeReader(jadeStorage, n5Container.getFilepath());
                N5DatasetDiscoverer datasetDiscoverer = new N5DatasetDiscoverer(
                        n5Reader,
                        Executors.newCachedThreadPool(),
                        Arrays.asList(n5vParsers),
                        Arrays.asList(n5vGroupParsers));
                N5TreeNode n5RootNode = datasetDiscoverer.discoverAndParseRecursive("/");

                for (N5TreeNode n5TreeNode : n5RootNode.childrenList()) {
                    list.add(n5TreeNode);
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

        public void refresh() {
            log.debug("Refreshing child factory for "+getClass().getSimpleName());
            refresh(true);
        }
    }
}
