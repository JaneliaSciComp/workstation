package org.janelia.jacs2.asyncservice.imageservices;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "AlignmentPipelineConfigurations")
@XmlAccessorType(XmlAccessType.FIELD)
public class AlignmentConfiguration {

    public static class Templates {
        @XmlElement(name = "atlasFBTX")
        String fbTxAtlas;
        @XmlElement(name = "tgtFBTX")
        String fbTx;
        @XmlElement(name = "tgtFBTXmarkers")
        String fbTxMarkers;
        @XmlElement(name = "tgtFBFX")
        String fbFx;
        @XmlElement(name = "tgtFBFXmarkers")
        String fbFxMarkers;
        @XmlElement(name = "tgtFBSXIS")
        String fbSxIs;
        @XmlElement(name = "tgtFBSXISmarkers")
        String fbSxIsMarkers;
        @XmlElement(name = "tgtFBSXAS")
        String fbSxAs;
        @XmlElement(name = "tgtFBSXASmarkers")
        String fbSxAsMarkers;
        @XmlElement(name = "tgtFBRCTX")
        String fbCtx;
        @XmlElement(name = "IDENTITYMATRIX")
        String identityMatrix;
        @XmlElement(name = "tgtFROLSX")
        String frolSx;
        @XmlElement(name = "FROLCROPMATRIX")
        String frolCropMatrix;
        @XmlElement(name = "FROLROTMATRIX")
        String frolTMatrix;
        @XmlElement(name = "FROLINVROTMATRIX")
        String frolInvRotMatrix;
        @XmlElement(name = "CMPBND")
        String cmpBnd;
        @XmlElement(name = "ORICMPBND")
        String orCmpBnd;
        @XmlElement(name = "TMPMIPNULL")
        String mipNull;
        @XmlElement(name = "LCRMASK")
        String mask;
        @XmlElement(name = "tgtFBTXDPX")
        String fbTxDpx;
        @XmlElement(name = "tgtFBSXDPX")
        String fbSxDpx;
        @XmlElement(name = "tgtFBTXRECDPX")
        String fbTxRecDpx;
        @XmlElement(name = "tgtFBSXRECDPX")
        String fbSxRecDpx;
        @XmlElement(name = "tgtFBTXDPXEXT")
        String fbTxDpxExt;
        @XmlElement(name = "tgtFBSXDPXEXT")
        String fbSxDpxExt;
        @XmlElement(name = "tgtFBSXDPXSS")
        String fbSxDpxSs;
        @XmlElement(name = "tgtFBSXRECDPXSS")
        String fbSxRecDpxSs;
        @XmlElement(name = "tgtFBSXRECDPXRS")
        String fbSxRECDpxRS;
        @XmlElement(name = "tgtMFBTXDPX")
        String mfbTxDpx;
        @XmlElement(name = "tgtMFBTXRECDPX")
        String mfbTxRecDpx;
        @XmlElement(name = "tgtMFBTXDPXEXT")
        String mfbTxDpxExt;
        @XmlElement(name = "tgtMFBSXDPX")
        String mfbSxDpx;
        @XmlElement(name = "tgtMFBSXRECDPX")
        String mfbSxRECDpx;
        @XmlElement(name = "tgtMFBSXDPXEXT")
        String MFBSxDpxEXT;
        @XmlElement(name = "tgtVNC20xAFemale")
        String fVnc20xA;
        @XmlElement(name = "tgtVNC20xAMale")
        String mVnc20xA;
        @XmlElement(name = "wfYTOA")
        String fyToa;
        @XmlElement(name = "wfYSXmarkers")
        String fySxMarkers;
        @XmlElement(name = "wfASXmarkers")
        String faSxMarkers;
        @XmlElement(name = "tgtCBMCFO")
        String cbmCfo;
        @XmlElement(name = "tgtCBMCFOEXT")
        String cbmCfoExt;
    }

    public static class Toolkits {
        @XmlElement(name = "ANTS")
        String ants;
        @XmlElement(name = "FSLFLIRT")
        String flirt;
        @XmlElement(name = "WARP")
        String warpMultiTransform;
        @XmlElement(name = "SMPL")
        String resample;
        @XmlElement(name = "ANTSMT")
        String registration;
        @XmlElement(name = "WARPMT")
        String transform;
        @XmlElement(name = "CNVT")
        String convertTransformation;
        @XmlElement(name = "Vaa3D")
        String vaa3d;
        @XmlElement(name = "JBA")
        String brainAligner;
        @XmlElement(name = "MAGICK")
        String imageMagick;
        @XmlElement(name = "CMTK")
        String cmtk;
        @XmlElement(name = "Fiji")
        String fiji;
        @XmlElement(name = "VNCScripts")
        String vncScripts;
    }

    public static class Misc {
        @XmlElement(name = "REFNO")
        int refNo;

        @XmlElement(name = "VSZX_20X_IS")
        double vSzIsX20x;
        @XmlElement(name = "VSZY_20X_IS")
        double vSzIsY20x;
        @XmlElement(name = "VSZZ_20X_IS")
        double vSzIsZ20x;

        @XmlElement(name = "VSZX_63X_IS")
        double vSzIsX63x;
        @XmlElement(name = "VSZY_63X_IS")
        double vSzIsY63x;
        @XmlElement(name = "VSZZ_63X_IS")
        double vSzIsZ63x;

        @XmlElement(name = "VSZX_63X_AS")
        double vSzAsX63x;
        @XmlElement(name = "VSZY_63X_AS")
        double vSzAsY63x;
        @XmlElement(name = "VSZZ_63X_AS")
        double vSzAsZ63x;

        @XmlElement(name = "VSZX_20X_IS_DPX")
        double vSzIsDpX20x;
        @XmlElement(name = "VSZY_20X_IS_DPX")
        double vSzIsDpY20x;
        @XmlElement(name = "VSZZ_20X_IS_DPX")
        double vSzIsDpZ20x;

        @XmlElement(name = "VSZX_63X_IS_DPX")
        double vSzIsDpX63x;
        @XmlElement(name = "VSZY_63X_IS_DPX")
        double vSzIsDpY63x;
        @XmlElement(name = "VSZZ_63X_IS_DPX")
        double vSzIsDpZ63x;
    }

    @XmlElement(name = "Templates")
    Templates templates;
    @XmlElement(name = "Toolkits")
    Toolkits toolkits;
    @XmlElement(name = "MISC")
    Misc misc;
}

