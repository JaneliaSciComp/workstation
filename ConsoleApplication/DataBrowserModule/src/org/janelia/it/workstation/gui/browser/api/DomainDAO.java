package org.janelia.it.workstation.gui.browser.api;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.janelia.it.jacs.model.TimebasedIdentifierGenerator;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.compartments.CompartmentSet;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentBoard;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.domain.sample.Image;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.screen.FlyLine;
import org.janelia.it.jacs.model.domain.screen.PatternMask;
import org.janelia.it.jacs.model.domain.screen.ScreenSample;
import org.janelia.it.jacs.model.domain.support.MongoUtils;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.domain.workspace.Workspace;
import org.janelia.it.jacs.shared.utils.ReflectionUtils;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.jongo.Jongo;
import org.jongo.MongoCollection;
import org.jongo.MongoCursor;
import org.jongo.marshall.jackson.JacksonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.MapperFeature;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

/**
 * The main domain-object DAO for the JACS system.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainDAO {

    private static final Logger log = LoggerFactory.getLogger(DomainDAO.class);

    protected MongoClient m;
    protected Jongo jongo;

    protected MongoCollection alignmentBoardCollection;
    protected MongoCollection annotationCollection;
    protected MongoCollection compartmentSetCollection;
    protected MongoCollection dataSetCollection;
    protected MongoCollection flyLineCollection;
    protected MongoCollection fragmentCollection;
    protected MongoCollection imageCollection;
    protected MongoCollection objectSetCollection;
    protected MongoCollection ontologyCollection;
    protected MongoCollection patternMaskCollection;
    protected MongoCollection sampleCollection;
    protected MongoCollection screenSampleCollection;
    protected MongoCollection subjectCollection;
    protected MongoCollection treeNodeCollection;

    public DomainDAO(String serverUrl, String databaseName) throws UnknownHostException {
        this(serverUrl, databaseName, null, null);
    }

    public DomainDAO(String serverUrl, String databaseName, String username, String password) throws UnknownHostException {

        if (username!=null && password!=null) {
            MongoCredential credential = MongoCredential.createMongoCRCredential(username, databaseName, password.toCharArray());
            this.m = new MongoClient(new ServerAddress(serverUrl), Arrays.asList(credential));
            log.info("Connected to MongoDB ("+databaseName+"@"+serverUrl+") as user "+username);
        }
        else {
            this.m = new MongoClient(serverUrl);
            log.info("Connected to MongoDB ("+databaseName+"@"+serverUrl+")");
        }

        m.setWriteConcern(WriteConcern.JOURNALED);
        this.jongo = new Jongo(m.getDB(databaseName),
                new JacksonMapper.Builder()
                        .enable(MapperFeature.AUTO_DETECT_GETTERS)
                        .enable(MapperFeature.AUTO_DETECT_SETTERS)
                        .build());
        this.alignmentBoardCollection = registerCollectionByClass(AlignmentBoard.class);
        this.annotationCollection = registerCollectionByClass(Annotation.class);
        this.compartmentSetCollection = registerCollectionByClass(CompartmentSet.class);
        this.dataSetCollection = registerCollectionByClass(DataSet.class);
        this.flyLineCollection = registerCollectionByClass(FlyLine.class);
        this.fragmentCollection = registerCollectionByClass(NeuronFragment.class);
        this.imageCollection = registerCollectionByClass(Image.class);
        this.objectSetCollection = registerCollectionByClass(ObjectSet.class);
        this.ontologyCollection = registerCollectionByClass(Ontology.class);
        this.patternMaskCollection = registerCollectionByClass(PatternMask.class);
        this.sampleCollection = registerCollectionByClass(Sample.class);
        this.screenSampleCollection = registerCollectionByClass(ScreenSample.class);
        this.subjectCollection = registerCollectionByClass(Subject.class);
        this.treeNodeCollection = registerCollectionByClass(TreeNode.class);
    }

    private MongoCollection registerCollectionByClass(Class<?> domainClass) {
        String collectionName = getCollectionName(domainClass);
        return jongo.getCollection(collectionName);
    }

    public Jongo getJongo() {
        return jongo;
    }

    public MongoClient getMongo() {
        return m;
    }

    public void setWriteConcern(WriteConcern writeConcern) {
        m.setWriteConcern(writeConcern);
    }

    private Class<? extends DomainObject> getObjectClass(String collectionName) {
        return MongoUtils.getObjectClass(collectionName);
    }

    private String getCollectionName(Class<?> domainClass) {
        return MongoUtils.getCollectionName(domainClass);
    }

    private String getCollectionName(DomainObject domainObject) {
        return MongoUtils.getCollectionName(domainObject);
    }

    private MongoCollection getCollectionByName(String collectionName) {
        return jongo.getCollection(collectionName);
    }

    /**
     * Return all the subjects.
     */
    public List<Subject> getSubjects() {
        return toList(subjectCollection.find().as(Subject.class));
    }

    /**
     * Return the set of subjectKeys which are readable by the given subject. This includes the subject itself, and all of the groups it is part of. 
     */
    private Set<String> getSubjectSet(String subjectKey) {
        Subject subject = subjectCollection.findOne("{key:#}",subjectKey).projection("{_id:0,groups:1}").as(Subject.class);
        if (subject==null) throw new IllegalArgumentException("No such subject: "+subjectKey);
        Set<String> groups = subject.getGroups();
        groups.add(subjectKey);
        return groups;
    }

    /**
     * Create a list of the result set in iteration order.
     */
    private <T> List<T> toList(MongoCursor<? extends T> cursor) {
        List<T> list = new ArrayList<>();
        for(T item : cursor) {
            list.add(item);
        }
        return list;
    }

    /**
     * Create a list of the result set in the order of the given id list.
     */
    private List<DomainObject> toList(MongoCursor<? extends DomainObject> cursor, Collection<Long> ids) {
        List<DomainObject> list = new ArrayList<>(ids.size());
        Map<Long,DomainObject> map = new HashMap<>(ids.size());
        for(DomainObject item : cursor) {
            map.put(item.getId(), item);
        }
        for(Long id : ids) {
            DomainObject item = map.get(id);
            if  (item!=null) {
                list.add(item);
            }
        }
        return list;
    }

    /**
     * Get the domain object referenced by the type and id. 
     */
    public <T extends DomainObject> T getDomainObject(String subjectKey, Class<T> domainClass, Long id) {
        Reference reference = new Reference(getCollectionName(domainClass), id);
        return (T)getDomainObject(subjectKey, reference);
    }

    /**
     * Get the domain object referenced by the given Reference.
     */
    public DomainObject getDomainObject(String subjectKey, Reference reference) {
        List<Long> ids = new ArrayList<>();
        ids.add(reference.getTargetId());
        List<DomainObject> objs = getDomainObjects(subjectKey, reference.getTargetType(), ids);
        if (objs.isEmpty()) {
            return null;
        }
        return objs.get(0);
    }

    public <T extends DomainObject> T getDomainObject(String subjectKey, T domainObject) {
        Reference ref = new Reference();
        ref.setTargetId(domainObject.getId());
        ref.setTargetType(MongoUtils.getCollectionName(domainObject));
        return (T)getDomainObject(subjectKey, ref);
    }

    /**
     * Get the domain objects referenced by the given list of References.
     */
    public List<DomainObject> getDomainObjects(String subjectKey, List<Reference> references) {

        List<DomainObject> domainObjects = new ArrayList<>();
        if (references==null || references.isEmpty()) return domainObjects;

        log.trace("getDomainObjects(subjectKey="+subjectKey+",references.size="+references.size()+")");

        Multimap<String,Long> referenceMap = ArrayListMultimap.<String,Long>create();
        for(Reference reference : references) {
            if (reference==null) {
                log.warn("Requested null reference");
                continue;
            }
            referenceMap.put(reference.getTargetType(), reference.getTargetId());
        }

        for(String type : referenceMap.keySet()) {
            List<DomainObject> objs = getDomainObjects(subjectKey, type, referenceMap.get(type));
            //log.info("Found {} objects of type {}",objs.size(),type);
            domainObjects.addAll(objs);
        }

        return domainObjects;
    }

    /**
     * Get the domain objects of the given type and ids.
     */
    public List<DomainObject> getDomainObjects(String subjectKey, String type, Collection<Long> ids) {

        long start = System.currentTimeMillis();
        log.trace("getDomainObjects(subjectKey="+subjectKey+",type="+type+",ids.size="+ids.size()+")");

        Set<String> subjects = subjectKey==null?null:getSubjectSet(subjectKey);

        Class<? extends DomainObject> clazz = getObjectClass(type);
        if (clazz==null) {
            return new ArrayList<>();
        }

        MongoCursor<? extends DomainObject> cursor = null;
        if (subjects == null) {
            cursor = getCollectionByName(type).find("{_id:{$in:#}}", ids).as(clazz);
        }
        else {
            cursor = getCollectionByName(type).find("{_id:{$in:#},readers:{$in:#}}", ids, subjects).as(clazz);
        }

        List<DomainObject> list = toList(cursor, ids);
        log.trace("Getting "+list.size()+" "+type+" objects took "+(System.currentTimeMillis()-start)+" ms");
        return list;
    }

//    public List<DomainObject> getDomainObjects(String subjectKey, ReverseReference reverseRef) {
//        Set<String> subjects = subjectKey==null?null:getSubjectSet(subjectKey);
//        String type = reverseRef.getReferringType();
//
//        MongoCursor<? extends DomainObject> cursor = null;
//        if (subjects==null) {
//        	cursor = getCollectionByName(type).find("{'"+reverseRef.getReferenceAttr()+"':#}", reverseRef.getReferenceId()).as(getObjectClass(type));
//        }
//        else {
//        	cursor = getCollectionByName(type).find("{'"+reverseRef.getReferenceAttr()+"':#,readers:{$in:#}}", reverseRef.getReferenceId(), subjects).as(getObjectClass(type));
//        }
//        
//        List<DomainObject> list = toList(cursor);
//        if (list.size()!=reverseRef.getCount()) {
//            log.warn("Reverse reference ("+reverseRef.getReferringType()+":"+reverseRef.getReferenceAttr()+":"+reverseRef.getReferenceId()+
//                    ") denormalized count ("+reverseRef.getCount()+") does not match actual count ("+list.size()+")");
//        }
//        return list;
//    }

    public List<Annotation> getAnnotations(String subjectKey, Long targetId) {
        Set<String> subjects = subjectKey==null?null:getSubjectSet(subjectKey);

        MongoCursor<Annotation> cursor = null;
        if (subjects==null) {
            cursor = annotationCollection.find("{targetId:#}",targetId).as(Annotation.class);
        }
        else {
            cursor = annotationCollection.find("{targetId:#,readers:{$in:#}}",targetId,subjects).as(Annotation.class);
        }

        return toList(cursor);
    }

    public List<Annotation> getAnnotations(String subjectKey, Collection<Long> targetIds) {
        Set<String> subjects = getSubjectSet(subjectKey);
        return toList(annotationCollection.find("{targetId:{$in:#},readers:{$in:#}}",targetIds,subjects).as(Annotation.class));
    }

    public Workspace getDefaultWorkspace(String subjectKey) {
        return treeNodeCollection.findOne("{class:#,ownerKey:#}",Workspace.class.getName(),subjectKey).as(Workspace.class);
    }

    public Collection<Workspace> getWorkspaces(String subjectKey) {
        Set<String> subjects = getSubjectSet(subjectKey);
        return toList(treeNodeCollection.find("{class:#,readers:{$in:#}}",Workspace.class.getName(),subjects).as(Workspace.class));
    }

    public Collection<Ontology> getOntologies(String subjectKey) {
        Set<String> subjects = getSubjectSet(subjectKey);
        return toList(ontologyCollection.find("{readers:{$in:#}}",subjects).as(Ontology.class));
    }

//    public List<LSMImage> getLsmsBySampleId(String subjectKey, Long id) {
//        Set<String> subjects = getSubjectSet(subjectKey);
//        return toList(imageCollection.find("{sampleId:#,readers:{$in:#}}",id, subjects).as(LSMImage.class));
//    }
//    
//    public List<ScreenSample> getScreenSampleByFlyLine(String subjectKey, String flyLine) {
//        Set<String> subjects = getSubjectSet(subjectKey);
//        return toList(screenSampleCollection.find("{flyLine:{$regex:#},readers:{$in:#}}",flyLine+".*", subjects).as(ScreenSample.class));
//    }
//    
//    public List<NeuronFragment> getNeuronFragmentsBySampleId(String subjectKey, Long sampleId) {
//        Set<String> subjects = getSubjectSet(subjectKey);
//        return toList(fragmentCollection.find("{sampleId:#,readers:{$in:#}}",sampleId,subjects).as(NeuronFragment.class));
//    }
//    
//    public List<NeuronFragment> getNeuronFragmentsBySeparationId(String subjectKey, Long separationId) {
//        Set<String> subjects = getSubjectSet(subjectKey);
//        return toList(fragmentCollection.find("{separationId:#,readers:{$in:#}}",separationId,subjects).as(NeuronFragment.class));
//    }
//    
//    public List<ScreenSample> getScreenSamples(String subjectKey) {
//        Set<String> subjects = getSubjectSet(subjectKey);
//        return toList(screenSampleCollection.find("{readers:{$in:#}}",subjectKey,subjects).as(ScreenSample.class));
//    }
//    
//    public List<PatternMask> getPatternMasks(String subjectKey, Long screenSampleId) {
//        Set<String> subjects = getSubjectSet(subjectKey);
//        return toList(patternMaskCollection.find("{screenSampleId:#,readers:{$in:#}}",screenSampleId,subjects).as(PatternMask.class));
//    }
//   
//    public TreeNode getTreeNodeById(String subjectKey, Long id) {
//        Set<String> subjects = getSubjectSet(subjectKey);
//        return treeNodeCollection.findOne("{_id:#,readers:{$in:#}}",id,subjects).as(TreeNode.class);
//    }
//    
//    public TreeNode getParentTreeNodes(String subjectKey, Long id) {
//        Set<String> subjects = getSubjectSet(subjectKey);
//        return treeNodeCollection.findOne("{'children.targetId':#,readers:{$in:#}}",id,subjects).as(TreeNode.class);
//    }

    public void changePermissions(String subjectKey, String type, Collection<Long> ids, String granteeKey, String rights, boolean grant) throws Exception {
        String op = grant ? "addToSet" : "pull";
        String attr = rights.equals("w") ? "writers" : "readers";
        MongoCollection collection = getCollectionByName(type);

        String logIds = ids.size()<6 ? ""+ids : ids.size()+" ids";

        log.info("Changing permissions on all "+type+" documents with ids: "+logIds);
        WriteResult wr = collection.update("{_id:{$in:#},writers:#}",ids,subjectKey).multi().with("{$"+op+":{"+attr+":#}}",granteeKey);
        log.info("Changed permissions on "+wr.getN()+" documents");

//        log.debug("  updated "+wr.getN()+" "+type);

//        
//        if (wr.getN()!=ids.size()) { 
//            
//            Set<Long> idSet = new HashSet<Long>(ids);
//            Set<Long> returnSet = new HashSet<Long>();
//            
//            int i = 0;
//            for(Object o : collection.find("{_id:{$in:#}}",ids).as(getObjectClass(type))) {
//                Sample s = (Sample)o;
//                returnSet.add(s.getId());
//            }
//
//            log.warn("WARN: Changing permissions on "+ids.size()+" items only affected "+wr.getN()+" Got: "+i);
//            idSet.removeAll(returnSet);
//            
//            log.warn(idSet);
//        }

//        Grant on VT MCFO Case 1 and took 147653 ms
//        Revoke on VT MCFO Case 1 and took 149949 ms

//        Grant on VT MCFO Case 1 and took 142196 ms
//        Revoke on VT MCFO Case 1 and took 149955 ms

//        No writers check:
//        Grant on VT MCFO Case 1 and took 145893 ms
//        Revoke on VT MCFO Case 1 and took 146716 ms

//        Unacknowledged, with writers check:
//        Grant on VT MCFO Case 1 and took 147216 ms
//        Revoke on VT MCFO Case 1 and took 152989 ms


        if ("treeNode".equals(type)) {
            for(Long id : ids) {

                TreeNode node = collection.findOne("{_id:#,writers:#}",id,subjectKey).projection("{_id:0,class:1,children:1}").as(TreeNode.class);

                if (node==null) {
                    throw new IllegalArgumentException("Could not find folder with id="+id);
                }

                if (node.hasChildren()) {
                    Multimap<String,Long> groupedIds = HashMultimap.<String,Long>create();
                    for(Reference ref : node.getChildren()) {
                        groupedIds.put(ref.getTargetType(), ref.getTargetId());
                    }

                    for(String refType : groupedIds.keySet()) {
                        Collection<Long> refIds = groupedIds.get(refType);
                        changePermissions(subjectKey, refType, refIds, granteeKey, rights, grant);
                    }
                }
            }
        }
        else if ("sample".equals(type)) {
            log.info("Changing permissions on all fragments and lsms associated with samples: "+logIds);
            WriteResult wr1 = fragmentCollection.update("{sampleId:{$in:#},writers:#}",ids,subjectKey).multi().with("{$"+op+":{"+attr+":#}}",granteeKey);
            log.info("Updated permissions on "+wr1.getN()+" fragments");
            WriteResult wr2 = imageCollection.update("{sampleId:{$in:#},writers:#}",ids,subjectKey).multi().with("{$"+op+":{"+attr+":#}}",granteeKey);
            log.info("Updated permissions on "+wr2.getN()+" lsms");
        }
        else if ("screenSample".equals(type)) {
            log.info("Changing permissions on all patternMasks associated with screenSamples: "+logIds);
            patternMaskCollection.update("{screenSampleId:{$in:#},writers:#}",ids,subjectKey).multi().with("{$"+op+":{"+attr+":#}}}",granteeKey);
        }
    }

    private <T extends DomainObject> T saveImpl(String subjectKey, T domainObject) throws Exception {
        String type = getCollectionName(domainObject);
        MongoCollection collection = getCollectionByName(type);
        try {
            if (domainObject.getId() == null) {
                Long id = TimebasedIdentifierGenerator.generateIdList(1).get(0);
                domainObject.setId(id);
                domainObject.setOwnerKey(subjectKey);
                Set<String> subjects = new HashSet<>();
                subjects.add(subjectKey);
                domainObject.setReaders(subjects);
                domainObject.setWriters(subjects);
                domainObject.setCreationDate(new Date());
                domainObject.setUpdatedDate(new Date());
                collection.save(domainObject);
            }
            else {
                WriteResult result = collection.update("{_id:#,writers:#,updatedDate:#}", domainObject.getId(), subjectKey, domainObject.getUpdatedDate()).with(domainObject);
                if (result.getN()!=1) {
                    throw new IllegalStateException("Updated "+result.getN()+" records instead of one: "+type+"#"+domainObject.getId());
                }
            }
            log.info("Saved "+domainObject.getClass().getName()+"#"+domainObject.getId());
            return domainObject;
        }
        catch (MongoException e) {
            throw new Exception(e);
        }
    }

    public <T extends DomainObject> T save(String subjectKey, T domainObject) throws Exception {
        saveImpl(subjectKey, domainObject);
        return getDomainObject(subjectKey, domainObject);
    }

    public void remove(String subjectKey, DomainObject domainObject) throws Exception {
        
        String type = getCollectionName(domainObject);
        MongoCollection collection = getCollectionByName(type);
        
        WriteResult result = collection.remove("{_id:#,writers:#}", domainObject.getId(), subjectKey);
        if (result.getN()!=1) {
            throw new IllegalStateException("Deleted "+result.getN()+" records instead of one: "+type+"#"+domainObject.getId());
        }
        
        // TODO: remove dependant objects?
    }

    public Ontology reorderTerms(String subjectKey, Long ontologyId, Long parentTermId, List<Long> childOrder) throws Exception {
        
        Ontology ontology = getDomainObject(subjectKey, Ontology.class, ontologyId);
        if (ontology==null) {
            throw new IllegalArgumentException("Ontology not found: "+ontologyId);
        }
        OntologyTerm parent = findTerm(ontology, parentTermId);
        
        Map<Long,OntologyTerm> childMap = new HashMap<>();
        for(OntologyTerm child : parent.getTerms()) {
            childMap.put(child.getId(), child);
        }
        
        List<OntologyTerm> ordered = new ArrayList<>();
        for(Long id : childOrder) {
            OntologyTerm child = childMap.get(id);
            if (child==null) {
                log.warn("Ontology term {} does not exist in parent {}",id,parentTermId);
            }
            else {
                ordered.add(child);
            }
        }
        parent.setTerms(ordered);
        
        return save(subjectKey, ontology);
    }    

    public Ontology addTerm(String subjectKey, Long ontologyId, Long parentTermId, OntologyTerm term) throws Exception {
        if (term.getName()==null) {
            throw new IllegalArgumentException("Ontology term may not have null name");
        }
        if (term.getId()==null) {
            Long id = TimebasedIdentifierGenerator.generateIdList(1).get(0);
            term.setId(id);
        }
        Ontology ontology = getDomainObject(subjectKey, Ontology.class, ontologyId);
        if (ontology==null) {
            throw new IllegalArgumentException("Ontology not found: "+ontologyId);
        }
        OntologyTerm parent = findTerm(ontology, parentTermId);
        if (parent.getTerms()==null) {
            parent.setTerms(new ArrayList<OntologyTerm>());
        }
        parent.getTerms().add(term);
        return save(subjectKey, ontology);
    }

    public Ontology removeTerm(String subjectKey, Long ontologyId, Long termId) throws Exception {
        Ontology ontology = getDomainObject(subjectKey, Ontology.class, ontologyId);
        if (ontology==null) {
            throw new IllegalArgumentException("Ontology not found: "+ontologyId);
        }
        OntologyTerm removed = findTermAndRemove(ontology, termId);
        if (removed==null) {
            throw new Exception("Could not find term to remove: "+termId);
        }
        return save(subjectKey, ontology);
    }
    
    public TreeNode reorderChildren(String subjectKey, TreeNode treeNode, int[] order) throws Exception {

        if (!treeNode.hasChildren()) {
            log.warn("Tree node has no children to reorder: "+treeNode.getId());
            return treeNode;
        }

        List<Reference> references = new ArrayList<>(treeNode.getChildren());

        if (log.isTraceEnabled()) {
            log.trace("{} has the following references: ",treeNode.getName());
            for(Reference reference : references) {
                log.trace("  {}#{}",reference.getTargetType(),reference.getTargetId());
            }

            log.trace("They should be put in this ordering: ");
            for(int i=0; i<order.length; i++) {
                log.trace("  "+order[i]);
            }
        }

        int originalSize = references.size();
        Reference[] reordered = new Reference[references.size()];
        for (int i = 0; i < order.length; i++) {
            int j = order[i];
            Reference c = references.get(i);
            references.set(i, null);
            reordered[j] = c;
        }

        treeNode.getChildren().clear();
        for(Reference ref : reordered) {
            treeNode.getChildren().add(ref);
        }
        for(Reference ref : references) {
            if (ref!=null) {
                log.info("Adding broken ref of type "+ref.getTargetType()+" at the end");
                treeNode.getChildren().add(ref);
            }
        }

        if (references.size()!=originalSize) {
            log.error("Reordered children have new size "+references.size()+" (was "+originalSize+")");
        }
        else {
            saveImpl(subjectKey, treeNode);
        }
        return getDomainObject(subjectKey, treeNode);
    }

    public TreeNode addChildren(String subjectKey, TreeNode treeNode, Collection<Reference> references) throws Exception {
        if (references==null) {
            throw new IllegalArgumentException("Cannot add null children");
        }
        for(Reference ref : references) {
            if (ref.getTargetId()==null) {
                throw new IllegalArgumentException("Cannot add child without an id");
            }
            if (ref.getTargetType()==null) {
                throw new IllegalArgumentException("Cannot add child without a type");
            }
            treeNode.addChild(ref);
        }
        log.info("Adding "+references.size()+" objects to "+treeNode.getName());
        saveImpl(subjectKey, treeNode);
        return getDomainObject(subjectKey, treeNode);
    }

    public TreeNode removeChildren(String subjectKey, TreeNode treeNode, Collection<Reference> references) throws Exception {
        if (references==null) {
            throw new IllegalArgumentException("Cannot remove null children");
        }
        for(Reference ref : references) {
            if (ref.getTargetId()==null) {
                throw new IllegalArgumentException("Cannot add child without an id");
            }
            if (ref.getTargetType()==null) {
                throw new IllegalArgumentException("Cannot add child without a type");
            }
            treeNode.removeChild(ref);
        }
        log.info("Removing "+references.size()+" objects from "+treeNode.getName());
        saveImpl(subjectKey, treeNode);
        return getDomainObject(subjectKey, treeNode);
    }

    public TreeNode removeReference(String subjectKey, TreeNode treeNode, Reference reference) throws Exception {
        if (treeNode.hasChildren()) {
            for(Iterator<Reference> i = treeNode.getChildren().iterator(); i.hasNext(); ) {
                Reference iref = i.next();
                if (iref.equals(reference)) {
                    i.remove();
                }
            }
            saveImpl(subjectKey, treeNode);
        }
        return getDomainObject(subjectKey, treeNode);
    }

    public ObjectSet addMembers(String subjectKey, ObjectSet objectSet, Collection<Reference> references) throws Exception {
        if (references==null) {
            throw new IllegalArgumentException("Cannot add null members");
        }
        for(Reference ref : references) {
            if (ref.getTargetId()==null) {
                throw new IllegalArgumentException("Cannot add member without an id");
            }
            String type = ref.getTargetType();
            if (objectSet.getTargetType()==null) {
                if (ref.getTargetType()==null) {
                    throw new IllegalArgumentException("Cannot add member without a type");
                }
                objectSet.setTargetType(type);
            }
            else if (!type.equals(objectSet.getTargetType())) {
                throw new IllegalArgumentException("Cannot add reference to type "+type+" to object set of type "+objectSet.getTargetType());
            }
            objectSet.addMember(ref.getTargetId());
        }
        log.info("Adding "+references.size()+" objects to "+objectSet.getName());
        saveImpl(subjectKey, objectSet);
        return getDomainObject(subjectKey, objectSet);
    }

    public ObjectSet removeMembers(String subjectKey, ObjectSet objectSet, Collection<Reference> references) throws Exception {
        if (references==null) {
            throw new IllegalArgumentException("Cannot remove null members");
        }

        for(Reference ref : references) {
            if (ref.getTargetId()==null) {
                throw new IllegalArgumentException("Cannot remove member without an id");
            }
            objectSet.removeMember(ref.getTargetId());
        }
        log.info("Removing "+references.size()+" objects from "+objectSet.getName());
        saveImpl(subjectKey, objectSet);
        return getDomainObject(subjectKey, objectSet);
    }

    public DomainObject updateProperty(String subjectKey, DomainObject domainObject, String propName, String propValue) {
        try {
            ReflectionUtils.set(domainObject, propName, propValue);
        }
        catch (Exception e) {
            throw new IllegalStateException("Could not update object attribute "+propName,e);
        }
        String type = getCollectionName(domainObject);
        MongoCollection collection = getCollectionByName(type);
        WriteResult wr = collection.update("{_id:#,writers:#}",domainObject.getId(),subjectKey).with("{$set: {"+propName+":#, updatedDate:#}}",propValue,new Date());
        if (wr.getN()!=1) {
            log.warn("Could not update "+type+"#"+domainObject.getId()+"."+propName+": "+wr.getError());
        }
        return getDomainObject(subjectKey, domainObject);
    }
    
    // UTILITY METHODS
    
    private OntologyTerm findTerm(OntologyTerm term, Long termId) {
        if (term.getId().equals(termId)) {
            return term;
        }
        if (term.getTerms()!=null) {
            for(OntologyTerm child : term.getTerms()) {
                OntologyTerm found = findTerm(child, termId);
                if (found!=null) {
                    return found;
                }
            }
        }
        return null;
    }

    private OntologyTerm findTermAndRemove(OntologyTerm term, Long termId) {
        if (term.getTerms()!=null) {
            for(Iterator<OntologyTerm> iterator = term.getTerms().iterator(); iterator.hasNext(); ) {
                OntologyTerm child = iterator.next();
                if (child.getId().equals(termId)) {
                    iterator.remove();
                    return child;
                }
                OntologyTerm found = findTermAndRemove(child, termId);
                if (found!=null) {
                    return found;
                }
            }
        }
        return null;
    }
    
    // UNSECURE METHODS, SERVER SIDE ONLY
    // TODO: MOVE THESE ELSEWHERE

    /**
     * Get the domain objects of the given type 
     */
//    public <T extends DomainObject> MongoCursor<T> getDomainObjects(Class<T> domainClass) {
//        return registerCollectionByClass(domainClass).find().as(domainClass);
//    }

    /**
     * Get the domain objects of the given type 
     */
//    public MongoCursor<? extends DomainObject> getDomainObjects(String type) {
//        Class<? extends DomainObject> clazz = getObjectClass(type);
//        if (clazz==null) {
//        	throw new IllegalArgumentException("No object type for "+type);
//        }
//
//        return getCollectionByName(type).find().as(clazz);
//    }

    /**
     * Get the raw domain objects of the given type 
     */
//    public MongoCursor<DBObject> getRawObjects(String type) {
//        return getCollectionByName(type).find().map(new RawResultHandler<DBObject>());
//    }

//    public static void main(String[] args) throws Exception {
//        
//        String MONGO_SERVER_URL = "rokicki-ws";
//        String MONGO_DATABASE = "jacs";
//        DomainDAO dao = new DomainDAO(MONGO_SERVER_URL, MONGO_DATABASE);
//        Collection<Workspace> workspaces = dao.getWorkspaces("user:asoy");
//        for(Workspace workspace : workspaces) {
//            System.out.println(workspace.getId()+" "+workspace);
//        }
//    }

}