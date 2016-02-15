package org.janelia.it.workstation.gui.browser.gui.support;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.workstation.gui.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.model.DomainModelViewUtils;
import org.janelia.it.workstation.gui.browser.model.DomainObjectAttribute;
import org.janelia.it.workstation.gui.browser.model.ResultDescriptor;
import org.janelia.it.workstation.shared.util.SystemInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An item to be downloaded, possibly with some other processing such as file format conversion. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DownloadItem {

    private static final Logger log = LoggerFactory.getLogger(DownloadItem.class);
    
    private static final File WS_IMAGES_DIR = new File(SystemInfo.getDownloadsDir(), "Workstation Images");
    
    public static final String ATTR_LABEL_RESULT_NAME = "Result Name";
    public static final String ATTR_LABEL_FILE_NAME = "File Name";
    public static final String ATTR_LABEL_SAMPLE_NAME = "Sample Name";
    public static final String ATTR_LABEL_EXTENSION = "Extension";
    
    private final List<String> itemPath;
    private final DomainObject domainObject;
    private boolean splitChannels = false;
    
    // Derived state
    private String errorMessage;
    private String resultName;
    private File sourceFile;
    private File targetFile;
    private String sourceExtension;
    private String targetExtension;
    
    public DownloadItem(List<String> itemPath, DomainObject domainObject) {
        this.itemPath = itemPath;
        this.domainObject = domainObject;
    }
    
    public void init(ResultDescriptor resultDescriptor, String targetExtension, boolean splitChannels, boolean flattenStructure, String filenamePattern) {

        this.targetExtension = targetExtension;
        this.splitChannels = splitChannels;
        
        // Reset derived state
        errorMessage = null;
        resultName = null;
        sourceFile = null;
        targetFile = null;
        sourceExtension = null;
        
        // Figure out what file we're downloading
        HasFiles fileProvider = null;
        if (domainObject instanceof Sample) {
            Sample sample = (Sample)domainObject;
            if (resultDescriptor==ResultDescriptor.LATEST) {
                // Get the actual result descriptor, for file naming purposes
                ResultDescriptor actualDescriptor = DomainModelViewUtils.getLatestResultDescriptor(sample);
                if (actualDescriptor!=null) {
                    resultName = actualDescriptor.getResultKey();
                }
            }
            else {
                resultName = resultDescriptor.getResultKey();
            }
            fileProvider = DomainModelViewUtils.getResult(sample, resultDescriptor);   
        }
        else if (domainObject instanceof HasFiles) {
            fileProvider = (HasFiles)domainObject;
        }

        String sourceFilePath = DomainUtils.getDefault3dImageFilePath(fileProvider);
        if (sourceFilePath==null) {
            errorMessage = "Cannot find result file for: "+domainObject.getName();
            return;
        }
            
        sourceFile = new File(sourceFilePath);
        sourceExtension = FileUtil.getExtension(sourceFilePath);

        if (this.targetExtension==null) {
            this.targetExtension = sourceExtension;
        }
        
        // Build the path
        File itemDir = null;
        if (itemPath!=null && !flattenStructure) {
            StringBuilder pathBuilder = new StringBuilder();
            for(String item : itemPath) {
                if (pathBuilder.length()!=0) pathBuilder.append("/");
                pathBuilder.append(item);
            }
            itemDir = new File(WS_IMAGES_DIR, pathBuilder.toString());
        }
        else {
            itemDir = WS_IMAGES_DIR;
        }

        targetFile = new File(itemDir, constructFilePath(filenamePattern));
    }

    private String constructFilePath(String filePattern) {
        
        Map<String, DomainObjectAttribute> attributeMap = new HashMap<>();
        for(DomainObjectAttribute attr : ClientDomainUtils.getSearchAttributes(domainObject.getClass())) {
            log.debug("Adding attribute: "+attr.getLabel());
            attributeMap.put(attr.getLabel(), attr);
        }

        log.info("Source filepath: {}", sourceFile);
        log.info("File pattern: {}", filePattern);
        
        Pattern pattern = Pattern.compile("\\{(.+?)\\}");
        Matcher matcher = pattern.matcher(filePattern);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String template = matcher.group(1);
            String replacement = null;
            log.info("  Matched: {}",template);
            for (String templatePart : template.split("\\|")) {
                String attrLabel = templatePart.trim();
                if (ATTR_LABEL_RESULT_NAME.equals(attrLabel)) {
                    replacement = resultName==null ? null : resultName;
                }
                else if (ATTR_LABEL_FILE_NAME.equals(attrLabel)) {
                    replacement = sourceFile.getName();
                } 
                else if (ATTR_LABEL_SAMPLE_NAME.equals(attrLabel)) {
                    if (domainObject instanceof Sample) {
                        replacement = domainObject.getName();
                    }
                    else if (domainObject instanceof LSMImage) {
                        LSMImage lsm = (LSMImage)domainObject;
                        Sample sample = (Sample)DomainMgr.getDomainMgr().getModel().getDomainObject(lsm.getSample());
                        if (sample!=null) {
                            replacement = sample.getName();
                        }
                    }
                } 
                else if (ATTR_LABEL_EXTENSION.equals(attrLabel)) {
                    replacement = targetExtension;
                } 
                else if (attrLabel.matches("\"(.*?)\"")) {
                	replacement = attrLabel.substring(1, attrLabel.length()-1);
                }
                else {
                    DomainObjectAttribute attr = attributeMap.get(attrLabel);
                    if (attr!=null) {
                        replacement = getStringAttributeValue(attr);    
                    }
                }

                if (replacement != null) {
                    matcher.appendReplacement(buffer, replacement);
                    log.info("    '{}'->'{}' = '{}'",template,replacement,buffer);
                    break;
                }
            }

            if (replacement==null) {
                log.warn("      Cannot find a replacement for: {}",template);
            }
        }
        matcher.appendTail(buffer);
        
        log.info("Final buffer: {}",buffer);
        
        // Strip extension, if any. We'll re-add it at the end.
        StringBuilder filepath = new StringBuilder(FileUtil.getBasename(buffer.toString()));
        
        if (splitChannels) {
            filepath.append("_#");
        }
        
        filepath.append(".").append(targetExtension);
        log.info("Final file path: {}",filepath);
        return filepath.toString();
    }

    private String getStringAttributeValue(DomainObjectAttribute attr) {
        try {
            Object value = attr.getGetter().invoke(domainObject);
            if (value!=null) {
                return value.toString();
            }
            return null;
        }
        catch (Exception e) {
            throw new IllegalStateException("Problem getting attribute "+attr.getName());
        }
    }
    
    public List<String> getItemPath() {
        return itemPath;
    }
    
    public DomainObject getDomainObject() {
        return domainObject;
    }
    
    public String getName() {
        return domainObject.getName();
    }
    
    public File getSourceFile() {
        return sourceFile;
    }

    public File getTargetFile() {
        return targetFile;
    }

    public String getSourceExtension() {
        return sourceExtension;
    }

    public String getTargetExtension() {
        return targetExtension;
    }

    public boolean isSplitChannels() {
        return splitChannels;
    }

    @Override
    public String toString() {
    	if (errorMessage!=null) {
			return errorMessage;
		}
    	if (targetFile==null) {
    		return "Error getting file for "+domainObject.getName();
    	}
        return targetFile.getAbsolutePath();
    }
}