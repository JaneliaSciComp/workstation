
package org.janelia.it.jacs.web.gwt.detail;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.collection.GWTEntityCleaner;
import org.janelia.it.jacs.model.genomics.*;

import java.io.File;
import java.text.ParseException;
import java.util.Set;


/**
 * Created by IntelliJ IDEA.
 * User: tnabeel
 * Date: Dec 26, 2006
 * Time: 1:56:26 PM
 */
public class ReadDetailTest extends TestCase {

    public static void main(String[] args) {
    // Test
    }

    private Session getHibernateSession() {
        File jarFile = new File("shared/testfiles/hibernate-configuration/hibernate.cfg.xml");
        Configuration configuration = new Configuration();
        configuration.configure(jarFile);
        SessionFactory sessionFactory = configuration.buildSessionFactory();
        Session session = sessionFactory.openSession();
        return session;
    }

    public ReadDetailTest(String name) {  
        super(name);
    }

    public void testReadRetrieval() throws ParseException {
        Session session=getHibernateSession();
        Transaction tx = session.beginTransaction();
        Read read = (Read) session.
                            getNamedQuery("findReadWithSequenceByBseEntityId").
                            setLong("entityId", 403393930L).
                            uniqueResult();

        // close session to test for lazy objects
        tx.commit();
        session.close();

        validateRead(read);

        try {
            assertNotNull(read.getBioSequence());
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertEquals(Integer.valueOf("114"), read.getSequenceLength());

    }

    public void testReadProxyFreeRetrieval() throws ParseException {
        Session session=getHibernateSession();
        Transaction tx = session.beginTransaction();
        Read read = (Read) session.
                            getNamedQuery("findReadWithLibraryByBseEntityId").
                            setLong("entityId", 403393930L).
                            uniqueResult();


        // Test proxy free
        //read = (Read)read.createProxyFreeCopy();

        GWTEntityCleaner.evictAndClean(read, session);

        tx.commit();
        session.close();

        validateRead(read);
    }

    public void testSequenceTextRetrieval() throws ParseException {
        Session session=getHibernateSession();
        Transaction tx = session.beginTransaction();
        String sequence = (String) session.
                                    getNamedQuery("findSequenceTextByBseEntityId").
                                    setLong("entityId", 403393930L).
                                    uniqueResult();
        // close session to test for lazy objects
        tx.commit();
        session.close();

        assertNotNull(sequence);
    }

    public void testSequenceRetrieval() throws ParseException {
        Session session=getHibernateSession();
        Transaction tx = session.beginTransaction();
        BioSequence sequence = (BioSequence) session.
                                                    getNamedQuery("findBioSequenceByBseEntityId").
                                                    setLong("entityId", 403393930L).
                                                    uniqueResult();
        // close session to test for lazy objects
        tx.commit();
        session.close();
        validateSequence(sequence);
    }

    public void testLibraryRetrieval() throws ParseException {
        Session session=getHibernateSession();
        Transaction tx = session.beginTransaction();
        Library library = (Library) session.
                getNamedQuery("findLibraryByBseEntityId").
                setLong("entityId", 403393930L).
                uniqueResult();

        // close session to test for lazy objects
        tx.commit();
        session.close();

        // Library
        validateLibrary(library);

        //validateSamples(library.getSamples());
    }

    public void testLibraryProxyFreeRetrieval() throws ParseException {
        Session session=getHibernateSession();
        Transaction tx = session.beginTransaction();
        Library library = (Library) session.getNamedQuery("findLibraryByBseEntityId").
                setLong("entityId", 403393930L).uniqueResult();


        GWTEntityCleaner.evictAndClean(library, session);

        tx.commit();
        session.close();

        // Library
        validateLibrary(library);

        //assertNull(library.getSamples());
        assertNull(library.getReads());
    }

    public void testSamplesByLibraryIdRetrieval() throws ParseException {
        Session session=getHibernateSession();
        Transaction tx = session.beginTransaction();
        Sample sample = (Sample) session.getNamedQuery("findSamplesWithSitesByLibraryId").
                setLong("libraryId", 281621516L).uniqueResult();

        GWTEntityCleaner.evictAndClean(sample,session);

        // close session to test for lazy objects
        tx.commit();
        session.close();

//        validateSample(sample);

        // Test for reattachment
        session=getHibernateSession();
        tx = session.beginTransaction();

        //session.update(sample);
        sample = (Sample)session.merge(sample); // does not work
        //session.lock(sample, LockMode.NONE); // does not work
        //session.refresh(sample);  // does not work
//        validateSample(sample);
        Library lib = (Library)sample.getLibraries().iterator().next();
        validateLibrary(lib);
        // Lazy samples should cause exception if createProxyFreeCopy does not
        // return a validate hibernate detached object
        tx.commit();
        session.close();

    }

    private void validateLibrary(Library library) {
        assertEquals(Long.valueOf("281621516"), library.getLibraryId());
        assertEquals("SCUMS_LIB_Arctic", library.getLibraryAcc());
        assertEquals("pyrosequencing (454)", library.getSequencingTechnology());
    }

//    private void validateSample(Sample sample) throws ParseException {
//        assertEquals(Long.valueOf(239426594), sample.getSampleId());
//        assertEquals("SCUMS_SMPL_Arctic", sample.getSampleAcc());
//
//        // Site
//        Set sites = sample.getBioMaterials();
//        assertTrue(sites.size() == 2);
//        for (Object site1 : sites) {
//            BioMaterial site = (BioMaterial) site1;
//            if (site.getMaterialAcc().equals("SCUMS_SITE_Arctic_CanadianArctic")) {
//                assertEquals("CAM_PROJ_MarineVirome", site.getProject());
//            }
//            else if (site.getMaterialAcc().equals("SCUMS_SITE_Arctic_ChukchiSea")) {
//                assertEquals("CAM_PROJ_MarineVirome", site.getProject());
//            }
//            else {
//                fail("Unknown site id " + site.getMaterialAcc());
//            }
//        }
//    }

    private void validateRead(Read read) {
        // GenericService bse data
        assertEquals("SCUMS_READ_Arctic2448841", read.getAccession());
        assertNull(read.getExternalAcc());
        //assertNull(read.getDescription());

        // Entity type
        EntityTypeGenomic entityType = read.getEntityType();
        assertEquals("Read", entityType.getName());
        assertEquals("READ", entityType.getAbbrev());
        assertEquals("", entityType.getDescription());
        assertEquals("NA", entityType.getSequenceType().getName());

        // Read specific data
        assertNull(read.getTraceAcc());
        assertNull(read.getTemplateAcc());
        assertNull(read.getSequencingDirection());
        assertNull(read.getClearRangeBegin());
        assertNull(read.getClearRangeEnd());


        // Library
        Library library = read.getLibrary();
        //validateLibrary(library);

        // Samples
//        validateSamples(read.getLibrary().getSamples());
    }

    private void validateSamples(Set samples) throws ParseException {
        assertTrue(samples.size() == 1);
        Sample sample = (Sample) samples.iterator().next();
//        validateSample(sample);
    }

    private void validateSequence(BioSequence sequence) {
        assertEquals("NA", sequence.getSequenceType().getName());
        assertEquals(Long.valueOf(403393932), sequence.getSequenceId());
    }


    public static Test suite() {
        return new TestSuite(ReadDetailTest.class);
    }

}