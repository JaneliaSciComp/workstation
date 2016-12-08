package org.janelia.jacs2.dao;

import org.janelia.it.jacs.model.domain.Subject;

public interface SubjectDao extends Dao<Subject, Number> {
    Subject findByName(String subjectName);
}
