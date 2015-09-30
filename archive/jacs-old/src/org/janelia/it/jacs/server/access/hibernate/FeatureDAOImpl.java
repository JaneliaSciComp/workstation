
package org.janelia.it.jacs.server.access.hibernate;

import org.apache.log4j.Logger;
import org.hibernate.*;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.genomics.*;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.server.access.FeatureDAO;
import org.janelia.it.jacs.server.utils.AnnotationUtil;
import org.janelia.it.jacs.shared.fasta.FASTAFileNodeHelper;
import org.janelia.it.jacs.web.gwt.detail.server.bse.BSEntityServiceImpl;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: tdolafi
 * Date: Mar 30, 2006
 * Time: 10:47:35 AM
 */
public class FeatureDAOImpl extends DaoBaseImpl implements FeatureDAO {
    private static Logger _logger = Logger.getLogger(FeatureDAOImpl.class.getName());

    public static final String ANNOT_ROW_SEP = "\n";
    public static final String ANNOT_LIST_SEP = "\t";

    // DAO's can only come from Spring's Hibernate
    private FeatureDAOImpl() {
    }

//    public List<Chromosome> getAllMicrobialChromosomes() throws DataAccessException, DaoException {
//        try {
//            DetachedCriteria criteria = DetachedCriteria.forClass(Chromosome.class);
//            return getHibernateTemplate().findByCriteria(criteria);
//        }
//        catch (DataAccessResourceFailureException e) {
//            throw handleException(e, "FeatureDAOImpl - getAllMicrobialChromosomes");
//        }
//        catch (IllegalStateException e) {
//            throw handleException(e, "FeatureDAOImpl - getAllMicrobialChromosomes");
//        }
//        catch (HibernateException e) {
//            throw convertHibernateAccessException(e);
//        }
//    }
//
    public List<BaseSequenceEntity> findAllBse() throws DataAccessException, DaoException {
        try {
            DetachedCriteria criteria = DetachedCriteria.forClass(BaseSequenceEntity.class);
            return getHibernateTemplate().findByCriteria(criteria);
        }
        catch (DataAccessResourceFailureException e) {
            throw handleException(e, "FeatureDAOImpl - findAllBse");
        }
        catch (IllegalStateException e) {
            throw handleException(e, "FeatureDAOImpl - findAllBse");
        }
        catch (HibernateException e) {
            throw convertHibernateAccessException(e);
        }
    }

    public BaseSequenceEntity findBseByUid(Long featureUid) throws DataAccessException, DaoException {
        try {
            return (BaseSequenceEntity) getHibernateTemplate().get(BaseSequenceEntity.class, featureUid);
        }
        catch (HibernateException e) {
            throw convertHibernateAccessException(e);
        }
        catch (DataAccessResourceFailureException e) {
            throw handleException(e, "FeatureDAOImpl - findByUid");
        }
        catch (IllegalStateException e) {
            throw handleException(e, "FeatureDAOImpl - findByUid");
        }
    }

    public BaseSequenceEntity findBseByAcc(String accesion) throws DataAccessException, DaoException {
        _logger.debug("findBseByAcc() called with accession=" + accesion);
        try {
            DetachedCriteria criteria = DetachedCriteria.forClass(BaseSequenceEntity.class);
            Criterion cr = Restrictions.eq("accession", accesion);
            criteria.add(cr);
            criteria.setFetchMode("assembly", FetchMode.JOIN);

            List<BaseSequenceEntity> bses = (List<BaseSequenceEntity>) getHibernateTemplate().findByCriteria(criteria);
            if (bses.size() < 1) {
                _logger.debug("no results - returning null");
                return null;
            }
            else {
                BaseSequenceEntity bse = bses.get(0);
                if (bse == null) {
                    _logger.debug("null result - returning null");
                }
                else {
                    _logger.debug("returning bse with accession=" + bse.getAccession());
                }
                return bses.get(0);
            }
        }
        catch (HibernateException e) {
            throw convertHibernateAccessException(e);
        }
        catch (DataAccessResourceFailureException e) {
            throw handleException(e, "FeatureDAOImpl - findByAcc");
        }
        catch (IllegalStateException e) {
            throw handleException(e, "FeatureDAOImpl - findByAcc");
        }
    }

    public BaseSequenceEntity findBseByAccOrId(String accOrId) throws DataAccessException, DaoException {
        // if numeric - search by id, if not - by acc
        BaseSequenceEntity bse;
        try {
            Long id = Long.valueOf(accOrId);
            bse = findBseByUid(id);
        }
        catch (NumberFormatException e) {
            // it's an accesion
            bse = findBseByAcc(accOrId);
        }
        return bse;
    }

    public List<Read> findReadsByLibraryId(String libraryId) throws DataAccessException, DaoException {
        try {
            DetachedCriteria criteria = DetachedCriteria.forClass(Read.class);
            Criterion libId = Restrictions.eq("library", libraryId);
            criteria.add(libId);
            return getHibernateTemplate().findByCriteria(criteria);
        }
        catch (HibernateException e) {
            throw convertHibernateAccessException(e);
        }
        catch (DataAccessResourceFailureException e) {
            throw handleException(e, "FeatureDAOImpl - findReadsByLibraryId");
        }
        catch (IllegalStateException e) {
            throw handleException(e, "FeatureDAOImpl - findReadsByLibraryId");
        }
    }

    public Integer getNumReadsFromSample(String sampleAcc) throws DataAccessException, DaoException {
        // we count the reads by summing the number of reads per library selected by sample
        // since this is more efficient than going through all the reads for the sample
        String hql = "select " +
                "sum(library.numberOfReads) " +
                "from Library library " +
                "where library.sampleAcc = :sampleAcc";
        Query query = getSession().createQuery(hql);
        query.setParameter("sampleAcc", sampleAcc);
        _logger.debug("Count reads for sample " + sampleAcc + " hql: " + hql);
        Long c = (Long) query.uniqueResult();
        return c == null ? 0 : c.intValue();
    }

    public List<Read> getPagedReadsFromSample(String sampleAcc,
                                              Set<String> readAccessions,
                                              int startIndex,
                                              int numRows,
                                              SortArgument[] sortArgs)
            throws DataAccessException, DaoException {
        StringBuffer orderByFieldsBuffer = new StringBuffer();
        if (sortArgs != null) {
            for (SortArgument sortArg : sortArgs) {
                String dataSortField = sortArg.getSortArgumentName();
                if (dataSortField == null || dataSortField.length() == 0) {
                    continue;
                }
                if (sortArg.isAsc()) {
                    if (orderByFieldsBuffer.length() > 0) {
                        orderByFieldsBuffer.append(',');
                    }
                    orderByFieldsBuffer.append(dataSortField + " asc");
                }
                else if (sortArg.isDesc()) {
                    if (orderByFieldsBuffer.length() > 0) {
                        orderByFieldsBuffer.append(',');
                    }
                    orderByFieldsBuffer.append(dataSortField + " desc");
                }
            } // end for all sortArgs
        }
        String orderByClause = "";
        if (orderByFieldsBuffer.length() > 0) {
            orderByClause = "order by " + orderByFieldsBuffer.toString();
        }
        StringBuffer hqlBuffer = new StringBuffer("select r " +
                "from Read r " +
                "where r.library.sampleAcc = :sampleAcc ");
        if (readAccessions != null && readAccessions.size() > 0) {
            hqlBuffer.append("and r.accession in (:readAccessions) ");
        }
        hqlBuffer.append(orderByClause);
        Query query = getSession().createQuery(hqlBuffer.toString());
        query.setParameter("sampleAcc", sampleAcc);
        if (readAccessions != null && readAccessions.size() > 0) {
            query.setParameterList("readAccessions", readAccessions);
        }
        if (startIndex >= 0) {
            query.setFirstResult(startIndex);
        }
        if (numRows > 0) {
            query.setMaxResults(numRows);
        }
        _logger.debug("Reads for sample " + sampleAcc + " hql: " + hqlBuffer.toString());
        return query.list();
    }

    public Map<String, String> getDeflinesBySeqUid(Set<String> SeqUid) throws DataAccessException, DaoException {
        try {
            Map<String, String> deflineMap = null;
            return deflineMap;
        }
        catch (HibernateException e) {
            throw convertHibernateAccessException(e);
        }
        catch (DataAccessResourceFailureException e) {
            throw handleException(e, "FeatureDAOImpl - getDeflinesBySeqUid");
        }
        catch (IllegalStateException e) {
            throw handleException(e, "FeatureDAOImpl - getDeflinesBySeqUid");
        }
    }

    /**
     * This method returns a Read given a BaseSequenceEntity Id
     *
     * @param bseEntityId
     * @return
     * @throws DaoException
     */
    public Read getReadByBseEntityId(Long bseEntityId) throws DaoException {
        try {
            Read read = (Read) findByNamedQueryAndNamedParam("findReadByBseEntityId", "entityId", bseEntityId, true);
            return read;  // keep read local variable for debugger
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            throw handleException(e, this.getClass().getName() + " - getReadByBseEntityId");
        }
    }

    /**
     * This method returns a Read given a BaseSequenceEntity Id
     *
     * @param accession
     * @return
     * @throws DaoException
     */
    public Read getReadWithLibraryByAccesion(String accession) throws DaoException {
        try {
            Read read = (Read) findByNamedQueryAndNamedParam("findReadWithLibraryByAccesion", "accesion", accession, true);
            if (read == null) {
                _logger.error("read returned null");
            }
            return read;  // keep read local variable for debugger
        }
        catch (Exception e) {
            _logger.error(e.getMessage(), e);
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            throw handleException(e, this.getClass().getName() + " - getReadWithLibraryByAccesion");
        }
    }

    /**
     * Returns a Read instance with it's library instance loaded
     *
     * @param bseEntityId
     * @return
     * @throws DaoException
     */
    public Read getReadWithSequenceByBseEntityId(Long bseEntityId) throws DaoException {
        try {
            Read read = (Read) findByNamedQueryAndNamedParam("findReadWithSequenceByBseEntityId", "entityId", bseEntityId, true);
            return read;  // keep read local variable for debugger
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            throw handleException(e, this.getClass().getName() + " - getReadWithSequenceByBseEntityId");
        }
    }

    /**
     * Returns a Read instance with it's library instance loaded
     *
     * @param bseEntityId
     * @return
     * @throws DaoException
     */
    public Read getReadWithLibraryByBseEntityId(Long bseEntityId) throws DaoException {
        try {
            Read read = (Read) findByNamedQueryAndNamedParam("findReadWithLibraryByBseEntityId", "entityId", bseEntityId, true);
            return read;  // keep read local variable for debugger
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            throw handleException(e, this.getClass().getName() + " - getReadWithLibraryByBseEntityId");
        }
    }

    /**
     * This method returns a Read given an accesion
     *
     * @param accesion
     * @return
     * @throws DaoException
     */
    public Read getReadByAccesion(String accesion) throws DaoException {
        try {
            Read read = (Read) findByNamedQueryAndNamedParam("findReadByAccesion", "accesion", accesion, true);
            return read;  // keep read local variable for debugger
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            throw handleException(e, this.getClass().getName() + " - getReadByAccesion");
        }
    }

    /**
     * This method retrieves all mates of the read identified by the <code>accession</code>.
     *
     * @param accession
     * @return a list of reads that
     * @throws DaoException
     */
    public List<Read> getPairedReadsByAccession(String accession) throws DaoException {
        try {
            List<Read> matedReads =
                    (List<Read>) findByNamedQueryAndNamedParam("findPairedReadsByAccession", "accesion", accession, false);
            return matedReads;  // keep read local variable for debugger
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            throw handleException(e, this.getClass().getName() + " - getReadMatesByAccesion");
        }
    }

    /**
     * This method retrieves all mates of the read identified by the <code>entityId</code>.
     *
     * @param entityId
     * @return the list of paired reads
     * @throws DaoException
     */
    public List<Read> getPairedReadsByEntityId(Long entityId) throws DaoException {
        try {
            List<Read> matedReads =
                    (List<Read>) findByNamedQueryAndNamedParam("findPairedReadsByEntityId", "entityId", entityId, false);
            return matedReads;  // keep read local variable for debugger
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            throw handleException(e, this.getClass().getName() + " - getPairedReadsByEntityId");
        }
    }

    /**
     * This method returns a BioSequence given a BaseSequenceEntity Id
     *
     * @param bseEntityId
     * @return
     * @throws DaoException
     */
    public String getBioSequenceTextByBseEntityId(Long bseEntityId) throws DaoException {
        try {
            String sequence = (String) findByNamedQueryAndNamedParam("findSequenceTextByBseEntityId", "entityId", bseEntityId, true);
            return sequence;  // keep bioSequence local variable for debugger
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            throw handleException(e, this.getClass().getName() + " - getBioSequenceTextByBseEntityId");
        }
    }

    /**
     * This method returns a BioSequence given a BaseSequenceEntity Id
     *
     * @param bseEntityId
     * @return
     * @throws DaoException
     */
    public BioSequence getBioSequenceByBseEntityId(Long bseEntityId) throws DaoException {
        try {
            BioSequence bioSequence = (BioSequence) findByNamedQueryAndNamedParam("findBioSequenceByBseEntityId", "entityId", bseEntityId, true);
            return bioSequence;  // keep bioSequence local variable for debugger
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            _logger.error(e);
            throw handleException(e, this.getClass().getName() + " - getBioSequenceByBseEntityId");
        }
    }

    /**
     * This method returns a BioSequence given a BaseSequenceEntity Id
     *
     * @param accesion
     * @return
     * @throws DaoException
     */
    public BioSequence getBioSequenceByAccession(String accesion) throws DaoException {
        try {
            BioSequence bioSequence = (BioSequence) findByNamedQueryAndNamedParam("findBioSequenceByAccession", "accesion", accesion, true);
            return bioSequence;  // keep bioSequence local variable for debugger
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            throw handleException(e, this.getClass().getName() + " - getBioSequenceByAccession");
        }
    }

    /**
     * This method returns a subject entity id given an alignment Id
     *
     * @param blastHitIdList
     * @return
     * @throws DaoException
     */
    public List<BaseSequenceEntity> getSubjectBSEList_ByBlastHitIdList(List<Long> blastHitIdList) throws DaoException {
        try {
            List<BaseSequenceEntity> resultList = new ArrayList<BaseSequenceEntity>();
            for (Iterator<Long> it = blastHitIdList.iterator(); it.hasNext();) {
                Long blastHitId = it.next();
                BaseSequenceEntity bse = (BaseSequenceEntity) findByNamedQueryAndNamedParam("findSubjectEntityByBlastHitId", "alignment_id", blastHitId, true);
                resultList.add(bse);
            }
            return resultList;
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            _logger.error(e);
            throw handleException(e, this.getClass().getName() + " - getSubjectBSESet_ByBlastHitIdSet");
        }
    }

    public List<BaseSequenceEntity> getQueryBSEList_ByBlastHitIdList(List<Long> blastHitIdList) throws DaoException {
        try {
            List<BaseSequenceEntity> resultList = new ArrayList<BaseSequenceEntity>();
            for (Iterator<Long> it = blastHitIdList.iterator(); it.hasNext();) {
                Long blastHitId = it.next();
                BaseSequenceEntity bse = (BaseSequenceEntity) findByNamedQueryAndNamedParam("findQueryEntityByBlastHitId", "alignment_id", blastHitId, true);
                resultList.add(bse);
            }
            return resultList;
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            _logger.error(e);
            throw handleException(e, this.getClass().getName() + " - getQueryBSESet_ByBlastHitIdSet");
        }
    }

    public Set<BaseSequenceEntity> getSubjectBSESet_ByResultNode(Long resultNodeId) throws DaoException {
        try {
            Set<BaseSequenceEntity> resultSet = new HashSet<BaseSequenceEntity>();
            List resList = (List) findByNamedQueryAndNamedParam("findDistinctSubjectEntitiesByResultNode", "resultNodeId", resultNodeId, false);
            for (Iterator it = resList.iterator(); it.hasNext();) {
                BaseSequenceEntity bse = (BaseSequenceEntity) it.next();
                resultSet.add(bse);
            }
            return resultSet;
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            _logger.error(e);
            throw handleException(e, this.getClass().getName() + " - getSubjectEntityIdByResultNode");
        }
    }

    public Set<BaseSequenceEntity> getQueryBSESet_ByResultNode(Long resultNodeId) throws DaoException {
        try {
            Set<BaseSequenceEntity> resultSet = new HashSet<BaseSequenceEntity>();
            List resList = (List) findByNamedQueryAndNamedParam("findDistinctQueryEntitiesByResultNode", "resultNodeId", resultNodeId, false);
            Map<Long, Set<String>> accessionsLeft2Search = new HashMap<Long, Set<String>>();
            for (Iterator it = resList.iterator(); it.hasNext();) {
                Object[] resEntry = (Object[]) it.next();
                BlastHit alignment = (BlastHit) resEntry[0];
                BaseSequenceEntity bse = (BaseSequenceEntity) resEntry[1];
                if (bse == null && alignment != null) {
                    // the query sequence is not present in the database but we have an aligment
                    // therefore we should be able to retrieve the sequence directly from the query FASTA file
                    if (alignment.getQueryNodeId() != null) {
                        Set<String> fastaNodeAccessions = accessionsLeft2Search.get(alignment.getQueryNodeId());
                        if (fastaNodeAccessions == null) {
                            fastaNodeAccessions = new HashSet<String>();
                            accessionsLeft2Search.put(alignment.getQueryNodeId(), fastaNodeAccessions);
                        }
                        fastaNodeAccessions.add(alignment.getQueryAcc());
                    }
                }
                if (bse != null) {
                    resultSet.add(bse);
                }
            }
            if (accessionsLeft2Search.size() > 0) {
                FASTAFileNodeHelper fastaNodeHelper = new FASTAFileNodeHelper();
                for (Long fastaNodeId : accessionsLeft2Search.keySet()) {
                    FastaFileNode fastaNode = (FastaFileNode) genericGet(FastaFileNode.class, fastaNodeId);
                    if (fastaNode != null) {
                        Set<String> fastaNodeAccessions = accessionsLeft2Search.get(fastaNodeId);
                        Set<BaseSequenceEntity> sequenceEntities =
                                fastaNodeHelper.readSequences(fastaNode, fastaNodeAccessions);
                        resultSet.addAll(sequenceEntities);
                    }
                }
            }
            return resultSet;
        }
        catch (Exception e) {
            _logger.error(e);
            throw handleException(e, this.getClass().getName() + " - getQueryEntityIdByResultNode");
        }
    }

    public Long getNumSubjectBSESet_ByTaskId(Long taskId, Set<Long> blastHitIdSet) throws DaoException {
        try {
            String hql = "select cast(count(distinct al.subjectAcc),long) " +
                    "from BlastHit al " +
                    "where al.resultNode.task.objectId = :taskId ";
            if (blastHitIdSet != null && blastHitIdSet.size() > 0) {
                hql += "and al.blastHitId in (:blastHitIdSet)";
            }
            _logger.debug("hql=" + hql);
            Query query = getSession().createQuery(hql);
            query.setParameter("taskId", taskId);
            if (blastHitIdSet != null && blastHitIdSet.size() > 0) {
                query.setParameterList("blastHitIdSet", blastHitIdSet);
            }
            return (Long) query.uniqueResult();
        }
        catch (Exception e) {
            _logger.error(e);
            throw handleException(e, this.getClass().getName() + " - getNumSubjectBSESet_ByTaskId");
        }
    }

    public Long getNumQueryBSESet_ByTaskId(Long taskId, Set<Long> blastHitIdSet) throws DaoException {
        try {
            Set<BaseSequenceEntity> resultSet = new HashSet<BaseSequenceEntity>();
            String hql = "select cast(count(distinct al.queryAcc),long) " +
                    "from BlastHit al " +
                    "where al.resultNode.task.objectId = :taskId ";
            if (blastHitIdSet != null && blastHitIdSet.size() > 0) {
                hql += "and al.blastHitId in (:blastHitIdSet)";
            }
            _logger.debug("hql=" + hql);
            Query query = getSession().createQuery(hql);
            query.setParameter("taskId", taskId);
            if (blastHitIdSet != null && blastHitIdSet.size() > 0) {
                query.setParameterList("blastHitIdSet", blastHitIdSet);
            }
            return (Long) query.uniqueResult();
        }
        catch (Exception e) {
            _logger.error(e);
            throw handleException(e, this.getClass().getName() + " - getNumQueryBSESet_ByTaskId");
        }
    }

    public Set<BaseSequenceEntity> getSubjectBSESet_ByTaskId(Long taskId, Set<Long> blastHitIdSet) throws DaoException {
        try {
            Set<BaseSequenceEntity> resultSet = new HashSet<BaseSequenceEntity>();
            String hql = "select distinct subject " +
                    "from BlastHit al left outer join al.subjectEntity subject " +
                    "where al.resultNode.task.objectId = :taskId ";
            if (blastHitIdSet != null && blastHitIdSet.size() > 0) {
                hql += "and al.blastHitId in (:blastHitIdSet)";
            }
            _logger.debug("hql=" + hql);
            Query query = getSession().createQuery(hql);
            query.setParameter("taskId", taskId);
            if (blastHitIdSet != null && blastHitIdSet.size() > 0) {
                query.setParameterList("blastHitIdSet", blastHitIdSet);
            }
            List<Object> resList = query.list();
            for (Object resEntry : resList) {
                BaseSequenceEntity bse = (BaseSequenceEntity) resEntry;
                if (bse != null) {
                    resultSet.add(bse);
                }
            }
            return resultSet;
        }
        catch (Exception e) {
            _logger.error(e);
            throw handleException(e, this.getClass().getName() + " - getSubjectEntityIdByTaskId");
        }
    }

    public Set<BaseSequenceEntity> getQueryBSESet_ByTaskId(Long taskId, Set<Long> blastHitIdSet) throws DaoException {
        try {
            Set<BaseSequenceEntity> resultSet = new HashSet<BaseSequenceEntity>();
            String hql = "select distinct al.queryNodeId, al.queryAcc, qry " +
                    "from BlastHit al left outer join al.queryEntity qry " +
                    "where al.resultNode.task.objectId = :taskId ";
            if (blastHitIdSet != null && blastHitIdSet.size() > 0) {
                hql += "and al.blastHitId in (:blastHitIdSet)";
            }
            _logger.debug("hql=" + hql);
            Query query = getSession().createQuery(hql);
            query.setParameter("taskId", taskId);
            if (blastHitIdSet != null && blastHitIdSet.size() > 0) {
                query.setParameterList("blastHitIdSet", blastHitIdSet);
            }
            List<Object[]> resList = query.list();
            Map<Long, Set<String>> accessionsLeft2Search = new HashMap<Long, Set<String>>();
            for (Iterator<Object[]> it = resList.iterator(); it.hasNext();) {
                Object[] resEntry = it.next();
                Long queryNodeId = (Long) resEntry[0];
                String queryAcc = (String) resEntry[1];
                BaseSequenceEntity bse = (BaseSequenceEntity) resEntry[2];
                if (bse == null && queryNodeId != null && queryNodeId != 0) {
                    // the query sequence is not present in the database but we have an aligment
                    // therefore we should be able to retrieve the sequence directly from the query FASTA file
                    Set<String> fastaNodeAccessions = accessionsLeft2Search.get(queryNodeId);
                    if (fastaNodeAccessions == null) {
                        fastaNodeAccessions = new HashSet<String>();
                        accessionsLeft2Search.put(queryNodeId, fastaNodeAccessions);
                    }
                    fastaNodeAccessions.add(queryAcc);
                }
                if (bse != null) {
                    resultSet.add(bse);
                }
            }
            if (accessionsLeft2Search.size() > 0) {
                FASTAFileNodeHelper fastaNodeHelper = new FASTAFileNodeHelper();
                for (Long fastaNodeId : accessionsLeft2Search.keySet()) {
                    FastaFileNode fastaNode = (FastaFileNode) genericGet(FastaFileNode.class, fastaNodeId);
                    if (fastaNode != null) {
                        Set<String> fastaNodeAccessions = accessionsLeft2Search.get(fastaNodeId);
                        Set<BaseSequenceEntity> sequenceEntities =
                                fastaNodeHelper.readSequences(fastaNode, fastaNodeAccessions);
                        resultSet.addAll(sequenceEntities);
                    }
                }
            }
            return resultSet;
        }
        catch (Exception e) {
            _logger.error(e);
            throw handleException(e, this.getClass().getName() + " - getQueryEntityIdByTaskId");
        }
    }

    public Long getResultNodeIdbyBlastHitId(Long blastHitId) throws DaoException {
        try {
            return (Long) findByNamedQueryAndNamedParam("findResultNodeIdByBlastHitId", "alignment_id", blastHitId, true);
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            _logger.error(e);
            throw handleException(e, this.getClass().getName() + " - getResultNodeIdbyBlastHitId");
        }
    }


    // Map from BSE ID to BSE
    public Map<Long, BaseSequenceEntity> getSubjectBses(Long resultNodeId) throws DaoException {
        Map<Long, BaseSequenceEntity> resultMap = new HashMap<Long, BaseSequenceEntity>();
        long start, stop;
        try {
            start = System.currentTimeMillis();

            List resList = (List) findByNamedQueryAndNamedParam("findSubjectBseByResultNode", "resultNodeId", resultNodeId, false);
            if (null == resList) {
                return resultMap;
            }
            for (Object result : resList) {
                Object obj[] = (Object[]) result;
                resultMap.put((Long) obj[0], (BaseSequenceEntity) obj[1]);
            }

            stop = System.currentTimeMillis();
            if (_logger.isInfoEnabled()) _logger.info("Time to do getSubjectBses (By ResultNode): " + (stop - start));
            return resultMap;
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            _logger.error(e);
            throw handleException(e, this.getClass().getName() + " - getSubjectBsesByResultNode");
        }
    }

    public Map<Long, BaseSequenceEntity> getSubjectBses(Set<Long> blastHitIdSet) throws DaoException {
        Map<Long, BaseSequenceEntity> resultMap = new HashMap<Long, BaseSequenceEntity>();
        long start, stop;
        try {
            start = System.currentTimeMillis();

            for (Long blastHitId : blastHitIdSet) {
                List resList = (List) findByNamedQueryAndNamedParam("findSubjectBseByBlastHitId", "blastHitId", blastHitId, false);
                if (null == resList) {
                    return resultMap;
                }

                for (Object result : resList) {
                    Object obj[] = (Object[]) result;
                    resultMap.put((Long) obj[0], (BaseSequenceEntity) obj[1]);
                }
            }

            stop = System.currentTimeMillis();
            if (_logger.isInfoEnabled())
                _logger.info("Time to do getSubjectBses (By AlignmentIDSet): " + (stop - start));
            return resultMap;
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            _logger.error(e);
            throw handleException(e, this.getClass().getName() + " - getSubjectBsesByResultNode");
        }
    }

    public List<Object[]> getReadsByAccessions(String[] accSet) throws DaoException {
        try {
            StringBuffer hql = new StringBuffer("select bse.defline, bse.bioSequence.sequence " +
                    "from BaseSequenceEntity bse " +
                    "where bse.accession in (");
            for (int i = 0; i < accSet.length; i++) {
                hql.append("?,");
            }
            hql.deleteCharAt(hql.length() - 1);
            hql.append(")");
            if (_logger.isDebugEnabled()) _logger.debug("accSet length=" + accSet.length + "\nhql=" + hql);
            return getHibernateTemplate().find(hql.toString(), accSet);
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            _logger.error(e);
            throw handleException(e, this.getClass().getName() + " - getReadsByAccessions");
        }
    }

    public List<BaseSequenceEntity> getSequenceEntitiesByAccessions(Collection<String> accCollection)
            throws DaoException {
        try {
            if (accCollection == null || accCollection.size() == 0) {
                // if no set of accessions given return an empty set
                // instead of returning everything
                return new ArrayList<BaseSequenceEntity>();
            }
            String hql = "select bse from BaseSequenceEntity bse where bse.accession in (:accessions)";
            _logger.debug("hql=" + hql);
            // Get the appropriate range of Node's hits, sorted by the specified field
            Query query = getSession().createQuery(hql);
            query.setParameterList("accessions", accCollection);
            return query.list();
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            _logger.error(e);
            throw handleException(e, this.getClass().getName() + " - getSequenceEntitiesByAccessions");
        }
    }

    // Map from Read Entity ID to Read
    public Map<Long, Read> getReadEntityIdToReadMap(Long resultNodeId) throws DaoException {
        Map<Long, Read> resultMap = new HashMap<Long, Read>();
        long start, stop;
        try {
            start = System.currentTimeMillis();

            List resList = (List) findByNamedQueryAndNamedParam("findReadByResultNode", "resultNodeId", resultNodeId, false);

            for (Iterator it = resList.iterator(); it.hasNext();) {
                Object obj[] = (Object[]) it.next();
                resultMap.put((Long) obj[0], (Read) obj[1]);
            }

            stop = System.currentTimeMillis();
            if (_logger.isInfoEnabled()) _logger.info("Time to do getReads (By ResultNode): " + (stop - start));
            return resultMap;
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            _logger.error(e);
            throw handleException(e, this.getClass().getName() + " - getReads ByResultNode");
        }
    }

    // Map from Read Entity ID to Read
    public Map<Long, Read> getReadEntityIdToReadMap(Set<Long> blastHitIdSet) throws DaoException {
        Map<Long, Read> resultMap = new HashMap<Long, Read>();
        long start, stop;
        try {
            start = System.currentTimeMillis();

            for (Iterator<Long> alignIdItr = blastHitIdSet.iterator(); alignIdItr.hasNext();) {

                Long blastHitId = alignIdItr.next();

                List resList = (List) findByNamedQueryAndNamedParam("findReadByBlastHitId", "blastHitId", blastHitId, false);

                for (Iterator it = resList.iterator(); it.hasNext();) {
                    Object obj[] = (Object[]) it.next();
                    resultMap.put((Long) obj[0], (Read) obj[1]);
                }
            }

            stop = System.currentTimeMillis();
            if (_logger.isInfoEnabled()) _logger.info("Time to do getReads (By AlignmentIDSet): " + (stop - start));
            return resultMap;
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            _logger.error(e);
            throw handleException(e, this.getClass().getName() + " - getReads ByBlastHitIdSet");
        }
    }

    /* This method is intended to be used for CSV generation of read metadata for the case where
     * the subject dataset is not reads, but rather bses containing reads in deflines. Since
     * it is going to the DB separately for each readId, this should be reimplemented
     * as a single call when the DB is capable of supporting this.
     */
    public Map<String, Read> getReadsByIdSet(Collection<String> readIdSet) throws DaoException {
        long start, stop;
        try {
            start = System.currentTimeMillis();
            if (_logger.isDebugEnabled()) _logger.debug("getReadsByIdSet looking for " + readIdSet.size());
            Map<String, Read> resultMap = new HashMap<String, Read>();
            for (String readAcc : readIdSet) {
                List list = (List) findByNamedQueryAndNamedParam("findReadByAccesion", "accesion", readAcc, false);
                Read read = (Read) list.get(0);
                resultMap.put(readAcc, read);
            }
            stop = System.currentTimeMillis();
            if (_logger.isDebugEnabled())
                _logger.debug("Time to do getReadsByIdSet (By read accession set): " + (stop - start));
            return resultMap;
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            _logger.error(e);
            throw handleException(e, this.getClass().getName() + " - getReadsByIdSet");
        }
    }

    public Set<BlastHit> getBlastHitsByBlastHitIds(Set<Long> blastHitIdSet) throws DaoException {
        try {
            Set<BlastHit> resultSet = new TreeSet<BlastHit>();

            for (Iterator<Long> alignIdItr = blastHitIdSet.iterator(); alignIdItr.hasNext();) {

                List resList = (List) findByNamedQueryAndNamedParam("findBlastHitsByBlastHitId", "blastHitId",
                        alignIdItr.next(), false);

                Iterator it = resList.iterator();
                if (it.hasNext()) {
                    BlastHit bh = (BlastHit) it.next();
                    resultSet.add(bh);
                }
            }

            return resultSet;
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            _logger.debug(e);
            throw handleException(e, this.getClass().getName() + " - getBlastHits ByBlastHitIdSet");
        }
    }

    /**
     * retrieve the number of actual blast hit results for the given taskId and/or set of IDs
     * the number may differ when the results are associated with "pooled" samples, i.e.,
     * samples associated with more than one site
     *
     * @param taskId
     * @param blastHitIdSet
     * @param includeAllSampleMaterials
     * @return
     * @throws DaoException
     */
    public Long getNumBlastResultsByTaskOrIDs(Long taskId, Set<Long> blastHitIdSet, boolean includeAllSampleMaterials)
            throws DaoException {
        try {
            _logger.debug("FeatureDAOImpl.getNumBlastResultsByTaskOrIDs");
            // Retrieve the hits from the node, plus the query and subject deflines
            String hitSelectCondition;
            if (blastHitIdSet != null && blastHitIdSet.size() > 0) {
                hitSelectCondition = "hit.blastHitId in (:blastHitIdSet)";
            }
            else if (taskId != null) {
                hitSelectCondition = "resultNode.task.objectId = :taskId ";
            }
            else {
                throw new IllegalArgumentException("No task ID or set of hit IDs provided");
            }
            String hql = "select cast(count(*),long) " +
                    "from BlastHit hit " +
                    "inner join hit.resultNode resultNode " +
                    "inner join resultNode.deflineMap queryDef " +
                    "inner join resultNode.deflineMap subjectDef " +
                    "left join hit.queryEntity queryEntity " +
                    "inner join hit.subjectEntity subjectEntity " +
                    "left join subjectEntity.sample sample " +
                    (includeAllSampleMaterials
                            ? "left join sample.bioMaterials bioMaterial "
                            : "") +
                    "where " + hitSelectCondition + " " +
                    "  and index(subjectDef) = hit.subjectAcc " +
                    "  and index(queryDef) = hit.queryAcc ";
            _logger.debug("hql=" + hql);
            // Get the appropriate range of Node's hits, sorted by the specified field
            Query query = getSession().createQuery(hql);
            if (blastHitIdSet != null && blastHitIdSet.size() > 0) {
                query.setParameterList("blastHitIdSet", blastHitIdSet);
            }
            else if (taskId != null) {
                query.setLong("taskId", taskId);
            }
            return (Long) query.uniqueResult();
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            _logger.error(e);
            throw handleException(e, "FeatureDAOImpl.getNumBlastResultsByTaskOrIDs");
        }
    }

    /**
     * This method retrieves a list of blast results for a task or if the IDs are already known
     * it can retrieve only the specified results
     *
     * @param taskId            the job ID
     * @param blastHitIdSet     the set of IDs of the required results
     * @param startIndex        results offset
     * @param numRows           results length
     * @param includeHSPRanking if true the results include the number of high scoring pairs (HSP)
     *                          for a certain query, subject sequence and the the HSP rank within the query - subject match
     * @param sortArgs          specify sort options
     * @return List<Object[]> the blast results where a blast result actually consists of an array of object in which
     *         blast result[0] is a BlastHit object
     *         blast result[1] is the query defline
     *         blast result[2] is the subject defline
     *         blast result[3] the HSP rank for a (query, subject) pair if includeHSPRanking = true
     *         blast result[3] the number of HSPs for a (query, subject) pair if includeHSPRanking = true
     * @throws DaoException
     */
    public List<Object[]> getBlastResultsByTaskOrIDs(Long taskId,
                                                     Set<Long> blastHitIdSet,
                                                     int startIndex,
                                                     int numRows,
                                                     boolean includeHSPRanking,
                                                     SortArgument[] sortArgs)
            throws DaoException {
        try {
            _logger.debug("FeatureDAOImpl.getBlastResultsByTaskOrIDs");
            StringBuffer orderByFieldsBuffer = new StringBuffer();
            String rankRestriction = null;
            if (sortArgs != null) {
                for (SortArgument sortArg : sortArgs) {
                    String dataSortField = sortArg.getSortArgumentName();
                    if (dataSortField == null || dataSortField.length() == 0) {
                        continue;
                    }
                    if (dataSortField.equals("rank")) {
                        dataSortField = "hit.rank";
                        if ((blastHitIdSet == null || blastHitIdSet.size() == 0) &&
                                startIndex >= 0 && numRows > 0) {
                            // only create a rank restriction if the startIndex and numRows are valid
                            // and there's no subset restriction either
                            rankRestriction = "hit.rank between " + startIndex + " and " + (startIndex + numRows - 1) + " ";
                        }
                    }
                    else if (dataSortField.equals("bitScore")) {
                        dataSortField = "hit.bitScore";
                    }
                    else if (dataSortField.equals("lengthAlignment")) {
                        dataSortField = "hit.lengthAlignment";
                    }
                    else if (dataSortField.equals("subjectAcc")) {
                        dataSortField = "hit.subjectAcc";
                    }
                    else if (dataSortField.equals("queryDef")) {
                        dataSortField = "queryDef";
                    }
                    else if (dataSortField.equals("subjectDef")) {
                        dataSortField = "subjectDef";
                    }
                    else if (dataSortField.equals("sampleName")) {
                        dataSortField = "sample.sampleName";
                    }
                    if (dataSortField != null && dataSortField.length() != 0) {
                        if (sortArg.isAsc()) {
                            if (orderByFieldsBuffer.length() > 0) {
                                orderByFieldsBuffer.append(',');
                            }
                            orderByFieldsBuffer.append(dataSortField + " asc");
                        }
                        else if (sortArg.isDesc()) {
                            if (orderByFieldsBuffer.length() > 0) {
                                orderByFieldsBuffer.append(',');
                            }
                            orderByFieldsBuffer.append(dataSortField + " desc");
                        }
                    }

                } // end for all sortArgs
            }
            String orderByClause;
            if (orderByFieldsBuffer.length() == 0) {
                orderByClause = "order by hit.bitScore desc ";
            }
            else {
                orderByClause = "order by " + orderByFieldsBuffer.toString();
            }
            String hspFields = null;
            if (includeHSPRanking) {
                hspFields = "(select count(h2.rank)+1 " +
                        "    from BlastHit h2 " +
                        "    where h2.resultNode.objectId = resultNode.objectId " +
                        "      and h2.queryAcc = hit.queryAcc " +
                        "      and h2.subjectAcc = hit.subjectAcc " +
                        "      and h2.rank < hit.rank) as hspRank, " +
                        "   (select count(*) " +
                        "    from BlastHit h2 " +
                        "    where h2.resultNode.objectId = resultNode.objectId " +
                        "      and h2.queryAcc = hit.queryAcc " +
                        "      and h2.subjectAcc = hit.subjectAcc) as nhsps ";
            }
            // Retrieve the hits from the node, plus the query and subject deflines
            String hitSelectCondition;
            if (blastHitIdSet != null && blastHitIdSet.size() > 0) {
                hitSelectCondition = "hit.blastHitId in (:blastHitIdSet)";
            }
            else if (taskId != null) {
                hitSelectCondition = "resultNode.task.objectId = :taskId ";
            }
            else {
                throw new IllegalArgumentException("No task ID or set of hit IDs provided");
            }
            String hql = "select hit, " +
                    "       queryDef, " +
                    "       subjectDef " +
                    (hspFields != null ? "," + hspFields : "") +
                    "from BlastHit hit " +
                    "inner join hit.resultNode resultNode " +
                    "inner join resultNode.deflineMap queryDef " +
                    "inner join resultNode.deflineMap subjectDef " +
                    "left join fetch hit.queryEntity queryEntity " +
                    "inner join fetch hit.subjectEntity subjectEntity " +
                    "left join fetch subjectEntity.sample sample " +
                    "where " + hitSelectCondition + " " +
                    "  and index(subjectDef) = hit.subjectAcc " +
                    "  and index(queryDef) = hit.queryAcc " +
                    ((rankRestriction != null) ? "and " + rankRestriction : "") +
                    orderByClause;
            _logger.debug("hql=" + hql);
            // Get the appropriate range of Node's hits, sorted by the specified field
            Query query = getSession().createQuery(hql);
            if (blastHitIdSet != null && blastHitIdSet.size() > 0) {
                query.setParameterList("blastHitIdSet", blastHitIdSet);
            }
            else if (taskId != null) {
                query.setLong("taskId", taskId);
            }
            if (rankRestriction == null) { // need to restrict result set if not done by rank clause
                if (startIndex > 0) {
                    query.setFirstResult(startIndex);
                }
                if (numRows > 0) {
                    query.setMaxResults(numRows);
                }
            }
            List<Object[]> results = query.list();
            _logger.debug("FeatureDAOImpl.getBlastResultsByTaskOrIDs found " +
                    ((results == null) ? 0 : results.size()) + " hits");
            return results;
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            _logger.error(e);
            throw handleException(e, "FeatureDAOImpl.getBlastResultsByTaskOrIDs");
        }
    }

    public Set<BlastHit> getBlastHitsByAlignmentIds(Set<Long> alignmentIdSet) throws DaoException {

        try {
            Set<BlastHit> resultSet = new TreeSet<BlastHit>();

            for (Iterator<Long> alignIdItr = alignmentIdSet.iterator(); alignIdItr.hasNext();) {

                List resList = (List) findByNamedQueryAndNamedParam("findBlastHitsByAlignmentId", "alignmentId",
                        alignIdItr.next(), false);

                Iterator it = resList.iterator();
                if (it.hasNext()) {
                    BlastHit bh = (BlastHit) it.next();
                    resultSet.add(bh);
                }
            }

            return resultSet;
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            _logger.error(e);
            throw handleException(e, this.getClass().getName() + " - getBlastHits ByAlignmentIdSet");
        }

    }

    // Support for Read Detail
    public int getNumScaffoldsForReadByAccNo(String readAccNo) throws DaoException {
        String hql = "select count(s) from ScaffoldReadAlignment s " +
                "where s.readAcc = :readAccNo";
        Query hqlQuery = getSession().createQuery(hql);
        hqlQuery.setParameter("readAccNo", readAccNo);
        List results = hqlQuery.list();
        Long lresult = (Long) results.get(0);
        int result = lresult.intValue();
        _logger.debug("getNumScaffoldsForReadByAccNo() return value=" + result);
        return result;
    }

//    public List<ScaffoldReadAlignment> getScaffoldsForReadByAccNo(String readAccNo, int startIndex, int numRecords, SortArgument[] sortArgs)
//            throws DaoException {
//        String hql = "select s from ScaffoldReadAlignment s " +
//                "where s.readAcc = :readAccNo";
//        StringBuffer sortBuffer = new StringBuffer();
//        if (sortArgs != null && sortArgs.length > 0) {
//            for (int i = 0; i < sortArgs.length; i++) {
//                if (sortArgs[i].getSortDirection() != SortArgument.SORT_NOTSET) {
//                    String sortArgName = sortArgs[i].getSortArgumentName();
//                    if (sortArgName == null || sortArgName.length() == 0) {
//                        continue;
//                    }
//                    String dir;
//                    if (sortArgs[i].getSortDirection() == SortArgument.SORT_ASC) {
//                        dir = "asc";
//                    }
//                    else if (sortArgs[i].getSortDirection() == SortArgument.SORT_DESC) {
//                        dir = "desc";
//                    }
//                    else {
//                        continue;
//                    }
//                    if (sortBuffer.length() == 0) {
//                        sortBuffer.append("order by ");
//                    }
//                    else {
//                        sortBuffer.append(",");
//                    }
//                    sortBuffer.append(sortArgName);
//                    sortBuffer.append(' ');
//                    sortBuffer.append(dir);
//                }
//            }
//        }
//        if (sortBuffer.length() > 0) {
//            hql = hql + " " + sortBuffer.toString();
//        }
//        _logger.debug("getScaffoldsForReadByAccNo HQL: " + hql);
//        Query query = getSession().createQuery(hql);
//        query.setString("readAccNo", readAccNo);
//        if (numRecords > 0) {
//            query.setFirstResult(startIndex);
//            query.setMaxResults(numRecords);
//        }
//        return (List<ScaffoldReadAlignment>) query.list();
//    }
//
    public int getNumRelatedNCRNAs(String entityAccNo) throws DaoException {
        int nonCodingRnaCount = getNumReadFeaturesBySubclassName(new String[]{"NonCodingRNA"}, entityAccNo);
        return nonCodingRnaCount;
    }

    public List<BaseSequenceEntity> getRelatedNCRNAs(String entityAccNo, int startIndex, int numRecords, SortArgument[] sortArgs) throws DaoException {
        return getReadFeaturesBySubclassName(new String[]{"NonCodingRNA"}, entityAccNo, startIndex, numRecords, sortArgs);
    }

    public int getNumRelatedORFs(String entityAccNo) throws DaoException {
        int orfCount = getNumReadFeaturesBySubclassName(new String[]{"ORF"}, entityAccNo);
        return orfCount;
    }

    public List<BaseSequenceEntity> getRelatedORFs(String entityAccNo, int startIndex, int numRecords, SortArgument[] sortArgs)
            throws DaoException {
        return getReadFeaturesBySubclassName(new String[]{"ORF"}, entityAccNo, startIndex, numRecords, sortArgs);
    }

    public int getNumReadFeaturesBySubclassName(String[] name, String entityAccNo) throws DaoException {
        String hql = "select count(se) " +
                "from BaseSequenceEntity dna, BaseSequenceEntity se " +
                "where dna.accession=:entityAccNo and se.dnaEntity=dna " +
                "and se.class in " +
                "(";
        for (int i = 0; i < name.length; i++) {
            if (i > 0) {
                hql += ",";
            }
            hql += name[i];
        }
        hql += ")";
        Query hqlQuery = getSession().createQuery(hql);
        hqlQuery.setParameter("entityAccNo", entityAccNo);
        List results = hqlQuery.list();
        Long lresult = (Long) results.get(0);
        int result = lresult.intValue();
        _logger.debug("getNumReadFeaturesBySubclassName() return value=" + result);
        return result;
    }

    public List<BaseSequenceEntity> getReadFeaturesBySubclassName(String[] name, String entityAccNo, int startIndex, int numRecords,
                                                                  SortArgument[] sortArgs) throws DaoException {
        String hql = "select se " +
                "from BaseSequenceEntity dna, BaseSequenceEntity feature " +
                "where dna.accession = :entityAccNo " +
                "and se.dnaEntity=dna and se.class in " +
                "(";

        for (int i = 0; i < name.length; i++) {
            if (i > 0) {
                hql += ",";
            }
            hql += name[i];
        }
        hql += ")";
        boolean sortFlag = false;
        if (sortArgs != null && sortArgs.length > 0) {
            for (int i = 0; i < sortArgs.length; i++) {
                if (sortArgs[i].getSortDirection() != SortArgument.SORT_NOTSET) {
                    String sortString = sortArgs[i].getSortArgumentName();
                    if (sortString == null || sortString.length() == 0) {
                        continue;
                    }
                    if (sortString.equals("entityType")) {
                        sortString = "se.entityType.name" +
                                "";
                    }
                    String dir = (sortArgs[i].getSortDirection() == SortArgument.SORT_ASC ? "asc" : "desc");
                    if (sortFlag) {
                        hql += (", " + sortString + " " + dir);
                    }
                    else {
                        hql += (" order by " + sortString + " " + dir);
                        sortFlag = true;
                    }
                }
            }
        }
        _logger.debug("getReadFeaturesBySubclassName HQL: " + hql);
        Query query = getSession().createQuery(hql);
        query.setString("entityAccNo", entityAccNo);
        if (numRecords > 0) {
            query.setFirstResult(startIndex);
            query.setMaxResults(numRecords);
        }
        return (List<BaseSequenceEntity>) query.list();
    }

//    public List<BseEntityDetail> getRelatedORFsAndRNAs(String readAcc, int startIndex, int numRecords, SortArgument[] sortArgs)
//            throws DaoException {
//        String orderByClause = buildOrderByClause(sortArgs);
//        String sql = "select " +
//                "se.accession as accession, " +
//                "se.dna_begin as dnaBegin, " +
//                "se.dna_end as dnaEnd, " +
//                "se.dna_orientation as dnaOrientation, " +
//                "se.translation_table as translationTable, " +
//                "se.stop_5_prime as stop5Prime, " +
//                "se.stop_3_prime as stop3Prime, " +
//                "null as geneSymbol, " +
//                "null as geneOntology, " +
//                "null  as enzymeCommission, " +
//                "se.sequence_length as length, " +
//                "null  as protein_function, " +
//                "se.type as type, " +
//                "se.entity_type_code as entityTypeCode " +
//                "from sequence_entity se " +
//                "where se.entity_type_code+0=8 and se.dna_id=(select entity_id from sequence_entity where accession=:readAcc) " +
//                "union all " +
//                " select " +
//                "se.accession as accession, " +
//                "se.dna_begin as dnaBegin, " +
//                "se.dna_end as dnaEnd, " +
//                "se.dna_orientation as dnaOrientation, " +
//                "se.translation_table as translationTable, " +
//                "se.stop_5_prime as stop5Prime, " +
//                "se.stop_3_prime as stop3Prime, " +
//                "pd.gene_symbol as geneSymbol, " +
//                "pd.gene_ontology as geneOntology, " +
//                "pd.enzyme_commission as enzymeCommission, " +
//                "pd.length as length, " +
//                "pd.protein_function as protein_function, " +
//                "se.type as type, " +
//                "se.entity_type_code as entityTypeCode " +
//                "from sequence_entity se inner join protein_detail pd on pd.protein_id=se.protein_id " +
//                "where se.entity_type_code+0=5 and se.dna_id=(select entity_id from sequence_entity where accession=:readAcc) " + orderByClause;
//
//        _logger.info("getRelatedOrfsAndRnas sql: " + sql);
//        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
//        sqlQuery.setString("readAcc", readAcc);
//        if (startIndex > 0) {
//            sqlQuery.setFirstResult(startIndex);
//        }
//        if (numRecords > 0) {
//            sqlQuery.setMaxResults(numRecords);
//        }
//        List<Object[]> results = sqlQuery.list();
//        List<BseEntityDetail> bseEntityDetailList = new ArrayList<BseEntityDetail>();
//        for (Object[] result : results) {
//            BseEntityDetail bseEntityDetail = new BseEntityDetail();
//            bseEntityDetail.setAcc((String) result[0]);
//            bseEntityDetail.setDnaBegin((Integer) result[1]);
//            bseEntityDetail.setDnaEnd((Integer) result[2]);
//            bseEntityDetail.setDnaOrientation((Integer) result[3]);
//            bseEntityDetail.setTranslationTable((String) result[4]);
//            bseEntityDetail.setStop5Prime((String) result[5]);
//            bseEntityDetail.setStop3Prime((String) result[6]);
//            bseEntityDetail.setGeneSymbol((String) result[7]);
//            bseEntityDetail.setGoAnnotationDescription(AnnotationUtil.createAnnotationListFromString((String) result[8], ANNOT_ROW_SEP, ANNOT_LIST_SEP));
//            bseEntityDetail.setEcAnnotationDescription(AnnotationUtil.createAnnotationListFromString((String) result[9], ANNOT_ROW_SEP, ANNOT_LIST_SEP));
//            bseEntityDetail.setLength((Integer) result[10]);
//            bseEntityDetail.setProteinFunction((String) result[11]);
//            bseEntityDetail.setType((String) result[12]);
//            bseEntityDetail.setEntityTypeCode((Integer) result[13]);
//            bseEntityDetailList.add(bseEntityDetail);
//        }
//        return bseEntityDetailList;
//    }
//
//    // Support for Protein Detail
//    public ProteinClusterMember getProteinClusterMemberInfo(String proteinAcc) throws DaoException {
//        String sql = "select  " +
//                "se.accession as proteinAcc, " +
//                "cc.core_cluster_acc as coreClusterAcc, " +
//                "cc.final_cluster_acc as finalClusterAcc, " +
//                "cp.nr_parent_acc, " +
//                "cc.cluster_quality as clusterQuality, " +
//                "se.defline " +
//                "from sequence_entity se " +
//                "inner join core_cluster_protein cp on cp.protein_id=se.entity_id " +
//                "inner join core_cluster cc on cp.core_cluster_id = cc.core_cluster_id " +
//                "where se.accession = :proteinAcc";
//        _logger.info("Retrieve protein cluster data sql: " + sql);
//        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
//        sqlQuery.setString("proteinAcc", proteinAcc);
//        List<Object[]> results = sqlQuery.list();
//        List<ProteinClusterMember> proteinClusterInfoList = new ArrayList<ProteinClusterMember>();
//        for (Object[] res : results) {
//            ProteinClusterMember proteinClusterInfo = new ProteinClusterMember();
//            proteinClusterInfo.setProteinAcc((String) res[0]);
//            proteinClusterInfo.setCoreClusterAcc((String) res[1]);
//            proteinClusterInfo.setFinalClusterAcc((String) res[2]);
//            proteinClusterInfo.setNonRedundantParentAcc((String) res[3]);
//            proteinClusterInfo.setClusterQuality((String) res[4]);
//            proteinClusterInfo.setDefline((String) res[5]);
//            proteinClusterInfoList.add(proteinClusterInfo);
//        }
//        return proteinClusterInfoList.size() > 0 ? proteinClusterInfoList.get(0) : null;
//    }
//
//    public List<ProteinAnnotation> getProteinAnnotations(String proteinAcc, SortArgument[] sortArgs)
//            throws DaoException {
//        String sql = "select  " +
//                "se.accession as protein_acc, " +
//                "pa.id, " +
//                "pa.category, " +
//                "pa.name, " +
//                "pa.evidence " +
//                "from sequence_entity se inner join protein_annotation pa on pa.protein_id=se.entity_id " +
//                "where se.accession=:proteinAcc";
//        _logger.info("Retrieve protein GO annotation sql: " + sql);
//        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
//        sqlQuery.setString("proteinAcc", proteinAcc);
//        List<Object[]> results = sqlQuery.list();
//        List<ProteinAnnotation> proteinAnnotationList = new ArrayList<ProteinAnnotation>();
//        for (Object[] res : results) {
//            ProteinAnnotation proteinAnnotation = new ProteinAnnotation();
//            proteinAnnotation.setAccession((String) res[0]);
//            proteinAnnotation.setAnnotationID((String) res[1]);
//            proteinAnnotation.setAnnotationType((String) res[2]);
//            proteinAnnotation.setDescription((String) res[3]);
//            proteinAnnotation.setAssignedBy((String) res[4]);
//            proteinAnnotationList.add(proteinAnnotation);
//        }
//        return proteinAnnotationList;
//    }
//
//    public int getNumOfPeptidesForScaffoldByAccNo(String scaffoldAccNo) throws DaoException {
//        return getNumReadFeaturesBySubclassName(new String[]{"Protein"}, scaffoldAccNo);
//    }
//
//    public List<PeptideDetail> getPeptidesForScaffoldByAccNo(String scaffoldAccNo, int startIndex, int numRecords, SortArgument[] sortArgs) throws DaoException {
//        String orderByClause = buildOrderByClause(sortArgs);
//        String sql = "select " +
//                "se.accession as accession, " +
//                "se.dna_begin as dnaBegin, " +
//                "se.dna_end as dnaEnd, " +
//                "se.dna_orientation as dnaOrientation, " +
//                "se.translation_table as translationTable, " +
//                "se.stop_5_prime as stop5Prime, " +
//                "se.stop_3_prime as stop3Prime, " +
//                "pd.gene_symbol as geneSymbol, " +
//                "pd.gene_ontology as geneOntology, " +
//                "pd.enzyme_commission as enzymeCommission, " +
//                "pd.length as length, " +
//                "pd.protein_function as proteinFunction " +
//                "from sequence_entity se inner join protein_detail pd on pd.protein_id=se.entity_id " +
//                "where se.dna_id = (select entity_id from sequence_entity where accession=:scaffoldAccNo) " +
//                "and se.entity_type_code in (6,7) " + orderByClause;
//
//        _logger.info("getPeptidesForScaffoldByAccNo sql: " + sql);
//        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
//        sqlQuery.setString("scaffoldAccNo", scaffoldAccNo);
//        if (startIndex > 0) {
//            sqlQuery.setFirstResult(startIndex);
//        }
//        if (numRecords > 0) {
//            sqlQuery.setMaxResults(numRecords);
//        }
//        List<Object[]> results = sqlQuery.list();
//        List<PeptideDetail> peptideDetailList = new ArrayList<PeptideDetail>();
//        for (Object[] result : results) {
//            PeptideDetail peptideDetail = new PeptideDetail();
//            peptideDetail.setAcc((String) result[0]);
//            peptideDetail.setDnaBegin((Integer) result[1]);
//            peptideDetail.setDnaEnd((Integer) result[2]);
//            peptideDetail.setDnaOrientation((Integer) result[3]);
//            peptideDetail.setTranslationTable((String) result[4]);
//            peptideDetail.setStop5Prime((String) result[5]);
//            peptideDetail.setStop3Prime((String) result[6]);
//            peptideDetail.setGeneSymbol((String) result[7]);
//            peptideDetail.setGoAnnotationDescription(AnnotationUtil.createAnnotationListFromString((String) result[8], ANNOT_ROW_SEP, ANNOT_LIST_SEP));
//            peptideDetail.setEcAnnotationDescription(AnnotationUtil.createAnnotationListFromString((String) result[9], ANNOT_ROW_SEP, ANNOT_LIST_SEP));
//            peptideDetail.setLength((Integer) result[10]);
//            peptideDetail.setProteinFunction((String) result[11]);
//            peptideDetailList.add(peptideDetail);
//        }
//        return peptideDetailList;
//
//    }
//
//    // Support for Scaffold Detail
//    public int getNumOfReadsForScaffoldByAccNo(String scaffoldAccNo) throws DaoException {
//        String hql = "select count(s) from ScaffoldReadAlignment s " +
//                "where s.scaffoldAcc = :scaffoldAccNo";
//        Query hqlQuery = getSession().createQuery(hql);
//        hqlQuery.setParameter("scaffoldAccNo", scaffoldAccNo);
//        List results = hqlQuery.list();
//        Long lresult = (Long) results.get(0);
//        int result = lresult.intValue();
//        _logger.debug("getNumOfReadsForScaffoldByAccNo() return value=" + result);
//        return result;
//    }
//
//    public List<ScaffoldReadAlignment> getReadsForScaffoldByAccNo(String scaffoldAccNo, int startIndex, int numRecords, SortArgument[] sortArgs)
//            throws DaoException {
//        String hql = "select s from ScaffoldReadAlignment s " +
//                "inner join s.read r " +
//                "where s.scaffoldAcc = :scaffoldAccNo";
//        StringBuffer sortBuffer = new StringBuffer();
//        if (sortArgs != null && sortArgs.length > 0) {
//            for (int i = 0; i < sortArgs.length; i++) {
//                if (sortArgs[i].getSortDirection() != SortArgument.SORT_NOTSET) {
//                    String sortArgName = sortArgs[i].getSortArgumentName();
//                    if (sortArgName == null || sortArgName.length() == 0) {
//                        continue;
//                    }
//                    else if (sortArgName.equals("readLength")) {
//                        sortArgName = "r.sequenceLength";
//                    }
//                    else if (sortArgName.equals("readTemplate")) {
//                        sortArgName = "r.templateAcc";
//                    }
//                    else if (!sortArgName.startsWith("s.")) {
//                        sortArgName = "s." + sortArgName;
//                    }
//                    String dir;
//                    if (sortArgs[i].getSortDirection() == SortArgument.SORT_ASC) {
//                        dir = "asc";
//                    }
//                    else if (sortArgs[i].getSortDirection() == SortArgument.SORT_DESC) {
//                        dir = "desc";
//                    }
//                    else {
//                        continue;
//                    }
//                    if (sortBuffer.length() == 0) {
//                        sortBuffer.append("order by ");
//                    }
//                    else {
//                        sortBuffer.append(",");
//                    }
//                    sortBuffer.append(sortArgName);
//                    sortBuffer.append(' ');
//                    sortBuffer.append(dir);
//                }
//            }
//        }
//        else {
//            sortBuffer.append("order by s.scaffoldBegin asc");
//        }
//        if (sortBuffer.length() > 0) {
//            hql = hql + " " + sortBuffer.toString();
//        }
//        _logger.debug("getReadsForScaffoldByAccNo HQL: " + hql);
//        Query query = getSession().createQuery(hql);
//        query.setString("scaffoldAccNo", scaffoldAccNo);
//        if (numRecords > 0) {
//            query.setFirstResult(startIndex);
//            query.setMaxResults(numRecords);
//        }
//        return (List<ScaffoldReadAlignment>) query.list();
//    }
//
//    // Support for Cluster Detail
//    public ProteinCluster getProteinCoreCluster(String clusterAcc) throws DaoException {
//        String sql = "select  " +
//                "cc.core_cluster_acc as coreClusterAcc, " +
//                "cc.final_cluster_acc as finalClusterAcc, " +
//                "cc.cluster_quality as clusterQuality, " +
//                "cc.num_nonredundant as numNonRedundant, " +
//                "cc.num_protein as numProteins " +
//                "from core_cluster cc " +
//                "where cc.core_cluster_acc = :clusterAcc";
//        _logger.info("Retrieve protein cluster data sql: " + sql);
//        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
//        sqlQuery.setString("clusterAcc", clusterAcc);
//        sqlQuery.addScalar("coreClusterAcc", Hibernate.STRING);
//        sqlQuery.addScalar("finalClusterAcc", Hibernate.STRING);
//        sqlQuery.addScalar("clusterQuality", Hibernate.STRING);
//        sqlQuery.addScalar("numNonRedundant", Hibernate.INTEGER);
//        sqlQuery.addScalar("numProteins", Hibernate.INTEGER);
//        List<Object[]> results = sqlQuery.list();
//        List<ProteinCluster> proteinClusters = new ArrayList<ProteinCluster>();
//        for (Object[] res : results) {
//            ProteinCluster proteinCluster = new ProteinCluster();
//            proteinCluster.setClusterAcc((String) res[0]);
//            proteinCluster.setParentClusterAcc((String) res[1]);
//            proteinCluster.setClusterQuality((String) res[2]);
//            proteinCluster.setNumNonRedundantProteins((Integer) res[3]);
//            proteinCluster.setNumProteins((Integer) res[4]);
//            proteinClusters.add(proteinCluster);
//        }
//        return proteinClusters.size() > 0 ? proteinClusters.get(0) : null;
//    }
//
//    public ProteinCluster getProteinFinalCluster(String clusterAcc) throws DaoException {
//        String sql =
//                "select " +
//                        " fc.final_cluster_acc as currentClusterAcc," +
//                        " fc.cluster_quality as clusterQuality," +
//                        " fc.num_core_cluster as numClusterMembers," +
//                        " fc.num_nonredundant as numNonRedundant," +
//                        " fc.num_protein as numProteins " +
//                        " from final_cluster fc " +
//                        " where fc.final_cluster_acc = :clusterAcc";
//        _logger.info("Retrieve protein cluster data sql: " + sql);
//        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
//        sqlQuery.setString("clusterAcc", clusterAcc);
//        sqlQuery.addScalar("currentClusterAcc", Hibernate.STRING);
//        sqlQuery.addScalar("clusterQuality", Hibernate.STRING);
//        sqlQuery.addScalar("numClusterMembers", Hibernate.INTEGER);
//        sqlQuery.addScalar("numNonRedundant", Hibernate.INTEGER);
//        sqlQuery.addScalar("numProteins", Hibernate.INTEGER);
//        List<Object[]> results = sqlQuery.list();
//        List<ProteinCluster> proteinClusters = new ArrayList<ProteinCluster>();
//        for (Object[] res : results) {
//            ProteinCluster proteinCluster = new ProteinCluster();
//            proteinCluster.setClusterAcc((String) res[0]);
//            proteinCluster.setClusterQuality((String) res[1]);
//            proteinCluster.setNumClusterMembers((Integer) res[2]);
//            proteinCluster.setNumNonRedundantProteins((Integer) res[3]);
//            proteinCluster.setNumProteins((Integer) res[4]);
//            proteinClusters.add(proteinCluster);
//        }
//        return proteinClusters.size() > 0 ? proteinClusters.get(0) : null;
//    }
//
//    public List<ClusterAnnotation> getCoreClusterAnnotations(String clusterAcc, SortArgument[] sortArgs)
//            throws DaoException {
//        String orderByClause = buildOrderByClause(sortArgs);
//        String sql = "select  " +
//                "cc.core_cluster_acc as accession, " +
//                "cca.category as annotationType, " +
//                "cca.id as annotationID, " +
//                "cca.name as description, " +
//                "cca.pct_assigned as evidencePct " +
//                "from core_cluster cc inner join core_cluster_annotation cca on cca.core_cluster_id=cc.core_cluster_id " +
//                "where cc.core_cluster_acc = :clusterAcc " +
//                orderByClause;
//        _logger.info("Retrieve core cluster annotation sql: " + sql);
//        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
//        sqlQuery.setString("clusterAcc", clusterAcc);
//        sqlQuery.addScalar("accession", Hibernate.STRING);
//        sqlQuery.addScalar("annotationType", Hibernate.STRING);
//        sqlQuery.addScalar("annotationID", Hibernate.STRING);
//        sqlQuery.addScalar("description", Hibernate.STRING);
//        sqlQuery.addScalar("evidencePct", Hibernate.FLOAT);
//        List<Object[]> results = sqlQuery.list();
//        List<ClusterAnnotation> clusterAnnotationList = new ArrayList<ClusterAnnotation>();
//        for (Object[] res : results) {
//            ClusterAnnotation clusterAnnotation = new ClusterAnnotation();
//            clusterAnnotation.setAccession((String) res[0]);
//            clusterAnnotation.setAnnotationType((String) res[1]);
//            clusterAnnotation.setAnnotationID((String) res[2]);
//            clusterAnnotation.setDescription((String) res[3]);
//            clusterAnnotation.setEvidencePct((Float) res[4]);
//            clusterAnnotationList.add(clusterAnnotation);
//        }
//        return clusterAnnotationList;
//    }
//
//    public List<ClusterAnnotation> getFinalClusterAnnotations(String clusterAcc, SortArgument[] sortArgs)
//            throws DaoException {
//        String orderByClause = buildOrderByClause(sortArgs);
//        String sql = "select  " +
//                "fc.final_cluster_acc as accession, " +
//                "fca.category as annotationType, " +
//                "fca.id as annotationID, " +
//                "fca.name as description, " +
//                "fca.pct_assigned as evidencePct " +
//                "from final_cluster fc " +
//                "inner join final_cluster_annotation fca on fca.final_cluster_id=fc.final_cluster_id " +
//                "where fc.final_cluster_acc = :clusterAcc " +
//                orderByClause;
//        _logger.info("Retrieve final cluster annotation sql: " + sql);
//        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
//        sqlQuery.setString("clusterAcc", clusterAcc);
//        sqlQuery.addScalar("accession", Hibernate.STRING);
//        sqlQuery.addScalar("annotationType", Hibernate.STRING);
//        sqlQuery.addScalar("annotationID", Hibernate.STRING);
//        sqlQuery.addScalar("description", Hibernate.STRING);
//        sqlQuery.addScalar("evidencePct", Hibernate.FLOAT);
//        List<Object[]> results = sqlQuery.list();
//        List<ClusterAnnotation> clusterAnnotationList = new ArrayList<ClusterAnnotation>();
//        for (Object[] res : results) {
//            ClusterAnnotation clusterAnnotation = new ClusterAnnotation();
//            clusterAnnotation.setAccession((String) res[0]);
//            clusterAnnotation.setAnnotationType((String) res[1]);
//            clusterAnnotation.setAnnotationID((String) res[2]);
//            clusterAnnotation.setDescription((String) res[3]);
//            clusterAnnotation.setEvidencePct((Float) res[4]);
//            clusterAnnotationList.add(clusterAnnotation);
//        }
//        return clusterAnnotationList;
//    }
//
//    public List<ProteinCluster> getPagedCoreClustersFromFinalCluster(String finalClusterAcc,
//                                                                     Set<String> clusterMemberAccs,
//                                                                     int startIndex,
//                                                                     int numRows,
//                                                                     SortArgument[] sortArgs)
//            throws DaoException {
//        String orderByClause = buildOrderByClause(sortArgs);
//        String sql = "select  " +
//                "cc.core_cluster_acc as coreClusterAcc, " +
//                "fc.final_cluster_acc as finalClusterAcc, " +
//                "cc.longest_protein_acc as longestMemberAcc, " +
//                "fc.cluster_quality as clusterQuality, " +
//                "cc.num_nonredundant as numNonRedundant, " +
//                "cc.num_protein as numProteins " +
//                "from final_cluster fc " +
//                "inner join core_cluster cc on cc.final_cluster_id=fc.final_cluster_id " +
//                "where fc.final_cluster_acc = :clusterAcc ";
//        if (clusterMemberAccs != null && clusterMemberAccs.size() > 0) {
//            sql += "and cc.core_cluster_acc in (:clusterMemberAccs) ";
//        }
//        sql += orderByClause;
//        _logger.info("Retrieve core cluster members sql: " + sql);
//        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
//        sqlQuery.setString("clusterAcc", finalClusterAcc);
//        if (clusterMemberAccs != null && clusterMemberAccs.size() > 0) {
//            sqlQuery.setParameterList("clusterMemberAccs", clusterMemberAccs);
//        }
//        sqlQuery.addScalar("coreClusterAcc", Hibernate.STRING);
//        sqlQuery.addScalar("finalClusterAcc", Hibernate.STRING);
//        sqlQuery.addScalar("longestMemberAcc", Hibernate.STRING);
//        sqlQuery.addScalar("clusterQuality", Hibernate.STRING);
//        sqlQuery.addScalar("numNonRedundant", Hibernate.INTEGER);
//        sqlQuery.addScalar("numProteins", Hibernate.INTEGER);
//        if (startIndex > 0) {
//            sqlQuery.setFirstResult(startIndex);
//        }
//        if (numRows > 0) {
//            sqlQuery.setMaxResults(numRows);
//        }
//        List<Object[]> results = sqlQuery.list();
//        List<ProteinCluster> proteinClusters = new ArrayList<ProteinCluster>();
//        for (Object[] res : results) {
//            ProteinCluster proteinCluster = new ProteinCluster();
//            proteinCluster.setClusterAcc((String) res[0]);
//            proteinCluster.setParentClusterAcc((String) res[1]);
//            proteinCluster.setLongestProteinMemberAcc((String) res[2]);
//            proteinCluster.setClusterQuality((String) res[3]);
//            proteinCluster.setNumNonRedundantProteins((Integer) res[4]);
//            proteinCluster.setNumProteins((Integer) res[5]);
//            proteinClusters.add(proteinCluster);
//        }
//        return proteinClusters;
//    }
//
//    public List<ProteinClusterMember> getPagedNRSeqMembersFromCoreCluster(String coreClusterAcc,
//                                                                          Set<String> clusterMemberAccs,
//                                                                          int startIndex,
//                                                                          int numRows,
//                                                                          SortArgument[] sortArgs)
//            throws DaoException {
//        String orderByClause = buildOrderByClause(sortArgs);
//        String sql = "select  " +
//                "pd.protein_acc as proteinAcc, " +
//                "cc.core_cluster_acc as coreClusterAcc, " +
//                "cp.final_cluster_acc as finalClusterAcc, " +
//                "cp.nr_parent_acc, " +
//                "cc.cluster_quality as clusterQuality, " +
//                "pd.gene_symbol as gene_symbol, " +
//                "pd.gene_ontology as gene_ontology, " +
//                "pd.enzyme_commission as enzyme_commission, " +
//                "pd.length as length, " +
//                "pd.protein_function as protein_function " +
//                "from core_cluster cc " +
//                "inner join core_cluster_protein cp on cc.core_cluster_id = cp.core_cluster_id " +
//                "inner join protein_detail pd on pd.protein_id=cp.protein_id " +
//                "where cc.core_cluster_acc = :coreClusterAcc " +
//                "and cp.nr_parent_acc is NULL ";
//        if (clusterMemberAccs != null && clusterMemberAccs.size() > 0) {
//            sql += "and pd.protein_acc in (:clusterMemberAccs) ";
//        }
//        sql += orderByClause;
//        _logger.info("Retrieve non redundant proteins sql: " + sql);
//        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
//        sqlQuery.setString("coreClusterAcc", coreClusterAcc);
//        if (clusterMemberAccs != null && clusterMemberAccs.size() > 0) {
//            sqlQuery.setParameterList("clusterMemberAccs", clusterMemberAccs);
//        }
//        if (startIndex > 0) {
//            sqlQuery.setFirstResult(startIndex);
//        }
//        if (numRows > 0) {
//            sqlQuery.setMaxResults(numRows);
//        }
//        List<Object[]> results = sqlQuery.list();
//        List<ProteinClusterMember> proteinClusterInfoList = new ArrayList<ProteinClusterMember>();
//        for (Object[] result : results) {
//            ProteinClusterMember proteinClusterInfo = new ProteinClusterMember();
//            proteinClusterInfo.setProteinAcc((String) result[0]);
//            proteinClusterInfo.setCoreClusterAcc((String) result[1]);
//            proteinClusterInfo.setFinalClusterAcc((String) result[2]);
//            proteinClusterInfo.setNonRedundantParentAcc((String) result[3]);
//            proteinClusterInfo.setClusterQuality((String) result[4]);
//
//            proteinClusterInfo.setGeneSymbol((String) result[5]);
//            proteinClusterInfo.setGoAnnotationDescription(AnnotationUtil.createAnnotationListFromString((String) result[6], ANNOT_ROW_SEP, ANNOT_LIST_SEP));
//            proteinClusterInfo.setEcAnnotationDescription(AnnotationUtil.createAnnotationListFromString((String) result[7], ANNOT_ROW_SEP, ANNOT_LIST_SEP));
//            proteinClusterInfo.setLength((Integer) result[8]);
//            proteinClusterInfo.setProteinFunction((String) result[9]);
//
//            proteinClusterInfoList.add(proteinClusterInfo);
//        }
//        return proteinClusterInfoList;
//    }
//
//    public List<ProteinClusterMember> getPagedSeqMembersFromCoreCluster(String coreClusterAcc,
//                                                                        Set<String> clusterMemberAccs,
//                                                                        int startIndex,
//                                                                        int numRows,
//                                                                        SortArgument[] sortArgs)
//            throws DaoException {
//        String orderByClause = buildOrderByClause(sortArgs);
//        String sql = "select  " +
//                "pd.protein_acc as proteinAcc, " +
//                "cc.core_cluster_acc as coreClusterAcc, " +
//                "cc.final_cluster_acc as finalClusterAcc, " +
//                "cp.nr_parent_acc, " +
//                "cc.cluster_quality as clusterQuality, " +
//                "pd.gene_symbol as gene_symbol, " +
//                "pd.gene_ontology as gene_ontology, " +
//                "pd.enzyme_commission as enzyme_commission, " +
//                "pd.length as length, " +
//                "pd.protein_function as protein_function " +
//                "from core_cluster cc " +
//                "inner join core_cluster_protein cp on cc.core_cluster_id = cp.core_cluster_id " +
//                "inner join protein_detail pd on pd.protein_id=cp.protein_id " +
//                "where cc.core_cluster_acc = :coreClusterAcc ";
//        if (clusterMemberAccs != null && clusterMemberAccs.size() > 0) {
//            sql += "and pd.protein_acc in (:clusterMemberAccs) ";
//        }
//        sql += orderByClause;
//        _logger.info("Retrieve protein cluster data sql: " + sql);
//        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
//        sqlQuery.setString("coreClusterAcc", coreClusterAcc);
//        if (clusterMemberAccs != null && clusterMemberAccs.size() > 0) {
//            sqlQuery.setParameterList("clusterMemberAccs", clusterMemberAccs);
//        }
//        if (startIndex > 0) {
//            sqlQuery.setFirstResult(startIndex);
//        }
//        if (numRows > 0) {
//            sqlQuery.setMaxResults(numRows);
//        }
//        List<Object[]> results = sqlQuery.list();
//        List<ProteinClusterMember> proteinClusterInfoList = new ArrayList<ProteinClusterMember>();
//        for (Object[] result : results) {
//            ProteinClusterMember proteinClusterInfo = new ProteinClusterMember();
//            proteinClusterInfo.setProteinAcc((String) result[0]);
//            proteinClusterInfo.setCoreClusterAcc((String) result[1]);
//            proteinClusterInfo.setFinalClusterAcc((String) result[2]);
//            proteinClusterInfo.setNonRedundantParentAcc((String) result[3]);
//            proteinClusterInfo.setClusterQuality((String) result[4]);
//
//            proteinClusterInfo.setGeneSymbol((String) result[5]);
//            proteinClusterInfo.setGoAnnotationDescription(AnnotationUtil.createAnnotationListFromString((String) result[6], ANNOT_ROW_SEP, ANNOT_LIST_SEP));
//            proteinClusterInfo.setEcAnnotationDescription(AnnotationUtil.createAnnotationListFromString((String) result[7], ANNOT_ROW_SEP, ANNOT_LIST_SEP));
//            proteinClusterInfo.setLength((Integer) result[8]);
//            proteinClusterInfo.setProteinFunction((String) result[9]);
//
//            proteinClusterInfoList.add(proteinClusterInfo);
//        }
//        return proteinClusterInfoList;
//    }
//
//    public List<ProteinClusterMember> getPagedNRSeqMembersFromFinalCluster(String finalClusterAcc,
//                                                                           Set<String> clusterMemberAccs,
//                                                                           int startIndex,
//                                                                           int numRows,
//                                                                           SortArgument[] sortArgs)
//            throws DaoException {
//        String orderByClause = buildOrderByClause(sortArgs);
//        String sql = "select  " +
//                "pd.protein_acc as proteinAcc, " +
//                "cp.core_cluster_acc as coreClusterAcc, " +
//                "fc.final_cluster_acc as finalClusterAcc, " +
//                "cp.nr_parent_acc, " +
//                "fc.cluster_quality as clusterQuality, " +
//                "pd.gene_symbol as gene_symbol, " +
//                "pd.gene_ontology as gene_ontology, " +
//                "pd.enzyme_commission as enzyme_commission, " +
//                "pd.length as length, " +
//                "pd.protein_function as protein_function " +
//                "from final_cluster fc " +
//                "inner join core_cluster_protein cp on fc.final_cluster_id = cp.final_cluster_id " +
//                "inner join protein_detail pd on pd.protein_id=cp.protein_id " +
//                "where fc.final_cluster_acc = :finalClusterAcc ";
//        if (clusterMemberAccs != null && clusterMemberAccs.size() > 0) {
//            sql += "and pd.protein_acc in (:clusterMemberAccs) ";
//        }
//        sql += orderByClause;
//        _logger.info("Retrieve non redundant proteins sql: " + sql);
//        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
//        sqlQuery.setString("finalClusterAcc", finalClusterAcc);
//        if (clusterMemberAccs != null && clusterMemberAccs.size() > 0) {
//            sqlQuery.setParameterList("clusterMemberAccs", clusterMemberAccs);
//        }
//        if (startIndex > 0) {
//            sqlQuery.setFirstResult(startIndex);
//        }
//        if (numRows > 0) {
//            sqlQuery.setMaxResults(numRows);
//        }
//        List<Object[]> results = sqlQuery.list();
//        List<ProteinClusterMember> proteinClusterInfoList = new ArrayList<ProteinClusterMember>();
//        for (Object[] result : results) {
//            ProteinClusterMember proteinClusterInfo = new ProteinClusterMember();
//            proteinClusterInfo.setProteinAcc((String) result[0]);
//            proteinClusterInfo.setCoreClusterAcc((String) result[1]);
//            proteinClusterInfo.setFinalClusterAcc((String) result[2]);
//            proteinClusterInfo.setNonRedundantParentAcc((String) result[3]);
//            proteinClusterInfo.setClusterQuality((String) result[4]);
//
//            proteinClusterInfo.setGeneSymbol((String) result[5]);
//            proteinClusterInfo.setGoAnnotationDescription(AnnotationUtil.createAnnotationListFromString((String) result[6], ANNOT_ROW_SEP, ANNOT_LIST_SEP));
//            proteinClusterInfo.setEcAnnotationDescription(AnnotationUtil.createAnnotationListFromString((String) result[7], ANNOT_ROW_SEP, ANNOT_LIST_SEP));
//            proteinClusterInfo.setLength((Integer) result[8]);
//            proteinClusterInfo.setProteinFunction((String) result[9]);
//
//            proteinClusterInfoList.add(proteinClusterInfo);
//        }
//        return proteinClusterInfoList;
//    }
//
//    public List<ProteinClusterMember> getPagedSeqMembersFromFinalCluster(String finalClusterAcc,
//                                                                         Set<String> clusterMemberAccs, int startIndex,
//                                                                         int numRows,
//                                                                         SortArgument[] sortArgs)
//            throws DaoException {
//        String orderByClause = buildOrderByClause(sortArgs);
//        String sql = "select  " +
//                "pd.protein_acc as proteinAcc, " +
//                "cp.core_cluster_acc as coreClusterAcc, " +
//                "fc.final_cluster_acc as finalClusterAcc, " +
//                "cp.nr_parent_acc, " +
//                "fc.cluster_quality as clusterQuality, " +
//                "pd.gene_symbol as gene_symbol, " +
//                "pd.gene_ontology as gene_ontology, " +
//                "pd.enzyme_commission as enzyme_commission, " +
//                "pd.length as length, " +
//                "pd.protein_function as protein_function " +
//                "from final_cluster fc " +
//                "inner join core_cluster_protein cp on fc.final_cluster_id = cp.final_cluster_id " +
//                "inner join protein_detail pd on pd.protein_id=cp.protein_id " +
//                "where fc.final_cluster_acc = :finalClusterAcc ";
//        if (clusterMemberAccs != null && clusterMemberAccs.size() > 0) {
//            sql += "and pd.protein_acc in (:clusterMemberAccs) ";
//        }
//        sql += orderByClause;
//        _logger.info("Retrieve protein cluster data sql: " + sql);
//        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
//        sqlQuery.setString("finalClusterAcc", finalClusterAcc);
//        if (clusterMemberAccs != null && clusterMemberAccs.size() > 0) {
//            sqlQuery.setParameterList("clusterMemberAccs", clusterMemberAccs);
//        }
//        if (startIndex > 0) {
//            sqlQuery.setFirstResult(startIndex);
//        }
//        if (numRows > 0) {
//            sqlQuery.setMaxResults(numRows);
//        }
//        List<Object[]> results = sqlQuery.list();
//        List<ProteinClusterMember> proteinClusterInfoList = new ArrayList<ProteinClusterMember>();
//        for (Object[] result : results) {
//            ProteinClusterMember proteinClusterInfo = new ProteinClusterMember();
//            proteinClusterInfo.setProteinAcc((String) result[0]);
//            proteinClusterInfo.setCoreClusterAcc((String) result[1]);
//            proteinClusterInfo.setFinalClusterAcc((String) result[2]);
//            proteinClusterInfo.setNonRedundantParentAcc((String) result[3]);
//            proteinClusterInfo.setClusterQuality((String) result[4]);
//
//            proteinClusterInfo.setGeneSymbol((String) result[5]);
//            proteinClusterInfo.setGoAnnotationDescription(AnnotationUtil.createAnnotationListFromString((String) result[6], ANNOT_ROW_SEP, ANNOT_LIST_SEP));
//            proteinClusterInfo.setEcAnnotationDescription(AnnotationUtil.createAnnotationListFromString((String) result[7], ANNOT_ROW_SEP, ANNOT_LIST_SEP));
//            proteinClusterInfo.setLength((Integer) result[8]);
//            proteinClusterInfo.setProteinFunction((String) result[9]);
//
//            proteinClusterInfoList.add(proteinClusterInfo);
//        }
//        return proteinClusterInfoList;
//    }
//
//    public int getNumMatchingRepsFromCoreClusterWithAnnotation(String coreClusterAcc, String annotationId)
//            throws DaoException {
//        String sql = "select count(1) as nMatches " +
//                "from core_cluster cc " +
//                "inner join protein_annotation pa on pa.core_cluster_id = cc.core_cluster_id and pa.id = :annotationID " +
//                "where cc.core_cluster_acc = :coreClusterAcc";
//        _logger.info("Retrieve number of proteins with matching annotation sql: " + sql);
//        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
//        sqlQuery.setString("coreClusterAcc", coreClusterAcc);
//        sqlQuery.setString("annotationID", annotationId);
//        sqlQuery.addScalar("nMatches", Hibernate.INTEGER);
//        return ((Integer) sqlQuery.uniqueResult()).intValue();
//    }
//
//    public List<ProteinClusterAnnotationMember> getPagedMatchingRepsFromCoreClusterWithAnnotation(String coreClusterAcc,
//                                                                                                  String annotationId,
//                                                                                                  Set<String> clusterMemberAccs,
//                                                                                                  int startIndex,
//                                                                                                  int numRows,
//                                                                                                  SortArgument[] sortArgs)
//            throws DaoException {
//        String orderByClause = buildOrderByClause(sortArgs);
//        String sql =
//                "select " +
//                        "pd.protein_acc as proteinAcc, " +
//                        "cc.core_cluster_acc as coreClusterAcc, " +
//                        "cp.final_cluster_acc as finalClusterAcc, " +
//                        "cp.nr_parent_acc, " +
//                        "cc.cluster_quality as clusterQuality, " +
//                        "pa.evidence, " +
//                        "pd.gene_symbol as gene_symbol, " +
//                        "pd.gene_ontology as gene_ontology, " +
//                        "pd.enzyme_commission as enzyme_commission, " +
//                        "pd.length as length, " +
//                        "pd.protein_function as protein_function " +
//                        "from core_cluster cc " +
//                        "inner join protein_annotation pa on pa.core_cluster_id = cc.core_cluster_id and pa.id = :annotationID " +
//                        "inner join core_cluster_protein cp on pa.protein_id = cp.protein_id " +
//                        "inner join protein_detail pd on pa.protein_id=pd.protein_id " +
//                        "where cc.core_cluster_acc = :coreClusterAcc ";
//
//        if (clusterMemberAccs != null && clusterMemberAccs.size() > 0) {
//            sql += "and pd.protein_acc in (:clusterMemberAccs) ";
//        }
//        sql += orderByClause;
//        _logger.info("Retrieve protein cluster data sql: " + sql);
//        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
//        sqlQuery.setString("coreClusterAcc", coreClusterAcc);
//        sqlQuery.setString("annotationID", annotationId);
//        if (clusterMemberAccs != null && clusterMemberAccs.size() > 0) {
//            sqlQuery.setParameterList("clusterMemberAccs", clusterMemberAccs);
//        }
//        if (startIndex > 0) {
//            sqlQuery.setFirstResult(startIndex);
//        }
//        if (numRows > 0) {
//            sqlQuery.setMaxResults(numRows);
//        }
//        List<Object[]> results = sqlQuery.list();
//        List<ProteinClusterAnnotationMember> proteinClusterInfoList = new ArrayList<ProteinClusterAnnotationMember>();
//        populateProteinClusterInfoList(results, proteinClusterInfoList);
//        return proteinClusterInfoList;
//    }
//
//    public int getNumMatchingRepsFromFinalClusterWithAnnotation(String finalClusterAcc, String annotationId)
//            throws DaoException {
//        String sql = "select count(1) as nMatches " +
//                "from final_cluster fc " +
//                "inner join protein_annotation pa on pa.final_cluster_id = fc.final_cluster_id and pa.id = :annotationID " +
//                "where fc.final_cluster_acc = :finalClusterAcc";
//        _logger.info("Retrieve number of proteins with matching annotation sql: " + sql);
//        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
//        sqlQuery.setString("finalClusterAcc", finalClusterAcc);
//        sqlQuery.setString("annotationID", annotationId);
//        sqlQuery.addScalar("nMatches", Hibernate.INTEGER);
//        return ((Integer) sqlQuery.uniqueResult()).intValue();
//    }
//
//    public List<ProteinClusterAnnotationMember> getPagedMatchingRepsFromFinalClusterWithAnnotation(String finalClusterAcc,
//                                                                                                   String annotationId,
//                                                                                                   Set<String> clusterMemberAccs,
//                                                                                                   int startIndex,
//                                                                                                   int numRows,
//                                                                                                   SortArgument[] sortArgs)
//            throws DaoException {
//        String orderByClause = buildOrderByClause(sortArgs);
//        String sql =
//                "select " +
//                        "pd.protein_acc as proteinAcc, " +
//                        "cp.core_cluster_acc as coreClusterAcc, " +
//                        "fc.final_cluster_acc as finalClusterAcc, " +
//                        "cp.nr_parent_acc, " +
//                        "fc.cluster_quality as clusterQuality, " +
//                        "pa.evidence, " +
//                        "pd.gene_symbol as gene_symbol, " +
//                        "pd.gene_ontology as gene_ontology, " +
//                        "pd.enzyme_commission as enzyme_commission, " +
//                        "pd.length as length, " +
//                        "pd.protein_function as protein_function " +
//                        "from final_cluster fc " +
//                        "inner join protein_annotation pa on pa.final_cluster_id = fc.final_cluster_id and pa.id = :annotationID " +
//                        "inner join core_cluster_protein cp on pa.protein_id = cp.protein_id " +
//                        "inner join protein_detail pd on pa.protein_id=pd.protein_id " +
//                        "where fc.final_cluster_acc = :finalClusterAcc ";
//
//        if (clusterMemberAccs != null && clusterMemberAccs.size() > 0) {
//            sql += "and pd.protein_acc in (:clusterMemberAccs) ";
//        }
//        sql += orderByClause;
//        _logger.info("Retrieve protein cluster data sql: " + sql);
//        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
//        sqlQuery.setString("finalClusterAcc", finalClusterAcc);
//        sqlQuery.setString("annotationID", annotationId);
//        if (clusterMemberAccs != null && clusterMemberAccs.size() > 0) {
//            sqlQuery.setParameterList("clusterMemberAccs", clusterMemberAccs);
//        }
//        if (startIndex > 0) {
//            sqlQuery.setFirstResult(startIndex);
//        }
//        if (numRows > 0) {
//            sqlQuery.setMaxResults(numRows);
//        }
//        List<Object[]> results = sqlQuery.list();
//        List<ProteinClusterAnnotationMember> proteinClusterInfoList = new ArrayList<ProteinClusterAnnotationMember>();
//        populateProteinClusterInfoList(results, proteinClusterInfoList);
//        return proteinClusterInfoList;
//    }
//
//    public void populateProteinClusterInfoList(List<Object[]> results, List<ProteinClusterAnnotationMember> proteinClusterInfoList) {
//        // First, we need to construct a list of evidence strings from which we can generate a list of
//        // external evidence links
//        List<String> evidenceList = new ArrayList<String>();
//        for (Object[] result : results) {
//            evidenceList.add((String) result[5]);
//        }
//        List<String> evidenceExternalLinkList = BSEntityServiceImpl.createExternalEvidenceLinks(evidenceList);
//        int i = 0;
//        for (Object[] result : results) {
//            ProteinClusterAnnotationMember proteinClusterInfo = new ProteinClusterAnnotationMember();
//            proteinClusterInfo.setProteinAcc((String) result[0]);
//            proteinClusterInfo.setCoreClusterAcc((String) result[1]);
//            proteinClusterInfo.setFinalClusterAcc((String) result[2]);
//            proteinClusterInfo.setNonRedundantParentAcc((String) result[3]);
//            proteinClusterInfo.setClusterQuality((String) result[4]);
//            proteinClusterInfo.setEvidence((String) result[5]);
//            proteinClusterInfo.setExternalEvidenceLink(evidenceExternalLinkList.get(i));
//            proteinClusterInfo.setGeneSymbol((String) result[6]);
//            proteinClusterInfo.setGoAnnotationDescription(AnnotationUtil.createAnnotationListFromString((String) result[7], ANNOT_ROW_SEP, ANNOT_LIST_SEP));
//            proteinClusterInfo.setEcAnnotationDescription(AnnotationUtil.createAnnotationListFromString((String) result[8], ANNOT_ROW_SEP, ANNOT_LIST_SEP));
//            proteinClusterInfo.setLength((Integer) result[9]);
//            proteinClusterInfo.setProteinFunction((String) result[10]);
//            proteinClusterInfoList.add(proteinClusterInfo);
//            i++;
//        }
//    }
//
    public List<String> getTaxonSynonyms(Integer taxonId) throws DaoException {
        ArrayList<String> taxonList = new ArrayList<String>();
        String sql = "select ts.name, ts.name_type from taxonomy_synonym ts where ts.taxon_id = :taxonId";
        logger.info("getTaxonSynonyms sql=" + sql);
        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
        sqlQuery.setInteger("taxonId", taxonId);
        List<Object[]> results = sqlQuery.list();
        logger.info("getTaxonSynonyms retuned " + results.size() + " results");
        for (Object[] result : results) {
            taxonList.add((String) result[0]);
        }
        return taxonList;
    }


}
