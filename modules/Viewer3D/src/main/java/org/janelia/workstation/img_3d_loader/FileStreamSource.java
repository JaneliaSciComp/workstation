package org.janelia.workstation.img_3d_loader;

import org.apache.commons.httpclient.methods.GetMethod;

import java.io.InputStream;

/**
 * Created by murphys on 5/18/2016.
 */
public interface FileStreamSource {

    public GetMethod getStreamForFile(String filepath) throws Exception;
}
