
package src.org.janelia.it.jacs.shared.lucene.data_retrievers;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Mar 5, 2008
 * Time: 6:02:14 PM
 */
public abstract class LuceneDataRetrieverBase {

    protected Logger _logger = Logger.getLogger(this.getClass());

    public static final int TEST_SET_SIZE = 51981;
    protected int setSize = TEST_SET_SIZE; // default
    protected int numOfDbChunks;
    protected int dbChunk;
    protected int offset;
    protected long testId;
    protected long initTestId;
    protected long totalRecordsProcessed;

    protected ResultSet rs = null;
    protected PreparedStatement statement = null;
    protected Connection connection = null;

    public void DataRetriever() {
        Date dt = new Date();
        initTestId = dt.getTime();
        testId = initTestId;
    }

    public int getSetSize() {
        return setSize;
    }

    public void setSetSize(int setSize) {
        this.setSize = setSize;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public Integer processNextResultChunk(int chunkSize, IndexWriter writer) {
        if (this.connection == null) {
            return null;
        }

        try {
            // The first execution, set up the statement and number of loops
            if (this.dbChunk == 0) {
                this.numOfDbChunks = getChunksCount(chunkSize);
            }

            // If not exceeding the total number of chunks, get data
            if (this.dbChunk < this.numOfDbChunks) {
                String sql = getSQLQueryForDocumentData();
                this.statement = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                System.out.println("Executing SQL chunk " + this.dbChunk);
                this.rs = statement.executeQuery();
                System.out.println("\texecuteQuery completed");
                List<Document> list = extractDocumentsFromResultSet();
                totalRecordsProcessed += list.size();
                for (Document doc : list) {
                    writer.addDocument(doc);
                }
                System.out.println("\tProcessed " + totalRecordsProcessed + " records so far. Just constructed " + list.size() + " documents");
                done();
                offset += chunkSize;
                dbChunk++;
                return list.size();
            }
            // no more to return
            else {
                return 0;
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private int getChunksCount(int chunkSize) throws SQLException {
        String sql = "select count(*) from " + getDatabaseDocumentTableName() + " ";
        Statement st = connection.createStatement();
        ResultSet r = st.executeQuery(sql);
        int cnt = 0;
        if (r.next()) {
            cnt = r.getInt(1);
        }
        else {
            System.out.println("====ERROR: Unable to retrieve max chunks from DB====");
        }
        r.close();
        st.close();
        if (cnt / chunkSize <= 1) {
            return 1;
        }
        else {
            return cnt / chunkSize;
        }
    }

    abstract public String getDatabaseDocumentTableName();

    abstract public String getSQLQueryForDocumentData();

    abstract public List<Document> extractDocumentsFromResultSet() throws SQLException;

    public String getStringFromResult(ResultSet result, int index) throws SQLException {
        String tmpString = result.getString(index);
        if (null == tmpString) {
            tmpString = "";
        }
        return tmpString;
    }

    public String getStringFromSplit(String piece) throws SQLException {
        if (null == piece) {
            return "";
        }
        return piece;
    }

    private void done() {
        try {
            if (rs != null)
                rs.close();
            if (statement != null)
                statement.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public long getTotalRecordsProcessed() {
        return totalRecordsProcessed;
    }

    public void executeDbDump(String dumpFilePath) throws SQLException {
//        String sql = "select writequeryresultstodisk(?,?,'\t')";
//        String sql = "? >> ?";
        SystemCall call = new SystemCall();
        try {
            String tmpQuery = "mysql -u val_flyportalApp -pv@l_flyp0rt@lApp -h clustrix flyportal -e "+
                    "\"select id, entity_type_id, user_id, name from entity;\" >> "+dumpFilePath;
            System.out.println(tmpQuery);
            call.emulateCommandLine(tmpQuery, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
//        PreparedStatement pstmt = connection.prepareStatement(sql);
//        pstmt.setString(1, getSQLQueryForDocumentData());
//        pstmt.setString(2, dumpFilePath);
//        pstmt.execute();
    }

    abstract public void processDocumentsFromDbFile(File dbDumpFile, IndexWriter writer) throws IOException;

    public void deleteDbDumpFile(String dumpFileName) {
        new File(dumpFileName).delete();
    }
}
