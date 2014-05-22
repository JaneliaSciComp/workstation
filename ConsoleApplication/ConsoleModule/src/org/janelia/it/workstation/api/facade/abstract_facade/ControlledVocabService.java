package org.janelia.it.workstation.api.facade.abstract_facade;


import org.janelia.it.jacs.shared.utils.ControlledVocabElement;

public interface ControlledVocabService {

    ControlledVocabElement[] getControlledVocab(Long entityOID, int vocabIndex) throws org.janelia.it.workstation.api.stub.data.NoDataException;
}
