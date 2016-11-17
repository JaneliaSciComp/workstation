package org.janelia.jacs2.dao.jpa;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableList;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.SortCriteria;
import org.janelia.jacs2.model.page.SortDirection;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

public class AbstractJpaDaoTest {

    private static class TestEntity {
        Long id;
        String sField;
    }

    private static class TestEntityDao extends AbstractJpaDao<TestEntity, Long> {
        public TestEntityDao(EntityManager entityManager) {
            super(entityManager);
        }
    }

    @Test
    public void findById() {
        EntityManager mockEm = mock(EntityManager.class);
        TestEntityDao testDao = new TestEntityDao(mockEm);
        testDao.findById(1L);
        verify(mockEm).find(TestEntity.class, 1L);
    }

    @Test
    public void findAllWithEmptyCriteriaInTheList() {
        EntityManager mockEm = mock(EntityManager.class);
        TestEntityDao testDao = new TestEntityDao(mockEm);
        PageRequest pageRequest = new PageRequest();
        pageRequest.setPageNumber(1);
        pageRequest.setPageSize(5);
        pageRequest.setSortCriteria(ImmutableList.of(new SortCriteria(), new SortCriteria()));
        TypedQuery<TestEntity> mockQuery = (TypedQuery<TestEntity>) mock(TypedQuery.class);
        String qstring = "select e from TestEntity e ";
        when(mockEm.createQuery(qstring, TestEntity.class)).thenReturn(mockQuery);
        testDao.findAll(pageRequest);
        verify(mockEm).createQuery("select e from TestEntity e ", TestEntity.class);
        verify(mockQuery).setFirstResult(anyInt());
        verify(mockQuery).setMaxResults(anyInt());
    }

    @Test
    public void findAllWithCriteriaInTheList() {
        EntityManager mockEm = mock(EntityManager.class);
        TestEntityDao testDao = new TestEntityDao(mockEm);
        PageRequest pageRequest = new PageRequest();
        pageRequest.setSortCriteria(ImmutableList.of(new SortCriteria("f1"), new SortCriteria("f2", SortDirection.DESC)));
        TypedQuery<TestEntity> mockQuery = mock(TypedQuery.class);
        when(mockEm.createQuery(anyString(), same(TestEntity.class))).thenReturn(mockQuery);
        testDao.findAll(pageRequest);
        verify(mockEm).createQuery("select e from TestEntity e order by f1 ASC,f2 DESC", TestEntity.class);
        verify(mockQuery, never()).setFirstResult(anyInt());
        verify(mockQuery, never()).setMaxResults(anyInt());
    }

    @Test
    public void countAll() {
        EntityManager mockEm = mock(EntityManager.class);
        TestEntityDao testDao = new TestEntityDao(mockEm);
        TypedQuery<Long> mockQuery = mock(TypedQuery.class);
        when(mockEm.createQuery(anyString(), same(Long.class))).thenReturn(mockQuery);
        when(mockQuery.getSingleResult()).thenReturn(0L);
        testDao.countAll();
        verify(mockEm).createQuery("select count(e) from TestEntity e", Long.class);
    }

}
