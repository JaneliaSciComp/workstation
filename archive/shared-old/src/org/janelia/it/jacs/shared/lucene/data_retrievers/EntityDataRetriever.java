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
 * User: saffordt
 * Date: 1/27/12
 * Time: 12:58 PM
 */
public class EntityDataRetriever extends LuceneDataRetrieverBase {

    public String getDatabaseDocumentTableName() {
        return "entity";
    }

    public String getSQLQueryForDocumentData() {
        return "select id, entity_type_id, user_id, name from "+getDatabaseDocumentTableName();
    }

    public List<Document> extractDocumentsFromResultSet() throws SQLException {
        List<Document> docList = new LinkedList<Document>();
        while (rs.next()) {
            Document doc = new Document();
            doc.add(new Field("id", getStringFromResult(rs, 1), Field.Store.YES, Field.Index.NO));
            doc.add(new Field("entity_type", getStringFromResult(rs,2), Field.Store.NO, Field.Index.TOKENIZED));
            doc.add(new Field("owner_key", getStringFromResult(rs,3), Field.Store.YES, Field.Index.TOKENIZED));
            doc.add(new Field("name", getStringFromResult(rs, 4), Field.Store.YES, Field.Index.TOKENIZED));
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
                doc.add(new Field("id", split[0], Field.Store.YES, Field.Index.NO));
                doc.add(new Field("entity_type", split[1], Field.Store.YES, Field.Index.TOKENIZED));
                doc.add(new Field("owner_key", split[2], Field.Store.YES, Field.Index.TOKENIZED));
                doc.add(new Field("name", split[3], Field.Store.YES, Field.Index.TOKENIZED));
                writer.addDocument(doc);
                totalRecordsProcessed++;
            }
        }
        finally {
            scanner.close();
        }
    }
}
