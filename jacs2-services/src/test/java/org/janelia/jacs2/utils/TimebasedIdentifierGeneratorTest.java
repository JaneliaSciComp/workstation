package org.janelia.jacs2.utils;

import com.google.common.collect.ImmutableSet;
import org.janelia.jacs2.dao.mongo.utils.TimebasedIdentifierGenerator;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

public class TimebasedIdentifierGeneratorTest {
    private TimebasedIdentifierGenerator idGenerator;

    @Before
    public void setUp() {
        idGenerator = new TimebasedIdentifierGenerator(0);
    }

    @Test
    public void generateLargeListOfIds() {
        List<Number> idList = idGenerator.generateIdList(16384);
        assertThat(ImmutableSet.copyOf(idList), hasSize(idList.size()));
    }
}
