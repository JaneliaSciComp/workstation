
package src.org.janelia.it.jacs.shared.lucene.data_retrievers;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Mar 5, 2008
 * Time: 6:10:32 PM
 */
public class ProjectsDataRetriever extends LuceneDataRetrieverBase {

    public String getDatabaseDocumentTableName() {
        return "project";
    }

    public String getSQLQueryForDocumentData() {
        return "select symbol, replace(replace(description,E'\n',' '),E'\t',' '), principal_investigators, organization, " +
                "email, website_url, name, funded_by, institutional_affiliation , 'the'" +
                "from " + getDatabaseDocumentTableName() + " where released";
    }

    public List<Document> extractDocumentsFromResultSet() throws SQLException {
        List<Document> docList = new LinkedList<Document>();
        while (rs.next()) {
            Document doc = new Document();
            doc.add(new Field("accession", getStringFromResult(rs, 1), Field.Store.YES, Field.Index.NO));
            doc.add(new Field("description", getStringFromResult(rs, 2), Field.Store.YES, Field.Index.TOKENIZED));
            doc.add(new Field("principal_investigators", getStringFromResult(rs, 3), Field.Store.YES, Field.Index.NO));
            doc.add(new Field("organization", getStringFromResult(rs, 4), Field.Store.YES, Field.Index.NO));
            doc.add(new Field("email", getStringFromResult(rs, 5), Field.Store.YES, Field.Index.NO));
            doc.add(new Field("website_url", getStringFromResult(rs, 6), Field.Store.YES, Field.Index.NO));
            doc.add(new Field("name", getStringFromResult(rs, 7), Field.Store.YES, Field.Index.TOKENIZED));
            doc.add(new Field("funded_by", getStringFromResult(rs, 8), Field.Store.YES, Field.Index.NO));
            doc.add(new Field("institutional_affiliation", getStringFromResult(rs, 9), Field.Store.YES, Field.Index.NO));
            doc.add(new Field("content", getStringFromResult(rs, 1) + " " + getStringFromResult(rs, 7) + " " + getStringFromResult(rs, 2), Field.Store.NO, Field.Index.TOKENIZED));
            docList.add(doc);
        }

        return docList;
    }

    public void processDocumentsFromDbFile(File dbDumpFile, IndexWriter writer) throws IOException {
        Scanner scanner = new Scanner(dbDumpFile);
        try {
            while (scanner.hasNextLine()) {
                String[] split = scanner.nextLine().split("\t");
                Document doc = new Document();
                doc.add(new Field("accession", split[0], Field.Store.YES, Field.Index.NO));
                doc.add(new Field("description", split[1], Field.Store.YES, Field.Index.TOKENIZED));
                doc.add(new Field("principal_investigators", split[2], Field.Store.YES, Field.Index.NO));
                doc.add(new Field("organization", split[3], Field.Store.YES, Field.Index.NO));
                doc.add(new Field("email", split[4], Field.Store.YES, Field.Index.NO));
                doc.add(new Field("website_url", split[5], Field.Store.YES, Field.Index.NO));
                doc.add(new Field("name", split[6], Field.Store.YES, Field.Index.TOKENIZED));
                doc.add(new Field("funded_by", split[7], Field.Store.YES, Field.Index.NO));
                doc.add(new Field("institutional_affiliation", split[8], Field.Store.YES, Field.Index.NO));
                doc.add(new Field("content", split[6] + " " + split[1], Field.Store.NO, Field.Index.TOKENIZED));
                writer.addDocument(doc);
                totalRecordsProcessed++;
            }
        }
        finally {
            scanner.close();
        }
    }
}