package org.janelia.it.workstation.browser.gui.dialogs.download;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.model.DomainModelViewUtils;
import org.janelia.it.workstation.browser.model.MappingType;
import org.janelia.it.workstation.browser.model.descriptors.ArtifactDescriptor;
import org.janelia.it.workstation.browser.util.PathUtil;
import org.janelia.it.workstation.browser.util.SystemInfo;
import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.access.domain.DynamicDomainObjectProxy;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.enums.FileType;
import org.janelia.model.domain.interfaces.HasAnatomicalArea;
import org.janelia.model.domain.interfaces.HasFilepath;
import org.janelia.model.domain.interfaces.HasFiles;
import org.janelia.model.domain.sample.LSMImage;
import org.janelia.model.domain.sample.NeuronFragment;
import org.janelia.model.domain.sample.NeuronSeparation;
import org.janelia.model.domain.sample.ObjectiveSample;
import org.janelia.model.domain.sample.PipelineResult;
import org.janelia.model.domain.sample.Sample;
import org.janelia.model.util.MapUnion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An item to be downloaded, possibly with some other processing such as file format conversion. 
 * 
 * Extends DownloadItem for backwards compatibility reasons. Eventually that can be removed when the legacy download dialog goes away.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DownloadFileItem {

    private static final Logger log = LoggerFactory.getLogger(DownloadFileItem.class);
    
    public static final String ATTR_LABEL_RESULT_NAME = "Result Name";
    public static final String ATTR_LABEL_OBJECTIVE = "Objective";
    public static final String ATTR_LABEL_ANATOMICAL_AREA = "Anatomical Area";
    public static final String ATTR_LABEL_FILE_NAME = "File Name";
    public static final String ATTR_LABEL_SAMPLE_NAME = "Sample Name";
    public static final String ATTR_LABEL_EXTENSION = "Extension";
    public static final String ATTR_LABEL_GUID = "GUID";
    
    private final Path downloadsDir = SystemInfo.getDownloadsDir();
    private final List<String> itemPath;
    private final DomainObject domainObject;
    private HasFiles fileProvider;
    private boolean splitChannels = false;
    
    // Derived state
    private String errorMessage;
    private String resultName;
    private String sourceFile;
    private String targetRelativePath;
    private Path targetLocalPath;
    private String sourceExtension;
    private String targetExtension;
    private boolean is3d;
    
    public DownloadFileItem(List<String> itemPath, DomainObject domainObject) {
        this.itemPath = itemPath;
        this.domainObject = domainObject;
    }
    
    public void init(ArtifactDescriptor artifactDescriptor, HasFiles fileProvider, FileType fileType, Map<String,String> outputExtensions, boolean splitChannels, boolean flattenStructure, String filenamePattern) {

        log.debug("Domain object type: {}",domainObject.getType());
        log.debug("Domain object id: {}",domainObject.getId());
        log.debug("File provider: {}",fileProvider.getClass().getName());
        log.debug("File type: {}",fileType);
        
        this.fileProvider = fileProvider;
        this.splitChannels = splitChannels;
        this.errorMessage = null;
        this.resultName = null;
        this.targetLocalPath = null;
        this.is3d = fileType.is3dImage();
        
        if (!fileType.is3dImage() && splitChannels) {
            throw new IllegalStateException("Cannot split channels for non-3d image");
        }
        
        String sourceFilePath = DomainUtils.getFilepath(fileProvider, fileType);
        if (sourceFilePath==null) {
            if (fileProvider instanceof PipelineResult) {
                // Try separation
                PipelineResult result = (PipelineResult)fileProvider;
                log.debug("Trying neuron separation: {}", result.getId());
                NeuronSeparation separation = result.getLatestSeparationResult();
                if (separation!=null) {
                    sourceFilePath = DomainUtils.getFilepath(separation, fileType); 
                    if (sourceFilePath==null) {
                        sourceFilePath = getStaticPath(separation, fileType);
                    }
                }
            }
            else if (fileProvider instanceof NeuronFragment) {
                NeuronFragment fragment = (NeuronFragment)fileProvider;
                log.debug("Trying neuron fragment: {}", fragment);
                // Try separation
                sourceFilePath = DomainUtils.getFilepath(fragment, fileType); 
                if (sourceFilePath==null) {
                    sourceFilePath = getStaticPath(fragment, fileType);
                }
            }
        }
        
        if (sourceFilePath==null) {
            errorMessage = "Cannot find '"+artifactDescriptor+"' with '"+fileType.getLabel()+"' file in: "+domainObject.getName();
            log.debug(errorMessage);
            return;
        }
        
        this.sourceFile = sourceFilePath;
        this.sourceExtension = FileUtil.getExtension(sourceFilePath);

        log.debug("Source path: {}",sourceFilePath);
        log.debug("Source extension: {}",sourceExtension);
        
        if (outputExtensions!=null) {
            this.targetExtension = outputExtensions.get(sourceExtension);
        }
        if (this.targetExtension==null || DownloadWizardState.NATIVE_EXTENSION.equals(targetExtension)) {
            this.targetExtension = sourceExtension;
        }
        log.debug("Output extension: {}",sourceExtension);
                
        // Build the path
        Path itemDir = null;
        if (itemPath!=null && !flattenStructure) {
            StringBuilder pathBuilder = new StringBuilder();
            for(String item : itemPath) {
                if (pathBuilder.length()!=0) pathBuilder.append("/");
                pathBuilder.append(item);
            }
            itemDir = downloadsDir.resolve(pathBuilder.toString());
        }
        else {
            itemDir = downloadsDir;
        }

        try {
            targetRelativePath = constructFilePath(filenamePattern);
            targetLocalPath = itemDir.resolve(targetRelativePath);
            log.debug("Target path: {}", targetLocalPath.toString());
            log.debug("Target extension: {}", this.targetExtension);
        }
        catch (Exception e) {
            ConsoleApp.handleException(e);
        }
    }
    
    private String getStaticPath(HasFilepath hasFilePath, FileType fileType) {
        if (hasFilePath.getFilepath()==null) return null;
        switch (fileType) {
            case NeuronAnnotatorLabel: return PathUtil.getStandardPath(Paths.get(hasFilePath.getFilepath(),"ConsolidatedLabel.v3dpbd"));
            case NeuronAnnotatorSignal: return PathUtil.getStandardPath(Paths.get(hasFilePath.getFilepath(),"ConsolidatedSignal.v3dpbd"));
            case NeuronAnnotatorReference: return PathUtil.getStandardPath(Paths.get(hasFilePath.getFilepath(),"Reference.v3dpbd"));
            default: break;
        }
        return null;
    }
    
    private String constructFilePath(String filePattern) throws Exception {

        log.debug("Objects used for path constructions: ");
        
        MapUnion<String, Object> keyValues = new MapUnion<>();
        
        keyValues.addMap(new DynamicDomainObjectProxy(domainObject));
        log.debug("  domainObject: {}", domainObject);

        String baseName = FileUtil.getBasename(new File(sourceFile).getName());
        keyValues.put(ATTR_LABEL_FILE_NAME, baseName);
        log.debug("  {}: {}", ATTR_LABEL_FILE_NAME, baseName);
        
        keyValues.put(ATTR_LABEL_EXTENSION, targetExtension);
        log.debug("  {}: {}", ATTR_LABEL_EXTENSION, targetExtension);

        if (fileProvider instanceof HasAnatomicalArea) {
            HasAnatomicalArea aaResult = (HasAnatomicalArea)fileProvider;
            keyValues.put(ATTR_LABEL_ANATOMICAL_AREA, aaResult.getAnatomicalArea());
            log.debug("  {}: {}", ATTR_LABEL_ANATOMICAL_AREA, aaResult.getAnatomicalArea());
        }

        if (fileProvider instanceof PipelineResult) {
            PipelineResult result = (PipelineResult)fileProvider;
            keyValues.put(ATTR_LABEL_GUID, result.getId());
            log.debug("  {}: {}", ATTR_LABEL_GUID, result.getId());
            String objective = result.getParentRun().getParent().getObjective();
            keyValues.put(ATTR_LABEL_OBJECTIVE, objective);
            log.debug("  {}: {}", ATTR_LABEL_OBJECTIVE, objective);
        }
        else if (fileProvider instanceof DomainObject) {
            keyValues.addMap(new DynamicDomainObjectProxy((DomainObject)fileProvider));
            log.debug("  fileProvider: {}", fileProvider);
        }

        if (domainObject instanceof Sample) {
            keyValues.put(ATTR_LABEL_SAMPLE_NAME, domainObject.getName());
            log.debug("  {}: {}", ATTR_LABEL_SAMPLE_NAME, domainObject.getName());
            keyValues.put(ATTR_LABEL_RESULT_NAME, resultName);
            log.debug("  {}: {}", ATTR_LABEL_RESULT_NAME, resultName);
        }
        else if (domainObject instanceof LSMImage) {
            List<DomainObject> mapped = DomainModelViewUtils.map(domainObject, MappingType.Sample);
            if (!mapped.isEmpty()) {
                Sample sample = (Sample)mapped.get(0);
                keyValues.addMap(new DynamicDomainObjectProxy(sample));
                log.debug("  sample: {}", domainObject);
                keyValues.put(ATTR_LABEL_SAMPLE_NAME, sample.getName());
                log.debug("  {}: {}", ATTR_LABEL_SAMPLE_NAME, sample.getName());
            }
        }
        else if (domainObject instanceof NeuronFragment) {
            NeuronFragment neuron = (NeuronFragment)domainObject;
            List<DomainObject> mapped = DomainModelViewUtils.map(neuron, MappingType.Sample);
            if (!mapped.isEmpty()) {
                Sample sample = (Sample)mapped.get(0);
                keyValues.addMap(new DynamicDomainObjectProxy(sample));
                log.debug("  sample: {}", domainObject);
                keyValues.put(ATTR_LABEL_SAMPLE_NAME, sample.getName());
                log.debug("  {}: {}", ATTR_LABEL_SAMPLE_NAME, sample.getName());
                
                List<NeuronSeparation> results = sample.getResultsById(NeuronSeparation.class, neuron.getSeparationId());
                if (!results.isEmpty()) {
                    NeuronSeparation separation = results.get(0);
                                                            
                    PipelineResult parentResult = separation.getParentResult();

                    keyValues.put(ATTR_LABEL_RESULT_NAME, parentResult.getName());
                    log.debug("  {}: {}", ATTR_LABEL_RESULT_NAME, parentResult.getName());
                    
                    if (parentResult instanceof HasAnatomicalArea) {
                        HasAnatomicalArea hasAA = (HasAnatomicalArea)parentResult;
                        keyValues.put(ATTR_LABEL_ANATOMICAL_AREA, hasAA.getAnatomicalArea());
                        log.debug("  {}: {}", ATTR_LABEL_ANATOMICAL_AREA, hasAA.getAnatomicalArea());
                    }

                    ObjectiveSample objectiveSample = parentResult.getParentRun().getParent();
                    keyValues.put(ATTR_LABEL_OBJECTIVE, objectiveSample.getObjective());
                    log.debug("  {}: {}", ATTR_LABEL_OBJECTIVE, objectiveSample.getObjective());
                }       
            }
        }
        
        log.debug("Filepath pattern: {}", filePattern);
        String filepath = StringUtils.replaceVariablePattern(filePattern, keyValues);
        log.debug("Interpolated filepath: {}", filepath);
        filepath = filepath.replaceAll("[^\\w\\.\\(\\)\\- /]", "_"); // Remove special characters
        filepath = filepath.replaceAll("^/+", ""); // Remove leading slashes which happen if path variables are not interpolated
        log.debug("Corrected filepath: {}", filepath);
        
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

    public Path getTargetFile() {
        return targetLocalPath;
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

    public boolean is3d() {
        return is3d;
    }

    @Override
    public String toString() {
    	if (errorMessage!=null) {
			return errorMessage;
		}
    	if (targetRelativePath == null) {
    		return "Error getting file for "+domainObject.getName();
    	}
    	return targetRelativePath;
    }
}