package org.janelia.it.workstation.api.facade.abstract_facade;


import org.janelia.it.jacs.shared.utils.ControlledVocabElement;
import org.janelia.it.workstation.api.stub.data.NoDataException;

public interface ControlledVocabService {

    ControlledVocabElement[] getControlledVocab(Long entityOID, int vocabIndex) throws NoDataException;
}
