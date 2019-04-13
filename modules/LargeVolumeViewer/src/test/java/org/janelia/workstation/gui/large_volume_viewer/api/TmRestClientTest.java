package org.janelia.workstation.gui.large_volume_viewer.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmProtobufExchanger;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * Integration tests for the TiledMicroscopeRestClient.
 * 
 * Currently requires a running server and a configured client in the classpath. In the future these should be mocked.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TmRestClientTest {

    private static final Logger log = LoggerFactory.getLogger(TmRestClientTest.class);

    private static TiledMicroscopeRestClient client;
    private static TmProtobufExchanger exchanger;

    private static final String TEST_SAMPLE_CRUD_SAMPLE = "testSampleCRUD";
    private static final String TEST_SAMPLE_PATH = "/dummy/sample/path";
    private static final String TEST_WORKSPACE_CRUD_WORKSPACE = "testWorkspaceCRUD";
    private static final String TEST_NEURON_CRUD_WORKSPACE = "testNeuronCRUD";
    

    @BeforeClass
    public static void beforeClass() throws Exception {
        AccessManager.setSubjectKey("user:rokickik");
        client = new TiledMicroscopeRestClient();
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
        for(TmSample sample : client.getTmSamples()) {
            if (testSamples.contains(sample.getName())) {
                log.info("Removing sample "+sample.getId());
                client.remove(sample);
            }
        }

        Set<String> testWorkspaces = Sets.newHashSet(TEST_WORKSPACE_CRUD_WORKSPACE, TEST_NEURON_CRUD_WORKSPACE);
        for(TmWorkspace workspace : client.getTmWorkspaces()) {
            if (testWorkspaces.contains(workspace.getName())) {
                for(Pair<TmNeuronMetadata, InputStream> pair : client.getWorkspaceNeuronPairs(workspace.getId())) {
                    TmNeuronMetadata tmNeuronMetadata = pair.getLeft();
                    log.info("Removing neuron "+tmNeuronMetadata.getId());
                    client.remove(tmNeuronMetadata);
                }
                log.info("Removing workspace "+workspace.getId());
                client.remove(workspace);
            }
        }
    }

    @Test
    public void testSampleCRUD() throws Exception {

        TmSample sample = new TmSample();
        sample.setName(TEST_SAMPLE_CRUD_SAMPLE);
        sample.setFilepath(TEST_SAMPLE_PATH);
        TmSample createdSample = client.create(sample);
        assertNotNull(createdSample);
        assertEquals(TEST_SAMPLE_PATH, createdSample.getFilepath());

        sample = DomainUtils.findObjectByPath(client.getTmSamples(), TEST_SAMPLE_PATH);
        assertNotNull(sample);

        String samplepath2 = "/new/path";
        sample.setFilepath(samplepath2);
        TmSample updatedSample = client.update(sample);
        assertNotNull(updatedSample);
        assertEquals(samplepath2, updatedSample.getFilepath());

        client.remove(sample);
        sample = DomainUtils.findObjectById(client.getTmSamples(), updatedSample.getId());
        assertNull(sample);
    }

    @Test
    public void testWorkspaceCRUD() throws Exception {

        // Must have a sample in order to create a workspace
        TmSample sample = new TmSample();
        sample.setName(TEST_SAMPLE_CRUD_SAMPLE);
        sample.setFilepath(TEST_SAMPLE_PATH);
        TmSample createdSample = client.create(sample);
        
        // Test workspace creation
        TmWorkspace workspace = new TmWorkspace(TEST_WORKSPACE_CRUD_WORKSPACE, createdSample.getId());
        TmWorkspace createdWorkspace = client.create(workspace);
        assertNotNull(createdWorkspace);
        assertEquals(TEST_WORKSPACE_CRUD_WORKSPACE, createdWorkspace.getName());
        assertEquals(Reference.createFor(createdSample), createdWorkspace.getSampleRef());
        assertEquals(false, createdWorkspace.isAutoPointRefinement());
        assertEquals(false, createdWorkspace.isAutoTracing());
        
        // Make sure created workspace can be retrieved by name
        workspace = DomainUtils.findObjectByName(client.getTmWorkspaces(), TEST_WORKSPACE_CRUD_WORKSPACE);
        assertNotNull(workspace);

        // Test workspace update
        workspace.setAutoPointRefinement(true);
        TmWorkspace updatedWorkspace = client.update(workspace);
        assertNotNull(updatedWorkspace);
        assertEquals(TEST_WORKSPACE_CRUD_WORKSPACE, updatedWorkspace.getName());
        assertEquals(true, updatedWorkspace.isAutoPointRefinement());

        // Test workspace deletion
        client.remove(workspace);
        workspace = DomainUtils.findObjectById(client.getTmWorkspaces(), workspace.getId());
        assertNull(workspace);
        
        // Clean up
        client.remove(createdSample);
    }

    @Test
    public void testNeuronCRUD() throws Exception {

        // Must have a sample in order to create a workspace
        TmSample sample = new TmSample();
        sample.setName(TEST_SAMPLE_CRUD_SAMPLE);
        sample.setFilepath(TEST_SAMPLE_PATH);
        TmSample createdSample = client.create(sample);
        
        // Create a workspace
        TmWorkspace workspace = new TmWorkspace(TEST_NEURON_CRUD_WORKSPACE, createdSample.getId());
        workspace.setName(TEST_NEURON_CRUD_WORKSPACE);
        workspace = client.create(workspace);
        assertNotNull(workspace);
        assertEquals(TEST_NEURON_CRUD_WORKSPACE, workspace.getName());

        // Create a neuron
        TmNeuronMetadata tmNeuronMetadata = new TmNeuronMetadata(workspace, "new neuron");
        tmNeuronMetadata.addRootAnnotation(1L);
        byte[] protobufBytes = exchanger.serializeNeuron(tmNeuronMetadata);
        TmNeuronMetadata neuronMetadata = client.create(tmNeuronMetadata, new ByteArrayInputStream(protobufBytes));
        assertEquals(AccessManager.getSubjectKey(), neuronMetadata.getOwnerKey());
        assertEquals(Reference.createFor(TmWorkspace.class, workspace.getId()), neuronMetadata.getWorkspaceRef());

        // Retrieve the neuron
        Collection<Pair<TmNeuronMetadata, InputStream>> neurons = client.getWorkspaceNeuronPairs(workspace.getId());
        assertEquals(1, neurons.size());
        TmNeuronMetadata savedNeuron = unpack(neurons.iterator().next());
        assertEquals(AccessManager.getSubjectKey(), savedNeuron.getOwnerKey());
        assertEquals(workspace.getId(), savedNeuron.getWorkspaceRef().getTargetId());
        assertTrue(savedNeuron.containsRootAnnotation(1L));
        assertFalse(savedNeuron.containsRootAnnotation(2L));

        // Modify neuron
        savedNeuron.addRootAnnotation(2L);
        client.update(savedNeuron, new ByteArrayInputStream(exchanger.serializeNeuron(savedNeuron)));
        neurons = client.getWorkspaceNeuronPairs(workspace.getId());
        assertEquals(1, neurons.size());
        savedNeuron = unpack(neurons.iterator().next());
        assertTrue(savedNeuron.containsRootAnnotation(2L));

        // Remove neuron
        client.remove(savedNeuron);
        neurons = client.getWorkspaceNeuronPairs(workspace.getId());
        assertEquals(0, neurons.size());

        // Remove workspace
        client.remove(workspace);
        workspace = DomainUtils.findObjectById(client.getTmWorkspaces(), workspace.getId());
        assertNull(workspace);
        
        // Clean up
        client.remove(createdSample);
    }

    private TmNeuronMetadata unpack(Pair<TmNeuronMetadata, InputStream> pair) throws Exception {
        TmNeuronMetadata neuronMetadata = pair.getLeft();
        exchanger.deserializeNeuron(pair.getRight(), neuronMetadata);
        return neuronMetadata;
    }

}