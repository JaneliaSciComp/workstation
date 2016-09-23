
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
public class SamplesDataRetriever extends LuceneDataRetrieverBase {

    public String getDatabaseDocumentTableName() {
        return "bio_sample";
    }

    public String getSQLQueryForDocumentData() {
        return "select sample_id, sample_acc, sample_name, sample_title, " +
                "appendrows('select coalesce(region,\\'\\')||'' ''||coalesce(location,\\'\\')||'' ''||coalesce(habitat_type,\\'\\')||'' ''" +
                "||coalesce(country,\\'\\')||'' ''||coalesce(host_organism,\\'\\')||'' ''||coalesce(host_details,\\'\\') from sample_site where sample_id='||sample_id, ' '), 'the' " +
                "from " + getDatabaseDocumentTableName();
    }

    public List<Document> extractDocumentsFromResultSet() throws SQLException {
        List<Document> docList = new LinkedList<Document>();
        while (rs.next()) {
            Document doc = new Document();
            doc.add(new Field("oid", getStringFromResult(rs, 1), Field.Store.YES, Field.Index.NO));
            doc.add(new Field("accession", getStringFromResult(rs, 2), Field.Store.YES, Field.Index.TOKENIZED));
            doc.add(new Field("name", getStringFromResult(rs, 3), Field.Store.YES, Field.Index.TOKENIZED));
            doc.add(new Field("title", getStringFromResult(rs, 4), Field.Store.YES, Field.Index.TOKENIZED));
            doc.add(new Field("appendrows", getStringFromResult(rs, 5), Field.Store.YES, Field.Index.TOKENIZED));
            doc.add(new Field("content", getStringFromResult(rs, 2) + " " + getStringFromResult(rs, 3) + " " +
                    getStringFromResult(rs, 4) + " " + getStringFromResult(rs, 5), Field.Store.NO, Field.Index.TOKENIZED));
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
                doc.add(new Field("oid", split[0], Field.Store.YES, Field.Index.NO));
                doc.add(new Field("accession", split[1], Field.Store.YES, Field.Index.TOKENIZED));
                doc.add(new Field("name", split[2], Field.Store.YES, Field.Index.TOKENIZED));
                doc.add(new Field("title", split[3], Field.Store.YES, Field.Index.TOKENIZED));
                doc.add(new Field("appendrows", split[4], Field.Store.YES, Field.Index.TOKENIZED));
                doc.add(new Field("content", split[1] + " " + split[2] + " " +
                        split[3] + " " + split[4], Field.Store.NO, Field.Index.TOKENIZED));
                writer.addDocument(doc);
                totalRecordsProcessed++;
            }
        }
        finally {
            scanner.close();
        }
    }
}