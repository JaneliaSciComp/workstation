package org.janelia.jacs2.dao.mongo;

import com.google.common.collect.ImmutableList;
import org.janelia.jacs2.dao.DomainObjectDao;
import org.janelia.jacs2.model.domain.DomainObject;
import org.janelia.jacs2.model.domain.Subject;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.page.SortCriteria;
import org.janelia.jacs2.model.page.SortDirection;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIn.isIn;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public abstract class AbstractDomainObjectDaoITest<T extends DomainObject> extends AbstractMongoDaoITest<T> {
    protected static final String TEST_OWNER_KEY = "user:test";

    protected void findByOwner(Subject subject, DomainObjectDao<T> dao) {
        String otherOwner = "group:findByOwner";
        List<T> testItems = createMultipleTestItems(10);
        List<T> otherOwnersItems = new ArrayList<>();
        testItems.parallelStream().forEach(s -> {
            if (dataGenerator.nextBoolean()) {
                s.setOwnerKey(otherOwner);
                otherOwnersItems.add(s);
            } else {
                s.setOwnerKey(TEST_OWNER_KEY);
            }
            dao.save(s);
        });
        PageRequest pageRequest = new PageRequest();
        pageRequest.setSortCriteria(ImmutableList.of(
                new SortCriteria("ownerKey", SortDirection.ASC),
                new SortCriteria("creationDate", SortDirection.DESC)));
        PageResult<T> u1Data = dao.findByOwnerKey(subject, TEST_OWNER_KEY, pageRequest);
        PageResult<T> u2Data = dao.findByOwnerKey(subject, otherOwner, pageRequest);
        assertThat(u1Data.getResultList(), everyItem(hasProperty("ownerKey", equalTo(TEST_OWNER_KEY))));
        assertThat(u2Data.getResultList(), everyItem(hasProperty("ownerKey", equalTo(otherOwner))));
    }

    protected void findByIdsWithNoSubject(DomainObjectDao<T> dao) {
        List<T> testItems = createMultipleTestItems(10);
        testItems.parallelStream().forEach(dao::save);
        List<Number> testItemIds = testItems.stream().map(d -> d.getId()).collect(Collectors.toCollection(ArrayList<Number>::new));
        List<T> res = dao.findByIds(null, testItemIds);
        assertThat(res, hasSize(testItems.size()));
        assertThat(res, everyItem(hasProperty("id", isIn(testItemIds))));
    }

    protected void findByIdsWithSubject(DomainObjectDao<T> dao) {
        String otherOwner = "user:findByIdsWithSubject";
        String otherGroup = "group:findByIdsWithSubject";
        Subject otherSubject = new Subject();
        otherSubject.setKey(otherOwner);
        otherSubject.addGroup(otherGroup);

        List<T> testItems = createMultipleTestItems(40);
        List<T> accessibleItems = new ArrayList<>();
        IntStream.range(0, testItems.size()).forEach(i -> {
            T testItem = testItems.get(i);
            if (i % 4 == 0) {
                testItem.setOwnerKey(TEST_OWNER_KEY);
            } else if (i % 4 == 1) {
                testItem.setOwnerKey(otherOwner);
                accessibleItems.add(testItem); // object owned by other owner
            } else if (i % 4 == 2) {
                testItem.addReader(otherOwner);
                accessibleItems.add(testItem); // object readable by other owner
            } else if (i % 4 == 3) {
                testItem.addReader(otherGroup);
                accessibleItems.add(testItem); // object readable by other group
            }
            dao.save(testItem);
        });
        List<Number> testItemIds = testItems.stream().map(d -> d.getId()).collect(Collectors.toCollection(ArrayList<Number>::new));
        List<T> res = dao.findByIds(otherSubject, testItemIds);
        assertThat(res, everyItem(hasProperty("id", isIn(accessibleItems.stream().map(s -> s.getId()).collect(Collectors.toList())))));
        assertTrue(res.size() == accessibleItems.size());
    }

}
