
package src.org.janelia.it.jacs.shared.lucene;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.varia.NullAppender;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import src.org.janelia.it.jacs.shared.lucene.searchers.LuceneSearcher;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: May 30, 2008
 * Time: 9:22:39 AM
 */
public class SearchLuceneCmd {

    private static Logger _logger = Logger.getLogger(SearchLuceneCmd.class);

    private String category;
    private Long resultID;
    private String indexRootPath;
    private File outputFile;
    private String searchTerms;
    private int maxSearchResultSet;

    public Long getResultID() {
        return resultID;
    }

    public void setResultID(String resultID) throws Exception {
        try {
            this.resultID = Long.parseLong(resultID);
        }
        catch (NumberFormatException e) {
            throw new Exception("SearchLuceneCmd: '" + resultID + "' is invalid result ID");
        }
    }

    public int getMaxSearchResultSet() {
        return maxSearchResultSet;
    }

    public void setMaxSearchResultSet(String maxNum) throws Exception {
        try {
            this.maxSearchResultSet = Integer.parseInt(maxNum);
        }
        catch (NumberFormatException e) {
            throw new Exception("SearchLuceneCmd: '" + maxNum + "' is invalid maximum result set value");
        }
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getIndexRootPath() {
        return indexRootPath;
    }

    public void setIndexRootPath(String indexRootPath) {
        this.indexRootPath = indexRootPath;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    public String getSearchTerms() {
        return searchTerms;
    }

    public void setSearchTerms(String searchTerms) {
        this.searchTerms = searchTerms;
    }

    private void processParams(String[] args) throws Exception {
        if (args.length < 6) // missing parameters
        {
            throw new Exception("SearchLuceneCmd: Invalid number of arguments");
        }

        // first param must be a category
        setCategory(args[0]);

        // second param must be a max result set requested
        setMaxSearchResultSet(args[1]);

        // third param must be a result ID
        setResultID(args[2]);

        // fourth param must be an index root path
        File file = new File(args[3]);
        if (file.canRead() && file.isDirectory()) {
            setIndexRootPath(args[3]);
        }
        else {
            throw new Exception("SearchLuceneCmd: '" + args[3] + "' is not a valid/readable index root");
        }

        // fifth param must be an output file name (full path)
        file = new File(args[4]);
        if (file.createNewFile()) {
            setOutputFile(file);
        }
        else {
            throw new Exception("SearchLuceneCmd: '" + args[4] + "' is not a valid/writable output file");
        }

        // remaining params are search terms
        StringBuffer termBuffer = new StringBuffer();
        for (int i = 5; i < args.length; i++) {
            termBuffer.append(args[i]).append(' ');
        }
        String s = termBuffer.toString().trim();
        if (s.length() > 0) {
            setSearchTerms(s);
        }
        else {
            throw new Exception("SearchLuceneCmd: empty search terms");
        }

    }

    private void doSearch(LuceneSearcher searcher) throws Exception {
        TopDocs topDocs = searcher.search(searchTerms, maxSearchResultSet);
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        _logger.debug(category + ": found " + scoreDocs.length + " lucene hits");
        if (scoreDocs.length > 0) {
            // using file descriptor to ensure that file is written to a disk before we are done
//            FileOutputStream os = new FileOutputStream(outputFile);
//            FileDescriptor fd = os.getFD();
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
            int cnt;
            try {
//            long outTime = 0, iterTime = 0;
//            Date one, two;
                cnt = 0;
                for (ScoreDoc scoreDoc : scoreDocs) {
                    //                    if (cnt==MAX_SEARCH_RESULT_SET) { break; }
                    //                one = new Date();
                    String outStr = searcher.writeRecordForDoc(scoreDoc, resultID);
                    //                two = new Date();
                    //                iterTime += two.getTime() - one.getTime();

                    out.println(outStr);
                    //                one = new Date();
                    //                outTime += one.getTime() - two.getTime();
                    // If we hit the max search limit stop.  The best scored hits are returned.
                    cnt++;
                }
            }
            finally {
                // close stream
                out.flush();
//            fd.sync();
                out.close();
            }
            System.out.println(cnt);
            _logger.debug(category + ": finished writing " + cnt + " hits to the file ");
//            _logger.debug(category + ": hit iteration time: " + iterTime + " msec; IO time: " + outTime + " msec");
        }
    }

    /**
     * Gets called by dma.sh with various arguments
     *
     * @param args command line: category resultID indexRootPath outputFileName searchterms
     * @see IndexerArgs
     */
    public static void main(String[] args) {
        // diasble log4j
        String initOverride = System.getProperty("log4j.defaultInitOverride");
        if ("true".equals(initOverride)) {
            LogManager.resetConfiguration();
            LogManager.getRootLogger().addAppender(new NullAppender());
        }

        SearchLuceneCmd luceneCmd = new SearchLuceneCmd();
        try {
            // process input param
            luceneCmd.processParams(args);
            LuceneSearcher searcher = LuceneDataFactory.getDocumentSearcher(luceneCmd.getCategory());
            if (searcher == null) {
                throw new Exception("SearchLuceneCmd: '" + luceneCmd.getCategory() + "' is invalid category");
            }
            luceneCmd.doSearch(searcher);
        }
        catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

}
