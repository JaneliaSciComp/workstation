package org.janelia.jacs2.asyncservice.sampleprocessing.zeiss;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class LSMMetadataTest {
    private static final String TEST_FILE = "src/test/resources/testdata/FLPO_20160121130448632_61713.lsm.json";

    @Test
    public void readFromFile() throws IOException {
        File testFile = new File(TEST_FILE);
        LSMMetadata lsmMetadata = LSMMetadata.fromFile(testFile);
        assertNotNull(lsmMetadata);
        assertThat(lsmMetadata.getStack(), equalTo("FLPO_20160121130448632_61713.lsm"));
        assertNotNull(lsmMetadata.getRecording());
        assertThat(lsmMetadata.getTracks().get(0).getBeamSplitters(), hasSize(3));
        assertThat(lsmMetadata.getTracks().get(0).getDataChannels(), hasSize(1));
    }
}
