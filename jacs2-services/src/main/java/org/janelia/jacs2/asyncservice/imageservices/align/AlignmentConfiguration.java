package org.janelia.jacs2.asyncservice.imageservices.align;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "AlignmentPipelineConfigurations")
@XmlAccessorType(XmlAccessType.FIELD)
public class AlignmentConfiguration {

    public static class Templates {
        @XmlElement(name = "atlasFBTX")
        public String fbTxAtlas;
        @XmlElement(name = "tgtFBTX")
        public String fbTx;
        @XmlElement(name = "tgtFBTXmarkers")
        public String fbTxMarkers;
        @XmlElement(name = "tgtFBFX")
        public String fbFx;
        @XmlElement(name = "tgtFBFXmarkers")
        public String fbFxMarkers;
        @XmlElement(name = "tgtFBSXIS")
        public String fbSxIs;
        @XmlElement(name = "tgtFBSXISmarkers")
        public String fbSxIsMarkers;
        @XmlElement(name = "tgtFBSXAS")
        public String fbSxAs;
        @XmlElement(name = "tgtFBSXASmarkers")
        public String fbSxAsMarkers;
        @XmlElement(name = "tgtFBRCTX")
        public String fbCtx;
        @XmlElement(name = "IDENTITYMATRIX")
        public String identityMatrix;
        @XmlElement(name = "tgtFROLSX")
        public String frolSx;
        @XmlElement(name = "FROLCROPMATRIX")
        public String frolCropMatrix;
        @XmlElement(name = "FROLROTMATRIX")
        public String frolTMatrix;
        @XmlElement(name = "FROLINVROTMATRIX")
        public String frolInvRotMatrix;
        @XmlElement(name = "CMPBND")
        public String cmpBnd;
        @XmlElement(name = "ORICMPBND")
        public String orCmpBnd;
        @XmlElement(name = "TMPMIPNULL")
        public String mipNull;
        @XmlElement(name = "LCRMASK")
        public String mask;
        @XmlElement(name = "tgtFBTXDPX")
        public String fbTxDpx;
        @XmlElement(name = "tgtFBSXDPX")
        public String fbSxDpx;
        @XmlElement(name = "tgtFBTXRECDPX")
        public String fbTxRecDpx;
        @XmlElement(name = "tgtFBSXRECDPX")
        public String fbSxRecDpx;
        @XmlElement(name = "tgtFBTXDPXEXT")
        public String fbTxDpxExt;
        @XmlElement(name = "tgtFBSXDPXEXT")
        public String fbSxDpxExt;
        @XmlElement(name = "tgtFBSXDPXSS")
        public String fbSxDpxSs;
        @XmlElement(name = "tgtFBSXRECDPXSS")
        public String fbSxRecDpxSs;
        @XmlElement(name = "tgtFBSXRECDPXRS")
        public String fbSxRecDpxRS;
        @XmlElement(name = "tgtMFBTXDPX")
        public String mfbTxDpx;
        @XmlElement(name = "tgtMFBTXRECDPX")
        public String mfbTxRecDpx;
        @XmlElement(name = "tgtMFBTXDPXEXT")
        public String mfbTxDpxExt;
        @XmlElement(name = "tgtMFBSXDPX")
        public String mfbSxDpx;
        @XmlElement(name = "tgtMFBSXRECDPX")
        public String mfbSxRecDpx;
        @XmlElement(name = "tgtMFBSXDPXEXT")
        public String mfbSxDpxExt;
        @XmlElement(name = "tgtVNC20xAFemale")
        public String fVnc20xA;
        @XmlElement(name = "tgtVNC20xAMale")
        public String mVnc20xA;
        @XmlElement(name = "wfYTOA")
        public String fyToa;
        @XmlElement(name = "wfYSXmarkers")
        public String fySxMarkers;
        @XmlElement(name = "wfASXmarkers")
        public String faSxMarkers;
        @XmlElement(name = "tgtCBMCFO")
        public String cbmCfo;
        @XmlElement(name = "tgtCBMCFOEXT")
        public String cbmCfoExt;
    }

    public static class Toolkits {
        @XmlElement(name = "ANTS")
        public String ants;
        @XmlElement(name = "FSLFLIRT")
        public String flirt;
        @XmlElement(name = "WARP")
        public String warpMultiTransform;
        @XmlElement(name = "SMPL")
        public String resample;
        @XmlElement(name = "ANTSMT")
        public String registration;
        @XmlElement(name = "WARPMT")
        public String transform;
        @XmlElement(name = "CNVT")
        public String convertTransformation;
        @XmlElement(name = "Vaa3D")
        public String vaa3d;
        @XmlElement(name = "JBA")
        public String brainAligner;
        @XmlElement(name = "MAGICK")
        public String imageMagick;
        @XmlElement(name = "CMTK")
        public String cmtk;
        @XmlElement(name = "Fiji")
        public String fiji;
        @XmlElement(name = "VNCScripts")
        public String vncScripts;
    }

    public static class Misc {
        @XmlElement(name = "REFNO")
        public int refNo;

        @XmlElement(name = "VSZX_20X_IS")
        public double vSzIsX20x;
        @XmlElement(name = "VSZY_20X_IS")
        public double vSzIsY20x;
        @XmlElement(name = "VSZZ_20X_IS")
        public double vSzIsZ20x;

        @XmlElement(name = "VSZX_63X_IS")
        public double vSzIsX63x;
        @XmlElement(name = "VSZY_63X_IS")
        public double vSzIsY63x;
        @XmlElement(name = "VSZZ_63X_IS")
        public double vSzIsZ63x;

        @XmlElement(name = "VSZX_63X_AS")
        public double vSzAsX63x;
        @XmlElement(name = "VSZY_63X_AS")
        public double vSzAsY63x;
        @XmlElement(name = "VSZZ_63X_AS")
        public double vSzAsZ63x;

        @XmlElement(name = "VSZX_20X_IS_DPX")
        public double vSzIsDpX20x;
        @XmlElement(name = "VSZY_20X_IS_DPX")
        public double vSzIsDpY20x;
        @XmlElement(name = "VSZZ_20X_IS_DPX")
        public double vSzIsDpZ20x;

        @XmlElement(name = "VSZX_63X_IS_DPX")
        public double vSzIsDpX63x;
        @XmlElement(name = "VSZY_63X_IS_DPX")
        public double vSzIsDpY63x;
        @XmlElement(name = "VSZZ_63X_IS_DPX")
        public double vSzIsDpZ63x;
    }

    @XmlElement(name = "Templates")
    public Templates templates = new Templates();
    @XmlElement(name = "Toolkits")
    public Toolkits toolkits = new Toolkits();
    @XmlElement(name = "MISC")
    public Misc misc = new Misc();
}

