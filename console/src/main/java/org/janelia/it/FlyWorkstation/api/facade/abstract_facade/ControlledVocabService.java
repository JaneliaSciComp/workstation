package org.janelia.it.FlyWorkstation.api.facade.abstract_facade;


import org.janelia.it.FlyWorkstation.api.stub.data.NoDataException;
import org.janelia.it.jacs.shared.utils.ControlledVocabElement;

public interface ControlledVocabService {

    ControlledVocabElement[] getControlledVocab(Long entityOID, int vocabIndex) throws NoDataException;
}
