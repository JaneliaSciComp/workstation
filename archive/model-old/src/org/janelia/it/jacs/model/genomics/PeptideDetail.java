
package org.janelia.it.jacs.model.genomics;

/**
 * Created by IntelliJ IDEA.
 * User: tnabeel
 * Date: Mar 12, 2008
 * Time: 4:26:16 PM
 */
public class PeptideDetail extends BseEntityDetail {

    private Sample sample;

    public Sample getSample() {
        return sample;
    }

    public void setSample(Sample sample) {
        this.sample = sample;
    }
}
