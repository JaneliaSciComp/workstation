package org.janelia.workstation.microjacsapi;

import com.codahale.metrics.health.HealthCheck;

public class MongoHealthCheck extends HealthCheck {
    
    private MongoManaged mm;

    public MongoHealthCheck(MongoManaged mm) {
        this.mm = mm;
    }

    @Override
    protected Result check() throws Exception {
        mm.getDatabase().getCollectionNames();
        return Result.healthy();
    }
}