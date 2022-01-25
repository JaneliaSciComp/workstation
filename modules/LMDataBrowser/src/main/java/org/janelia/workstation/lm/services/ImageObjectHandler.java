package org.janelia.workstation.lm.services;

import com.google.common.io.Files;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.enums.FileType;
import org.janelia.model.domain.sample.Image;
import org.janelia.workstation.common.gui.editor.ParentNodeSelectionEditor;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.integration.spi.domain.DomainObjectHandler;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A helper for working with ImageNodes.
 * 
 * @author goinac
 */
@ServiceProvider(service = DomainObjectHandler.class)
public class ImageObjectHandler implements DomainObjectHandler {

    private final static Logger log = LoggerFactory.getLogger(ImageObjectHandler.class);
    
    @Override
    public boolean isCompatible(DomainObject domainObject) {
        return isCompatible(domainObject.getClass());
    }
    
    @Override
    public boolean isCompatible(Class<? extends DomainObject> clazz) {
        return Image.class.isAssignableFrom(clazz);
    }

    @Override
    public Node getNode(DomainObject domainObject, ChildFactory parentChildFactory) throws Exception {
        return null;
    }

    @Override
    public Class<? extends ParentNodeSelectionEditor<? extends DomainObject,?,?>> getEditorClass(DomainObject domainObject) {
        return  null;
    }
    
    @Override   
    public String getLargeIcon(DomainObject domainObject) {
        return null; // no icon file
    }

    @Override
    public boolean supportsRemoval(DomainObject domainObject) {
        if (domainObject instanceof Image) {
            Image imageObject = (Image) domainObject;
            return imageObject.getUserDataFlag() != null && imageObject.getUserDataFlag();
        } else {
            return false;
        }
    }
    
    @Override
    public void remove(DomainObject domainObject) throws Exception {
        
        log.info("Removing {}", domainObject);
        
        // remove the image
        DomainModel model = DomainMgr.getDomainMgr().getModel();
        Image imageObject = (Image) domainObject;
        model.remove(Arrays.asList(imageObject));
        
        // remove the image files
        model.removeStorage(Stream.concat(
                imageObject.getFiles().entrySet().stream().map(e -> ImmutablePair.of(e.getKey(), e.getValue())),
                Stream.of(ImmutablePair.<FileType, String>of(null, imageObject.getFilepath())))
                .filter(fp -> {
                    if (fp.getLeft() == null) {
                        // only try this branch if this is the main filepath which has no filetype associated with it.
                        String fpExt = Files.getFileExtension(fp.getRight());
                        if (fpExt.length() > 0) {
                            switch (fpExt.toLowerCase()) {
                                case "gif":
                                case "png":
                                case "tif":
                                case "tiff":
                                case "lsm":
                                case "v3draw":
                                case "v3dpbd":
                                    return true;
                                default:
                                    return false;
                            }
                        } else {
                            return false;
                        }
                    } else {
                        return true;
                    }
                })
                .map(fp -> {
                    if (fp.getLeft() == null) {
                        return fp.getRight();
                    } else {
                        return DomainUtils.getFilepath(imageObject, fp.getLeft());
                    }
                })
                .collect(Collectors.toList()));
    }

}
