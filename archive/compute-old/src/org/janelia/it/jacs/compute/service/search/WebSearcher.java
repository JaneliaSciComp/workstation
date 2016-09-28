
package src.org.janelia.it.jacs.compute.service.search;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.search.SearchTask;
import org.janelia.it.jacs.model.user_data.search.SearchResultNode;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Nov 13, 2007
 * Time: 3:30:30 PM
 */

public class WebSearcher {
    public static final String WEBSEARCHURL_PROPERTY = "WebSiteSearchURL";
    private static final String DEFAULT_ENCODING = "ISO-8859-1";
    private static final String RECORD_TAG = "record";
    private static final String TITLE_TAG = "title";
    private static final String BLURB_TAG = "blurb";

//    private static final boolean DEVELOPMENT_TESTING=false;

    Logger _logger = Logger.getLogger(this.getClass());
    private Connection _connection;

    private class DrupalSearchResultDocHandler extends DefaultHandler {
        private SearchResultNode _currentResultNode;
        private StringBuffer _parserCharBuffer;
        private int _currentRecordNo;
        private String _currentTitle;
        private String _currentBlurb;
        private String _resultInsertSql =
                "insert into website_ts_result " +
                        "( node_id, docid, accession, docname, headline ) " +
                        "values ( ?,?,?,?,? )";
        private PreparedStatement _statement = null;

        DrupalSearchResultDocHandler(SearchTask currentTask) {
            _currentResultNode = currentTask.getSearchResultNode();
            _parserCharBuffer = new StringBuffer();
            _currentRecordNo = 0;
        }

        public int getRecCount() {
            return _currentRecordNo;
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equals(RECORD_TAG)) {
                endRecord();
            }
            else if (qName.equals(TITLE_TAG)) {
                endTitle();
            }
            else if (qName.equals(BLURB_TAG)) {
                endBlurb();
            }
        }

        public void characters(char ch[], int start, int length) throws SAXException {

            _parserCharBuffer.append(ch, start, length);
        }

        private void endBlurb() {
            _currentBlurb = _parserCharBuffer.toString().trim();
            // fix non-latin charachters
            try {
                byte[] bytes = _currentBlurb.getBytes("ISO-8859-1");
                _currentBlurb = new String(bytes, "ISO-8859-1");
            }
            catch (UnsupportedEncodingException e) {
                _currentBlurb = "Text is not retrieved. Please clisk on the link to see original text";
            }
            _parserCharBuffer.setLength(0);
        }

        private void endRecord() {
            if (_currentTitle == null || _currentTitle.length() == 0) {
                // ignore the entry if there's no title
                return;
            }
            try {
                _currentRecordNo++;
                if (_statement == null) {
                    _statement = _connection.prepareStatement(_resultInsertSql);
                }
                //            _logger.debug("createWebResultEntry sql: "+sql);
                _statement.setLong(1, _currentResultNode.getObjectId());
                _statement.setLong(2, _currentResultNode.getObjectId());
                _statement.setString(3, String.valueOf(_currentResultNode.getObjectId()) + "_" +
                        String.valueOf(_currentRecordNo));
                _statement.setString(4, _currentTitle);
                _statement.setString(5, _currentBlurb);
                // invoke the query
                _statement.executeUpdate();
            }
            catch (SQLException e) {
                _logger.error("Unable to add websearch result record to the DB. Title: '"
                        + _currentTitle + "'; Blurb: '" + _currentBlurb + "'", e);

            }
        }

        private void endTitle() {
            _currentTitle = _parserCharBuffer.toString().trim();
            _parserCharBuffer.setLength(0);
        }

        public void endDocument() throws SAXException {
            if (_statement != null) {
                try {
                    _connection.commit();
                    _statement.close();
                }
                catch (SQLException e) {
                    _logger.error("Unable to process websearch results", e);
                }
            }
        }

    }

    public WebSearcher(Connection conn) {
        _connection = conn;
    }

    public int populateSearchResult(SearchTask searchTask)
            throws Exception {
        _logger.debug("Searching category '" + SearchTask.TOPIC_WEBSITE + "'");
        URL webSearchURL = createWebSearchURL(searchTask);
        int recCount = -1;
        if (webSearchURL != null) {
            // the URL is not null
            recCount = readSearchResult(webSearchURL, searchTask);
        }
        _logger.debug(SearchTask.TOPIC_WEBSITE + " search complete. Found " + recCount + " hits");
        return recCount;
    }

    private URL createWebSearchURL(SearchTask st) {
        String webSearchURL;
        try {
            String searchQuery = st.getSearchString();
            webSearchURL = SystemConfigurationProperties.getString(WEBSEARCHURL_PROPERTY, null);
            if (webSearchURL != null && webSearchURL.trim().length() > 0) {
                if (webSearchURL.endsWith(".htm") || webSearchURL.endsWith(".html")) {
                    // with drupal this is only for debugging and development purposes
                    // in production we should throw an exception here
                    //assert(DEVELOPMENT_TESTING);
                    webSearchURL = webSearchURL + "?" +
                            "query" + "=" + URLEncoder.encode(searchQuery, DEFAULT_ENCODING);
                }
                else {
                    if (!webSearchURL.endsWith("/")) {
                        // append the slash if needed
                        webSearchURL = webSearchURL + "/";
                    }
                    webSearchURL = webSearchURL + URLEncoder.encode(searchQuery, DEFAULT_ENCODING);
                }
                return new URL(webSearchURL);
            }
        }
        catch (Exception e) {
            _logger.error("Unexpected error while getting the web search URL", e);
        }
        return null;
    }

    private int readSearchResult(URL searchURL, SearchTask st)
            throws Exception {
        InputStream searchResultStream = null;
        int hitCount = -1;
        try {
            DrupalSearchResultDocHandler resultParser = new DrupalSearchResultDocHandler(st);
            searchResultStream = searchURL.openStream();
            SAXParserFactory parserFactory = SAXParserFactory.newInstance();
            SAXParser saxParser = parserFactory.newSAXParser();
            saxParser.parse(searchResultStream, resultParser);
            hitCount = resultParser.getRecCount();
        }
        finally {
            if (searchResultStream != null) {
                try {
                    searchResultStream.close();
                }
                catch (Exception ignore) {
                }
            }
        }
        return hitCount;
    }

}
