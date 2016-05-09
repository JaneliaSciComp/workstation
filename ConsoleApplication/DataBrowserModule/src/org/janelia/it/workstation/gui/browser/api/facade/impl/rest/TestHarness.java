package org.janelia.it.workstation.gui.browser.api.facade.impl.rest;

/**
 * Implementation of the DomainFacade using secure RESTful connection
 *
 * TODO: needs to be updated to use the new facade API 
 *
 * @author <a href="mailto:schauderd@janelia.hhmi.org">David Schauder</a>
 */
public class TestHarness {

    public static void main(String[] args) throws Exception {
        // SOME QUICKIE TESTS BEFORE UNIT TESTS ARE ADDED

//        String REST_SERVER_URL = "http://schauderd-ws1.janelia.priv:8080/compute/";
//        TestHarness testclient = new TestHarness(REST_SERVER_URL);
//        Sample test = (Sample)testclient.getDomainObject(org.janelia.it.jacs.model.domain.sample.Sample.class, new Long("1734424924644180066"));
//        System.out.println (test.getLine());
//        test = (Sample)testclient.getDomainObject(new Reference("org.janelia.it.jacs.model.domain.sample.Sample", new Long("1714569876758069346")));
//        System.out.println(test.getLine());
//        List<Reference> referenceList = new ArrayList<>();
//        referenceList.add(new Reference("org.janelia.it.jacs.model.domain.sample.Sample", new Long("1714569876758069346")));
//        referenceList.add(new Reference("org.janelia.it.jacs.model.domain.sample.Sample", new Long("1734424924644180066")));
//        List<DomainObject> test2 = testclient.getDomainObjects(referenceList);
//        System.out.println(test2);
//        DomainObject foo = testclient.updateProperty(test, "effector", "QUACKTHEDUCK");
//        System.out.println(((Sample) foo).getEffector());
//
//        // annotations
//        Annotation testAnnotation = new Annotation();
//        testAnnotation.setKey("Partly_OK");
//        testAnnotation.setTarget(new Reference("org.janelia.it.jacs.model.domain.sample.Sample", new Long("1714569876758069346")));
//        testAnnotation.setName("TESTANNOTATION");
//        OntologyTermReference ontTest = new OntologyTermReference();
//        ontTest.setOntologyId(new Long("1870641514531520601"));
//        ontTest.setOntologyTermId(new Long("1898740256916635737"));
//        testAnnotation.setKeyTerm(ontTest);
//        Annotation newAnnotation = testclient.create(testAnnotation);
//        System.out.println(newAnnotation.getName());
//        referenceList = new ArrayList<>();
//        referenceList.add(new Reference("org.janelia.it.jacs.model.domain.sample.Sample", new Long("1714569876758069346")));
//        referenceList.add(new Reference("org.janelia.it.jacs.model.domain.sample.Sample", new Long("1714569876758069234")));
//        List<Annotation> annotationList = testclient.getAnnotations(referenceList);
//        System.out.println(annotationList);
//        testclient.remove(newAnnotation);
//
//        // workspaces
//        Workspace workspace = testclient.getDefaultWorkspace();
//        System.out.println (workspace.getChildren());
//        Collection<Workspace> workspaces = testclient.getWorkspaces();
//        System.out.println (workspaces);
//
//        // ontologies
//        Collection<Ontology> ontologies = testclient.getOntologies();
//        System.out.println (ontologies);
//        Ontology ont = new Ontology();
//        List<OntologyTerm> terms = new ArrayList<>();
//        Category category = new Category();
//        category.setName("IISCATEGORY");
//        terms.add(category);
//        Ontology newOnt = testclient.create(ont);
//        newOnt.setTerms(new ArrayList<OntologyTerm>());
//        System.out.println(newOnt.getId());
//
//        // ontology terms
//        newOnt = testclient.addTerms(newOnt.getId(),newOnt.getId(), terms, 0);
//        System.out.println(newOnt.getTerms());
//        List<OntologyTerm> categoryList = new ArrayList<>();
//        Tag tag = new Tag();
//        tag.setName("IISTAG1");
//        categoryList.add(tag);
//        tag = new Tag();
//        tag.setName("IISTAG2");
//        categoryList.add(tag);
//        newOnt = testclient.addTerms(newOnt.getId(),newOnt.getTerms().get(0).getId(), categoryList, 0);
//        testclient.reorderTerms(newOnt.getId(), newOnt.getTerms().get(0).getId(), new int[]{1, 0});
//        OntologyTerm testTerm = newOnt.getTerms().get(0).getTerms().get(0);
//        newOnt = testclient.removeTerm(newOnt.getId(),newOnt.getTerms().get(0).getId(), testTerm.getId());
//        System.out.println(newOnt.getTerms().get(0).getTerms());
//        testclient.removeOntology(newOnt.getId());
//
//        // filter
//        Filter newFilter = new Filter();
//        newFilter.setSearchString("whatevers");
//        newFilter.setSearchClass("whateversclass");
//        List<Criteria> critList = new ArrayList<>();
//        TreeNodeCriteria crit = new TreeNodeCriteria();
//        crit.setTreeNodeName("objectName");
//        crit.setTreeNodeReference(new Reference("org.janelia.it.jacs.model.domain.sample.Sample", new Long("1714569876758069346")));
//        critList.add(crit);
//        newFilter.setCriteriaList(critList);
//        newFilter = testclient.create(newFilter);
//        System.out.println (newFilter.getId());
//        newFilter.setName("WACKAMOLE");
//        newFilter = testclient.update(newFilter);
//        System.out.println (newFilter.getName());
//
//        // treenodes
//        TreeNode treenode = new TreeNode();
//        treenode = testclient.create(treenode);
//        System.out.println (treenode.getId());
//        treenode = testclient.addChildren(treenode, referenceList, new Integer (0));
//        System.out.println (treenode.getNumChildren());
//        int[] order = {1,0};
//        treenode = testclient.reorderChildren(treenode, order);
//        System.out.println (treenode.getChildren().get(0).getTargetId());
//        treenode = testclient.removeChildren(treenode, referenceList);
//        System.out.println (treenode.getNumChildren());
//
//        // objectsets
//        ObjectSet objectset = new ObjectSet();
//        objectset = testclient.create(objectset);
//        System.out.println (objectset.getId());
//        objectset = testclient.addMembers(objectset, referenceList);
//        System.out.println (objectset.getNumMembers());
//        objectset = testclient.removeMembers(objectset, referenceList);
//        System.out.println (objectset.getNumMembers());
//
//        // user settings
//        List<Subject> subjects = testclient.getSubjects();
//        System.out.println(subjects);
//        List<Preference> preferences = testclient.getPreferences();
//        System.out.println (preferences);
//        Preference newPref = new Preference();
//        newPref.setCategory("TEST");
//        newPref.setKey("test");
//        newPref.setValue("value");
//        newPref = testclient.savePreference(newPref);
//        System.out.println (newPref.getCategory());
//        objectset = (ObjectSet)testclient.changePermissions(objectset, "user:schauderd", "write", true);
//        System.out.println (objectset.getWriters());
    }
}
