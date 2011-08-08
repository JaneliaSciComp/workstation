package org.janelia.it.FlyWorkstation.gui.ontology;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.types.Category;
import org.janelia.it.jacs.model.ontology.types.OntologyElementType;
import org.janelia.it.jacs.model.ontology.types.Tag;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLOntologyCreationIOException;
import org.semanticweb.owlapi.io.OWLParser;
import org.semanticweb.owlapi.io.OWLParserException;
import org.semanticweb.owlapi.io.UnparsableOntologyException;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;

/**
 * Parses OWL files and loads them into the Entity model as ontologies.
 * <p/>
 * May be involved directly with the loadAsEntities() method, or asynchronously as a worker task. In the case
 * of the worker task, it supports progress updates via PropertyChangeListener.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OWLDataLoader extends SimpleWorker {

    private static final int INDENT = 4;

    private final OWLOntologyManager manager;
    private final OWLReasonerFactory reasonerFactory;

    private OWLOntology ontology;
    private OWLReasoner reasoner;
    private String ontologyName;
    private int classCount = 0;

    private int classesDone = 0;

    private PrintStream out;
    private boolean saveObjects = true;
    private Entity root;

    protected OWLDataLoader() {
        this.manager = OWLManager.createOWLOntologyManager();
        this.reasonerFactory = new StructuralReasonerFactory();
    }

    public OWLDataLoader(String url) throws OWLException {
        this();
        IRI iri = IRI.create(url);
        init(manager.loadOntologyFromOntologyDocument(iri));
    }

    public OWLDataLoader(File file) throws OWLException {
        this();
        init(manager.loadOntologyFromOntologyDocument(file));
    }

    protected void init(OWLOntology ontology) throws OWLException {

        this.ontology = ontology;
        this.reasoner = reasonerFactory.createNonBufferingReasoner(ontology);

        String label = ontology.getOntologyID().toString();
        for (OWLAnnotation a : ontology.getAnnotations()) {
            if ("rdfs:label".equals(a.getProperty().toString())) {
                label = a.getValue().toString();
                break;
            }
        }
        label = label.replaceAll("\"", "").replaceAll("<", "").replaceAll(">", "");
        this.ontologyName = label;

        for (OWLClass cl : ontology.getClassesInSignature()) {
            if (reasoner.isSatisfiable(cl)) {
                classCount++;
            }
        }
    }

    public void setOutput(PrintStream out) {
        this.out = out;
    }

    public void setSaveObjects(boolean saveObjects) {
        this.saveObjects = saveObjects;
    }

    public String getOntologyName() {
        return ontologyName;
    }

    public void setOntologyName(String ontologyName) {
        this.ontologyName = ontologyName;
    }

    public int getClassCount() {
        return classCount + 1;
    }

    public Entity getResult() {
        return root;
    }

    private void incrementProgress() {
        classesDone++;
        int p = (int) Math.round(100 * ((double) classesDone / (double) getClassCount()));
        // There is always one more class than class count (i.e. the root), so the progress
        // will always go slightly above 100, but that's good because it lets the user have a glimpse of the
        // "Completed 100%" message before the progress dialog disappears.
        if (p > 100) p = 100;
        setProgress(p);
    }

    @Override
    protected void doStuff() throws Exception {
        root = loadAsEntities();
    }

    @Override
    protected void hadSuccess() {
    }

    @Override
    protected void hadError(Throwable error) {
        error.printStackTrace();
    }

    /**
     * Load the entire ontology into the Entity model with the given user as owner.
     *
     * @return the root Entity of the new ontology tree
     * @throws Exception
     */
    public Entity loadAsEntities() throws Exception {

        setProgress(0);

        IRI classIRI = OWLRDFVocabulary.OWL_THING.getIRI();
        OWLClass clazz = manager.getOWLDataFactory().getOWLClass(classIRI);

        root = saveObjects ? ModelMgr.getModelMgr().createOntologyRoot(SessionMgr.getUsername(), ontologyName) : new Entity();
        incrementProgress();

        if (out != null) out.println(ontologyName + " (Category saved as " + root.getId() + ")");

        if (isCancelled()) return root;

        loadAsEntities(root, clazz, 1, 0);


        if (out != null) {
            /* Now print out any unsatisfiable classes */
            for (OWLClass cl : ontology.getClassesInSignature()) {
                if (!reasoner.isSatisfiable(cl)) {
                    out.println("XXX: " + labelFor(cl));
                }
            }
        }

        reasoner.dispose();
        setProgress(100);

        return root;
    }

    private void loadAsEntities(Entity parentEntity, OWLClass clazz, int level, int orderIndex) throws Exception {

        if (out != null) {
            for (int i = 0; i < level * INDENT; i++) {
                out.print(" ");
            }
        }

        String label = labelFor(clazz);
        if (label.contains("#")) label = label.substring(label.indexOf("#") + 1);

        boolean hasChildren = false;
        for (OWLClass child : reasoner.getSubClasses(clazz, true).getFlattened()) {
            if (!child.equals(clazz)) {
                if (reasoner.isSatisfiable(child)) {
                    hasChildren = true;
                    break;
                }
            }
        }

        OntologyElementType type = hasChildren ? new Category() : new Tag();
        EntityData newData = saveObjects ? ModelMgr.getModelMgr().createOntologyTerm(SessionMgr.getUsername(), parentEntity.getId(), label, type, orderIndex) : new EntityData();
        incrementProgress();

        if (out != null) out.println(label + " (" + type.getName() + " saved as " + newData.getId() + ")");

        // Find the children and recurse
        int childOrder = 0;
        for (OWLClass child : reasoner.getSubClasses(clazz, true).getFlattened()) {
            if (!child.equals(clazz)) {
                if (reasoner.isSatisfiable(child)) {
                    loadAsEntities(newData.getChildEntity(), child, level + 1, childOrder++);
                    if (isCancelled()) return;
                }
            }
        }
    }

    private String labelFor(OWLClass clazz) {

        // Use a visitor to extract label annotations
        LabelExtractor le = new LabelExtractor();

        Set<OWLAnnotation> annotations = clazz.getAnnotations(ontology);
        for (OWLAnnotation anno : annotations) {
            anno.accept(le);
        }

        // Return the label if there is one. If not, just use the class URI.
        if (le.getResult() != null) {
            return le.getResult().toString();
        }
        else {
            return clazz.getIRI().toString();
        }
    }

    private class LabelExtractor implements OWLAnnotationObjectVisitor {

        private String result;

        @Override
        public void visit(OWLLiteral owlLiteral) {
        }

        @Override
        public void visit(OWLAnnotationAssertionAxiom owlAnnotationAssertionAxiom) {
        }

        @Override
        public void visit(OWLSubAnnotationPropertyOfAxiom owlSubAnnotationPropertyOfAxiom) {
        }

        @Override
        public void visit(OWLAnnotationPropertyDomainAxiom owlAnnotationPropertyDomainAxiom) {
        }

        @Override
        public void visit(OWLAnnotationPropertyRangeAxiom owlAnnotationPropertyRangeAxiom) {
        }

        @Override
        public void visit(IRI iri) {
        }

        @Override
        public void visit(OWLAnonymousIndividual owlAnonymousIndividual) {
        }

        public void visit(OWLAnnotation annotation) {
            /*
            * If it's a label, grab it as the result. Note that if there are
            * multiple labels, the last one will be used.
            */
            if (annotation.getProperty().isLabel()) {
                OWLLiteral c = (OWLLiteral) annotation.getValue();
                result = c.getLiteral();
            }
        }

        public String getResult() {
            return result;
        }
    }

    /**
     * Test harness
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        try {
            // Mouse anatomy = http://www.berkeleybop.org/ontologies/owl/MA
            // 46 - Amino acid = http://www.co-ode.org/ontologies/amino-acid/2006/05/18/amino-acid.owl
            // 83 - Phylogenetic = http://rest.bioontology.org/bioportal/ontologies/download/45588?applicationid=4ea81d74-8960-4525-810b-fa1baab576ff
            // 132 - Drosophila dev = http://www.berkeleybop.org/ontologies/owl/FBdv
            // 743 - FlyBase CV = http://www.berkeleybop.org/ontologies/owl/FBcv
            // 6599 - Flybase taxa = http://www.berkeleybop.org/ontologies/owl/FBsp

            OWLDataLoader loader = new OWLDataLoader("http://www.berkeleybop.org/ontologies/owl/FBdv");
            loader.setOutput(System.out);
            loader.setSaveObjects(false);
            loader.loadAsEntities();
        }
        catch (OWLOntologyCreationIOException e) {
            // IOExceptions during loading get wrapped in an OWLOntologyCreationIOException
            IOException ioException = e.getCause();
            if (ioException instanceof FileNotFoundException) {
                System.out.println("Could not load ontology. File not found: " + ioException.getMessage());
            }
            else if (ioException instanceof UnknownHostException) {
                System.out.println("Could not load ontology. Unknown host: " + ioException.getMessage());
            }
            else {
                System.out.println("Could not load ontology: " + ioException.getClass().getSimpleName() + " " + ioException.getMessage());
            }
        }
        catch (UnparsableOntologyException e) {
            // If there was a problem loading an ontology because there are syntax errors in the document (file) that
            // represents the ontology then an UnparsableOntologyException is thrown
            System.out.println("Could not parse the ontology: " + e.getMessage());
            // A map of errors can be obtained from the exception
            Map<OWLParser, OWLParserException> exceptions = e.getExceptions();
            // The map describes which parsers were tried and what the errors were
            for (OWLParser parser : exceptions.keySet()) {
                System.out.println("Tried to parse the ontology with the " + parser.getClass().getSimpleName() + " parser");
                System.out.println("Failed because: " + exceptions.get(parser).getMessage());
            }
        }
        catch (UnloadableImportException e) {
            // If our ontology contains imports and one or more of the imports could not be loaded then an
            // UnloadableImportException will be thrown (depending on the missing imports handling policy)
            System.out.println("Could not load import: " + e.getImportsDeclaration());
            // The reason for this is specified and an OWLOntologyCreationException
            OWLOntologyCreationException cause = e.getOntologyCreationException();
            System.out.println("Reason: " + cause.getMessage());
        }
        catch (OWLOntologyCreationException e) {
            System.out.println("Could not load ontology: " + e.getMessage());
        }
    }


}
