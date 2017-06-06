package org.janelia.it.jacs.model.domain.support;

import static org.janelia.it.jacs.model.domain.support.DomainUtils.abbr;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.janelia.it.jacs.model.TimebasedIdentifierGenerator;
import org.janelia.it.jacs.model.domain.DomainConstants;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Preference;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ReverseReference;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.compartments.CompartmentSet;
import org.janelia.it.jacs.model.domain.enums.OrderStatus;
import org.janelia.it.jacs.model.domain.enums.PipelineStatus;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentBoard;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentContext;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.gui.search.criteria.FacetCriteria;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.Category;
import org.janelia.it.jacs.model.domain.ontology.EnumItem;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.jacs.model.domain.ontology.OntologyTermReference;
import org.janelia.it.jacs.model.domain.orders.IntakeOrder;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.domain.sample.Image;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.LineRelease;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SampleLock;
import org.janelia.it.jacs.model.domain.sample.StatusTransition;
import org.janelia.it.jacs.model.domain.screen.FlyLine;
import org.janelia.it.jacs.model.domain.subjects.Group;
import org.janelia.it.jacs.model.domain.subjects.GroupRole;
import org.janelia.it.jacs.model.domain.subjects.User;
import org.janelia.it.jacs.model.domain.subjects.UserGroupRole;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.domain.workspace.Workspace;
import org.jongo.Aggregate;
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
import com.google.common.collect.Sets;
import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

/**
 * Data access object for the domain object model.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainDAO {

    private static final Logger log = LoggerFactory.getLogger(DomainDAO.class);

    protected MongoClient m;
    protected Jongo jongo;

    protected String databaseName;

    protected MongoCollection preferenceCollection;
    protected MongoCollection alignmentBoardCollection;
    protected MongoCollection alignmentContextCollection;
    protected MongoCollection annotationCollection;
    protected MongoCollection compartmentSetCollection;
    protected MongoCollection dataSetCollection;
    protected MongoCollection releaseCollection;
    protected MongoCollection flyLineCollection;
    protected MongoCollection fragmentCollection;
    protected MongoCollection imageCollection;
    protected MongoCollection ontologyCollection;
    protected MongoCollection pipelineStatusCollection;
    protected MongoCollection intakeOrdersCollection;
    protected MongoCollection sampleCollection;
    protected MongoCollection sampleLockCollection;
    protected MongoCollection subjectCollection;
    protected MongoCollection treeNodeCollection;
    protected MongoCollection tmSampleCollection;
    protected MongoCollection tmWorkspaceCollection;
    protected MongoCollection tmNeuronCollection;

    public DomainDAO(String serverUrl, String databaseName) throws UnknownHostException {
        this(serverUrl, databaseName, null, null);
    }

    public DomainDAO(String serverUrl, String databaseName, String username, String password) throws UnknownHostException {

        this.databaseName = databaseName;

        List<ServerAddress> members = new ArrayList<>();
        for (String serverMember : serverUrl.split(",")) {
            members.add(new ServerAddress(serverMember));
        }

        if (!StringUtils.isEmpty(username) && !StringUtils.isEmpty(password)) {
            MongoCredential credential = MongoCredential.createMongoCRCredential(username, databaseName, password.toCharArray());
            this.m = new MongoClient(members, Arrays.asList(credential));
            log.info("Connected to MongoDB (" + databaseName + "@" + serverUrl + ") as user " + username);
        }
        else {
            this.m = new MongoClient(members);
            log.info("Connected to MongoDB (" + databaseName + "@" + serverUrl + ")");
        }

        m.setWriteConcern(WriteConcern.JOURNALED);
        this.jongo = new Jongo(m.getDB(databaseName),
                new JacksonMapper.Builder()
                .enable(MapperFeature.AUTO_DETECT_GETTERS)
                .enable(MapperFeature.AUTO_DETECT_SETTERS)
                .build());
        this.alignmentBoardCollection = getCollectionByClass(AlignmentBoard.class);
        this.alignmentContextCollection = getCollectionByClass(AlignmentContext.class);
        this.annotationCollection = getCollectionByClass(Annotation.class);
        this.compartmentSetCollection = getCollectionByClass(CompartmentSet.class);
        this.dataSetCollection = getCollectionByClass(DataSet.class);
        this.releaseCollection = getCollectionByClass(LineRelease.class);
        this.flyLineCollection = getCollectionByClass(FlyLine.class);
        this.fragmentCollection = getCollectionByClass(NeuronFragment.class);
        this.imageCollection = getCollectionByClass(Image.class);
        this.ontologyCollection = getCollectionByClass(Ontology.class);
        this.sampleCollection = getCollectionByClass(Sample.class);
        this.sampleLockCollection = getCollectionByClass(SampleLock.class);
        this.pipelineStatusCollection = getCollectionByClass(StatusTransition.class);
        this.intakeOrdersCollection = getCollectionByClass(IntakeOrder.class);
        this.subjectCollection = getCollectionByClass(Subject.class);
        this.treeNodeCollection = getCollectionByClass(TreeNode.class);
        this.preferenceCollection = getCollectionByClass(Preference.class);
        this.tmSampleCollection = getCollectionByClass(TmSample.class);
        this.tmWorkspaceCollection = getCollectionByClass(TmWorkspace.class);
        this.tmNeuronCollection = getCollectionByClass(TmNeuronMetadata.class);
    }

    public final MongoCollection getCollectionByClass(Class<?> domainClass) {
        String collectionName = DomainUtils.getCollectionName(domainClass);
        return jongo.getCollection(collectionName);
    }

    public MongoCollection getCollectionByName(String collectionName) {
        if (collectionName == null) {
            throw new IllegalArgumentException("collectionName argument may not be null");
        }
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

    // Subjects
    
    /**
     * Save the given subject.
     */
    public Subject save(Subject subject) {
        log.debug("save({})", subject);
        if (subject.getId() == null) {
            subject.setId(getNewId());
            subjectCollection.insert(subject);
        }
        else {
            subjectCollection.update("{_id:#}", subject.getId()).with(subject);
        }

        log.trace("Saved " + subject.getClass().getName() + "#" + subject.getId());
        return subject;
    }

    /**
     * Return all the subjects.
     */
    public List<Subject> getSubjects() {
        return toList(subjectCollection.find().as(Subject.class));
    }

    public List<Subject> getUsers() throws Exception {
        return toList(subjectCollection.find("{key:{$regex:#}}", "^user:").as(Subject.class));
    }
    
    public List<Subject> getGroups() throws Exception {
        return toList(subjectCollection.find("{key:{$regex:#}}", "^group:").as(Subject.class));
    }

    /**
     * Return the set of subjectKeys which are readable by the given subject. 
     * This includes the subject itself, and all of the groups it has read access for.
     */
    public Set<String> getReaderSet(String subjectKey) {
        if (subjectKey == null) return null;
        Subject subject = subjectCollection.findOne("{key:#}", subjectKey).as(Subject.class);
        if (subject == null) {
            throw new IllegalArgumentException("No such subject: " + subjectKey);
        }
        Set<String> groups = new HashSet<>();
        groups.add(subjectKey);
        if (subject instanceof User) {
            User user = (User)subject;
            for(UserGroupRole role : user.getUserGroupRoles()) {
                if (role.getRole().isRead()) {
                    groups.add(role.getGroupKey());
                }
            }
        }
        groups.add(subjectKey);
        return groups;
    }

    /**
     * Return the set of subjectKeys which are writable by the given subject. 
     * This includes the subject itself, and all of the groups it has write access for.
     */
    public Set<String> getWriterSet(String subjectKey) {
        if (subjectKey == null) return null;
        Subject subject = subjectCollection.findOne("{key:#}", subjectKey).as(Subject.class);
        if (subject == null) {
            throw new IllegalArgumentException("No such subject: " + subjectKey);
        }
        Set<String> groups = new HashSet<>();
        groups.add(subjectKey);
        if (subject instanceof User) {
            User user = (User)subject;
            for(UserGroupRole role : user.getUserGroupRoles()) {
                if (role.getRole().isWrite()) {
                    groups.add(role.getGroupKey());
                }
            }
        }
        groups.add(subjectKey);
        return groups;
    }

    public Subject getSubjectByKey(String subjectKey) {
        return subjectCollection.findOne("{key:#}", subjectKey).as(Subject.class);
    }

    public Subject getSubjectByName(String subjectName) {
        return subjectCollection.findOne("{name:#}", subjectName).as(Subject.class);
    }
    
    /**
     * Return subject by name or key.
     */
    public Subject getSubjectByNameOrKey(String subjectNameOrKey) {
        return subjectCollection.findOne("{$or:[{name:#},{key:#}]}", subjectNameOrKey, subjectNameOrKey).as(Subject.class);
    }

    /**
     * Return user by name or key.
     */
    public User getUserByNameOrKey(String subjectNameOrKey) {
        log.debug("getUserByNameOrKey({})", subjectNameOrKey);
        return subjectCollection.findOne("{$or:[{name:#},{key:#}], class:#}", subjectNameOrKey, subjectNameOrKey, User.class.getName()).as(User.class);
    }

    /**
     * Return group by name or key.
     */
    public Group getGroupByNameOrKey(String subjectNameOrKey) {
        return subjectCollection.findOne("{$or:[{name:#},{key:#}], class:#}", subjectNameOrKey, subjectNameOrKey, Group.class.getName()).as(Group.class);
    }

    public User createUser(String name, String fullName, String email) throws Exception {
        log.debug("createUser(name={}, fullName={}, email={})", name, fullName, email);
        User newSubject = new User();
        newSubject.setId(getNewId());
        newSubject.setName(name);
        newSubject.setKey("user:" + name);
        newSubject.setFullName(fullName);
        newSubject.setEmail(email);
        // Add user to the "everyone" group
        newSubject.setUserGroupRole(Subject.USERS_KEY, GroupRole.Reader);
        subjectCollection.insert(newSubject);
        
        User user = getUserByNameOrKey(name);
        if (user!=null) {
            log.debug("Created user " + user.getKey());
            // If the user was created, make sure they have a workspace
            createWorkspace(user.getKey());
        }
        else {
            throw new Exception("Problem creating user "+name);
        }
        
        return user;
    }

    public Group createGroup(String name, String fullName) throws Exception {
        log.debug("createGroup(name={}, fullName={})", name, fullName);
        Group newSubject = new Group();
        newSubject.setId(getNewId());
        newSubject.setName(name);
        newSubject.setKey("group:"+name);
        newSubject.setFullName(fullName);
        subjectCollection.insert(newSubject);

        Group group = getGroupByNameOrKey(name);
        if (group!=null) {
            log.info("Created group " + group.getKey());
            createWorkspace(group.getKey());
        }
        else {
            throw new Exception("Problem creating group "+name);
        }
        
        return group;
    }

    public void remove(Subject subject) throws Exception {
        log.debug("remove({})", subject);
        WriteResult result = subjectCollection.remove("{_id:#}", subject.getId());
        if (result.getN() != 1) {
            throw new IllegalStateException("Deleted " + result.getN() + " records instead of one: Subject#" + subject.getId());
        }    
    }
    
    public void removeUser(String userNameOrKey) throws Exception {
        log.debug("removeUser(subjectNameOrKey)", userNameOrKey);
        Subject user = getUserByNameOrKey(userNameOrKey);
        if (user==null) throw new IllegalArgumentException("User not found: "+userNameOrKey);
        remove(user);
    }
    
    public void removeGroup(String groupNameOrKey) throws Exception {
        log.debug("removeGroup(subjectNameOrKey)", groupNameOrKey);
        Subject group = getGroupByNameOrKey(groupNameOrKey);
        if (group==null) throw new IllegalArgumentException("Group not found: "+groupNameOrKey);
        remove(group);   
    }

    public void addUserToGroup(String userNameOrKey, String groupNameOrKey, GroupRole role) throws Exception {
        log.debug("addUserToGroup(user={}, group={})", userNameOrKey, groupNameOrKey);
        User user = getUserByNameOrKey(userNameOrKey);
        if (user==null) throw new IllegalArgumentException("User not found: "+userNameOrKey);
        Group group = getGroupByNameOrKey(groupNameOrKey);
        if (group==null) throw new IllegalArgumentException("Group not found: "+groupNameOrKey);
        
        UserGroupRole ugr = user.getRole(group.getKey());
        if (ugr!=null) {
            if (ugr.getRole().equals(role)) {
                log.info("User "+userNameOrKey+" already has role "+role+" in group "+groupNameOrKey+". Skipping add...");
            }
            else {
                ugr.setRole(role);
                subjectCollection.save(user);
                log.info("Set role for "+userNameOrKey+" to "+role+" in group "+groupNameOrKey);
            }
        }
        else {
            user.setUserGroupRole(group.getKey(), role);
            subjectCollection.save(user);
            log.info("Set role for " + userNameOrKey + " to " + role + " in group " + groupNameOrKey);
        }
    }

    public void removeUserFromGroup(String userNameOrKey, String groupNameOrKey) throws Exception {
        log.debug("removeUserFromGroup(user={}, group={})", userNameOrKey, groupNameOrKey);
        User user = getUserByNameOrKey(userNameOrKey);
        if (user==null) throw new IllegalArgumentException("User not found: "+userNameOrKey);
        Group group = getGroupByNameOrKey(groupNameOrKey);
        if (group==null) throw new IllegalArgumentException("Group not found: "+groupNameOrKey);

        boolean dirty = false;
        // Purge all roles for this group
        UserGroupRole ugr = null;
        while ((ugr = user.getRole(group.getKey()))!=null) {
            user.getUserGroupRoles().remove(ugr);
            dirty = true;
        }
        
        if (dirty) {
            subjectCollection.save(user);
            log.info("Removed user "+userNameOrKey+" from group "+groupNameOrKey);
        }
        else {
            log.debug("User "+userNameOrKey+" does not belong to group "+groupNameOrKey+". Skipping removal...");
        }
    }

    // Workspaces
    
    public void createWorkspace(String ownerKey) throws Exception {
        log.debug("createWorkspace({})", ownerKey);
        if (getDefaultWorkspace(ownerKey)!=null) {
            log.info("User "+ownerKey+" already has at least one workspace, skipping creation step.");
            return;
        }
        Workspace workspace = new Workspace();
        workspace.setName(DomainConstants.NAME_DEFAULT_WORKSPACE);
        save(ownerKey, workspace);
        log.info("Created workspace (id="+workspace.getId()+") for "+ownerKey);
    }
    
    public List<Workspace> getWorkspaces(String subjectKey) {
        log.debug("getWorkspaces({})", subjectKey);
        Set<String> subjects = getReaderSet(subjectKey);
        if (subjects == null) {
            return toList(treeNodeCollection.find("{class:#}", Workspace.class.getName()).as(Workspace.class));
        }
        else {
            return toList(treeNodeCollection.find("{class:#,readers:{$in:#}}", Workspace.class.getName(), subjects).as(Workspace.class));
        }
    }
    
    public Workspace getDefaultWorkspace(String subjectKey) {
        log.debug("getDefaultWorkspace({})", subjectKey);
        return treeNodeCollection.findOne("{class:#,ownerKey:#}", Workspace.class.getName(), subjectKey).as(Workspace.class);
    }
    
    // Preferences
    
    /**
     * Return all the preferences for a given subject.
     */
    public List<Preference> getPreferences(String subjectKey) {
        log.debug("getPreferences({})", subjectKey);
        return toList(preferenceCollection.find("{subjectKey:#}", subjectKey).as(Preference.class));
    }

    public List<Preference> getPreferences(String subjectKey, String category) throws Exception {
        log.debug("getPreferences({}, category={})", subjectKey, category);
        return toList(preferenceCollection.find("{subjectKey:#,category:#}", subjectKey, category).as(Preference.class));
    }

    public Preference getPreference(String subjectKey, String category, String key) {
        log.debug("getPreference({}, category={}, key={})", subjectKey, category, key);
        return preferenceCollection.findOne("{subjectKey:#,category:#,key:#}", subjectKey, category, key).as(Preference.class);
    }

    public Object getPreferenceValue(String subjectKey, String category, String key) {
        Preference preference = getPreference(subjectKey, category, key);
        if (preference == null) {
            return null;
        }
        else {
            return preference.getValue();
        }
    }

    public void setPreferenceValue(String subjectKey, String category, String key, Object value) throws Exception {
        Preference preference = getPreference(subjectKey, category, key);
        if (preference == null) {
            preference = new Preference(subjectKey, category, key, value);
        }
        else {
            preference.setValue(value);
        }
        save(subjectKey, preference);
    }
	
    /**
     * Saves the given subject preference.
     *
     * @param subjectKey
     * @param preference
     * @return
     * @throws Exception
     */
    public Preference save(String subjectKey, Preference preference) throws Exception {

        log.debug("save({}, {})", subjectKey, preference);

        if (preference.getId() == null) {
            preference.setId(getNewId());
            preferenceCollection.insert(preference);
        }
        else {
        	WriteResult result = preferenceCollection.update("{_id:#,subjectKey:#}", preference.getId(), subjectKey).with(preference);
            if (result.getN() != 1) {
                throw new IllegalStateException("Updated " + result.getN() + " records instead of one: preference#" + preference.getId());
            }
        }

        log.trace("Saved " + preference.getClass().getName() + "#" + preference.getId());
        return preference;
    }

    /**
     * Check whether the DomainObject has any ancestor references in TreeNode and ObjectSet.
     *
     * @param domainObject
     * @return boolean
     * @throws Exception
     */
    public List<Reference> getContainerReferences(DomainObject domainObject) throws Exception {

        log.trace("Checking to see whether  " + domainObject.getId() + " has any parent references");
        if (domainObject.getId() == null) {
            return null;
        }

        String refStr = Reference.createFor(domainObject).toString();
        List<Reference> refList = new ArrayList<>();
        MongoCursor<TreeNode> treeCursor = treeNodeCollection.find("{children:#}", refStr).as(TreeNode.class);
        for (TreeNode item : treeCursor) {
            Reference newRef = Reference.createFor(item.getClass(), item.getId());
            refList.add(newRef);
        }
        return refList;
    }

    /**
     * Create a list of the result set in iteration order.
     */
    public <T> List<T> toList(MongoCursor<? extends T> cursor) {
        List<T> list = new ArrayList<>();
        for (T item : cursor) {
            list.add(item);
        }
        return list;
    }

    /**
     * Create a list of the result set in the order of the given id list. If ids is null then
     * return the result set in the order it comes back.
     */
    public <T extends DomainObject> List<T> toList(MongoCursor<T> cursor, Collection<Long> ids) {
        if (ids == null) {
            List<T> list = new ArrayList<>();
            for (T item : cursor) {
                list.add(item);
            }
            return list;
        }
        List<T> list = new ArrayList<>(ids.size());
        Map<Long, T> map = new HashMap<>(ids.size());
        for (T item : cursor) {
            map.put(item.getId(), item);
        }
        for (Long id : ids) {
            T item = map.get(id);
            if (item != null) {
                list.add(item);
            }
        }
        return list;
    }

    /**
     * Retrieve a refresh copy of the given domain object from the database.
     */
    @SuppressWarnings("unchecked")
    public <T extends DomainObject> T getDomainObject(String subjectKey, T domainObject) {
        return (T) getDomainObject(subjectKey, domainObject.getClass(), domainObject.getId());
    }

    /**
     * Get the domain object referenced by the collection name and id.
     */
    @SuppressWarnings("unchecked")
    public <T extends DomainObject> T getDomainObject(String subjectKey, Class<T> domainClass, Long id) {
        Reference reference = Reference.createFor(domainClass, id);
        return (T) getDomainObject(subjectKey, reference);
    }

    /**
     * Get the domain object referenced by the given Reference.
     */
    public DomainObject getDomainObject(String subjectKey, Reference reference) {
        List<DomainObject> objs = getDomainObjects(subjectKey, reference.getTargetClassName(), Arrays.asList(reference.getTargetId()));
        return objs.isEmpty() ? null : objs.get(0);
    }

    private DomainObject getDomainObject(String subjectKey, String className, Long id) {
        List<DomainObject> objs = getDomainObjects(subjectKey, className, Arrays.asList(id));
        return objs.isEmpty() ? null : objs.get(0);
    }
    
    public <T extends DomainObject> List<T> getDomainObjectsAs(List<Reference> references, Class<T> clazz) {
        return getDomainObjectsAs(null, references, clazz);
    }

    public <T extends DomainObject> List<T> getDomainObjectsAs(String subjectKey, List<Reference> references, Class<T> clazz) {
        List<T> list = new ArrayList<>();
        for (DomainObject object : getDomainObjects(subjectKey, references)) {
            if (clazz.isAssignableFrom(object.getClass())) {
                list.add((T) object);
            }
            else {
                log.warn("Referenced object is " + object.getClass().getSimpleName() + " not " + clazz.getSimpleName());
            }
        }
        return list;
    }

    public List<DomainObject> getDomainObjects(String subjectKey, List<Reference> references) {
        return getDomainObjects(subjectKey, references, false);
    }
    
    private Collection<DomainObject> getUserDomainObjects(String subjectKey, List<Reference> references) {
        return getDomainObjects(subjectKey, references, true);
    }
    
    /**
     * Get the domain objects referenced by the given list of References.
     */
    public List<DomainObject> getDomainObjects(String subjectKey, List<Reference> references, boolean ownedOnly) {

        List<DomainObject> domainObjects = new ArrayList<>();
        if (references == null || references.isEmpty()) {
            return domainObjects;
        }

        Multimap<String, Long> referenceMap = ArrayListMultimap.create();
        for (Reference reference : references) {
            if (reference == null) {
                log.warn("{} requested null reference", subjectKey);
                continue;
            }
            referenceMap.put(reference.getTargetClassName(), reference.getTargetId());
        }

        for (String className : referenceMap.keySet()) {
            List<DomainObject> objs = ownedOnly?
                    getUserDomainObjects(subjectKey, className, referenceMap.get(className)) :
                    getDomainObjects(subjectKey, className, referenceMap.get(className));
            domainObjects.addAll(objs);
        }

        return domainObjects;
    }

    /**
     * Get the domain objects of a single class with the specified ids.
     *
     * @param subjectKey
     * @param className
     * @param ids
     * @return
     */
    public <T extends DomainObject> List<T> getDomainObjects(String subjectKey, String className, Collection<Long> ids) {
    	Class<T> clazz;
    	try {
    		clazz = (Class<T>) DomainUtils.getObjectClassByName(className);
    	}
    	catch (IllegalArgumentException e) {
    		log.error("Unknown domain object class: "+className);
    		return new ArrayList<T>();
    	}
        return getDomainObjects(subjectKey, clazz, ids);
    }

    public <T extends DomainObject> List<T> getDomainObjects(String subjectKey, Class<T> domainClass) {
        return getDomainObjects(subjectKey, domainClass, null);
    }
    
    /**
     * Get the domain objects in the given collection name with the specified ids.
     */
    public <T extends DomainObject> List<T> getDomainObjects(String subjectKey, Class<T> domainClass, Collection<Long> ids) {

        if (domainClass == null) return new ArrayList<>();

        log.debug("getDomainObjects({}, className={}, ids={})", subjectKey, domainClass.getSimpleName(), abbr(ids));

        long start = System.currentTimeMillis();

        Set<String> subjects = getReaderSet(subjectKey);

        String collectionName = DomainUtils.getCollectionName(domainClass);
        MongoCursor<T> cursor;
        if (ids == null) {
            if (subjects == null || subjects.contains(Subject.ADMIN_KEY)) {
                cursor = getCollectionByName(collectionName).find().as(domainClass);
            }
            else {
                cursor = getCollectionByName(collectionName).find("{readers:{$in:#}}", subjects).as(domainClass);
            }
        }
        else {
            if (subjects == null || subjects.contains(Subject.ADMIN_KEY)) {
                cursor = getCollectionByName(collectionName).find("{_id:{$in:#}}", ids).as(domainClass);
            }
            else {
                cursor = getCollectionByName(collectionName).find("{_id:{$in:#},readers:{$in:#}}", ids, subjects).as(domainClass);
            }
        }

        List<T> list = toList(cursor, ids);
        log.trace("Getting " + list.size() + " " + collectionName + " objects took " + (System.currentTimeMillis() - start) + " ms");
        return list;
    }

    public <T extends DomainObject> List<T> getUserDomainObjects(String subjectKey, Class<T> domainClass) {
        return getUserDomainObjects(subjectKey, domainClass, null);
    }

    public <T extends DomainObject> List<T> getUserDomainObjects(String subjectKey, String className, Collection<Long> ids) {
        return (List<T>)getUserDomainObjects(subjectKey, DomainUtils.getObjectClassByName(className), ids);
    }
    
    /**
     * Get the domain objects owned by the given user, in the given collection name, with the specified ids.
     */
    public <T extends DomainObject> List<T> getUserDomainObjects(String subjectKey, Class<T> domainClass, Collection<Long> ids) {

        if (domainClass == null) return new ArrayList<>();

        log.debug("getUserDomainObjects({}, className={}, ids={})", subjectKey, domainClass.getSimpleName(), abbr(ids));

        long start = System.currentTimeMillis();

        String collectionName = DomainUtils.getCollectionName(domainClass);
        MongoCursor<T> cursor;
        if (ids == null) {
            cursor = getCollectionByName(collectionName).find("{ownerKey:#}", subjectKey).as(domainClass);
        }
        else {
            cursor = getCollectionByName(collectionName).find("{_id:{$in:#},ownerKey:#}", ids, subjectKey).as(domainClass);
        }

        List<T> list = toList(cursor, ids);
        log.trace("Getting " + list.size() + " " + collectionName + " objects took " + (System.currentTimeMillis() - start) + " ms");
        return list;
    }

    /**
     * Get the domain objects referenced by the given reverse reference.
     *
     * @param subjectKey
     * @param reverseRef
     * @return
     */
    public List<DomainObject> getDomainObjects(String subjectKey, ReverseReference reverseRef) {

        log.debug("getDomainObjects({}, reverseRef={})", subjectKey, reverseRef);

        Set<String> subjects = getReaderSet(subjectKey);
        Class<? extends DomainObject> clazz = DomainUtils.getObjectClassByName(reverseRef.getReferringClassName());
        String collectionName = DomainUtils.getCollectionName(reverseRef.getReferringClassName());

        MongoCursor<? extends DomainObject> cursor = null;
        if (subjects == null || subjects.contains(Subject.ADMIN_KEY)) {
            cursor = getCollectionByName(collectionName).find("{'" + reverseRef.getReferenceAttr() + "':#}", reverseRef.getReferenceId()).sort("{_id: 1}").as(clazz);
        }
        else {
            cursor = getCollectionByName(collectionName).find("{'" + reverseRef.getReferenceAttr() + "':#,readers:{$in:#}}", reverseRef.getReferenceId(), subjects).sort("{_id: 1}").as(clazz);
        }

        List<DomainObject> list = toList(cursor);
        if (list.size() != reverseRef.getCount()) {
            log.warn("Reverse reference (" + reverseRef.getReferringClassName() + ":" + reverseRef.getReferenceAttr() + ":" + reverseRef.getReferenceId()
                    + ") denormalized count (" + reverseRef.getCount() + ") does not match actual count (" + list.size() + ")");
        }
        return list;
    }

    /**
     * Get the domain object by name.
     */
    public <T extends DomainObject> List<T> getDomainObjectsByName(String subjectKey, Class<T> domainClass, String name) {

        if (domainClass == null) return null;

        log.debug("getDomainObjectsByName({}, className={}, name={})", subjectKey, domainClass.getSimpleName(), name);

        long start = System.currentTimeMillis();

        Set<String> subjects = getReaderSet(subjectKey);

        String collectionName = DomainUtils.getCollectionName(domainClass);
        MongoCursor<T> cursor;
        if (subjects == null || subjects.contains(Subject.ADMIN_KEY)) {
            cursor = getCollectionByName(collectionName).find("{name:#}", name).as(domainClass);
        }
        else {
            cursor = getCollectionByName(collectionName).find("{name:#,readers:{$in:#}}", name, subjects).as(domainClass);
        }

        List<T> list = toList(cursor);
        log.trace("Getting " + list.size() + " " + collectionName + " objects took " + (System.currentTimeMillis() - start) + " ms");
        return list;
    }

    /**
     * Get the domain object by name.
     */
    public <T extends DomainObject> List<T> getUserDomainObjectsByName(String subjectKey, Class<T> domainClass, String name) {

        if (domainClass == null || name == null) {
            return null;
        }

        long start = System.currentTimeMillis();
        log.debug("getUserDomainObjectsByName({}, className={}, name={})", subjectKey, domainClass.getSimpleName(), name);

        String collectionName = DomainUtils.getCollectionName(domainClass);
        MongoCursor<T> cursor = getCollectionByName(collectionName).find("{ownerKey:#,name:#}", subjectKey, name).as(domainClass);

        List<T> list = toList(cursor);
        log.trace("Getting " + list.size() + " " + collectionName + " objects took " + (System.currentTimeMillis() - start) + " ms");
        return list;
    }

    /**
     * Get domain objects of a given type with a given specified property value.
     */
    public <T extends DomainObject> List<T> getDomainObjectsWithProperty(String subjectKey, Class<T> domainClass, String propName, String propValue) {

        if (domainClass == null) {
            return null;
        }

        long start = System.currentTimeMillis();
        log.debug("getDomainObjectsWithProperty({}, className={}, name={}, value={})", subjectKey, domainClass.getSimpleName(), propName, propValue);

        Set<String> subjects = getReaderSet(subjectKey);

        String collectionName = DomainUtils.getCollectionName(domainClass);
        MongoCursor<T> cursor;
        if (subjects == null || subjects.contains(Subject.ADMIN_KEY)) {
            cursor = getCollectionByName(collectionName).find("{" + propName + ":#}", propValue).as(domainClass);
        }
        else {
            cursor = getCollectionByName(collectionName).find("{" + propName + ":#,readers:{$in:#}}", propValue, subjects).as(domainClass);
        }

        List<T> list = toList(cursor);
        log.trace("Getting " + list.size() + " " + collectionName + " objects took " + (System.currentTimeMillis() - start) + " ms");
        return list;
    }

    public List<Annotation> getAnnotations(String subjectKey, Reference reference) {
    	return getAnnotations(subjectKey, Arrays.asList(reference));
    }
    
    public List<Annotation> getAnnotations(String subjectKey, Collection<Reference> references) {
        log.debug("getAnnotations({}, references={})", subjectKey, abbr(references));
        Set<String> subjects = getReaderSet(subjectKey);

        List<String> targetRefs = new ArrayList<>();
        for (Reference reference : references) {
            targetRefs.add(reference.toString());
        }

        MongoCursor<Annotation> cursor = null;
        if (subjects == null || subjects.contains(Subject.ADMIN_KEY)) {
            cursor = annotationCollection.find("{target:{$in:#}}", targetRefs).as(Annotation.class);
        }
        else {
            cursor = annotationCollection.find("{target:{$in:#},readers:{$in:#}}", targetRefs, subjects).as(Annotation.class);
        }

        return toList(cursor);
    }

    public List<Ontology> getOntologies(String subjectKey) {
        log.debug("getOntologies({})", subjectKey);
        Set<String> subjects = getReaderSet(subjectKey);
        return toList(ontologyCollection.find("{readers:{$in:#}}", subjects).as(Ontology.class));
    }

    public OntologyTerm getErrorOntologyCategory() {
        // TODO: this needs to be exposed to the client
        log.debug("getErrorOntologyCategory()");
        List<Ontology> ontologies = getDomainObjectsByName(DomainConstants.GENERAL_USER_GROUP_KEY, Ontology.class, DomainConstants.ERROR_ONTOLOGY_NAME);
        if (ontologies.size() > 1) {
            log.warn("Multiple error ontologies detected. Please ensure that " + DomainConstants.GENERAL_USER_GROUP_KEY + " only owns a single ontology with name " + DomainConstants.ERROR_ONTOLOGY_NAME);
        }
        for (Ontology ontology : ontologies) {
            OntologyTerm term = ontology.findTerm(DomainConstants.ERROR_ONTOLOGY_CATEGORY);
            if (term instanceof Category) {
                return term;
            }
        }
        throw new IllegalStateException("Error ontology category could not be found");
    }

    public Annotation createAnnotation(String subjectKey, Reference target, OntologyTermReference ontologyTermReference, Object value) throws Exception {

        log.debug("createAnnotation({}, target={}, ontologyTerm={}, value={})", subjectKey, target, ontologyTermReference, value);

        Ontology ontology = getDomainObject(subjectKey, Ontology.class, ontologyTermReference.getOntologyId());
        OntologyTerm ontologyTerm = ontology.findTerm(ontologyTermReference.getOntologyTermId());

        OntologyTerm keyTerm = ontologyTerm;
        OntologyTerm valueTerm = null;
        String keyString = keyTerm.getName();
        String valueString = value == null ? null : value.toString();

        if (keyTerm instanceof EnumItem) {
            keyTerm = ontologyTerm.getParent();
            valueTerm = ontologyTerm;
            keyString = keyTerm.getName();
            valueString = valueTerm.getName();
        }

        final Annotation annotation = new Annotation();
        annotation.setKey(keyString);
        annotation.setValue(valueString);
        annotation.setTarget(target);

        annotation.setKeyTerm(new OntologyTermReference(ontology, keyTerm));
        if (valueTerm != null) {
            annotation.setValueTerm(new OntologyTermReference(ontology, valueTerm));
        }

        String tag = (annotation.getValue() == null ? annotation.getKey()
                : annotation.getKey() + " = " + annotation.getValue());
        annotation.setName(tag);

        Annotation savedAnnotation = save(subjectKey, annotation);
        log.trace("Saved annotation as " + savedAnnotation.getId());

        // TODO: auto-share annotation based on auto-share template (this logic is currently in the client)
        return savedAnnotation;
    }

    // Data sets
    
    public List<DataSet> getDataSets() {
        return getDataSets(null);
    }

    public List<DataSet> getDataSets(String subjectKey) {
        log.debug("getDataSets({})", subjectKey);
        Set<String> subjects = getReaderSet(subjectKey);
        if (subjects == null) {
            return toList(dataSetCollection.find().as(DataSet.class));
        }
        else {
            return toList(dataSetCollection.find("{readers:{$in:#}}", subjects).as(DataSet.class));
        }
    }

    public List<DataSet> getUserDataSets(String subjectKey) {
        log.debug("getUserDataSets({})", subjectKey);
        if (subjectKey == null) {
            return toList(dataSetCollection.find().as(DataSet.class));
        }
        else {
            return toList(dataSetCollection.find("{ownerKey:#}", subjectKey).as(DataSet.class));
        }
    }

    public DataSet getDataSetByIdentifier(String subjectKey, String dataSetIdentifier) {

        log.debug("getDataSetByIdentifier({}, dataSetIdentifier={})", subjectKey, dataSetIdentifier);

        Set<String> subjects = getReaderSet(subjectKey);
        if (subjects == null || subjects.contains(Subject.ADMIN_KEY)) {
            return dataSetCollection.findOne("{identifier:#}", dataSetIdentifier).as(DataSet.class);
        }
        else {
            return dataSetCollection.findOne("{readers:{$in:#},identifier:#}", subjects, dataSetIdentifier).as(DataSet.class);
        }
    }
    
    public DataSet createDataSet(String subjectKey, DataSet dataSet) throws Exception {
        
        DataSet saved = save(subjectKey, dataSet);
        
        String filterName = dataSet.getName();
        log.info("Creating data set filter for "+filterName+", shared with "+subjectKey);
        Filter filter = createDataSetFilter(subjectKey, dataSet, filterName);
        
        // Now add it to the owner's Data Sets folder

        TreeNode sharedDataFolder = getOrCreateDefaultFolder(subjectKey, DomainConstants.NAME_DATA_SETS);
        addChildren(subjectKey, sharedDataFolder, Arrays.asList(Reference.createFor(filter)));
        
        return saved;
    }

    // Sample locks
    
    /**
     * Attempts to lock a sample for the given task id an owner.
     * @param subjectKey
     * @param sampleId
     * @param taskId
     * @param description
     * @return
     */
    public SampleLock lockSample(String subjectKey, Long sampleId, Long taskId, String description) {

        Reference ref = Reference.createFor(Sample.class, sampleId);
        
        // First attempt to refresh an existing lock (reentrant lock)
        WriteResult result = sampleLockCollection
                .update("{ownerKey:#, taskId:#, sampleRef:#}", subjectKey, taskId, ref.toString())
                .with("{$currentDate:{'creationDate':true}}");

        if (result.getN() < 1) {
            // Nothing to update

            // If there's no existing lock, then create a new one
            SampleLock lock = new SampleLock();
            lock.setCreationDate(new Date());
            lock.setOwnerKey(subjectKey);
            lock.setTaskId(taskId);
            lock.setSampleRef(ref);
            lock.setDescription(description);
            
            try {
                sampleLockCollection.insert(lock);
                log.info("Task {} ({}) has locked sample {}", taskId, subjectKey, sampleId);
                return lock;
            }
            catch (DuplicateKeyException e) {
                log.error("Task {} ({}) tried to lock {} and failed", taskId, subjectKey, sampleId);
                return null;
            }
        }
        else {
            SampleLock lock = sampleLockCollection
                    .findOne("{ownerKey:#, taskId:#, sampleRef:#}", subjectKey, taskId, ref.toString())
                    .as(SampleLock.class);
            
            if (lock==null) {
                log.error("Task {} ({}) reconfirmed lock on sample {}, but it cannot be found.", taskId, subjectKey, sampleId);
            }
            else {
                log.info("Task {} ({}) reconfirmed lock on sample {}", taskId, subjectKey, sampleId);
                
            }
            
            return lock;
        }
    }
    
    /**
     * Attempts to unlock a sample, given the lock holder's task id and owner. 
     * @param subjectKey
     * @param sampleId
     * @param taskId
     * @return
     */
    public boolean unlockSample(String subjectKey, Long sampleId, Long taskId) {

        Reference ref = Reference.createFor(Sample.class, sampleId);
        WriteResult result = sampleLockCollection
                .remove("{ownerKey:#, taskId:#, sampleRef:#}", subjectKey, taskId, ref.toString());
        
        if (result.getN() != 1) {

            SampleLock lock = sampleLockCollection
                    .findOne("{sampleRef:#}", ref.toString()).as(SampleLock.class);
            if (lock==null) {
                log.error("Task {} ({}) tried to remove lock on {} and failed. "
                        + "It looks like the lock may have expired.", taskId, subjectKey, sampleId);    
            }
            else {
                log.error("Task {} ({}) tried to remove lock on {} and failed. "
                        + "It looks like the lock is owned by someone else: {}.", taskId, subjectKey, sampleId, lock);
            }
            
            return false;
        }
        
        log.info("Task {} ({}) removed lock on {}", taskId, subjectKey, sampleId);
        return true;
    }
    
    // Samples by data set

    public List<Sample> getActiveSamplesForDataSet(String subjectKey, String dataSetIdentifier) {
        return getSamplesForDataSet(subjectKey, dataSetIdentifier, true);
    }

    public List<Sample> getSamplesForDataSet(String subjectKey, String dataSetIdentifier) {
        return getSamplesForDataSet(subjectKey, dataSetIdentifier, false);
    }

    private List<Sample> getSamplesForDataSet(String subjectKey, String dataSetIdentifier, boolean activeOnly) {

        log.debug("getActiveSamplesForDataSet({}, dataSetIdentifier={})", subjectKey, dataSetIdentifier);

        long start = System.currentTimeMillis();
        Set<String> subjects = getReaderSet(subjectKey);

        MongoCursor<Sample> cursor;
        if (subjects == null || subjects.contains(Subject.ADMIN_KEY)) {
            cursor = sampleCollection.find("{dataSet:#"+(activeOnly?",sageSynced:true":"")+"}", dataSetIdentifier).as(Sample.class);
        }
        else {
            cursor = sampleCollection.find("{dataSet:#"+(activeOnly?",sageSynced:true":"")+",readers:{$in:#}}", dataSetIdentifier, subjects).as(Sample.class);
        }

        List<Sample> list = toList(cursor);
        log.trace("Getting " + list.size() + " Sample objects took " + (System.currentTimeMillis() - start) + " ms");
        return list;
    }

    // LSMSs by data set

    public List<LSMImage> getActiveLsmsForDataSet(String subjectKey, String dataSetIdentifier) {
        return getLsmsForDataSet(subjectKey, dataSetIdentifier, true);
    }

    public List<LSMImage> getLsmsForDataSet(String subjectKey, String dataSetIdentifier) {
        return getLsmsForDataSet(subjectKey, dataSetIdentifier, false);
    }

    private List<LSMImage> getLsmsForDataSet(String subjectKey, String dataSetIdentifier, boolean activeOnly) {

        log.debug("getActiveLsmsForDataSet({}, dataSetIdentifier={})", subjectKey, dataSetIdentifier);

        long start = System.currentTimeMillis();
        Set<String> subjects = getReaderSet(subjectKey);

        MongoCursor<LSMImage> cursor;
        if (subjects == null || subjects.contains(Subject.ADMIN_KEY)) {
            cursor = imageCollection.find("{dataSet:#"+(activeOnly?",sageSynced:true":"")+"}", dataSetIdentifier).as(LSMImage.class);
        }
        else {
            cursor = imageCollection.find("{dataSet:#"+(activeOnly?",sageSynced:true":"")+",readers:{$in:#}}", dataSetIdentifier, subjects).as(LSMImage.class);
        }

        List<LSMImage> list = toList(cursor);
        log.trace("Getting " + list.size() + " LSMImage objects took " + (System.currentTimeMillis() - start) + " ms");
        return list;
    }

    // Samples by slide code

    public List<Sample> getSamplesBySlideCode(String subjectKey, String dataSetIdentifier, String slideCode) {
        return getSamplesBySlideCode(subjectKey, dataSetIdentifier, slideCode, false);
    }

    public Sample getActiveSampleBySlideCode(String subjectKey, String dataSetIdentifier, String slideCode) {
        List<Sample> list = getSamplesBySlideCode(subjectKey, dataSetIdentifier, slideCode, true);
        if (list.isEmpty()) {
            return null;
        }
        if (list.size()>1) {
            log.warn("More than one active sample found for "+dataSetIdentifier+"/"+slideCode);
        }
        return list.get(0);
    }

    private List<Sample> getSamplesBySlideCode(String subjectKey, String dataSetIdentifier, String slideCode, boolean activeOnly) {

        log.debug("getActiveSampleBySlideCode({}, dataSetIdentifier={}, slideCode={})", subjectKey, dataSetIdentifier, slideCode);

        long start = System.currentTimeMillis();
        Set<String> subjects = getReaderSet(subjectKey);

        MongoCursor<Sample> cursor;
        if (subjects == null || subjects.contains(Subject.ADMIN_KEY)) {
            cursor = sampleCollection.find("{dataSet:#,slideCode:#"+(activeOnly?",sageSynced:true":"")+"}", dataSetIdentifier, slideCode).as(Sample.class);
        }
        else {
            cursor = sampleCollection.find("{dataSet:#,slideCode:#"+(activeOnly?",sageSynced:true":"")+",readers:{$in:#}}", dataSetIdentifier, slideCode, subjects).as(Sample.class);
        }

        List<Sample> list = toList(cursor);
        log.trace("Getting " + list.size() + " Sample objects took " + (System.currentTimeMillis() - start) + " ms");
        return list;
    }

    // User samples by slide code

    public List<Sample> getUserSamplesBySlideCode(String subjectKey, String dataSetIdentifier, String slideCode) {

        log.debug("getUserSamplesBySlideCode({}, dataSetIdentifier={}, slideCode={})", subjectKey, dataSetIdentifier, slideCode);

        long start = System.currentTimeMillis();

        MongoCursor<Sample> cursor;
        if (subjectKey == null) {
            cursor = sampleCollection.find("{dataSet:#,slideCode:#}", dataSetIdentifier, slideCode).as(Sample.class);
        }
        else {
            cursor = sampleCollection.find("{dataSet:#,slideCode:#,ownerKey:#}", dataSetIdentifier, slideCode, subjectKey).as(Sample.class);
        }

        List<Sample> list = toList(cursor);
        log.trace("Getting " + list.size() + " Sample objects took " + (System.currentTimeMillis() - start) + " ms");
        return list;
    }

    public List<LSMImage> getActiveLsmsBySampleId(String subjectKey, Long sampleId) {
        log.debug("getActiveLsmsBySampleId({}, {})", subjectKey, sampleId);
        String refStr = "Sample#"+sampleId;
        Set<String> subjects = getReaderSet(subjectKey);
        if (subjects == null || subjects.contains(Subject.ADMIN_KEY)) {
            return toList(imageCollection.find("{sampleRef:#,sageSynced:true}", refStr).as(LSMImage.class));
        }
        else {
            return toList(imageCollection.find("{sampleRef:#,sageSynced:true,readers:{$in:#}}", refStr, subjects).as(LSMImage.class));
        }
    }

    public List<LSMImage> getUnactiveLsmsBySampleId(String subjectKey, Long sampleId) {
        log.debug("getUnactiveLsmsBySampleId({}, {})", subjectKey, sampleId);
        String refStr = "Sample#"+sampleId;
        Set<String> subjects = getReaderSet(subjectKey);
        if (subjects == null || subjects.contains(Subject.ADMIN_KEY)) {
            return toList(imageCollection.find("{sampleRef:#,sageSynced:false}", refStr).as(LSMImage.class));
        }
        else {
            return toList(imageCollection.find("{sampleRef:#,sageSynced:false,readers:{$in:#}}", refStr, subjects).as(LSMImage.class));
        }
    }

    public List<LSMImage> getAllLsmsBySampleId(String subjectKey, Long sampleId) {
        log.debug("getAllLsmsBySampleId({}, {})", subjectKey, sampleId);
        String refStr = "Sample#"+sampleId;
        Set<String> subjects = getReaderSet(subjectKey);
        if (subjects == null || subjects.contains(Subject.ADMIN_KEY)) {
            return toList(imageCollection.find("{sampleRef:#}", refStr).as(LSMImage.class));
        }
        else {
            return toList(imageCollection.find("{sampleRef:#,readers:{$in:#}}", refStr, subjects).as(LSMImage.class));
        }
    }

    public LSMImage getActiveLsmBySageId(String subjectKey, Integer sageId) {
        log.debug("getActiveLsmBySageId({}, {})", subjectKey, sageId);
        Set<String> subjects = getReaderSet(subjectKey);
        if (subjects == null || subjects.contains(Subject.ADMIN_KEY)) {
            return imageCollection.findOne("{sageId:#,sageSynced:true}", sageId).as(LSMImage.class);
        }
        else {
            return imageCollection.findOne("{sageId:#,sageSynced:true,readers:{$in:#}}", sageId, subjects).as(LSMImage.class);
        }
    }

    public List<LSMImage> getUserLsmsBySageId(String subjectKey, Integer sageId) {
        log.debug("getUserLsmsBySageId({}, {})", subjectKey, sageId);
        if (subjectKey == null) {
            return toList(imageCollection.find("{sageId:#}", sageId).as(LSMImage.class));
        }
        else {
            return toList(imageCollection.find("{sageId:#, ownerKey:#}", sageId, subjectKey).as(LSMImage.class));
        }
    }

    public List<NeuronFragment> getNeuronFragmentsBySampleId(String subjectKey, Long sampleId) {
        log.debug("getNeuronFragmentsBySampleId({}, {})", subjectKey, sampleId);
        String refStr = "Sample#"+sampleId;
        Set<String> subjects = getReaderSet(subjectKey);
        return toList(fragmentCollection.find("{sampleRef:#, readers:{$in:#}}", refStr, subjects).as(NeuronFragment.class));
    }

    public List<NeuronFragment> getNeuronFragmentsBySeparationId(String subjectKey, Long separationId) {
        log.debug("getNeuronFragmentsBySeparationId({}, {})", subjectKey, separationId);
        Set<String> subjects = getReaderSet(subjectKey);
        return toList(fragmentCollection.find("{separationId:#, readers:{$in:#}}", separationId, subjects).as(NeuronFragment.class));
    }

    public Sample getSampleBySeparationId(String subjectKey, Long separationId) {
        log.debug("getSampleBySeparationId({}, {})", subjectKey, separationId);
        Set<String> subjects = getReaderSet(subjectKey);
        return sampleCollection.findOne("{objectiveSamples.pipelineRuns.results.results.id:#, readers:{$in:#}}", separationId, subjects).as(Sample.class);
    }

    public NeuronSeparation getNeuronSeparation(String subjectKey, Long separationId) throws Exception {
        log.debug("getNeuronSeparation({}, {})", subjectKey, separationId);
        Set<String> subjects = getReaderSet(subjectKey);
        // TODO: match subject set to ensure user has read permission
        Aggregate.ResultsIterator<NeuronSeparation> results = sampleCollection.aggregate("{$match: {\"objectiveSamples.pipelineRuns.results.results.id\": " + separationId + "}}")
                .and("{$unwind: \"$objectiveSamples\"}")
                .and("{$unwind: \"$objectiveSamples.pipelineRuns\"}")
                .and("{$unwind: \"$objectiveSamples.pipelineRuns.results\"}")
                .and("{$unwind: \"$objectiveSamples.pipelineRuns.results.results\"}")
                .and("{$match: {\"objectiveSamples.pipelineRuns.results.results.id\": "+separationId + "}}")
                .and("{$project: {class : \"$objectiveSamples.pipelineRuns.results.results.class\" ," +
                        "id : \"$objectiveSamples.pipelineRuns.results.results.id\"," +
                        "name : \"$objectiveSamples.pipelineRuns.results.results.name\"," +
                        "filepath : \"$objectiveSamples.pipelineRuns.results.results.filepath\"," +
                        "creationDate : \"$objectiveSamples.pipelineRuns.results.results.creationDate\"," +
                        "fragments : \"$objectiveSamples.pipelineRuns.results.results.fragments\"," +
                        "hasWeights : \"$objectiveSamples.pipelineRuns.results.results.hasWeights\"}}")
                .as(NeuronSeparation.class);
        if (results.hasNext()) {
            return results.next();
        }
        return null;
    }

    public TreeNode getTreeNodeById(String subjectKey, Long id) {
        log.debug("getTreeNodeById({}, {})", subjectKey, id);
        return getDomainObject(subjectKey, TreeNode.class, id);
    }

    public TreeNode getParentTreeNodes(String subjectKey, Reference ref) {
        log.debug("getParentTreeNodes({}, {})", subjectKey, ref);
        String refStr = ref.toString();
        Set<String> subjects = getReaderSet(subjectKey);
        if (subjects == null) {
            return treeNodeCollection.findOne("{'children':#}", refStr).as(TreeNode.class);
        }
        else {
            return treeNodeCollection.findOne("{'children':#,readers:{$in:#}}", refStr, subjects).as(TreeNode.class);
        }
    }

    public List<Subject> getMembersByGroupId(String groupId) {
        log.debug("getMembersByGroupId({})", groupId);
            return toList(subjectCollection.find("{userGroupRoles.groupKey:#}", groupId).as(Subject.class));
    }

    public List<Sample> getSamplesByDataSet(String dataset, int pageNumber, int pageSize, String sortBy) {
        log.debug("getSamplesByDataSet({})", dataset);
        List<Sample> samples = toList(sampleCollection.find("{dataSet:#}", dataset).sort("{"+sortBy+":1}").skip(pageSize * (pageNumber - 1)).limit(pageSize).as(Sample.class));
            return samples;
        //return toList(sampleCollection.find("{dataSet:#}", dataset).as(Sample.class));
    }

    public boolean isAdmin( String user){
        log.debug("isAdmin", user);
            if (toList(subjectCollection.find("{userGroupRoles.groupKey:'group:admin', name:#}",user).as(Subject.class)).size() != 0)
                return true;
            else
                return false;
    }


    /**
     * Create the given object, with the given id. Dangerous to use if you don't know what you're doing! Use save() instead.
     * @param subjectKey
     * @param domainObject
     * @return
     * @throws Exception
     */
    public <T extends DomainObject> T createWithPrepopulatedId(String subjectKey, T domainObject) throws Exception {
        String collectionName = DomainUtils.getCollectionName(domainObject);
        MongoCollection collection = getCollectionByName(collectionName);
        try {
            Date now = new Date();
            domainObject.setOwnerKey(subjectKey);
            domainObject.getReaders().add(subjectKey);
            domainObject.getWriters().add(subjectKey);
            domainObject.setCreationDate(now);
            domainObject.setUpdatedDate(now);
            collection.save(domainObject);
            log.trace("Created new object " + domainObject);
            return domainObject;
        }
        catch (MongoException e) {
            throw new Exception(e);
        }
    }
    
    private <T extends DomainObject> T saveImpl(String subjectKey, T domainObject) throws Exception {
        String collectionName = DomainUtils.getCollectionName(domainObject);
        MongoCollection collection = getCollectionByName(collectionName);
        try {
            Date now = new Date();
            if (domainObject.getId() == null) {
                domainObject.setId(getNewId());
                domainObject.setOwnerKey(subjectKey);
                domainObject.getReaders().add(subjectKey);
                domainObject.getWriters().add(subjectKey);
                domainObject.setCreationDate(now);
                domainObject.setUpdatedDate(now);
                collection.save(domainObject);
                log.trace("Created new object " + domainObject);
            }
            else {
                Set<String> subjects = getWriterSet(subjectKey);
                domainObject.setUpdatedDate(now);
                
                // At least one of the writers must match
                WriteResult result = collection.update("{_id:#,writers:{$in:#}}", domainObject.getId(), subjects).with(domainObject);
                if (result.getN() != 1) {
                    throw new IllegalStateException("Updated " + result.getN() + " records instead of one: " + collectionName + "#" + domainObject.getId());
                }
                log.trace("Updated " + result.getN()+ " rows for " + domainObject);
            }
            log.trace("Saved " + domainObject);
            return domainObject;
        }
        catch (MongoException e) {
            throw new Exception(e);
        }
    }

    /**
     * Saves the given object and returns a saved copy.
     *
     * @param subjectKey The subject saving the object. If this is a new object, then this subject becomes the owner of the new object.
     * @param domainObject The object to be saved. If the id is not set, then a new object is created.
     * @return a copy of the saved object
     * @throws Exception
     */
    public <T extends DomainObject> T save(String subjectKey, T domainObject) throws Exception {

        log.debug("save({}, {})", subjectKey, Reference.createFor(domainObject));
        saveImpl(subjectKey, domainObject);
        // TODO: The only reason this retrieves the saved object is to avoid errors during development where the client incorrectly 
        // depends on input object being returned. However, it's needlessly inefficient, so once we have remote clients written 
        // we may want to optimize by just returning domainObject here. 
        return getDomainObject(subjectKey, domainObject);
    }

    public void remove(String subjectKey, DomainObject domainObject) throws Exception {

        String collectionName = DomainUtils.getCollectionName(domainObject);
        MongoCollection collection = getCollectionByName(collectionName);

        log.debug("remove({}, {})", subjectKey, Reference.createFor(domainObject));

        Set<String> subjects = getWriterSet(subjectKey);
        
        WriteResult result;
        if (subjects == null) {
            result = collection.remove("{_id:#}", domainObject.getId());
        }
        else {
            result = collection.remove("{_id:#,writers:{$in:#}}", domainObject.getId(), subjects);
        }

        if (result.getN() != 1) {
            throw new IllegalStateException("Deleted " + result.getN() + " records instead of one: " + collectionName + "#" + domainObject.getId());
        }

        // TODO: remove dependent objects?
    }

    public Ontology reorderTerms(String subjectKey, Long ontologyId, Long parentTermId, int[] order) throws Exception {

        Ontology ontology = getDomainObject(subjectKey, Ontology.class, ontologyId);
        if (ontology == null) {
            throw new IllegalArgumentException("Ontology not found: " + ontologyId);
        }
        OntologyTerm parent = ontology.findTerm(parentTermId);
        if (parent == null) {
            throw new IllegalArgumentException("Term not found: " + parentTermId);
        }

        log.debug("reorderTerms({}, ontologyId={}, parentTermId={}, order={})", subjectKey, ontologyId, parentTermId, order);

        List<OntologyTerm> childTerms = new ArrayList<>(parent.getTerms());

        if (log.isTraceEnabled()) {
            log.trace("{} has the following terms: ", parent.getName());
            for (OntologyTerm term : childTerms) {
                log.trace("  {}", term.getId());
            }
            log.trace("They should be put in this ordering: ");
            for (int i = 0; i < order.length; i++) {
                log.trace("  {} -> {}", i, order[i]);
            }
        }

        int originalSize = childTerms.size();
        OntologyTerm[] reordered = new OntologyTerm[childTerms.size()];
        for (int i = 0; i < order.length; i++) {
            int j = order[i];
            reordered[j] = childTerms.get(i);
            childTerms.set(i, null);
        }

        parent.getTerms().clear();
        for (OntologyTerm ref : reordered) {
            parent.getTerms().add(ref);
        }
        for (OntologyTerm term : childTerms) {
            if (term != null) {
                log.warn("Adding broken term " + term.getId() + " at the end");
                parent.getTerms().add(term);
            }
        }

        if (childTerms.size() != originalSize) {
            throw new IllegalStateException("Reordered children have new size " + childTerms.size() + " (was " + originalSize + ")");
        }

        log.trace("Reordering children of ontology term '{}'", parent.getName());
        saveImpl(subjectKey, ontology);
        return getDomainObject(subjectKey, ontology);
    }

    public Ontology addTerms(String subjectKey, Long ontologyId, Long parentTermId, Collection<OntologyTerm> terms, Integer index) throws Exception {

        if (terms == null) {
            throw new IllegalArgumentException("Cannot add null children");
        }
        Ontology ontology = getDomainObject(subjectKey, Ontology.class, ontologyId);
        if (ontology == null) {
            throw new IllegalArgumentException("Ontology not found: " + ontologyId);
        }
        OntologyTerm parent = ontology.findTerm(parentTermId);
        if (parent == null) {
            throw new IllegalArgumentException("Term not found: " + parentTermId);
        }

        log.debug("addTerms({}, ontologyId={}, parentTermId={}, terms={}, index={})", subjectKey, ontologyId, parentTermId, abbr(terms), index);

        int i = 0;
        for (OntologyTerm childTerm : terms) {
            if (childTerm.getId() == null) {
                childTerm.setId(getNewId());
            }
            if (index != null) {
                parent.insertChild(index + i, childTerm);
            }
            else {
                parent.addChild(childTerm);
            }
            i++;
        }

        log.trace("Adding " + terms.size() + " terms to " + parent.getName());
        saveImpl(subjectKey, ontology);
        return getDomainObject(subjectKey, ontology);
    }

    public Ontology removeTerm(String subjectKey, Long ontologyId, Long parentTermId, Long termId) throws Exception {

        Ontology ontology = getDomainObject(subjectKey, Ontology.class, ontologyId);
        if (ontology == null) {
            throw new IllegalArgumentException("Ontology not found: " + ontologyId);
        }
        OntologyTerm parent = ontology.findTerm(parentTermId);
        if (parent.getTerms() == null) {
            throw new Exception("Term has no children: " + parentTermId);
        }

        log.debug("removeTerm({}, ontologyId={}, parentTermId={}, termId={})", subjectKey, ontologyId, parentTermId, termId);

        OntologyTerm removed = null;
        for (Iterator<OntologyTerm> iterator = parent.getTerms().iterator(); iterator.hasNext();) {
            OntologyTerm child = iterator.next();
            if (child != null && child.getId() != null && child.getId().equals(termId)) {
                removed = child;
                iterator.remove();
                break;
            }
        }
        if (removed == null) {
            throw new Exception("Could not find term to remove: " + termId);
        }

        log.trace("Removing term '{}' from '{}'", removed.getName(), parent.getName());
        saveImpl(subjectKey, ontology);
        return getDomainObject(subjectKey, ontology);
    }

    public TreeNode getOrCreateDefaultFolder(String subjectKey, String folderName) throws Exception {
        Workspace defaultWorkspace = getDefaultWorkspace(subjectKey);
        TreeNode folder = DomainUtils.findObjectByTypeAndName(getUserDomainObjects(subjectKey, defaultWorkspace.getChildren()), TreeNode.class, folderName);
        if (folder==null) {
            log.debug("Existing folder named {} and owned by {} was not found in the default workspace. Creating one now.", folderName, subjectKey);
            folder = new TreeNode();
            folder.setName(folderName);
            folder = save(subjectKey, folder);
            addChildren(subjectKey, defaultWorkspace, Arrays.asList(Reference.createFor(folder)));
        }
        return folder;
    }
    
    public TreeNode reorderChildren(String subjectKey, TreeNode treeNodeArg, int[] order) throws Exception {

        TreeNode treeNode = getDomainObject(subjectKey, TreeNode.class, treeNodeArg.getId());
        if (treeNode == null) {
            throw new IllegalArgumentException("Tree node not found: " + treeNodeArg.getId());
        }

        log.debug("reorderChildren({}, TreeNode#{}, order={})", subjectKey, treeNode.getId(), order);

        if (!treeNode.hasChildren()) {
            log.warn("Tree node has no children to reorder: " + treeNode.getId());
            return treeNode;
        }

        List<Reference> references = new ArrayList<>(treeNode.getChildren());

        if (references.size() != order.length) {
            throw new IllegalArgumentException("Order array must be the same size as the child array (" + order.length + "!=" + references.size() + ")");
        }

        if (log.isTraceEnabled()) {
            log.trace("{} has the following references: ", treeNode.getName());
            for (Reference reference : references) {
                log.trace("  {}#{}", reference.getTargetClassName(), reference.getTargetId());
            }
            log.trace("They should be put in this ordering: ");
            for (int i = 0; i < order.length; i++) {
                log.trace("  {} -> {}", i, order[i]);
            }
        }

        int originalSize = references.size();
        Reference[] reordered = new Reference[references.size()];
        for (int i = 0; i < order.length; i++) {
            int j = order[i];
            reordered[j] = references.get(i);
            references.set(i, null);
        }

        treeNode.getChildren().clear();
        for (Reference ref : reordered) {
            treeNode.getChildren().add(ref);
        }
        for (Reference ref : references) {
            if (ref != null) {
                log.warn("Adding broken ref to collection " + ref.getTargetClassName() + " at the end");
                treeNode.getChildren().add(ref);
            }
        }

        if (references.size() != originalSize) {
            throw new IllegalStateException("Reordered children have new size " + references.size() + " (was " + originalSize + ")");
        }

        saveImpl(subjectKey, treeNode);
        return getDomainObject(subjectKey, treeNode);
    }

    public List<DomainObject> getChildren(String subjectKey, TreeNode treeNode) {
        return getDomainObjects(subjectKey, treeNode.getChildren());
    }

    public TreeNode addChildren(String subjectKey, TreeNode treeNodeArg, Collection<Reference> references) throws Exception {
        return addChildren(subjectKey, treeNodeArg, references, null);
    }

    public TreeNode addChildren(String subjectKey, TreeNode treeNodeArg, Collection<Reference> references, Integer index) throws Exception {
        if (references == null) {
            throw new IllegalArgumentException("Cannot add null children");
        }
        TreeNode treeNode = getDomainObject(subjectKey, TreeNode.class, treeNodeArg.getId());
        if (treeNode == null) {
            throw new IllegalArgumentException("Tree node not found: " + treeNodeArg.getId());
        }
        log.debug("addChildren({}, TreeNode#{}, references={}, index={})", subjectKey, treeNode.getId(), abbr(references), index);
        Set<String> refs = new HashSet<>();
        for (Reference reference : treeNode.getChildren()) {
            refs.add(reference.toString());
        }
        int i = 0;
        List<Reference> added = new ArrayList<>();
        for (Reference ref : references) {
            if (ref.getTargetId() == null) {
                throw new IllegalArgumentException("Cannot add child without an id");
            }
            if (ref.getTargetClassName() == null) {
                throw new IllegalArgumentException("Cannot add child without a target class name");
            }
            if (refs.contains(ref.toString())) {
                log.trace("TreeNode#{} already contains {}, skipping add." , treeNode.getId(), ref);
                continue;
            }
            if (index != null) {
                treeNode.insertChild(index + i, ref);
            }
            else {
                treeNode.addChild(ref);
            }
            added.add(ref);
            i++;
        }
        saveImpl(subjectKey, treeNode);

        for (Reference ref : added) {
            addPermissions(treeNode.getOwnerKey(), ref.getTargetClassName(), ref.getTargetId(), treeNode, false);
        }

        return getDomainObject(subjectKey, treeNode);
    }

    public TreeNode removeChildren(String subjectKey, TreeNode treeNodeArg, Collection<Reference> references) throws Exception {
        if (references == null) {
            throw new IllegalArgumentException("Cannot remove null children");
        }
        TreeNode treeNode = getDomainObject(subjectKey, TreeNode.class, treeNodeArg.getId());
        if (treeNode == null) {
            throw new IllegalArgumentException("Tree node not found: " + treeNodeArg.getId());
        }
        log.debug("removeChildren({}, TreeNode#{}, references={})", subjectKey, treeNode.getId(), abbr(references));
        for (Reference ref : references) {
            if (ref.getTargetId() == null) {
                throw new IllegalArgumentException("Cannot add child without an id");
            }
            if (ref.getTargetClassName() == null) {
                throw new IllegalArgumentException("Cannot add child without a target class name");
            }
            treeNode.removeChild(ref);
        }
        saveImpl(subjectKey, treeNode);
        return getDomainObject(subjectKey, treeNode);
    }

    public TreeNode removeReference(String subjectKey, TreeNode treeNodeArg, Reference reference) throws Exception {
        TreeNode treeNode = getDomainObject(subjectKey, TreeNode.class, treeNodeArg.getId());
        if (treeNode == null) {
            throw new IllegalArgumentException("Tree node not found: " + treeNodeArg.getId());
        }
        log.debug("removeReference({}, TreeNode#{}, {})", subjectKey, treeNode.getId(), reference);
        if (treeNode.hasChildren()) {
            for (Iterator<Reference> i = treeNode.getChildren().iterator(); i.hasNext();) {
                Reference iref = i.next();
                if (iref.equals(reference)) {
                    i.remove();
                }
            }
            saveImpl(subjectKey, treeNode);
        }
        return getDomainObject(subjectKey, treeNode);
    }

    public <T extends DomainObject> T updateProperty(String subjectKey, Class<T> clazz, Long id, String propName, Object propValue) throws Exception {
        return (T) updateProperty(subjectKey, clazz.getName(), id, propName, propValue);
    }

    public DomainObject updateProperty(String subjectKey, String className, Long id, String propName, Object propValue) throws Exception {
        Class<? extends DomainObject> clazz = DomainUtils.getObjectClassByName(className);
        DomainObject domainObject = getDomainObject(subjectKey, clazz, id);
        try {
            set(domainObject, propName, propValue);
        }
        catch (Exception e) {
            throw new IllegalStateException("Could not update object attribute " + propName, e);
        }

        Set<String> subjects = getWriterSet(subjectKey);
        
        log.debug("updateProperty({}, {}, name={}, value={})",subjectKey, Reference.createFor(domainObject), propName, propValue);
        String collectionName = DomainUtils.getCollectionName(className);
        MongoCollection collection = getCollectionByName(collectionName);
        WriteResult wr = collection.update("{_id:#,writers:{$in:#}}", domainObject.getId(), subjects).with("{$set: {" + propName + ":#, updatedDate:#}}", propValue, new Date());
        if (wr.getN() != 1) {
            throw new Exception("Could not update " + collectionName + "#" + domainObject.getId() + "." + propName);
        }
        return domainObject;
    }

    public <T extends DomainObject> void deleteProperty(String ownerKey, Class<T> clazz, String propName) {
        String collectionName = DomainUtils.getCollectionName(clazz);
        log.debug("deleteProperty({}, collection={}, name={})", ownerKey, collectionName, propName);
        MongoCollection collection = getCollectionByName(collectionName);
        WriteResult wr = collection.update("{ownerKey:#}", ownerKey).with("{$unset: {" + propName + ":\"\"}}");
        if (wr.getN() != 1) {
            log.warn("Could not delete property " + collectionName + "." + propName);
        }
    }
    
    public void addPermissions(String ownerKey, String className, Long id, DomainObject permissionTemplate, boolean forceChildUpdates) throws Exception {
        log.debug("addPermissions({}, className={}, id={}, permissionTemplate={}, forceChildUpdates={})", ownerKey, className, id, permissionTemplate, forceChildUpdates);
        changePermissions(ownerKey, className, Arrays.asList(id), permissionTemplate.getReaders(), permissionTemplate.getWriters(), true, forceChildUpdates, false);
    }
    
    public void setPermissions(String ownerKey, String className, Long id, String grantee, boolean read, boolean write, boolean forceChildUpdates) throws Exception {

        DomainObject targetObject = getDomainObject(ownerKey, className, id);
        
        Set<String> readAdd = new HashSet<>();
        Set<String> readRemove = new HashSet<>();
        Set<String> writeAdd = new HashSet<>();
        Set<String> writeRemove = new HashSet<>();
        
        if (DomainUtils.hasReadAccess(targetObject, grantee)) {
            if (!read) {
                if (DomainUtils.isOwner(targetObject, grantee)) {
                    log.warn("Cannot remove owner's read permission for {}", targetObject);
                }
                else {
                    readRemove.add(grantee);
                }
            }
        }
        else {
            if (read) {
                readAdd.add(grantee);
            }
        }

        if (DomainUtils.hasWriteAccess(targetObject, grantee)) {
            if (!write) {
                if (DomainUtils.isOwner(targetObject, grantee)) {
                    log.warn("Cannot remove owner's write permission for {}", targetObject);
                }
                else {
                    writeRemove.add(grantee);
                }
            }
        }
        else {
            if (write) {
                writeAdd.add(grantee);
            }
        }

        if (!readAdd.isEmpty() || !writeAdd.isEmpty()) {
            changePermissions(ownerKey, className, Arrays.asList(id), readAdd, writeAdd, true, forceChildUpdates, true);
        }
        
        if (!readRemove.isEmpty() || !writeRemove.isEmpty()) {
            changePermissions(ownerKey, className, Arrays.asList(id), readRemove, writeRemove, false, forceChildUpdates, true);
        }
    }
    
    private void changePermissions(String subjectKey, String className, Collection<Long> ids, Collection<String> readers, Collection<String> writers, 
            boolean grant, boolean forceChildUpdates, boolean createSharedDataLinks) throws Exception {
        changePermissions(subjectKey, className, ids, readers, writers, grant, forceChildUpdates, createSharedDataLinks, new HashSet<Long>());
    }
    
    private void changePermissions(String subjectKey, String className, Collection<Long> ids, Collection<String> readers, Collection<String> writers, 
            boolean grant, boolean forceChildUpdates, boolean createSharedDataLinks, Set<Long> visited) throws Exception {

        Set<String> subjects = getWriterSet(subjectKey);
        String logIds = DomainUtils.abbr(ids);

        String collectionName = DomainUtils.getCollectionName(className);
        String op = grant ? "$addToSet" : "$pull";
        String iter = grant ? "$each" : "$in";
        String withClause = "{"+op+":{readers:{"+iter+":#},writers:{"+iter+":#}}}";

        Class<? extends DomainObject> clazz = DomainUtils.getObjectClassByName(className);
        List<Reference> objectRefs = new ArrayList<>();
        for(Long id : ids) {
            objectRefs.add(Reference.createFor(clazz, id));
        }
        
        if (grant) {
            log.debug("grantPermissions({}, {}, ids={}, readers={}, writers={})", subjectKey, collectionName, logIds, readers, writers);
        }
        else {
            log.debug("revokePermissions({}, {}, ids={}, readers={}, writers={})", subjectKey, collectionName, logIds, readers, writers);
        }
        
        if (readers.isEmpty() && writers.isEmpty()) return;

        MongoCollection collection = getCollectionByName(collectionName);
        WriteResult wr = collection.update("{_id:{$in:#},writers:{$in:#}}", ids, subjects).multi().with(withClause, readers, writers);

        log.debug("Changing permissions on {} documents", wr.getN());

        if (forceChildUpdates || (wr.getN() > 0)) {
            // Update related objects
            // TODO: this class shouldn't know about these domain object classes, it should delegate somewhere else.
            
            if ("treeNode".equals(collectionName)) {
                log.trace("Changing permissions on all members of the folders: {}", logIds);
                for (Long id : ids) {
                    
                    if (visited.contains(id)) {
                        log.trace("Already visited folder with id="+id);
                        continue;
                    }
                    visited.add(id);
                    
                    TreeNode node = collection.findOne("{_id:#,writers:{$in:#}}", id, subjects).as(TreeNode.class);
                    if (node == null) {
                        log.warn("Could not find folder with id=" + id);
                    }
                    else if (node.hasChildren()) {
                        Multimap<String, Long> groupedIds = HashMultimap.create();
                        for (Reference ref : node.getChildren()) {
                            groupedIds.put(ref.getTargetClassName(), ref.getTargetId());
                        }

                        for (String refClassName : groupedIds.keySet()) {
                            Collection<Long> refIds = groupedIds.get(refClassName);
                            changePermissions(subjectKey, refClassName, refIds, readers, writers, grant, forceChildUpdates, false, visited);
                        }
                    }
                }
            }
            else if ("sample".equals(collectionName)) {

                log.trace("Changing permissions on all fragments and lsms associated with samples: {}", logIds);

                List<String> sampleRefs = DomainUtils.getRefStrings(objectRefs);

                WriteResult wr1 = fragmentCollection.update("{sampleRef:{$in:#},writers:{$in:#}}", sampleRefs, subjects).multi().with(withClause, readers, writers);
                log.trace("Updated permissions on {} fragments", wr1.getN());

                WriteResult wr2 = imageCollection.update("{sampleRef:{$in:#},writers:{$in:#}}", sampleRefs, subjects).multi().with(withClause, readers, writers);
                log.trace("Updated permissions on {} lsms", wr2.getN());

            }
            else if ("dataSet".equals(collectionName)) {
                log.trace("Changing permissions on all samples and LSMs of the data sets: {}", logIds);
                for (Long id : ids) {
                    
                    // Retrieve the data set in order to find its identifier

                    DataSet dataSet = collection.findOne("{_id:#,writers:{$in:#}}", id, subjects).as(DataSet.class);
                    if (dataSet == null) {
                        throw new IllegalArgumentException("Could not find an writeable data set with id=" + id);
                    }

                    // Get all sample ids for a given data set
                    List<String> sampleRefs = new ArrayList<>();
                    for(Sample sample : sampleCollection.find("{dataSet:#}",dataSet.getIdentifier()).projection("{class:1,_id:1}").as(Sample.class)) {
                        sampleRefs.add("Sample#"+sample.getId());
                    }

                    // This could just call changePermissions recursively, but batching is far more efficient.
                    WriteResult wr1 = sampleCollection.update("{dataSet:#,writers:{$in:#}}", dataSet.getIdentifier(), subjects).multi().with(withClause, readers, writers);
                    log.trace("Changed permissions on {} samples",wr1.getN());

                    WriteResult wr2 = fragmentCollection.update("{sampleRef:{$in:#},writers:{$in:#}}", sampleRefs, subjects).multi().with(withClause, readers, writers);
                    log.trace("Updated permissions on {} fragments", wr2.getN());

                    WriteResult wr3 = imageCollection.update("{sampleRef:{$in:#},writers:{$in:#}}", sampleRefs, subjects).multi().with(withClause, readers, writers);
                    log.trace("Updated permissions on {} lsms", wr3.getN());

                    // JW-25275: Automatically create data set filters when sharing data sets
                    
                    if (grant) {
                        for (String granteeKey : readers) {
    
                            if (granteeKey.equals(dataSet.getOwnerKey())) continue;
    
                            String filterName = dataSet.getName()+" ("+dataSet.getOwnerName()+")";
                            
                            if (getUserDomainObjectsByName(granteeKey, Filter.class, filterName).isEmpty()) {
                                // Grantee has no filters for this data set, so let's create one
                                
                                log.info("Creating data set filter for "+filterName+", shared with "+granteeKey);
                                Filter filter = createDataSetFilter(granteeKey, dataSet, filterName);
                                
                                // Now add it to their Saved Data folder

                                TreeNode sharedDataFolder = getOrCreateDefaultFolder(granteeKey, DomainConstants.NAME_SHARED_DATA);
                                addChildren(granteeKey, sharedDataFolder, Arrays.asList(Reference.createFor(filter)));
                            }
                        }
                    }
                }
            }
            else if ("tmWorkspace".equals(collectionName)) {
                
                log.trace("Changing permissions on the TmSamples associated with the TmWorkspaces: {}", logIds);

                List<Long> sampleIds = new ArrayList<>();
                for(TmWorkspace workspace : tmWorkspaceCollection.find("{_id:{$in:#}}",ids).projection("{class:1,sampleRef:1}").as(TmWorkspace.class)) {
                    sampleIds.add(workspace.getSampleId());
                }
                
                WriteResult wr1 = tmSampleCollection.update("{_id:{$in:#},writers:{$in:#}}", sampleIds, subjects).multi().with(withClause, readers, writers);
                log.trace("Updated permissions on {} TmSamples", wr1.getN());

                List<String> workspaceRefs = DomainUtils.getRefStrings(objectRefs);
                log.trace("Changing permissions on the TmNeurons associated with the TmWorkspaces: {}", workspaceRefs);
                WriteResult wr2 = tmNeuronCollection.update("{workspaceRef:{$in:#},writers:{$in:#}}", workspaceRefs, subjects).multi().with(withClause, readers, writers);
                log.trace("Updated permissions on {} TmNeurons", wr2.getN());
            }
        }

        if (createSharedDataLinks) {
            // Ensure shared items are in the grantee's Shared Data folder
            Set<String> grantees = new HashSet<String>();
            grantees.addAll(readers);
            grantees.addAll(writers);
            
            // TODO: need a better way to specify the object classes that can be added to Shared Data
            if (clazz.isAssignableFrom(TreeNode.class) 
                    || clazz.isAssignableFrom(Filter.class) 
                    || clazz.isAssignableFrom(Sample.class) 
                    || clazz.isAssignableFrom(NeuronFragment.class) 
                    || clazz.isAssignableFrom(TmSample.class) 
                    || clazz.isAssignableFrom(TmWorkspace.class)) {
    
                for(String grantee : grantees) {
                    if (!grantee.equals(subjectKey)) {
                        TreeNode sharedDataFolder = getOrCreateDefaultFolder(grantee, DomainConstants.NAME_SHARED_DATA);
                        if (grant) {
                            addChildren(grantee, sharedDataFolder, objectRefs);
                        }
                        else {
                            removeChildren(grantee, sharedDataFolder, objectRefs);
                        }
                    }
                }
            }
        }
    }
    
    private Filter createDataSetFilter(String subjectKey, DataSet dataSet, String filterName) throws Exception {

        Filter filter = new Filter();
        filter.setName(filterName);
        filter.setSearchClass(Sample.class.getSimpleName());

        FacetCriteria dataSetCriteria = new FacetCriteria();
        dataSetCriteria.setAttributeName("dataSet");
        dataSetCriteria.setValues(Sets.newHashSet(dataSet.getIdentifier()));
        filter.addCriteria(dataSetCriteria);

        FacetCriteria syncFacet = new FacetCriteria();
        syncFacet.setAttributeName("sageSynced");
        syncFacet.setValues(Sets.newHashSet("true"));
        filter.addCriteria(syncFacet);

        save(subjectKey, filter);
        
        return filter;
    }

    public void addPipelineStatusTransition(Long sampleId, PipelineStatus source, PipelineStatus target, String orderNo,
                                            String process, Map<String,Object> parameters) throws Exception {
        log.info("adding StateTransition (source={}, target={}, sampleId={}, orderNo={}, process={})", source, target, sampleId, orderNo,
                process);
        StatusTransition newStatusTransition = new StatusTransition();
        newStatusTransition.setSampleId(sampleId);
        newStatusTransition.setSource(source);
        newStatusTransition.setTarget(target);
        newStatusTransition.setTransitionDate(new Date());
        newStatusTransition.setProcess(process);
        newStatusTransition.setParameters(parameters);
        newStatusTransition.setOrderNo(orderNo);
        pipelineStatusCollection.insert(newStatusTransition);
    }

    public List<StatusTransition> getPipelineStatusTransitionsBySampleId(Long sampleId) throws Exception {
       return toList(pipelineStatusCollection.find("{sampleId: #}", sampleId).as(StatusTransition.class));
    }

    public void addIntakeOrder(String orderNo,String owner) throws Exception {
        log.info("adding IntakeOrder (orderNo={}, owner={})", orderNo, owner);
        IntakeOrder newOrder = new IntakeOrder();
        newOrder.setOrderNo(orderNo);
        newOrder.setOwner(owner);
        newOrder.setStartDate(Calendar.getInstance().getTime());
        newOrder.setStatus(OrderStatus.Intake);
        intakeOrdersCollection.insert(newOrder);
    }

    public void addOrUpdateIntakeOrder(IntakeOrder order) throws Exception {
        log.info("adding/updating IntakeOrder (orderNo={}, owner={})", order.getOrderNo(), order.getOwner());
        IntakeOrder prevOrder = getIntakeOrder(order.getOrderNo());
        if (prevOrder==null) {
            intakeOrdersCollection.insert(order);
        } else {
            intakeOrdersCollection.update("{orderNo: #}}", order.getOrderNo()).with(order);
        }
    }

    // returns order information (including Sample Ids) given a number of hours time window
    public List<IntakeOrder> getIntakeOrders(Calendar cutoffDate) throws Exception {
        return toList(intakeOrdersCollection.find("{startDate: {$gte: #}}", cutoffDate).as(IntakeOrder.class));
    }

    // returns specific order information
    public IntakeOrder getIntakeOrder(String orderNo) throws Exception {
        List<IntakeOrder> orderList = toList(intakeOrdersCollection.find("{orderNo: #}}", orderNo).as(IntakeOrder.class));
        if (orderList==null || orderList.size()==0)
            return null;
        return orderList.get(0);
    }

    public void addSampleToOrder(String orderNo, Long sampleId) throws Exception {
        intakeOrdersCollection.update("{orderNo: #}", orderNo).with("{$addToSet: { sampleIds: # } }", sampleId);
    }

    // add SampleIds to order as they get processed
    public void addSampleIdsToOrder(String orderNo, Long[] sampleIds) throws Exception {
        intakeOrdersCollection.update("{orderNo: #}", orderNo).with("{$push: { sampleIds: { $each: # } } }", sampleIds);
    }

    // Copy and pasted from ReflectionUtils in shared module
    private void set(Object obj, String attributeName, Object value) throws Exception {
        Class<?>[] argTypes = {value.getClass()};
        Object[] argValues = {value};
        String methodName = getAccessor("set", attributeName);
        obj.getClass().getMethod(methodName, argTypes).invoke(obj, argValues);
    }

    // Copy and pasted from ReflectionUtils in shared module
    private static String getAccessor(String prefix, String attributeName) {
        String firstChar = attributeName.substring(0, 1).toUpperCase();
        return prefix + firstChar + attributeName.substring(1);
    }

    public Long getNewId() {
        return TimebasedIdentifierGenerator.generateIdList(1).get(0);
    }

    public List<LineRelease> getLineReleases(String subjectKey) {
        log.debug("getLineReleases({})", subjectKey);
        Set<String> subjects = getReaderSet(subjectKey);
        if (subjects == null) {
            return toList(releaseCollection.find().as(LineRelease.class));
        }
        else {
            return toList(releaseCollection.find("{readers:{$in:#}}", subjects).as(LineRelease.class));
        }
    }

    public LineRelease createLineRelease(String subjectKey, String name, Date releaseDate, Integer lagTimeMonths, List<String> dataSets) throws Exception {
        log.debug("createLineRelease({}, name={}, releaseDate={}, lagTimeMonths={}, dataSets={})", subjectKey, name, dataSets);
        LineRelease release = new LineRelease();
        release.setName(name);
        release.setReleaseDate(releaseDate);
        release.setLagTimeMonths(lagTimeMonths);
        release.setDataSets(dataSets);
        return save(subjectKey, release);
    }

    public static void main(String[] args) throws Exception {
        
        String MONGO_SERVER_URL = "dev-mongodb";
        String MONGO_DATABASE = "jacs";
        DomainDAO dao = new DomainDAO(MONGO_SERVER_URL, MONGO_DATABASE);

//        String owner = "user:rokickik";
//        for(Workspace workspace : dao.getWorkspaces(owner)) {
//            System.out.println(""+workspace.getName());
//            for(DomainObject topLevelObj : dao.getDomainObjects(owner, workspace.getChildren())) {
//                System.out.println("  "+topLevelObj.getName());
//                if (topLevelObj instanceof TreeNode) {
//                    for(DomainObject domainObject : dao.getDomainObjects(owner, ((TreeNode)topLevelObj).getChildren())) {
//                        System.out.println("    "+domainObject.getName());
//                    }
//                }
//            }
//        }

//        dao.changePermissions("group:heberleinlab", DataSet.class.getSimpleName(), 1831437750079848537L, Arrays.asList("user:rokickik", "user:saffordt"), "r", false);
    }
}
