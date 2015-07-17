package org.janelia.jos.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.janelia.jos.model.JOSObject;
import org.janelia.jos.mongo.MongoManaged;
import org.janelia.jos.scality.ScalityDAO;
import org.jongo.MongoCollection;
import org.jongo.MongoCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AbstractScheduledService;

/**
 * Periodically purge deleted files from Scality.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ScalityDeletionTask extends AbstractScheduledService {

    private final Logger log = LoggerFactory.getLogger(ScalityDeletionTask.class);

    private MongoManaged mm;
    private ScalityDAO scality;
    
    public ScalityDeletionTask(MongoManaged mm, ScalityDAO scality) {
        this.mm = mm;
        this.scality = scality;
    }
    
    @Override
    protected void runOneIteration() throws Exception {

        MongoCursor<JOSObject> cursor = getObjectCollection().find("{deleted:#}",true).as(JOSObject.class);
        List<String> paths = new ArrayList<>();
        for (JOSObject obj : cursor) {
            if (obj.getPath()!=null) {
                log.info("Adding path for deletion: path="+obj.getPath()+" id="+obj.getId()+" name="+obj.getName());
                paths.add(obj.getPath());
            }
            
        }
        
        log.debug("Purging {} objects marked for deletion",paths.size());

        int c = 0;
        for(String path : paths) {
            if (scality.delete(path)) {
                getObjectCollection().remove("{path:#}",path);
                c++;
            }
        }
        
        if (c>0) {
            log.info("Purged {} objects marked for deletion",c);
        }
    }

    @Override
    protected AbstractScheduledService.Scheduler scheduler() {
        return AbstractScheduledService.Scheduler.newFixedRateSchedule(5, 300, TimeUnit.SECONDS);
    }
    
    private MongoCollection getObjectCollection() {
        return mm.getJongo().getCollection("object");
    }
}
