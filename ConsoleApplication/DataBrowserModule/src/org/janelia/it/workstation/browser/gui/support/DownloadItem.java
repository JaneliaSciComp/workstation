package org.janelia.it.workstation.browser.gui.support;

import java.io.File;
import java.util.List;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFilepath;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.support.DynamicDomainObjectProxy;
import org.janelia.it.jacs.model.domain.support.MapUnion;
import org.janelia.it.jacs.model.domain.support.ResultDescriptor;
import org.janelia.it.jacs.model.domain.support.SampleUtils;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.util.SystemInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An item to be downloaded, possibly with some other processing such as file format conversion. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DownloadItem {

    private static final Logger log = LoggerFactory.getLogger(DownloadItem.class);
    
    public static final File workstationImagesDir = new File(SystemInfo.getDownloadsDir(), "Workstation Images");
    
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
    private String sourceFile;
    private File targetFile;
    private String sourceExtension;
    private String targetExtension;
    
    public DownloadItem(List<String> itemPath, DomainObject domainObject) {
        this.itemPath = itemPath;
        this.domainObject = domainObject;
    }
    
    public void init(ResultDescriptor resultDescriptor, String imageType, String targetExtension, boolean splitChannels, boolean flattenStructure, String filenamePattern) {

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
            if (resultDescriptor!=null) {
                Sample sample = (Sample)domainObject;
                if (resultDescriptor==ResultDescriptor.LATEST) {
                    // Get the actual result descriptor, for file naming purposes
                    ResultDescriptor actualDescriptor = SampleUtils.getLatestResultDescriptor(sample);
                    if (actualDescriptor!=null) {
                        resultName = actualDescriptor.getResultName();
                    }
                }
                else {
                    resultName = resultDescriptor.getResultName();
                }
                fileProvider = SampleUtils.getResult(sample, resultDescriptor);
            }
        }
        else if (domainObject instanceof HasFiles) {
            fileProvider = (HasFiles)domainObject;
        }

        log.debug("Domain object type: {}",domainObject.getType());
        log.debug("Domain object id: {}",domainObject.getId());
        log.debug("File provider: {}",fileProvider);

        FileType fileType = FileType.valueOf(imageType);

        String sourceFilePath = DomainUtils.getFilepath(fileProvider, imageType);
        if (sourceFilePath==null && fileProvider instanceof PipelineResult) {
            // Try separation
            PipelineResult result = (PipelineResult)fileProvider;
            NeuronSeparation separation = result.getLatestSeparationResult();
            if (separation!=null) {
                sourceFilePath = DomainUtils.getFilepath(separation, fileType); 
                if (sourceFilePath==null) {
                    sourceFilePath = getStaticPath(separation, fileType);
                }
            }
        }
        
        if (sourceFilePath==null) {
            if (resultDescriptor==null) {
                errorMessage = "Cannot find '"+fileType.getLabel()+"' file in: "+domainObject.getName();
            }
            else {
                errorMessage = "Cannot find '"+fileType.getLabel()+"' file for '"+resultDescriptor+"' result in: "+domainObject.getName();
            }
            return;
        }
            
        sourceFile = sourceFilePath;
        sourceExtension = FileUtil.getExtension(sourceFilePath);

        log.debug("Source path: {}",sourceFilePath);
        log.debug("Source extension: {}",sourceExtension);
        
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
            itemDir = new File(workstationImagesDir, pathBuilder.toString());
        }
        else {
            itemDir = workstationImagesDir;
        }

        try {
            targetFile = new File(itemDir, constructFilePath(filenamePattern));
            log.debug("Target path: {}", targetFile.getAbsolutePath());
            log.debug("Target extension: {}", targetExtension);
        }
        catch (Exception e) {
            ConsoleApp.handleException(e);
        }
    }
    
    private String getStaticPath(HasFilepath hasFilePath, FileType fileType) {
        switch (fileType) {
            case NeuronAnnotatorLabel: return new File(hasFilePath.getFilepath(),"ConsolidatedLabel.v3dpbd").getAbsolutePath();
            case NeuronAnnotatorSignal: return new File(hasFilePath.getFilepath(),"ConsolidatedSignal.v3dpbd").getAbsolutePath();
            case NeuronAnnotatorReference: return new File(hasFilePath.getFilepath(),"Reference.v3dpbd").getAbsolutePath();
            default: break;
        }
        return null;
    }
 
    private String constructFilePath(String filePattern) throws Exception {

        DynamicDomainObjectProxy proxy = new DynamicDomainObjectProxy(domainObject);
        
        MapUnion<String, Object> keyValues = new MapUnion<>();
        keyValues.addMap(proxy);
        keyValues.put(ATTR_LABEL_RESULT_NAME, resultName);
        keyValues.put(ATTR_LABEL_FILE_NAME, FileUtil.getBasename(new File(sourceFile).getName()));
        keyValues.put(ATTR_LABEL_EXTENSION, targetExtension);

        if (domainObject instanceof Sample) {
            keyValues.put(ATTR_LABEL_SAMPLE_NAME, domainObject.getName());
        }
        else if (domainObject instanceof LSMImage) {
            LSMImage lsm = (LSMImage) domainObject;
            Sample sample = (Sample) DomainMgr.getDomainMgr().getModel().getDomainObject(lsm.getSample());
            if (sample != null) {
                keyValues.put(ATTR_LABEL_SAMPLE_NAME, sample.getName());
            }
        }
        
        log.debug("Filepath pattern: {}", filePattern);
        String filepath = StringUtils.replaceVariablePattern(filePattern, keyValues);
        log.debug("Interpolated filepath: {}", filepath);
        
        // Strip extension, if any. We'll re-add it at the end.
        StringBuilder sb = new StringBuilder(filepath);

        if (splitChannels) {
            sb.append("_#");
        }

        if (!StringUtils.isEmpty(targetExtension)) {
            sb.append(".").append(targetExtension);
        }
        else {
            sb.append(".").append(sourceExtension);
        }
        
        log.debug("Final file path: {}", sb);
        return sb.toString();

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
    
    public String getSourceFile() {
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
        int cut = workstationImagesDir.getAbsolutePath().length()+1;
        return targetFile.getAbsolutePath().substring(cut);
    }
}