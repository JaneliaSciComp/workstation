package org.janelia.it.workstation.gui.browser.api;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.jacs.model.user_data.tiled_microscope_protobuf.TmProtobufExchanger;
import org.janelia.it.workstation.gui.browser.api.facade.impl.rest.TiledMicroscopeFacadeImpl;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TmWebClientTest {

    private static final Logger log = LoggerFactory.getLogger(TmWebClientTest.class);

    private static TiledMicroscopeFacadeImpl facade;
    private static TmProtobufExchanger exchanger;

    private static final String TEST_SAMPLE_CRUD_SAMPLE = "testSampleCRUD";
    private static final String TEST_WORKSPACE_CRUD_WORKSPACE = "testWorkspaceCRUD";
    private static final String TEST_NEURON_CRUD_WORKSPACE = "testNeuronCRUD";

    @BeforeClass
    public static void beforeClass() throws Exception {
        AccessManager.setSubjectKey("user:rokickik");
        facade = new TiledMicroscopeFacadeImpl();
        exchanger = new TmProtobufExchanger();
        cleanup();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        cleanup();
    }

    /**
     * Clean up any test data that was created but failed to be deleted.
     * @throws Exception
     */
    private static void cleanup() throws Exception {

        log.info("Cleaning up test data");

        Set<String> testSamples = Sets.newHashSet(TEST_SAMPLE_CRUD_SAMPLE);
        for(TmSample sample : facade.getTmSamples()) {
            if (testSamples.contains(sample.getName())) {
                log.info("Removing sample "+sample.getId());
                facade.remove(sample);
            }
        }

        Set<String> testWorkspaces = Sets.newHashSet(TEST_WORKSPACE_CRUD_WORKSPACE, TEST_NEURON_CRUD_WORKSPACE);
        for(TmWorkspace workspace : facade.getTmWorkspaces()) {
            if (testWorkspaces.contains(workspace.getName())) {
                for(TmNeuron neuron : facade.getWorkspaceNeurons(workspace.getId())) {
                    log.info("Removing neuron "+neuron.getId());
                    facade.remove(neuron);
                }
                log.info("Removing workspace "+workspace.getId());
                facade.remove(workspace);
            }
        }
    }

    @Test
    public void testSampleCRUD() throws Exception {

        String samplePath = "/dummy/sample/path";
        TmSample sample = new TmSample();
        sample.setName(TEST_SAMPLE_CRUD_SAMPLE);
        sample.setFilepath(samplePath);
        TmSample createdSample = facade.create(sample);
        assertNotNull(createdSample);
        assertEquals(samplePath, createdSample.getFilepath());

        sample = DomainUtils.findObjectByPath(facade.getTmSamples(), samplePath);
        assertNotNull(sample);

        String samplepath2 = "/new/path";
        sample.setFilepath(samplepath2);
        TmSample updatedSample = facade.update(sample);
        assertNotNull(updatedSample);
        assertEquals(samplepath2, updatedSample.getFilepath());

        facade.remove(sample);
        sample = DomainUtils.findObjectById(facade.getTmSamples(), updatedSample.getId());
        assertNull(sample);
    }

    @Test
    public void testWorkspaceCRUD() throws Exception {

        String workspaceName = TEST_WORKSPACE_CRUD_WORKSPACE;

        TmWorkspace workspace = new TmWorkspace();
        workspace.setName(workspaceName);
        TmWorkspace createdWorkspace = facade.create(workspace);
        assertNotNull(createdWorkspace);
        assertEquals(workspaceName, createdWorkspace.getName());

        workspace = DomainUtils.findObjectByName(facade.getTmWorkspaces(), workspaceName);
        assertNotNull(workspace);

        workspace.setSampleRef(Reference.createFor(TmSample.class, 1L));
        TmWorkspace updatedWorkspace = facade.update(workspace);
        assertNotNull(updatedWorkspace);
        assertEquals(workspaceName, updatedWorkspace.getName());
        assertEquals(Reference.createFor(TmSample.class, 1L), updatedWorkspace.getSampleRef());

        facade.remove(workspace);
        workspace = DomainUtils.findObjectById(facade.getTmWorkspaces(), workspace.getId());
        assertNull(workspace);
    }

    @Test
    public void testNeuronCRUD() throws Exception {

        String neuronName = "new neuron";
        String workspaceName = TEST_NEURON_CRUD_WORKSPACE;

        TmWorkspace workspace = new TmWorkspace();
        workspace.setName(workspaceName);
        workspace = facade.create(workspace);
        assertNotNull(workspace);
        assertEquals(workspaceName, workspace.getName());

        TmNeuron tmNeuron = new TmNeuron();
        tmNeuron.setName(neuronName);
        tmNeuron.setWorkspaceId(workspace.getId());
        tmNeuron.addRootAnnotation(1L);
        TmNeuronMetadata neuronMetadata = facade.create(tmNeuron);
        assertEquals(AccessManager.getSubjectKey(), neuronMetadata.getOwnerKey());
        assertEquals(Reference.createFor(TmWorkspace.class, workspace.getId()), neuronMetadata.getWorkspaceRef());

        List<TmNeuron> neurons = facade.getWorkspaceNeurons(workspace.getId());
        assertEquals(1, neurons.size());
        TmNeuron savedNeuron = neurons.get(0);

        assertEquals(AccessManager.getSubjectKey(), savedNeuron.getOwnerKey());
        assertEquals(workspace.getId(), savedNeuron.getWorkspaceId());
        assertTrue(savedNeuron.containsRootAnnotation(1L));
        assertFalse(savedNeuron.containsRootAnnotation(2L));

        tmNeuron.addRootAnnotation(2L);
        facade.update(tmNeuron);

        neurons = facade.getWorkspaceNeurons(workspace.getId());
        assertEquals(1, neurons.size());
        savedNeuron = neurons.get(0);
        assertTrue(savedNeuron.containsRootAnnotation(2L));

        facade.remove(tmNeuron);
        neurons = facade.getWorkspaceNeurons(workspace.getId());
        assertEquals(0, neurons.size());

        facade.remove(workspace);
        workspace = DomainUtils.findObjectById(facade.getTmWorkspaces(), workspace.getId());
        assertNull(workspace);
    }

}