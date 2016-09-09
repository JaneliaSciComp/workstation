
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
public class ProteinsDataRetriever extends LuceneDataRetrieverBase {

    public String getDatabaseDocumentTableName() {
        return "protein_detail";
    }

    public String getSQLQueryForDocumentData() {
        return "select protein_id, protein_acc, orf_acc, defline, external_source, external_acc," +
                "ncbi_gi_number, taxon_names, protein_function, gene_symbol, replace(replace(gene_ontology,E'\n',' '),E'\t',' ')," +
                "replace(replace(enzyme_commission,E'\n',' '),E'\t',' '), 'the' from " + getDatabaseDocumentTableName();
    }

    public List<Document> extractDocumentsFromResultSet() throws SQLException {
        return new LinkedList<Document>();
    }

    public void processDocumentsFromDbFile(File dbDumpFile, IndexWriter writer) throws IOException {
        Scanner scanner = new Scanner(dbDumpFile);
        try {
            while (scanner.hasNextLine()) {
                String[] split = scanner.nextLine().split("\t");
                Document doc = new Document();
                doc.add(new Field("oid", split[0], Field.Store.YES, Field.Index.NO));
                doc.add(new Field("accession", split[1], Field.Store.YES, Field.Index.TOKENIZED));
//                doc.add(new Field("orf_acc", split[2], Field.Store.YES, Field.Index.NO));
//                doc.add(new Field("defline", split[3], Field.Store.YES, Field.Index.NO));
//                doc.add(new Field("external_source", split[4], Field.Store.YES, Field.Index.NO));
//                doc.add(new Field("external_acc", split[5], Field.Store.YES, Field.Index.NO));
//                doc.add(new Field("ncbi_gi_number", split[6], Field.Store.YES, Field.Index.NO));
//                doc.add(new Field("taxon_names", split[7], Field.Store.YES, Field.Index.NO));
//                doc.add(new Field("protein_function", split[8], Field.Store.YES, Field.Index.NO));
//                doc.add(new Field("gene_symbol", split[9], Field.Store.YES, Field.Index.NO));
//                doc.add(new Field("gene_ontology", split[10], Field.Store.YES, Field.Index.NO));
//                doc.add(new Field("enzyme_commission", split[11], Field.Store.YES, Field.Index.NO));
                doc.add(new Field("content", split[1] + " " + split[2] + " " + split[3] + " " + split[4] + " " + split[5] + " " +
                        split[6] + " " + split[7] + " " + split[8] + " " + split[9] + " " + split[10] + " " + split[11],
                        Field.Store.NO, Field.Index.TOKENIZED));
                writer.addDocument(doc);
                totalRecordsProcessed++;

                // Don't let the index file get too huge!!!
                if (totalRecordsProcessed % 5000000 == 0) {
                    System.out.println("\n\nOptimizing " + totalRecordsProcessed / 1000000 + " million...\n\n");
                    writer.optimize();
                }
            }
        }
        finally {
            if (null != scanner) {
                scanner.close();
            }
        }
    }
}
