
package org.janelia.it.jacs.web.control.image;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
//import org.janelia.it.jacs.model.user_data.recruitment.RecruitmentResultFileNode;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

/**
 * Spring controller to return a tab-delimited text version of a GWT SortableTable.  The table sends its data to
 * this servlet, which streams it back to the browser so the user can save as a CSV or tab-delimited file.
 *
 * @author Michael Press
 */
public class TiledImageRetriever implements Controller {

    private static Logger logger = Logger.getLogger(TiledImageRetriever.class.getName());

    //    public static final String USER_PARAM = "user";
    public static final String X_PARAM = "x";
    public static final String Y_PARAM = "y";
    public static final String Z_PARAM = "z";
    public static final String NODE_OWNER = "nodeOwner";
    public static final String NODE_ID_PARAM = "nodeId";
    public static final String X_AXIS_PARAM = "xAxis";
    public static final String Y_AXIS_PARAM = "yAxis";
    public static final String CENTER_PARAM = "center";
    public static final String LEGEND_PARAM = "legend";
    private String filestoreLoc = SystemConfigurationProperties.getString("FileStore.CentralDir");
    private String imageExtension = SystemConfigurationProperties.getString("RecruitmentViewer.ImageExtension");

    /**
     * Implementation for Controller. Accepts data to export from the client, and resends on the HttpResponse
     */
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
//        logger.debug("DownloadTableController.handleRequest()");
        // Get the data stored in the session
//        String user   = User.SYSTEM_USER_LOGIN;
        String nodeOwner = request.getParameter(NODE_OWNER);
        String nodeId = request.getParameter(NODE_ID_PARAM);
        String x = request.getParameter(X_PARAM);
        String y = request.getParameter(Y_PARAM);
        String z = request.getParameter(Z_PARAM);
        String xAxis = request.getParameter(X_AXIS_PARAM);
        String yAxis = request.getParameter(Y_AXIS_PARAM);
//        String legend = request.getParameter(LEGEND_PARAM);

        String filenameBase = new StringBuffer(filestoreLoc).append(File.separator).
                append(nodeOwner).append(File.separator).append(RecruitmentResultFileNode.SUB_DIRECTORY).
                append(File.separator).append(nodeId).append(File.separator).append("zoomlevel").
                append(z).append(File.separator).toString();
        String filename;
        if ("true".equals(xAxis)) {
            filename = filenameBase + X_AXIS_PARAM + "Tile" + x + "_" + y + "_" + z + imageExtension;
        }
        else if ("true".equals(yAxis)) {
            filename = filenameBase + Y_AXIS_PARAM + "Tile" + x + "_" + y + "_" + z + imageExtension;
        }
        else {
            filename = filenameBase + CENTER_PARAM + "Tile" + x + "_" + y + "_" + z + imageExtension;
        }

//        if (logger.isDebugEnabled())
//        logger.info("Get tile:" + filename);
//        if (logger.isDebugEnabled()) logger.debug("nodeId=" + nodeId);

        // Return the image
        doDownload(request, response, filename);

        return null; // Signal: am handling this operation here.
    }

    /**
     * Sends its input to the ServletResponse output stream.
     *
     * @param request  request incoming
     * @param response reference to outgoing response
     * @param filename filename to download
     * @throws IOException      unable to reach the file
     * @throws ServletException error with the servlet execution
     */
    private void doDownload(HttpServletRequest request, HttpServletResponse response, String filename)
            throws IOException, ServletException {
        response.setContentType("image/" + imageExtension.substring(1, imageExtension.length()));
        response.setStatus(200); // "OK"
        response.setHeader("Content-disposition", "inline; filename=" + filename);

        // Output the image to the response
        OutputStream outStream = response.getOutputStream();
        try {
            outStream.write(readFile(request, filename));
            outStream.flush();
        }
        catch (Exception e) {
            logger.error("Error reading file: ", e);
        }
        finally {
            outStream.close();
        }
    }

    /**
     * Read in the file bytes
     *
     * @param request  request incoming
     * @param filename file desired
     * @return byte array of the file
     * @throws IOException error reading file
     */
    private byte[] readFile(HttpServletRequest request, String filename) throws IOException {
        InputStream instream = null;
        byte bytes[] = null;
        try {
            instream = new FileInputStream(filename);
            if (instream != null) {
                bytes = new byte[instream.available()];
                instream.read(bytes);
            }
            else
                logger.warn("File not found: " + filename);
        }
        finally {
            if (instream != null)
                instream.close();
        }
        return bytes;
    }
}
