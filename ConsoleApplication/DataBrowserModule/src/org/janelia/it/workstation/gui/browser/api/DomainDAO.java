package org.janelia.it.workstation.gui.browser.api;

import java.net.UnknownHostException;
import java.util.ArrayList;
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
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.screen.PatternMask;
import org.janelia.it.jacs.model.domain.screen.ScreenSample;
import org.janelia.it.jacs.model.domain.support.MongoUtils;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.domain.workspace.Workspace;
import org.jongo.Jongo;
import org.jongo.MongoCollection;
import org.jongo.RawResultHandler;
import org.jongo.marshall.jackson.JacksonMapper;

import com.fasterxml.jackson.databind.MapperFeature;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

/**
 * The main domain-object DAO for the JACS system.
 * 
 * TODO: COPYING THIS CLASS FROM COMPUTE IS JUST A TEMPORARY HACK, IN THE FUTURE WE'LL HAVE A REAL API 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainDAO {

    private static final Logger log = Logger.getLogger(DomainDAO.class);
    
    protected MongoClient m;
    protected DB db;
    protected Jongo jongo;
    protected MongoCollection subjectCollection;
    protected MongoCollection treeNodeCollection;
    protected MongoCollection sampleCollection;
    protected MongoCollection screenSampleCollection;
    protected MongoCollection patternMaskCollection;
    protected MongoCollection imageCollection;
    protected MongoCollection fragmentCollection;
    protected MongoCollection annotationCollection;
    protected MongoCollection ontologyCollection;
    
    public DomainDAO(String serverUrl, String databaseName) throws UnknownHostException {
        m = new MongoClient(serverUrl);
        m.setWriteConcern(WriteConcern.JOURNALED);
        //m.setWriteConcern(WriteConcern.UNACKNOWLEDGED);
        db = m.getDB(databaseName);
        jongo = new Jongo(db, 
                new JacksonMapper.Builder()
                    .enable(MapperFeature.AUTO_DETECT_GETTERS)
                    .enable(MapperFeature.AUTO_DETECT_SETTERS)
                    .build());
        subjectCollection = jongo.getCollection("subject");
        treeNodeCollection = jongo.getCollection("treeNode");
        sampleCollection = jongo.getCollection("sample");
        screenSampleCollection = jongo.getCollection("screenSample");
        patternMaskCollection = jongo.getCollection("patternMask");
        imageCollection = jongo.getCollection("image");
        fragmentCollection = jongo.getCollection("fragment");
        annotationCollection = jongo.getCollection("annotation");
        ontologyCollection = jongo.getCollection("ontology");
    }
    
    public Jongo getJongo() {
        return jongo;
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
    private <T> List<T> toList(Iterable<? extends T> iterable) {
        List<T> list = new ArrayList<T>();
        for(T item : iterable) {
            list.add(item);
        }
        return list;
    }

    /**
     * Create a list of the result set in the order of the given id list.
     */
    private List<DomainObject> toList(Iterable<? extends DomainObject> iterable, Collection<Long> ids) {
        List<DomainObject> list = new ArrayList<DomainObject>(ids.size());
        Map<Long,DomainObject> map = new HashMap<Long,DomainObject>(ids.size());
        for(DomainObject item : iterable) {
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
    	List<Long> ids = new ArrayList<Long>();
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
    	
        List<DomainObject> domainObjects = new ArrayList<DomainObject>();
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
        // TODO: remove this after the next db load fixes it
        if ("workspace".equals(type)) type = "treeNode"; 
        
        long start = System.currentTimeMillis();
        log.trace("getDomainObjects(subjectKey="+subjectKey+",type="+type+",ids.size="+ids.size()+")");

        Set<String> subjects = subjectKey==null?null:getSubjectSet(subjectKey);

        Class<? extends DomainObject> clazz = getObjectClass(type);
        if (clazz==null) {
        	log.error("No object type for "+type);
        	return new ArrayList<DomainObject>();
        }
        
        Iterable<? extends DomainObject> iterable = null;
        if (subjects==null) {
        	iterable = getCollectionByName(type).find("{_id:{$in:#}}", ids).as(clazz);
        }
        else {
        	iterable = getCollectionByName(type).find("{_id:{$in:#},readers:{$in:#}}", ids, subjects).as(clazz);	
        }
        
        
        List<DomainObject> list = toList(iterable, ids);
        log.trace("Getting "+list.size()+" "+type+" objects took "+(System.currentTimeMillis()-start)+" ms");
        return list;
    }

    public List<DomainObject> getDomainObjects(String subjectKey, ReverseReference reverseRef) {
        Set<String> subjects = subjectKey==null?null:getSubjectSet(subjectKey);
        String type = reverseRef.getReferringType();

        Iterable<? extends DomainObject> iterable = null;
        if (subjects==null) {
        	iterable = getCollectionByName(type).find("{"+reverseRef.getReferenceAttr()+":#}", reverseRef.getReferenceId()).as(getObjectClass(type));
        }
        else {
        	iterable = getCollectionByName(type).find("{"+reverseRef.getReferenceAttr()+":#,readers:{$in:#}}", reverseRef.getReferenceId(), subjects).as(getObjectClass(type));
        }
        
        List<DomainObject> list = toList(iterable);
        if (list.size()!=reverseRef.getCount()) {
            log.warn("Reverse reference ("+reverseRef.getReferringType()+":"+reverseRef.getReferenceAttr()+":"+reverseRef.getReferenceId()+
                    ") denormalized count ("+reverseRef.getCount()+") does not match actual count ("+list.size()+")");
        }
        return list;
    }
    
    public List<Annotation> getAnnotations(String subjectKey, Long targetId) {
        Set<String> subjects = subjectKey==null?null:getSubjectSet(subjectKey);

        Iterable<Annotation> iterable = null;
        if (subjects==null) {
        	iterable = annotationCollection.find("{targetId:#}",targetId).as(Annotation.class);
        }
        else {
        	iterable = annotationCollection.find("{targetId:#,readers:{$in:#}}",targetId,subjects).as(Annotation.class);
        }
        
        return toList(iterable);
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
        Collection<Long> ids = new ArrayList<Long>();
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
        log.info("Saving "+domainObject.getClass().getName()+"#"+domainObject.getId());
        WriteResult wr = collection.update("{_id:#,writers:#,updatedDate:#}",domainObject.getId(),subjectKey,domainObject.getUpdatedDate()).with(domainObject);
        if (wr.getN()!=1) {
            throw new Exception("Error saving object "+domainObject.getId()+": "+wr.getError());
        }
    }

    public void reorderChildren(String subjectKey, TreeNode treeNode, int[] order) throws Exception {
        
        List<Reference> references = new ArrayList<Reference>(treeNode.getChildren());
        
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
        Reference ref = new Reference();
        ref.setTargetId(domainObject.getId());
        ref.setTargetType(getCollectionName(domainObject));
        treeNode.getChildren().add(ref);
        save(subjectKey, treeNode);
    }
    
    public void removeChild(String subjectKey, TreeNode treeNode, DomainObject domainObject) throws Exception {
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
    public <T extends DomainObject> Iterable<T> getDomainObjects(Class<T> domainClass) { 
        return getCollectionByClass(domainClass).find().as(domainClass);
    }
    
    /**
     * Get the domain objects of the given type 
     */
    public Iterable<? extends DomainObject> getDomainObjects(String type) {
        Class<? extends DomainObject> clazz = getObjectClass(type);
        if (clazz==null) {
        	log.error("No object type for "+type);
        	return new ArrayList<DomainObject>();
        }

        return getCollectionByName(type).find().as(clazz);
    }

    /**
     * Get the raw domain objects of the given type 
     */
    public Iterable<DBObject> getRawObjects(String type) {
        return getCollectionByName(type).find().map(new RawResultHandler<DBObject>());
    }
    
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
