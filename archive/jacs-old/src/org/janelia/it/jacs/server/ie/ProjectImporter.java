
package org.janelia.it.jacs.server.ie;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.mapping.SimpleValue;
import org.janelia.it.jacs.model.TimebasedIdentifierGenerator;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.download.*;
import org.janelia.it.jacs.model.metadata.Sample;
import org.janelia.it.jacs.server.ie.jaxb.*;
import org.janelia.it.jacs.server.utils.HibernateSessionSource;
import org.janelia.it.jacs.shared.utils.FileUtil;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * This class is responsible for reading all the projects in the directroy (xml_publication.iedir)
 * specified in jacs.properties and persisting the contents to the database
 * From jacs.properties
 * # Project data will be loaded from and written to this directory.  Use by Project Exporter/Importer utility classes only.
 #xml_project.ie.dir=C:/download/projects
 xml_project.ie.dir=
 # Publication data will be loaded from and written to this directory.  Use by Project Exporter/Importer utility classes only.
 #xml_publication.ie.dir=C:/download/publications
 xml_publication.ie.dir=
 *
 * @author Tareq Nabeel
 */
public class ProjectImporter {

    static Logger logger = Logger.getLogger(ProjectImporter.class.getName());

    private HibernateSessionSource sessionSource = new HibernateSessionSource();
    private String projectBaseDir = SystemConfigurationProperties.getString("xml_project.ie.dir");
    private String publicationBaseDir = SystemConfigurationProperties.getString("xml_publication.ie.dir");
    private Unmarshaller unmarshaller;
    private Session session;

    public static void main(String[] args) {
        ProjectImporter projectImporter = new ProjectImporter();
        projectImporter.changeIdGenStrategyForClasses();
        projectImporter.importData();
    }

    /**
     * This method is responsible for loading all the projects and publications on the
     * filesytem and persisting them to the database
     */
    private void importData() {
        session = sessionSource.getOrCreateSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        try {
            unmarshaller = createContextPackage().createUnmarshaller();
            importProjects();
            importPublications();
            transaction.commit();
        }
        catch (HibernateException e) {
            rollback(transaction, e);
        }
        catch (JAXBException e) {
            rollback(transaction, e);
        }
        catch (IOException e) {
            rollback(transaction, e);
        }
        finally {
            session.close();
        }
    }


    /**
     * This method is responsible for reading all the projects on the filesytem and persisting
     * them to the database
     */
    private void importProjects() throws JAXBException, IOException {
        List<Project> preExistingProjects = (List<Project>)session.createCriteria(Project.class).list();
        List<Project> projectsToImport = new ArrayList<Project>();
        // Get the list of projects on the filesystem to import
        List<File> projFilesList = this.loadFiles(projectBaseDir);
        for (File projectFile : projFilesList) {
            // Create the jaxb project from projectFile
            ProjectType jbProject = (ProjectType) unmarshaller.unmarshal(projectFile);

            // Publications are loaded separately from project because they're in separate xml files
            Project project = importProject(jbProject);

            projectsToImport.add(project);
        }

        deleteProjects(preExistingProjects, projectsToImport);
    }


    /**
     * This method creates a hibernate-aware project object based on the jaxb project loaded from projectFile
     *
     * @return project
     */
    private Project importProject(ProjectType jbProject) {
        Project project = (Project) session.get(Project.class, jbProject.getSymbol());
        if (project == null) {
            project = new Project();
            project.setSymbol(jbProject.getSymbol());
            session.save(project);
        }
        project.setDescription(jbProject.getDescription());
        project.setEmail(jbProject.getEmail());
        project.setFunded_by(jbProject.getFundedBy());
        project.setInstitutional_affiliation(jbProject.getInstitutionalAffiliation());
        project.setName(jbProject.getName());
        project.setOrganization(jbProject.getOrganization());
        project.setPrincipal_investigators(jbProject.getPrincipalInvestigators());
        project.setReleased(jbProject.isReleased());
        project.setWebsite_url(jbProject.getWebsiteUrl());
        return project;
    }


    /**
     * This method creates a list of hibernate-aware publication objects based on the jaxb list of
     * publications loaded from publicationFiles
     *
     * @throws JAXBException
     * @throws IOException
     */
    private void importPublications() throws JAXBException, IOException {
        List<File> pubFilesList = this.loadFiles(publicationBaseDir);

        List<Publication> preExistingPublications = (List<Publication>)session.createCriteria(Publication.class).list();
        List<Publication> publicationsToImport = new ArrayList<Publication>();
        for (File pubFile : pubFilesList) {
            PublicationType jbPublication = (PublicationType) unmarshaller.unmarshal(pubFile);
            Publication publication = importPublication(jbPublication);
            publicationsToImport.add(publication);
        }
        deletePublications(preExistingPublications, publicationsToImport);
    }

    /**
     * This method creates a hibernate-aware publication object based on teh jaxb publication loaded
     * from a publication file
     *
     * @param jbPublication
     * @return
     * @throws JAXBException
     */
    private Publication importPublication(PublicationType jbPublication) throws JAXBException {
        // If publication exists in the database, use it so it can be updated in the database with modified values
        // in the xml file.  Otherwise, create a new publication which will be inserted into database
        Publication publication = (Publication) session.get(Publication.class, jbPublication.getObjectId());
        if (publication == null) {
            publication = new Publication();
            if (jbPublication.getObjectId() > 0) {
                publication.setObjectId(jbPublication.getObjectId());
            }
            else {
                publication.setObjectId((Long) new TimebasedIdentifierGenerator().generate(null, null));
            }
            session.save(publication);
        }
        setProjects(publication, jbPublication);
        setPublicationAttributes(publication, jbPublication);
        setAuthors(publication, jbPublication);
        setDataFiles(publication, jbPublication.getDataFile());
        setHierarchyChildNodes(publication, jbPublication.getHierarchyNode());
        return publication;
    }

    /**
     * This method sets the projects for a publication
     *
     * @param publication
     * @param jbPublication
     */
    private void setProjects(Publication publication, PublicationType jbPublication) {
        List<Project> projectsToRetain = new ArrayList<Project>();
        for (PubProjectType jbPubProject : jbPublication.getProjects().getProject()) {
            // Project referenced in publication xml file has to exist in session.
            Project project = (Project) session.get(Project.class, jbPubProject.getSymbol());
            if (project == null) {
                continue;
            }
            projectsToRetain.add(project);
        }

        //Removal first
        for (Project currentProjectInDB : publication.getProjects()) {
            if (!projectsToRetain.contains(currentProjectInDB)) {
                currentProjectInDB.getPublications().remove(publication);
            }
        }

        publication.getProjects().retainAll(projectsToRetain);

        //Adds
        for (Project project : projectsToRetain) {
            if (!project.getPublications().contains(publication)) {
                project.getPublications().add(publication);
            }
        }

    }

    /**
     * This method sets the hibernate-aware publication attributes based on the supplied jaxb publication attributes
     *
     * @param publication
     * @param jbPublication
     */
    private void setPublicationAttributes(Publication publication, PublicationType jbPublication) {
        publication.setAbstractOfPublication(jbPublication.getAbstractOfPublication());
        publication.setDescription(jbPublication.getDescription());
        publication.setSubjectDocument(jbPublication.getFullText().getLocation());
        publication.setSupplementalText(jbPublication.getSupplementalText().getLocation());
        publication.setPublicationAccession(jbPublication.getPublicationAccession());
        publication.setSummary(jbPublication.getSummary());
        publication.setTitle(jbPublication.getTitle());
        publication.setPubDate(jbPublication.getPubDate());
        setJournalEntry(publication, jbPublication);
    }

    /**
     * This method sets the hibernate-aware publication authors based on the supplied
     * jaxb publication authors
     *
     * @param publication
     * @param jbPublication
     */
    private void setAuthors(Publication publication, PublicationType jbPublication) {
        List<Author> authors = new ArrayList<Author>();
        for (AuthorType jbAuthor : jbPublication.getAuthors().getAuthor()) {
            // If author exists in the database, use it so it can be updated in the database with modified values
            // in the xml file.  Otherwise, create a new author which will be inserted into database
            Author author = (Author) session.get(Author.class, jbAuthor.getName());
            if (author == null) {
                author = new Author();
                author.setName(jbAuthor.getName());
                session.save(author);
            }
            authors.add(author);
        }
        publication.setAuthors(authors);
    }

    /**
     * This method creates and sets the datafiles of the parentNode (hibernate-aware Publication or HierarchyNode)
     * based on the supplied list of jaxb dataFiles
     *
     * @param parentNode     Hibernate-aware Publication or HierarchyNode object
     * @param jbDataFileList
     */
    private void setDataFiles(Object parentNode, List<DataFileType> jbDataFileList) {
        clearDataFilesOfParentNode(parentNode);
        for (DataFileType jbDataFile : jbDataFileList) {
            // Create and add a hibernate-aware datafile object to parentNode's list of datafiles
            DataFile dataFile = (DataFile) session.get(DataFile.class, jbDataFile.getObjectId());
            if (dataFile == null) {
                dataFile = new DataFile();
                if (jbDataFile.getObjectId() > 0) {
                    dataFile.setObjectId(jbDataFile.getObjectId());
                }
                else {
                    dataFile.setObjectId((Long) new TimebasedIdentifierGenerator().generate(null, null));
                }
                session.save(dataFile);
            }
            // Set the state of this newly created hibernate-aware datafile based
            // on the supplied jaxb dataFile
            dataFile.setDescription(jbDataFile.getDescription());
            dataFile.setInfoLocation(jbDataFile.getInfoLocation());
            dataFile.setMultifileArchive(jbDataFile.isMultifileArchive());
            dataFile.setPath(jbDataFile.getPath());
            dataFile.setSize(jbDataFile.getSize());
            addSamples(dataFile, jbDataFile);
            addDataFileToParentNode(parentNode, dataFile);
        }
    }

    /**
     * This method creates and sets dataFile's hibernate-aware Sample objects
     * based on jbDatafile samples.
     *
     * @param dataFile
     * @param jbDataFile
     */
    private void addSamples(DataFile dataFile, DataFileType jbDataFile) {
        Set<Sample> samples = new HashSet<Sample>();
        for (SampleType jbSample : jbDataFile.getSample()) {
            Sample sample = (Sample) session.get(Sample.class, jbSample.getId());
            if (sample == null) {
                sample = new Sample();
                sample.setSampleId(jbSample.getId());
                session.save(sample);
            }
            samples.add(sample);
        }
        dataFile.setSamples(samples);
    }

    /**
     * This method adds the datafile to the list of datafiles of parentNode (hibernate-aware
     * Publication or HierarchyNode).
     *
     * @param parentNode
     * @param dataFile
     */
    private void addDataFileToParentNode(Object parentNode, DataFile dataFile) {
        List<DataFile> dataFiles;
        if (parentNode instanceof Publication) {
            dataFiles = ((Publication) parentNode).getRolledUpArchives();
        }
        else {
            dataFiles = ((HierarchyNode) parentNode).getDataFiles();
        }
        dataFiles.add(dataFile);
    }

    /**
     * This method clears the parent node's list items before new data files are added
     *
     * @param parentNode
     */
    private void clearDataFilesOfParentNode(Object parentNode) {
        List<DataFile> dataFiles;
        if (parentNode instanceof Publication) {
            dataFiles = ((Publication) parentNode).getRolledUpArchives();
        }
        else {
            dataFiles = ((HierarchyNode) parentNode).getDataFiles();
        }
        dataFiles.clear();
    }

    /**
     * This method creates and sets the hierarchy nodes of the parentNode (hibernate-aware Publication or HierarchyNode)
     * based on the supplied list of jaxb hierarchy nodes
     *
     * @param parentNode
     * @param jbHierarchyChildrenNodes
     * @throws JAXBException
     */
    private void setHierarchyChildNodes(Object parentNode, List<HierarchyNodeType> jbHierarchyChildrenNodes) throws JAXBException {
        clearHierarchyNodesOfParentNode(parentNode);
        for (HierarchyNodeType jbHierarchyChildNode : jbHierarchyChildrenNodes) {
            // Create a hibernate Hierarchy node based on the jaxb hierarchy node supplied to this method
            HierarchyNode hierarchyChildNode = createHibHierarchyChildNode(parentNode, jbHierarchyChildNode);
            // Create the tree recursively
            setHierarchyChildNodes(hierarchyChildNode, jbHierarchyChildNode.getHierarchyNode());
        }
    }

    /**
     * This method creates a jaxb Hierarchy Node object and sets it's state based on the state
     * of the supplied hibernate Hierarch Node object
     *
     * @param jbHierarchyNode
     * @return HierarchyNode
     */
    private HierarchyNode createHibHierarchyChildNode(Object parentNode, HierarchyNodeType jbHierarchyNode) {
        HierarchyNode hierarchyNode = (HierarchyNode) session.get(HierarchyNode.class, jbHierarchyNode.getObjectId());
        if (hierarchyNode == null) {
            hierarchyNode = new HierarchyNode();
            if (jbHierarchyNode.getObjectId() > 0) {
                hierarchyNode.setObjectId(jbHierarchyNode.getObjectId());
            }
            else {
                hierarchyNode.setObjectId((Long) new TimebasedIdentifierGenerator().generate(null, null));
            }
            session.save(hierarchyNode);
        }

        addHierarchyNodeToParentNode(parentNode, hierarchyNode);

        hierarchyNode.setName(jbHierarchyNode.getName());
        //hierarchyNode.setDescription(jbHierarchyNode.getDescription()); // Kevin Li found it redundant in the xml file
        hierarchyNode.setDescription("Directory");

        // Create the hibernate data files based on the jaxb data files
        setDataFiles(hierarchyNode, jbHierarchyNode.getDataFile());

        return hierarchyNode;
    }

    /**
     * This method adds the hierarchy node to the list of hierarchy nodes of parentNode (hibernate-aware
     * Publication or HierarchyNode).
     */
    private void addHierarchyNodeToParentNode(Object parentNode, HierarchyNode hierarchyNode) {
        List<HierarchyNode> hierarchyNodes;
        if (parentNode instanceof Publication) {
            hierarchyNodes = ((Publication) parentNode).getHierarchyRootNodes();
        }
        else {
            hierarchyNodes = ((HierarchyNode) parentNode).getChildren();
        }
        // Add the hib Hierarchy node to the list of hib hierarchy nodes
        hierarchyNodes.add(hierarchyNode);
    }

    /**
     * This method clears the hierarchy nodes of the parent node before new nodes are added
     *
     * @param parentNode
     */
    private void clearHierarchyNodesOfParentNode(Object parentNode) {
        List<HierarchyNode> hierarchyNodes;
        if (parentNode instanceof Publication) {
            hierarchyNodes = ((Publication) parentNode).getHierarchyRootNodes();
        }
        else {
            hierarchyNodes = ((HierarchyNode) parentNode).getChildren();
        }
        hierarchyNodes.clear();
    }


    /**
     * This method sets the hibernate-aware publication journal-entry based on the supplied
     * jaxb publication journal
     * This is temporary method until we figure out if we want to normalize journal attributes
     * into a separate table
     *
     * @param publication
     * @param jbPublication
     */
    private void setJournalEntry(Publication publication, PublicationType jbPublication) {
        JournalEntryType jbJournalEntry = jbPublication.getJournal();
        if (jbJournalEntry == null)
            return;

        // Set publication journal properties: <journal name="PLoS" volume="54" issue="3" page="30-33"/>
        final String delim = "/";
        String journal = jbJournalEntry.getName() + delim +
                jbJournalEntry.getVolume() + delim +
                jbJournalEntry.getIssue() + delim +
                jbJournalEntry.getPage();

        publication.setJournalEntry(journal);
    }


    /**
     * This method creates a JAXB Context Package
     *
     * @return
     * @throws JAXBException
     */
    private JAXBContext createContextPackage() throws JAXBException {
        String contextPackage = ObjectFactory.class.getPackage().toString();
        int spacepos = contextPackage.indexOf(' ');
        if (spacepos > -1)
            contextPackage = contextPackage.substring(spacepos + 1);

        return JAXBContext.newInstance(contextPackage);
    }

    /**
     * This method is responsible for loading the project and publication xml
     * files exported to the filestem
     *
     * @return
     * @throws IOException
     */
    private List<File> loadFiles(String dirName) throws IOException {
        //Pattern projectfilePattern = Pattern.compile("^w+(?:\\-[0-9])*\\.xml$");
        Pattern projectfilePattern = Pattern.compile("[A-z0-9\\-]+\\.xml$");

        List<File> filesList = new ArrayList<File>();
        File[] files = FileUtil.checkFileExists(dirName).listFiles();
        for (File file : files) {
            if (projectfilePattern.matcher(file.getName()).matches()) {
                filesList.add(file);
            }
            else {
                logger.warn("Ignoring unrecognized file: " + file.getName());
            }
        }
        return filesList;
    }


    private void changeIdGenStrategyForClasses() {
        changeIdGenStrategy(Publication.class.getName());
        changeIdGenStrategy(DataFile.class.getName());
        changeIdGenStrategy(HierarchyNode.class.getName());
    }

    private void changeIdGenStrategy(String className) {
        SimpleValue keyValue = (SimpleValue) sessionSource.
                getOrCreateConfiguration().
                getClassMapping(className).
                getIdentifier();

        keyValue.setIdentifierGeneratorStrategy("assigned");
        keyValue.setNullValue("undefined");
    }

    /**
     * Rolls back the transaction and logs the exception
     *
     * @param transaction
     * @param e
     */
    private void rollback(Transaction transaction, Exception e) {
        logger.error(getClass().getName(), e);
        e.printStackTrace();
        transaction.rollback();
    }

    /**
     * This method deletes publications in the database that don't exist on the filesystem.
     *
     * @param preExistingPublications
     * @param publicationsToImport
     */
    private void deletePublications(List<Publication> preExistingPublications, List<Publication> publicationsToImport) {
        for (Publication publication : preExistingPublications) {
            if (!publicationsToImport.contains(publication)) {
                // Necessary to avoid fk-constraint violation
                removePublicationFromProjects(publication);
                // For some reason, we need to call clear on authors even though Publication.hbm.xml
                // specified all for authors cascade level (also tried all-delete-orphan)
                publication.getAuthors().clear();
                publication.getRolledUpArchives().clear();
                publication.getHierarchyRootNodes().clear();
                // This will not be enough because we don't want cascade on this end to be true
                publication.getProjects().clear();
                session.delete(publication);
            }
        }
    }

    /**
     * This method deletes projects in the database that don't exist on the filesystem.
     *
     * @param preExistingProjects
     * @param projectsToImport
     */
    private void deleteProjects(List<Project> preExistingProjects, List<Project> projectsToImport) {
        for (Project project : preExistingProjects) {
            if (!projectsToImport.contains(project)) {
                for (Publication publication : project.getPublications()) {
                    publication.getProjects().remove(project);
                }
                project.getPublications().clear();
                session.delete(project);
                session.flush();
            }
        }
    }

    private void removePublicationFromProjects(Publication publication) {
        for (Project project : publication.getProjects()) {
            project.getPublications().remove(publication);
        }
    }

}





