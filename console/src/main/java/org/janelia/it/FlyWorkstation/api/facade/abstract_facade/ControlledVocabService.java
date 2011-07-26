package org.janelia.it.FlyWorkstation.api.facade.abstract_facade;


import org.janelia.it.FlyWorkstation.api.stub.data.NoData;
import org.janelia.it.jacs.shared.utils.ControlledVocabElement;

public interface ControlledVocabService {

  ControlledVocabElement[] getControlledVocab(Long entityOID, int vocabIndex)
    throws NoData;
}
