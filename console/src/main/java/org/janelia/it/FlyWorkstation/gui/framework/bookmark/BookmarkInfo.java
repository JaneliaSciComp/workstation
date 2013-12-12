package org.janelia.it.FlyWorkstation.gui.framework.bookmark;

import org.janelia.it.FlyWorkstation.gui.framework.navigation_tools.NavigationPath;
import org.janelia.it.FlyWorkstation.shared.preferences.InfoObject;
import org.janelia.it.FlyWorkstation.shared.preferences.PreferenceManager;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

import java.util.Properties;


public class BookmarkInfo extends InfoObject {

    private static final String OID_IDENTIFIER    = "OIDIdentifier";
    private static final String OID_NAMESPACE     = "OIDNamespace";
    private static final String GENOME_VERSION_ID = "GenomeVersionID";
    private static final String NAME              = "Name";
    private static final String SEARCH_VALUE      = "SearchValue";
    private static final String TYPE              = "Type";
    private static final String SPECIES           = "Species";
    private static final String URL_STRING        = "URL";
    private static final String COMMENTS          = "Comments";

    private Long oid;
    private String species="";
    private String searchValue="";
    private String type="";
    private String bookmarkURLText="";
    private String comments="";

    public BookmarkInfo(String keyBase, Properties inputProperties, String sourceFile) {
        int genomeVersionID;
        String oidIdentifier;
        String oidNamespace;

        this.keyBase=keyBase;
        this.sourceFile=sourceFile;
        String tmpString;

        tmpString = inputProperties.getProperty(keyBase+"."+SEARCH_VALUE);
        if (tmpString!=null) searchValue=tmpString;
        else searchValue="Unknown";

        tmpString = inputProperties.getProperty(keyBase+"."+TYPE);
        if (tmpString!=null) type=tmpString;
        else type="Unknown";

        tmpString = inputProperties.getProperty(keyBase+"."+SPECIES);
        if (tmpString!=null) species=tmpString;
        else species="Unknown";

        tmpString = inputProperties.getProperty(keyBase+"."+URL_STRING);
        if (tmpString!=null) bookmarkURLText=tmpString;
        else bookmarkURLText="Unknown";

        tmpString = inputProperties.getProperty(keyBase+"."+COMMENTS);
        if (tmpString!=null) comments=tmpString;
        else comments="Unknown";

        tmpString = inputProperties.getProperty(keyBase+"."+OID_IDENTIFIER);
        if (tmpString!=null) oidIdentifier=tmpString;
        else oidIdentifier="Unknown";

        tmpString = inputProperties.getProperty(keyBase+"."+OID_NAMESPACE);
        if (tmpString!=null) oidNamespace=tmpString;
        else oidNamespace="Unknown";

        tmpString = inputProperties.getProperty(keyBase+"."+GENOME_VERSION_ID);
        if (tmpString!=null) {
            Integer tmpInt = new Integer(tmpString);
            genomeVersionID=tmpInt.intValue();
        }
        else genomeVersionID=0;

        tmpString = inputProperties.getProperty(keyBase+"."+NAME);
        if (tmpString!=null) name=tmpString;
        else name="Unknown";

        this.oid = new Long(oidIdentifier);
    }


    public BookmarkInfo(Entity bookmarkEntity) {
        this.oid=bookmarkEntity.getId();
        getBookmarkAttributesForEntity(bookmarkEntity);
        this.name = this.oid.toString();
        this.species = "Drosophila Melanogaster";/*bookmarkEntity.getGenomeVersion().getSpecies().toString();*/
        this.keyBase= PreferenceManager.getKeyForName(name, true);
        this.bookmarkURLText = getURLForEntity(bookmarkEntity);
    }


    // This constructor should only be used for the clone.
    private BookmarkInfo(String keyBase, String name, String sourceFile, Long oid,
                         String bookmarkURLText, String comments, String searchValue, String type,
                         String species){
        this.oid = oid;
        this.keyBase = keyBase;
        this.name = name;
        this.searchValue = searchValue;
        this.type = type;
        this.species = species;
        this.sourceFile = sourceFile;
        this.bookmarkURLText = bookmarkURLText;
        this.comments = comments;
    }


    public Long getId() {
        return oid;
    }


    public String getComments() { return comments; }
    void setComments(String newComments) {
        isDirty=true;
        if (newComments!=null) this.comments = newComments;
    }


    public String getDisplayName() {
        return type + ":     " + searchValue + "     " + oid.toString();
    }


    /**
     * @todo Should this be an official URL class instead of a string?
     * No big deal to change it later as it will get constructed from properties
     * and written to properties anyway.
     */
    public String getBookmarkURLText() { return bookmarkURLText; }
    void setBookmarkURLText(String bookmarkURLText) {
        isDirty=true;
        if (bookmarkURLText!=null) this.bookmarkURLText = bookmarkURLText;
    }


    /**
     * This method returns a classical URL bookmark to the calling class and allows
     * for external navigation the feature in question.  This mechanism uses the OID
     * search as the OID is unique to a genome version.
     */
    public static String getURLForEntity(Entity ge) {
        return "http://localhost:30000/?action=search&unknown_oid="+ge.getId()+"&redir=204";
    }


    public String getSpecies() { return species; }
    void setSpecies(String species) {
        isDirty = true;
        this.species = species;
    }

    public String getBookmarkType() { return type; }
    void setBookmarkType(String type) {
        isDirty = true;
        this.type = type;
    }

    public String getSearchValue() { return searchValue; }
    void setSearchValue(String searchValue) {
        isDirty = true;
        this.searchValue = searchValue;
    }

    public String toString() {
        return getDisplayName();
    }


    public Object clone() {
        BookmarkInfo tmpInfo = new BookmarkInfo(this.keyBase, this.name, this.sourceFile,
                oid, this.bookmarkURLText, this.comments, this.searchValue,
                this.type, this.species);
        return tmpInfo;
    }


    public String getKeyName(){
        return "Bookmark." + keyBase;
    }


    /**
     * This method is so the object will provide the formatted properties
     * for the writeback mechanism.
     */
    public Properties getPropertyOutput(){
        Properties outputProperties=new Properties();
        String key = getKeyName()+".";

        outputProperties.put(key+NAME,name);
        outputProperties.put(key+SEARCH_VALUE,searchValue);
        outputProperties.put(key+TYPE,type);
        outputProperties.put(key+SPECIES,species);
        outputProperties.put(key+URL_STRING, bookmarkURLText);
        outputProperties.put(key+COMMENTS, comments);
        outputProperties.put(key+OID_IDENTIFIER, oid.toString());

        return outputProperties;
    }


    /**
     * This method provides the name to use for the bookmark menu item and any other
     * request to create a bookmark.
     */
    private void getBookmarkAttributesForEntity(Entity ge) {
//        if (ge instanceof Species) {
//            type = "Species";
//            if (ge.getDisplayName()!=null && !ge.getDisplayName().equals(""))
//                searchValue = ge.getDisplayName();
//        }
//        else if (ge instanceof Chromosome) {
//            type = "Chromosome";
//            if (ge.getDisplayName()!=null && !ge.getDisplayName().equals(""))
//                searchValue = ge.getDisplayName();
//        }
//        else if (ge instanceof GenomicAxis) {
//            type = "Genomic Axis";
//            if (ge.getDisplayName()!=null && !ge.getDisplayName().equals(""))
//                searchValue = ge.getDisplayName();
//        }
//        else if (ge instanceof Contig) {
//            type = "Contig";
//            searchValue = "Contig";
//        }
//        else if (ge instanceof CuratedGene) {
//            type = "Gene";
//            if (propertyExists(GeneFacade.GENE_ACCESSION_PROP, ge))
//                searchValue = ge.getProperty(GeneFacade.GENE_ACCESSION_PROP).getInitialValue();
//            else if (propertyExists(FeatureFacade.GROUP_TAG_PROP, ge))
//                searchValue = ge.getProperty(FeatureFacade.GROUP_TAG_PROP).getInitialValue();
//        }
//        else if (ge instanceof CuratedTranscript) {
//            type = "Transcript";
//            if (propertyExists(TranscriptFacade.TRANSCRIPT_ACCESSION_PROP, ge)) {
//                searchValue = ge.getProperty(TranscriptFacade.TRANSCRIPT_ACCESSION_PROP).getInitialValue();
//            }
//            else if (propertyExists(FeatureFacade.GROUP_TAG_PROP, ge))
//                searchValue = ge.getProperty(FeatureFacade.GROUP_TAG_PROP).getInitialValue();
//        }
//        else if (ge instanceof Feature) {
//            type = "Feature";
//            if (propertyExists(FeatureFacade.GROUP_TAG_PROP, ge))
//                searchValue = ge.getProperty(FeatureFacade.GROUP_TAG_PROP).getInitialValue();
//        }
        searchValue = EntityConstants.TYPE_SAMPLE;
    }

    public NavigationPath getNavigationPath() /*throws InvalidPropertyFormat*/ {
        NavigationPath[] paths = new NavigationPath[0];
//        if (getGenomeVersion()==null) return null;
//        else paths=getGenomeVersion().getNavigationPathsToOIDInThisGenomeVersion(oid);
        return paths.length==0 ? null : paths[0];
    }


//    private GenomeVersion getGenomeVersion() {
//        return ModelMgr.getModelMgr().getGenomeVersionById(oid.getGenomeVersionId());
//    }
}