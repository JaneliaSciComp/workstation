package org.janelia.it.FlyWorkstation.gui.ontology;

import org.janelia.it.FlyWorkstation.gui.framework.api.EJBFactory;
import org.janelia.it.jacs.model.entity.Entity;
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;

/**
 * Loads an OWL file into the Entity schema as an ontology.
 */
public class OWLDataLoader {

    private static int INDENT = 4;

    private static boolean DRYRUN = false;

    private OWLReasonerFactory reasonerFactory;

    private OWLOntology ontology;

    private PrintStream out;

    public OWLDataLoader(OWLOntologyManager manager, OWLReasonerFactory reasonerFactory)
            throws OWLException, MalformedURLException {
        this.reasonerFactory = reasonerFactory;
        out = System.out;
    }

    private String labelFor( OWLClass clazz) {
        /*
         * Use a visitor to extract label annotations
         */
        LabelExtractor le = new LabelExtractor();
        Set<OWLAnnotation> annotations = clazz.getAnnotations(ontology);
        for (OWLAnnotation anno : annotations) {
            anno.accept(le);
        }
        /* Print out the label if there is one. If not, just use the class URI */
        if (le.getResult() != null) {
            return le.getResult().toString();
        } else {
            return clazz.getIRI().toString();
        }
    }

    /**
     * Print the class hierarchy for the given ontology from this class down, assuming this class is at
     * the given level. Makes no attempt to deal sensibly with multiple
     * inheritance.
     */
    public void printHierarchy(OWLOntology ontology,  OWLClass clazz) throws OWLException {
        OWLReasoner reasoner = reasonerFactory.createNonBufferingReasoner(ontology);
        this.ontology = ontology;

        String label = ontology.getOntologyID().toString();
        for(OWLAnnotation a: ontology.getAnnotations()) {
            if ("rdfs:label".equals(a.getProperty().toString())) {
                label = a.getValue().toString();
                break;
            }
        }
        label = label.replaceAll("\"","").replaceAll("<", "").replaceAll(">","");

        Entity newNode = DRYRUN ? new Entity() : EJBFactory.getRemoteAnnotationBean().createOntologyRoot(
                System.getenv("USER"), label);

        out.println(label+" (saved as "+newNode.getId()+")");

        printHierarchy(reasoner, newNode, null, clazz, 1);

        /* Now print out any unsatisfiable classes */
        for (OWLClass cl: ontology.getClassesInSignature()) {
            if (!reasoner.isSatisfiable(cl)) {
                out.println("XXX: " + labelFor(cl));
            }
        }
        reasoner.dispose();
    }

    /**
     * Print the class hierarchy from this class down, assuming this class is at
     * the given level. Makes no attempt to deal sensibly with multiple
     * inheritance.
     */
    public void printHierarchy(OWLReasoner reasoner, Entity parentEntity, OWLClass parent, OWLClass clazz, int level)
            throws OWLException {
        /*
         * Only print satisfiable classes -- otherwise we end up with bottom
         * everywhere
         */
        if (reasoner.isSatisfiable(clazz)) {
            for (int i = 0; i < level * INDENT; i++) {
                out.print(" ");
            }

            String label = labelFor(clazz);
            if (label.contains("#")) label = label.substring(label.indexOf("#")+1);
            
            Entity newNode = DRYRUN ? new Entity() : EJBFactory.getRemoteAnnotationBean().createOntologyTerm(
                    System.getenv("USER"), parentEntity.getId().toString(), label);

            out.println(label+" (saved as "+newNode.getId()+")");

            /* Find the children and recurse */
            for (OWLClass child : reasoner.getSubClasses(clazz, true).getFlattened()) {
                if (!child.equals(clazz)) {
                    printHierarchy(reasoner, newNode, clazz, child, level + 1);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        try {
            // Get hold of an ontology manager
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

            // Mouse anatomy = http://www.berkeleybop.org/ontologies/owl/MA
            // 46 - Amino acid = http://www.co-ode.org/ontologies/amino-acid/2006/05/18/amino-acid.owl
            // 83 - Phylogenetic = http://rest.bioontology.org/bioportal/ontologies/download/45588?applicationid=4ea81d74-8960-4525-810b-fa1baab576ff
            // 132 - Drosophila dev = http://www.berkeleybop.org/ontologies/owl/FBdv
            // 743 - FlyBase CV = http://www.berkeleybop.org/ontologies/owl/FBcv
            // 6599 - Flybase taxa = http://www.berkeleybop.org/ontologies/owl/FBsp

            // Let's load an ontology from the web
            IRI iri = IRI.create("http://www.berkeleybop.org/ontologies/owl/FBdv");
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(iri);
            System.out.println("Loaded ontology: " + ontology);

            // We can always obtain the location where an ontology was loaded from
            IRI documentIRI = manager.getOntologyDocumentIRI(ontology);
            System.out.println("from: " + documentIRI);

            // / Create a new SimpleHierarchy object with the given reasoner.
            OWLDataLoader simpleHierarchy = new OWLDataLoader(
                    manager, new StructuralReasonerFactory());

            // Get Thing
            IRI classIRI = OWLRDFVocabulary.OWL_THING.getIRI();
            OWLClass clazz = manager.getOWLDataFactory().getOWLClass(classIRI);

            // Print the hierarchy below thing
            simpleHierarchy.printHierarchy(ontology, clazz);

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

    private class LabelExtractor implements OWLAnnotationObjectVisitor {

        String result;

        public LabelExtractor() {
            result = null;
        }

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

}
