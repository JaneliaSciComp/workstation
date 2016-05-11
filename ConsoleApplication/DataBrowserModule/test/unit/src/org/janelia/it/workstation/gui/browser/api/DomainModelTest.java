/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.browser.api;

import java.util.List;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentContext;
import org.janelia.it.workstation.gui.browser.api.facade.impl.rest.DomainFacadeImpl;
import org.janelia.it.workstation.gui.browser.api.facade.impl.rest.OntologyFacadeImpl;
import org.janelia.it.workstation.gui.browser.api.facade.impl.rest.SampleFacadeImpl;
import org.janelia.it.workstation.gui.browser.api.facade.impl.rest.SubjectFacadeImpl;
import org.janelia.it.workstation.gui.browser.api.facade.impl.rest.WorkspaceFacadeImpl;
import org.janelia.it.workstation.gui.browser.api.facade.interfaces.DomainFacade;
import org.janelia.it.workstation.gui.browser.api.facade.interfaces.OntologyFacade;
import org.janelia.it.workstation.gui.browser.api.facade.interfaces.SampleFacade;
import org.janelia.it.workstation.gui.browser.api.facade.interfaces.SubjectFacade;
import org.janelia.it.workstation.gui.browser.api.facade.interfaces.WorkspaceFacade;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author fosterl
 */
public class DomainModelTest {
    private DomainFacade domainFacade;
    private DomainModel domainModel;
    
    public DomainModelTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() throws Exception {
        domainFacade = new DomainFacadeImpl();
        OntologyFacade ontologyFacade = new OntologyFacadeImpl();
        SampleFacade sampleFacade = new SampleFacadeImpl();
        SubjectFacade subjectFacade = new SubjectFacadeImpl();
        WorkspaceFacade workspaceFacade = new WorkspaceFacadeImpl();
        /*
        DomainFacade domainFacade, OntologyFacade ontologyFacade, SampleFacade sampleFacade, 
            SubjectFacade subjectFacade, WorkspaceFacade workspaceFacade
         */
        domainModel = new DomainModel(domainFacade, ontologyFacade, sampleFacade, subjectFacade, workspaceFacade);
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testAllInstanceFetch() throws Exception {
        List<DomainObject> contexts = domainModel.getAllDomainObjectsByClass(AlignmentContext.class.getName());
        assertTrue("Empty context collection.", ! contexts.isEmpty());
        for (DomainObject dobj: contexts) {
            assertTrue("Not expected domain object type.  Not Alignment Context.", dobj instanceof AlignmentContext);
            AlignmentContext ctx = (AlignmentContext)dobj;
            assertNotNull("Null Alignment Space name", ctx.getAlignmentSpace());
            assertNotNull("Null Image Size", ctx.getImageSize());
            assertNotNull("Null Optical Resolution", ctx.getOpticalResolution());
            System.out.println(String.format("Name=%s, ImageSize=%s, OpticalRes=%s", ctx.getAlignmentSpace(), ctx.getImageSize(), ctx.getOpticalResolution()));
        }
    }

}
