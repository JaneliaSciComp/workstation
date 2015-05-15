package org.janelia.it.workstation.gui.browser.api;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ReverseReference;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.compartments.CompartmentSet;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentBoard;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.domain.sample.Image;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.screen.FlyLine;
import org.janelia.it.jacs.model.domain.screen.PatternMask;
import org.janelia.it.jacs.model.domain.screen.ScreenSample;
import org.janelia.it.jacs.model.domain.support.MongoUtils;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.domain.workspace.Workspace;
import org.jongo.Jongo;
import org.jongo.MongoCollection;
import org.jongo.MongoCursor;
import org.jongo.marshall.jackson.JacksonMapper;

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
import java.util.HashSet;
import org.janelia.it.jacs.model.TimebasedIdentifierGenerator;

/**
 * The main domain-object DAO for the JACS system.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainDAO {

    private static final Logger log = Logger.getLogger(DomainDAO.class);
    
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
        this.alignmentBoardCollection = getCollectionByClass(AlignmentBoard.class);
        this.annotationCollection = getCollectionByClass(Annotation.class);
        this.compartmentSetCollection = getCollectionByClass(CompartmentSet.class);
        this.dataSetCollection = getCollectionByClass(DataSet.class);
        this.flyLineCollection = getCollectionByClass(FlyLine.class);
        this.fragmentCollection = getCollectionByClass(NeuronFragment.class);
        this.imageCollection = getCollectionByClass(Image.class);
        this.objectSetCollection = getCollectionByClass(ObjectSet.class);
        this.ontologyCollection = getCollectionByClass(Ontology.class);
        this.patternMaskCollection = getCollectionByClass(PatternMask.class);
        this.sampleCollection = getCollectionByClass(Sample.class);
        this.screenSampleCollection = getCollectionByClass(ScreenSample.class);
    	this.subjectCollection = getCollectionByClass(Subject.class);
    	this.treeNodeCollection = getCollectionByClass(TreeNode.class);
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

    public Class<? extends DomainObject> getObjectClass(String collectionName) {
        return MongoUtils.getObjectClass(collectionName);
    }

    public String getCollectionName(Class<?> domainClass) {
        return MongoUtils.getCollectionName(domainClass);
    }
    
    public String getCollectionName(DomainObject domainObject) {
        return MongoUtils.getCollectionName(domainObject);
    }
    
    public MongoCollection getCollectionByName(String collectionName) {
        return jongo.getCollection(collectionName);
    }

    public MongoCollection getCollectionByClass(Class<?> domainClass) {
        return jongo.getCollection(getCollectionName(domainClass));
    }
    
    /** 
     * Return the set of subjectKeys which are readable by the given subject. This includes the subject itself, and all of the groups it is part of. 
     */
    public Set<String> getSubjectSet(String subjectKey) {
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
        List<T> list = new ArrayList<T>();
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
     * Get the domain objects referenced by the given ObjectSet/
     */
    public List<DomainObject> getDomainObjects(String subjectKey, ObjectSet objectSet) {

        List<DomainObject> domainObjects = new ArrayList<>();
        if (objectSet.getMembers()==null || objectSet.getMembers().isEmpty()) return domainObjects;
        
        List<Long> members = objectSet.getMembers();
        
        log.trace("getDomainObjects(subjectKey="+subjectKey+",references.size="+members.size()+")");
  
        Multimap<String,Long> referenceMap = ArrayListMultimap.<String,Long>create();
        for(Long member : members) {
            if (member==null) {
                log.warn("Requested null member id");
                continue;
            }
            referenceMap.put(objectSet.getTargetType(), member);
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
        // TODO: remove this after the next db load fixes it
        if ("workspace".equals(type)) type = "treeNode"; 
        
        long start = System.currentTimeMillis();
        log.trace("getDomainObjects(subjectKey="+subjectKey+",type="+type+",ids.size="+ids.size()+")");

        Set<String> subjects = subjectKey==null?null:getSubjectSet(subjectKey);

        Class<? extends DomainObject> clazz = getObjectClass(type);
        if (clazz==null) {
//            throw new IllegalArgumentException("No object type for "+type);
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

    public List<DomainObject> getDomainObjects(String subjectKey, ReverseReference reverseRef) {
        Set<String> subjects = subjectKey==null?null:getSubjectSet(subjectKey);
        String type = reverseRef.getReferringType();

        MongoCursor<? extends DomainObject> cursor = null;
        if (subjects==null) {
        	cursor = getCollectionByName(type).find("{'"+reverseRef.getReferenceAttr()+"':#}", reverseRef.getReferenceId()).as(getObjectClass(type));
        }
        else {
        	cursor = getCollectionByName(type).find("{'"+reverseRef.getReferenceAttr()+"':#,readers:{$in:#}}", reverseRef.getReferenceId(), subjects).as(getObjectClass(type));
        }
        
        List<DomainObject> list = toList(cursor);
        if (list.size()!=reverseRef.getCount()) {
            log.warn("Reverse reference ("+reverseRef.getReferringType()+":"+reverseRef.getReferenceAttr()+":"+reverseRef.getReferenceId()+
                    ") denormalized count ("+reverseRef.getCount()+") does not match actual count ("+list.size()+")");
        }
        return list;
    }
    
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
    
    public Collection<Workspace> getWorkspaces(String subjectKey) {
        Set<String> subjects = getSubjectSet(subjectKey);
        return toList(treeNodeCollection.find("{class:#,readers:{$in:#}}",Workspace.class.getName(),subjects).as(Workspace.class));
    }

    public Collection<Ontology> getOntologies(String subjectKey) {
        Set<String> subjects = getSubjectSet(subjectKey);
        return toList(ontologyCollection.find("{readers:{$in:#}}",subjects).as(Ontology.class));
    }

    public List<LSMImage> getLsmsBySampleId(String subjectKey, Long id) {
        Set<String> subjects = getSubjectSet(subjectKey);
        return toList(imageCollection.find("{sampleId:#,readers:{$in:#}}",id, subjects).as(LSMImage.class));
    }
    
    public List<ScreenSample> getScreenSampleByFlyLine(String subjectKey, String flyLine) {
        Set<String> subjects = getSubjectSet(subjectKey);
        return toList(screenSampleCollection.find("{flyLine:{$regex:#},readers:{$in:#}}",flyLine+".*", subjects).as(ScreenSample.class));
    }
    
    public List<NeuronFragment> getNeuronFragmentsBySampleId(String subjectKey, Long sampleId) {
        Set<String> subjects = getSubjectSet(subjectKey);
        return toList(fragmentCollection.find("{sampleId:#,readers:{$in:#}}",sampleId,subjects).as(NeuronFragment.class));
    }
    
    public List<NeuronFragment> getNeuronFragmentsBySeparationId(String subjectKey, Long separationId) {
        Set<String> subjects = getSubjectSet(subjectKey);
        return toList(fragmentCollection.find("{separationId:#,readers:{$in:#}}",separationId,subjects).as(NeuronFragment.class));
    }
    
    public List<ScreenSample> getScreenSamples(String subjectKey) {
        Set<String> subjects = getSubjectSet(subjectKey);
        return toList(screenSampleCollection.find("{readers:{$in:#}}",subjectKey,subjects).as(ScreenSample.class));
    }
    
    public List<PatternMask> getPatternMasks(String subjectKey, Long screenSampleId) {
        Set<String> subjects = getSubjectSet(subjectKey);
        return toList(patternMaskCollection.find("{screenSampleId:#,readers:{$in:#}}",screenSampleId,subjects).as(PatternMask.class));
    }
   
    public TreeNode getTreeNodeById(String subjectKey, Long id) {
        Set<String> subjects = getSubjectSet(subjectKey);
        return treeNodeCollection.findOne("{_id:#,readers:{$in:#}}",id,subjects).as(TreeNode.class);
    }
    
    public void changePermissions(String subjectKey, String type, Long id, String granteeKey, String rights, boolean grant) throws Exception {
        Collection<Long> ids = new ArrayList<>();
        ids.add(id);
        changePermissions(subjectKey, type, ids, granteeKey, rights, grant);
    }
    
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

    public void save(String subjectKey, DomainObject domainObject) throws Exception {
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
                collection.update("{_id:#,writers:#,updatedDate:#}", domainObject.getId(), subjectKey, domainObject.getUpdatedDate()).with(domainObject);
            }
            log.info("Saved "+domainObject.getClass().getName()+"#"+domainObject.getId());
        }
        catch (MongoException e) {
            throw new Exception(e);
        }
    }

    public void reorderChildren(String subjectKey, TreeNode treeNode, int[] order) throws Exception {
        
        List<Reference> references = new ArrayList<>(treeNode.getChildren());
        
//        log.info("{} has the following references: ",treeNode.getName());
//        for(Reference reference : references) {
//            log.info("  {}#{}",reference.getTargetType(),reference.getTargetId());
//        }
//        
//        log.info("They should be put in this ordering: ");
//        for(int i=0; i<order.length; i++) {
//            log.info("  "+order[i]);
//        }
        
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
            save(subjectKey, treeNode);    
        }
    }

    public void addChild(String subjectKey, TreeNode treeNode, DomainObject domainObject) throws Exception {
        if (domainObject==null) {
            throw new IllegalArgumentException("Cannot add null child");
        }
        if (domainObject.getId()==null) {
            throw new IllegalArgumentException("Cannot add child without an id");
        }
        Reference ref = new Reference();
        ref.setTargetId(domainObject.getId());
        ref.setTargetType(getCollectionName(domainObject));
        treeNode.getChildren().add(ref);
        save(subjectKey, treeNode);
    }
    
    public void removeChild(String subjectKey, TreeNode treeNode, DomainObject domainObject) throws Exception {
        if (domainObject==null) {
            throw new IllegalArgumentException("Cannot remove null child");
        }
        if (domainObject.getId()==null) {
            throw new IllegalArgumentException("Cannot remove child without an id");
        }
        Long targetId = domainObject.getId();
        String targetType = getCollectionName(domainObject);
        Reference reference = new Reference(targetType, targetId);
        removeReference(subjectKey, treeNode, reference);
    }
    
    public void removeReference(String subjectKey, TreeNode treeNode, Reference reference) throws Exception {
        for(Iterator<Reference> i = treeNode.getChildren().iterator(); i.hasNext(); ) {
            Reference iref = i.next();
            if (iref.equals(reference)) {
                i.remove();
            }
        }
        save(subjectKey, treeNode);
    }
    
    public void updateProperty(String subjectKey, DomainObject domainObject, String propName, String propValue) {
        String type = getCollectionName(domainObject);
        MongoCollection collection = getCollectionByName(type);
        WriteResult wr = collection.update("{_id:#,writers:#}",domainObject.getId(),subjectKey).with("{$set: {"+propName+":#, updatedDate:#}}",propValue,new Date());
        if (wr.getN()!=1) {
            log.warn("Could not update "+type+"#"+domainObject.getId()+"."+propName+": "+wr.getError());
        }
    }
    
    
    // UNSECURE METHODS, SERVER SIDE ONLY
    // TODO: MOVE THESE ELSEWHERE

    /**
     * Get the domain objects of the given type 
     */
    public <T extends DomainObject> MongoCursor<T> getDomainObjects(Class<T> domainClass) {
        return getCollectionByClass(domainClass).find().as(domainClass);
    }
    
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
    
    public static void main(String[] args) throws Exception {
        
        String MONGO_SERVER_URL = "rokicki-ws";
        String MONGO_DATABASE = "jacs";
        DomainDAO dao = new DomainDAO(MONGO_SERVER_URL, MONGO_DATABASE);
        Collection<Workspace> workspaces = dao.getWorkspaces("user:asoy");
        for(Workspace workspace : workspaces) {
            System.out.println(workspace.getId()+" "+workspace);
        }
    }
    
}
