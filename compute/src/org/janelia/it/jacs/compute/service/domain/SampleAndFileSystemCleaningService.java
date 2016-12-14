package org.janelia.it.jacs.compute.service.domain;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jms.IllegalStateException;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.domain.DomainDAL;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.service.domain.util.SampleHelperNG;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.domain.DomainConstants;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.enums.PipelineStatus;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.PipelineError;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.support.DomainUtils;

/**
 * Removes redundant (unannotated, not final) results from Samples or Sub-Samples. In the process, this creates
 * now-unreferenced paths.  Cleans off those paths as well. Borrowed from Konrad Rokicki's service of similar name.
 *
 * @author <a href="mailto:fosterl@janelia.hhmi.org">Les Foster</a>
 */
public class SampleAndFileSystemCleaningService extends AbstractDomainService {

    public transient static final String CENTRAL_DIR_PROP = "FileStore.CentralDir";
    public transient static final String CENTRAL_ARCHIVE_DIR_PROP = "FileStore.CentralDir.Archived";

    public transient static final String PARAM_testRun = "is test run";

    private File userFilestore;
    private File archiveFilestore;

    private boolean isDebug = false;     //In dev, debug by default.
    private boolean archiveEnabled;   // TODO: figure out if this needs be addressed differently.
    private SampleHelperNG sampleHelper;
    private int numSamples = 0;
    private int numRunsDeleted = 0;
    private List<Long> deletedRunList = new ArrayList<>();
    private String username;

    public void execute() throws Exception {
        this.username = DomainUtils.getNameFromSubjectKey(ownerKey);
        this.userFilestore = new File(SystemConfigurationProperties.getString(CENTRAL_DIR_PROP) + File.separator + username + File.separator);
        this.archiveFilestore = new File(SystemConfigurationProperties.getString(CENTRAL_ARCHIVE_DIR_PROP) + File.separator + username + File.separator);

        logger.info("Synchronizing file share directory to DB: "+userFilestore.getAbsolutePath());

        if (isDebug) {
            logger.info("This is a test run. No files will be moved or deleted.");
        }
        else {
            logger.info("This is the real thing. Files will be moved and/or deleted!");
        }

        archiveEnabled = !userFilestore.getAbsolutePath().equals(archiveFilestore.getAbsolutePath());

        String testRun = task.getParameter(PARAM_testRun);
        if (testRun!=null) {
            isDebug = Boolean.parseBoolean(testRun);
        }
        //if (! isDebug) throw new IllegalArgumentException("May not run non-debug while in development!");

        this.sampleHelper = new SampleHelperNG(ownerKey, logger, contextLogger);

        String entityIdStr = task.getParameter(SampleTrashCompactorService.PARAM_sampleId);
        if (entityIdStr == null) {
            // All samples for user.
            logger.info("Cleaning old results from samples for user: "+ownerKey);
            List<Sample> samples = domainDao.getUserDomainObjects(ownerKey, Sample.class);
            logger.info("Will process "+samples.size()+" samples...");

            for(Sample sample : samples) {
                executeForOneSample(sample);
            }
        }
        else {
            // Only the given sample.
            Long sampleId = Long.parseLong(entityIdStr);
            Sample sample = domainDao.getDomainObject(ownerKey, Sample.class, sampleId);
            if (sample == null) {
                throw new IllegalArgumentException("No sample found for id " + sampleId);
            }
            logger.info("Cleaning old results from sample "+getDisplayIdent(sample)+" for user: "+ownerKey);
            executeForOneSample(sample);
        }

        logger.info("Considered "+numSamples+" samples. Deleted "+numRunsDeleted+" results.");
    }

    /**
     * Cleanup the given sample.
     *
     * @param sample with redundant information.
     * @throws Exception thrown by called methods.
     */
    public void executeForOneSample(Sample sample) throws Exception {
        Collection<String> deleteCandidateBasePaths = new HashSet<>();
        if (inAccessibleState(sample)) {
            try {
                processSample(sample, deleteCandidateBasePaths);
                carryOutFilesystemAndNodeDeletion(sample, deleteCandidateBasePaths);
            } catch (IllegalStateException ise) {
                logger.warn(ise.getMessage());
            }
            numSamples++;
        }
        else {
            logger.warn("Sample " + sample.getName() + ":" + sample.getId() + " in state " + sample.getStatus() + ", and not accessible for cleanup.  No action taken.");
        }
    }

    /**
     * Given the sample, find redundant runs, and delete them from the object model.  As doing so,
     * collect the base paths referenced by the runs.  Such paths are "candidates" for garbage
     * collection, since they may be referenced by other runs within the sample.
     *
     * @param sample has runs to be examined.
     * @param deleteCandidateBasePaths an output: add base paths for filesystem storage.
     * @throws Exception
     */
    private void processSample(Sample sample, Collection<String> deleteCandidateBasePaths) throws Exception {
        logger.debug("Cleaning up sample "+ getDisplayIdent(sample));
        int runsDeleted = 0;
        for(ObjectiveSample objectiveSample : sample.getObjectiveSamples()) {
            runsDeleted += processSample(objectiveSample, deleteCandidateBasePaths);
        }

        numRunsDeleted += runsDeleted;

        if (runsDeleted>0) {
            if (isDebug) {
                logger.info("Would have changed sample: "+getDisplayIdent(sample));
            }
            else {
                logger.info("Saving changes to sample: "+getDisplayIdent(sample));
                // Re-read sample, and verify in proper state before save.
                Sample testSample = (Sample)domainDao.getDomainObject(sample.getOwnerKey(), Sample.class.getSimpleName(), sample.getId());
                if (! inAccessibleState(testSample)) {
                    throw new IllegalStateException("Cannot save sample: " + getDisplayIdent(sample) + " inaccessible state: " + testSample.getStatus() + ", no changes.");
                }
                sampleHelper.saveSample(sample);
            }
        }
    }

    private boolean inAccessibleState(Sample sample) {
        // Null status is allowed.  Probably not encountered...
        return (sample != null)  && (!PipelineStatus.Processing.toString().equals(sample.getStatus()));
    }

    private int processSample(ObjectiveSample objectiveSample, Collection<String> deleteCandidateBasePaths) throws Exception {

        List<SamplePipelineRun> runs = objectiveSample.getPipelineRuns();
        if (runs.isEmpty()) return 0;

        // Group by pipeline process
        Map<String,List<SamplePipelineRun>> processRunMap = new HashMap<>();

        getAreaRuns(runs, processRunMap);

        int numDeleted = 0;
        for(String process : processRunMap.keySet()) {
            List<SamplePipelineRun> processRuns = processRunMap.get(process);
            if (processRuns.isEmpty()) continue;

            logger.debug("  Processing "+objectiveSample.getObjective()+" pipeline runs for process: "+process);

            // Remove latest run, we don't want to touch it
            SamplePipelineRun lastRun = processRuns.remove(processRuns.size()-1);

            if (lastRun.hasError()) {
                logger.info("    Keeping last error run: "+lastRun.getId() + " in sample " + getDisplayIdent(objectiveSample));
                // Last run had an error, let's keep that, but still try to find a good run to keep
                Collections.reverse(processRuns);
                Integer keeper = null;
                int curr = 0;
                for(SamplePipelineRun pipelineRun : processRuns) {
                    if (!pipelineRun.hasError()) {
                        keeper = curr;
                        break;
                    }
                    curr++;
                }
                if (keeper!=null) {
                    SamplePipelineRun lastGoodRun = processRuns.remove(keeper.intValue());
                    logger.info("    Keeping last good run: "+lastGoodRun.getId() + " in sample " + getDisplayIdent(objectiveSample));

                }
                else {
                    logger.info("    Could not find a good run to keep in sample " + getDisplayIdent(objectiveSample));
                }
            }
            else {
                logger.debug("    Keeping last good run: "+lastRun.getId() + " in sample " + getDisplayIdent(objectiveSample));
            }

            // Clean up everything else
            numDeleted += deleteUnannotated(objectiveSample, processRuns, deleteCandidateBasePaths);
        }

        return numDeleted;
    }

    private String getDisplayIdent(ObjectiveSample objectiveSample) {
        return getDisplayIdent(objectiveSample.getParent());
    }

    private String getDisplayIdent(Sample sample) {
        return sample.getName()+" (id="+sample.getId()+")";
    }

    /**
     * Build up the mapping of process-to-run.
     */
    private void getAreaRuns(List<SamplePipelineRun> runs, Map<String, List<SamplePipelineRun>> processRunMap) {
        for(SamplePipelineRun pipelineRun : runs) {
            String process = pipelineRun.getPipelineProcess();
            if (process == null) process = "";
            List<SamplePipelineRun> areaRuns = processRunMap.get(process);
            if (areaRuns == null) {
                areaRuns = new ArrayList<>();
                processRunMap.put(process,areaRuns);
            }
            areaRuns.add(pipelineRun);
        }
    }

    private int deleteUnannotated(ObjectiveSample objectiveSample, List<SamplePipelineRun> toDelete, Collection<String> deleteCandidateBasePaths) throws ComputeException {

        Set<SamplePipelineRun> toReallyDelete = new HashSet<>();
        for(SamplePipelineRun pipelineRun : toDelete) {

            long numFound = getNumNeuronsAnnotated(pipelineRun);
            if (numFound>0) {
                logger.info("    Rejecting candidate "+pipelineRun.getId()+" because it contains neurons with "+numFound+" annotations");
                continue;
            }
            toReallyDelete.add(pipelineRun);
        }

        if (toReallyDelete.isEmpty()) return 0;
        logger.info("    Found "+toReallyDelete.size()+" non-annotated results for deletion:");
        for(SamplePipelineRun child : toReallyDelete) {
            objectiveSample.removeRun(child);
            logger.debug("    Delete run: " + child.getId() + " from " + objectiveSample.getParent().getName());
            deletedRunList.add(child.getId());
            getBasePathsUnderRun(child, deleteCandidateBasePaths);
        }
        logger.info("    Deleted "+toReallyDelete.size()+" pipeline runs");
        return toReallyDelete.size();
    }

    private long getNumNeuronsAnnotated(SamplePipelineRun pipelineRun) {

        int numAnnotations = 0;
        for(PipelineResult result : pipelineRun.getResults()) {
            for(NeuronSeparation separation : result.getResultsOfType(NeuronSeparation.class)) {
                // TODO: is this going to be too slow? we should be able to go directly to the annotations.
                List<DomainObject> neurons = domainDao.getDomainObjects(ownerKey, separation.getFragmentsReference());
                List<Reference> references = DomainUtils.getReferences(neurons);
                numAnnotations += domainDao.getAnnotations(null, references).size();
            }
        }

        return numAnnotations;
    }

    private void getBasePathsUnderRun(SamplePipelineRun pipelineRun, Collection<String> basePaths) {
        PipelineError pipelineError = pipelineRun.getError();
        if (pipelineError != null  &&  pipelineError.getFilepath() != null) {
            basePaths.add(pipelineError.getFilepath());
        }
        recursivelyFindResultPaths(pipelineRun.getResults(), basePaths);
    }

    /**
     * Descend through all pipeline results, finding all paths.  If the top-level is being deleted, then all
     * its descendants' filepaths should go away (if not used elsewhere).
     *
     * @param pipelineResults collection which may have base paths.
     * @param basePaths grow this collection of paths.
     */
    private void recursivelyFindResultPaths(List<PipelineResult> pipelineResults, Collection<String> basePaths) {
        for (PipelineResult result : pipelineResults) {
            basePaths.add(result.getFilepath());
            recursivelyFindResultPaths(result.getResults(), basePaths);
        }
    }

    /**
     * Return all the base paths from all runs under this sample.
     *
     * @param sample has runs
     * @return paths found.
     */
    private List<String> getAllSampleBasePaths(Sample sample) {
        List<String> sampleBasePaths = new ArrayList<>();
        for(ObjectiveSample objectiveSample : sample.getObjectiveSamples()) {
            List<SamplePipelineRun> runs = objectiveSample.getPipelineRuns();
            if (! runs.isEmpty()) {
                // Group by pipeline process
                Map<String,List<SamplePipelineRun>> processRunMap = new HashMap<>();

                getAreaRuns(runs, processRunMap);

                for(String process : processRunMap.keySet()) {
                    List<SamplePipelineRun> processRuns = processRunMap.get(process);
                    for (SamplePipelineRun spr: processRuns) {
                        getBasePathsUnderRun(spr, sampleBasePaths);
                    }
                }
            }
        }
        return sampleBasePaths;
    }

    /**
     * Walk through sample, getting all the base paths that are still in use in what is left of the sample.
     * Then remove all such paths from the deleted-path candidate list.  Whatever remains is validly deleted.
     */
    private void carryOutFilesystemAndNodeDeletion(Sample sample, Collection<String> deleteCandidateBasePaths) {
        List<String> preservedBasePaths = getAllSampleBasePaths(sample);
        List<String> finalDeletionList = new ArrayList<>(deleteCandidateBasePaths);
        finalDeletionList.removeAll(preservedBasePaths);

        for (String filePath: finalDeletionList) {
            if (new File(filePath).getAbsolutePath().startsWith(userFilestore.getAbsolutePath())) {
                deleteFileNode(filePath);
            }
            else if (archiveEnabled  &&
                    new File(filePath).getAbsolutePath().startsWith(archiveFilestore.getAbsolutePath())) {
                deleteFileNode(filePath);
            }
            else {
                logger.warn("Rejecting file from wrong base path/sample/owner: " + filePath + " " + sample.getName() + " " + sample.getOwnerKey());
            }
        }
    }

    /**
     * Assumption here: file paths end in node ids, and the nodes backing those IDs also have pointing back to the
     * file path.
     *
     * @param filePath used as a guide for finding node.
     */
    private void deleteFileNode(String filePath) {
        try {
            Long nodeId = getNodeIdFromPath(filePath);
            if (!isDebug) {
                if (null == computeBean.getNodeById(nodeId)) {
                    logger.warn("No existing node found for ID " + nodeId + ".  Perhaps previously deleted.");
                }
                else {
                    computeBean.deleteNode(username, nodeId, (!isDebug));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.warn("Failed to deprecated file node representing " + filePath);
        }
    }

    public Long getNodeIdFromPath(String filePath) {
        String[] pathParts = filePath.split("/");
        for (int i = pathParts.length - 1; i >= 0; i--) {
            // Looking for at least 4 digits, probably many more.
            if (pathParts[i].matches("[1-9][0-9][0-9][0-9]+")) {
                return Long.parseLong(pathParts[i]);
            }
        }
        throw new IllegalArgumentException("Not a GUID.");
    }
}
