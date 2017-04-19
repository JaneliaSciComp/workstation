package org.janelia.it.workstation.gui.large_volume_viewer.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.gui.large_volume_viewer.api.model.dtw.DtwDecision;
import org.janelia.it.workstation.gui.large_volume_viewer.api.model.dtw.DtwGraph;
import org.janelia.it.workstation.gui.large_volume_viewer.api.model.dtw.DtwSession;
import org.janelia.it.workstation.gui.large_volume_viewer.api.model.dtw.DtwSessionType;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for the DirectedTracingWorkflowRestClient.
 * 
 * Currently requires a running server and a configured client in the classpath. In the future these should be mocked.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DtwRestClientTest {

    private static DirectedTracingWorkflowRestClient client;

    private static final String TEST_SAMPLE_PATH = "/dummy/sample/path9";

    @BeforeClass
    public static void beforeClass() throws Exception {
        AccessManager.setSubjectKey("user:rokickik");
        client = new DirectedTracingWorkflowRestClient();
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

    }

    @Test
    public void testGraphSessionCreation() throws Exception {

        DtwGraph graph = client.getLatestGraph(TEST_SAMPLE_PATH);
        
        if (graph == null || graph.getId()==null) {
            graph = new DtwGraph();
            graph.setSamplePath(TEST_SAMPLE_PATH);
            graph = client.create(graph);
            assertNotNull(graph);
            assertNotNull(graph.getCreationDate());
            assertEquals(TEST_SAMPLE_PATH, graph.getSamplePath());
        }
        
        DtwSession session = client.createSession(graph, DtwSessionType.AffinityLearning);
        assertNotNull(session);
        assertEquals(graph.getId(), session.getGraphId());
        assertNotNull(session.getStartDate());
        assertEquals(DtwSessionType.AffinityLearning.getLabel(), session.getSessionType());
     
        DtwDecision nextDecision = client.getNextDecision(session.getId());
        assertNotNull(nextDecision);
        assertNotNull(nextDecision.getBranches());
        assertNotNull(nextDecision.getChoices());
        assertNotNull(nextDecision.getOrderDate());
        assertNotNull(nextDecision.getViewingFocus());
        
    }


}