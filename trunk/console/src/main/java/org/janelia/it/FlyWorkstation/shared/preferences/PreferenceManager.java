package org.janelia.it.FlyWorkstation.shared.preferences;

import java.io.*;
import java.util.*;


/**
 * This is the Generic class constructed to provide support to developers who wish
 * to work with default and user-defined preference objects.  The object definition
 * maintains a "dirty" state and this class manages the lifecycle of the object.
 * The steps of operation are: subclass provides filenames, this class
 * reads default and user property files and organizes keys hierarchially (user overriding default),
 * the subclass defines the factory to create their specific type of InfoObjects and the
 * methods to work with those objects.  This class commits changes or
 * rebuilds the collections as needed, and finally writes out
 * the user-defined properties upon system exit.
 * As example, see the GB ViewPrefMgr.java class.
 */
public abstract class PreferenceManager {

  protected final int DEFAULT = 0;
  private final int GROUP     = 1;
  private final int USER      = 2;

  protected boolean DEBUG=false;

  protected static final String DESCRIPTION         = "Description";
  public final static String GROUP_DIR              = "GROUP_DIR";

  protected ArrayList<PrefMgrListener> listeners = new ArrayList<PrefMgrListener>();
  protected String filenameFilter = ".properties";
  protected String userDirectory = System.getProperty("user.home");

  // These attributes handle the files used as Strings
  protected String defaultFilename = "";
  // Group files are determined by directory and not explicitly set.
  private String groupFilename = "";
  protected String userFilename = "";
  protected String userFileDescription = "No Description.";
  protected boolean userDescriptionChanged = false;

  /**
   * The Master collections of Default, User, and Dirty Info Objects
   * These collections will determine the hierarchy.
   * Default->Group->User->Dirty
   * Dirty is used for the change management.
   */
  protected TreeMap defaultMasterCollection=new TreeMap();
  protected TreeMap groupMasterCollection=new TreeMap();
  protected TreeMap userMasterCollection=new TreeMap();
  protected TreeMap deletedInfos=new TreeMap();
  protected TreeMap dirtyInfos=new TreeMap();

  public PreferenceManager() {}


  /**
   * Prep the source collections to be re-read from scratch.
   * Build the User, and Default collections of Info Objects.
   */
  public void initializeMasterInfoObjects() {
    defaultMasterCollection=new TreeMap(new MyStringComparator());
    groupMasterCollection=new TreeMap(new MyStringComparator());
    userMasterCollection=new TreeMap(new MyStringComparator());
    dirtyInfos=new TreeMap(new MyStringComparator());

    if (DEBUG) System.out.println("Initing and building the group collections.");
    // These orders are important to maintain the hierarchy.
    buildInfoCollection(DEFAULT);
    buildInfoCollection(GROUP);
    buildInfoCollection(USER);
  }


  /**
   * This method takes the group directory provided and finds the appropriate
   * preference file that is to be used.
   */
   public void setGroupPreferenceDirectory(String newDir) {
    if (newDir!=null && !newDir.equals("")) {
      String[] dirFiles = (new File(newDir)).list(new MyFilenameFilter());
      if (dirFiles!=null) {
        try {
          if (dirFiles.length==0) groupFilename="";
          // There should be only one file in the group directory.  This needs
          // to be enforced somehow.
          else groupFilename=newDir + File.separator + dirFiles[0];
        }
        catch (Exception ex) {
          groupFilename = "";
          handlePrefException(ex);
        }
        //  These four actions might need to go into a reset method or something.
        closeOutCurrentUserFile();
        initializeMasterInfoObjects();
        commitChanges(false);
        firePreferencesChangedEvent();
      }
    }
   }


  /**
   * This method compiles the possible data sources for User or Default
   * and passes that information on to populate the Info Objects for the specific
   * Master collection.
   */
  private void buildInfoCollection(int mode) {
    Map targetMasterCollection = new TreeMap();
    String filename = "";
    if (mode==DEFAULT) {
      filename = defaultFilename;
      targetMasterCollection=defaultMasterCollection;
    }
    if (mode==USER) {
      filename = userFilename;
      targetMasterCollection=userMasterCollection;
    }
    if (mode==GROUP) {
      filename=groupFilename;
      targetMasterCollection=groupMasterCollection;
    }


    // In case the filename does not exist.
    if (filename==null || filename.equals("")) return;

    Properties targetProperties = new Properties();
    InputStream targetFile;
    try {
      if (mode==DEFAULT) targetFile = this.getClass().getResourceAsStream(filename);
      else if (mode==USER) {
        File tmpFile = new File(filename);
        if (!tmpFile.exists()) tmpFile.createNewFile();
        targetFile = new FileInputStream(tmpFile);
      }
      else targetFile = new FileInputStream(new File(filename));
      if (DEBUG) System.out.println("Loading Preferences from "+filename);
      targetProperties.load(targetFile);
      if (mode==USER) userFileDescription = targetProperties.getProperty(DESCRIPTION);
      if (userFileDescription==null || userFileDescription.equals("")) {
          userFileDescription = "No Description.";
      }
      populateInfoObjects(targetProperties, targetMasterCollection, filename);
    }
    catch (Exception ex) {
     handlePrefException(ex);
    }
  }


  /**
   * Method pings the system for the necessary settings and uses them to fill
   * out the file locations for default user, group, and the file filter to be used.
   */
   public abstract void setFileParameters();


  /**
   * This method sifts through the Java property keys and puts them in sub-groups by
   * type.  It then passes those Properties objects
   * on to the build methods.  Remember this is for only one source,
   * Default or Group or User, at a time.
   */
   /**
    * Here is the factory method that the user will define to determine how to
    * populate their own InfoObjects.
    */
  protected abstract void populateInfoObjects(Properties allProperties, Map targetMasterCollection,
    String sourceFile);


  /**
   * This method is supposed to hierarchially add info objects to the final
   * collections.  Any deletion should be a cause for remerging; for example, if
   * a user defined info object is removed it could be replaced with a group
   * or default object.
   */
  /**
   * This method should be used by the subclasses to populate their own collections
   * from the items gained during property file load.
   */
  protected abstract void mergeIntoWorkingCollections(Map<String, InfoObject> targetMasterCollection);


  /**
   * This method is intended to be overridden so that the subclass can determine
   * how to notify the user that the default keys cannot be deleted.
   */
  public abstract void handleDefaultKeyOverrideRequest();

  /**
   * This method is intended to be overridden by the subclass.  The subclass will
   * then send all of its proprietary collections to formatOutput() one-by-one.
   */
  protected abstract void writeOutAllCollections(FileWriter writer, String destinationFile);


  /**
   * This method should be overridden in order to properly determine how to
   * handle an error event.
   */
   protected abstract void handleOutputWriteError();


  /**
   * This method should be overridden in order to determine what notification the
   * user receives from the error.
   */
  protected void handlePrefException(Exception ex) {
    ex.printStackTrace();
  }


  /**
   * This helper method looks through the properties handed to it and returns a
   * set of the keys unique by the first string token.
   */
  public static Set getUniqueKeys (Properties inputProperties) {
    Set<String> uniqueKeys = new HashSet<String>();
    for (Enumeration e = inputProperties.propertyNames() ; e.hasMoreElements() ;) {
      String tempKey = ((String)e.nextElement());
      StringTokenizer mainToken = new StringTokenizer(tempKey,".");
      if (tempKey!=null && !"".equals(tempKey)) {
        String value = mainToken.nextToken();
        uniqueKeys.add(value);
      }
    }
    return uniqueKeys;
  }


  /**
   * Clears the Working collections of Info Objects and rebuilds from the
   * Master collections; merging them into their usable state.
   * Before calling: subclasses clear out their specific InfoObject collections.
   */
  public void resetWorkingCollections() {
    if (DEBUG) System.out.println("Resetting the working collections.");
    deletedInfos=new TreeMap(new MyStringComparator());
    if (DEBUG) System.out.println("Merging the collections.");
    mergeIntoWorkingCollections(defaultMasterCollection);
    mergeIntoWorkingCollections(groupMasterCollection);
    mergeIntoWorkingCollections(userMasterCollection);
    mergeIntoWorkingCollections(dirtyInfos);
  }


  /**
   * For now, this method is the all purpose notification that something has
   * happened to a Preference Info Object.  Finer granularity will be added
   * in a subsequent release.
   */
  public void firePreferencesChangedEvent() {
      for (PrefMgrListener listener : listeners) {
          (listener).preferencesChanged();
      }
  }


  /**
   * This method should be used when panels bulk modify Info Objects in the Working
   * collections or when one change is made that should be saved out when the
   * session exits.  If the Working collection has dirty objects that should be cancelled
   * then commitPermanently should be false; true, and they should be written to
   * the Master collection.
   */
   /**
    * @todo Is it necessary to reset the working collections for the Hell of it?
    * Just to ensure any half hearted changes are not propagated?
    */
  public void commitChanges(boolean commitPermanently) {
    if (!commitPermanently) {
      resetWorkingCollections();
    }
    else commitChangesToSourceObjects();
  }


  /**
   * This method changes the preference file being used by the program.
   */
  public void setUserPreferenceFile(String newFilename, String oldDescription) {
    if (!newFilename.equals(userFilename)) {
      setUserFileDescription(oldDescription);
      closeOutCurrentUserFile();
      userFilename = newFilename;
      initializeMasterInfoObjects();
      commitChanges(false);
      firePreferencesChangedEvent();
    }
    else {
      setUserFileDescription(oldDescription);
    }
  }


  /**
   * Obvious method.
   */
  public void setUserFileDescription(String description) {
    if (description!=null && !description.equals("") && !userFileDescription.equals(description)) {
      userFileDescription = description;
      userDescriptionChanged = true;
    }
  }

  /**
   * Obvious method.
   */
  public String getUserFileDescription() {
    return userFileDescription;
  }


  /**
   * Creates a list of possible output files that match this filter.  Perhaps so
   * a subclass can make a user select one to save changes to from a list.
   */
  public ArrayList getAvailableOutputFiles() {
    ArrayList tmpList = new ArrayList();
    File files = new File(userDirectory);
    File[] fileArray = files.listFiles(new MyFilenameFilter());
      for (File aFileArray : fileArray) {
          tmpList.add(aFileArray);
      }
    return tmpList;
  }


  /**
   * This highly-used method toggles a string from the unique-key state to
   * the readable string that the user sees.  The conversion is necessary for
   * keys in Java properties files.
   */
  public static String getKeyForName(String name, boolean formatForOutput) {
    if (formatForOutput) {
      name=name.replace(' ','^');
      name=name.replace('.','$');
      name=name.replace(':','?');
    }
    else {
      name=name.replace('^',' ');
      name=name.replace('$','.');
      name=name.replace('?',':');
    }
    return name;
  }


  /**
   * @todo This class has to be overridden in the subclasses in order for them to work
   * yet this is not forced.  Need an abstract class to make this work better.
   */
  /**
   * This is the private method that does all the real committing work.
   * First, Info Objects added to the deletedInfo collection attempt to be
   * removed from the Master collection that has influence in the hierarchy.
   * Deletion checks uncommitted Info's first.  Then User->Group->Default.
   * As it proceeds, it keeps track of which sourcefile will need to be written
   * back to because of deletion.  After this, it calls
   * handleDirtyOrUnknownInfoObjects() on the Working collections.
   * Once done, it resets the Working collections.
   */
  protected void commitChangesToSourceObjects() {
    // First remove the unwanteds.  Only need to check top of hierarchy on down.
    // If the object is found in the user map, bingo.
      for (Object o : deletedInfos.keySet()) {
          InfoObject tmpObject = (InfoObject) deletedInfos.get(o);
          String targetName = tmpObject.getKeyName();
          if (dirtyInfos.containsKey(targetName) && tmpObject.getSourceFile().equals("Unknown"))
              dirtyInfos.remove(targetName);
          else if (userMasterCollection.containsKey(targetName)) {
              userMasterCollection.remove(targetName);
          } else if (groupMasterCollection.containsKey(targetName)) {
              groupMasterCollection.remove(targetName);
          } else if (defaultMasterCollection.containsKey(targetName)) handleDefaultKeyOverrideRequest();
          else if (DEBUG) System.out.println("No Master collection has key " + targetName);
      }
    deletedInfos=new TreeMap(new MyStringComparator());
  }


  /**
   * This method goes through the working collection and puts all infos that
   * have changed or have unknown source (new) into the dirty collection.
   * These have hierarchy precendence over all other infos of the same name and
   * will be written out at session exit.
   */
  protected void handleDirtyOrUnknownInfoObjects(TreeMap workingCollection) {
      for (Object o : workingCollection.keySet()) {
          InfoObject tmpObject = (InfoObject) workingCollection.get(o);
          if (tmpObject.hasChanged() || tmpObject.getSourceFile().equals("Unknown"))
              dirtyInfos.put(tmpObject.getKeyName(), tmpObject);
      }
  }


  /**
   * This area takes care of the close out of the current user pref file.
   * Sometimes used at systemWillExit and sometimes used to start the change of
   * user pref files.
   */
  protected void closeOutCurrentUserFile() {
    // Commit any changes to flush out the dirty objects.
    commitChangesToSourceObjects();

    // Set the dirty objects to the selected destination file and push to
    // user Master collection.
    String dirtyInfoDestination = userFilename;
    TreeMap<String, InfoObject> tmpMap = userMasterCollection;

    if (DEBUG) System.out.println("Flushing dirty objects to Master collection: "+dirtyInfoDestination);
      for (Object o : dirtyInfos.keySet()) {
          InfoObject tmpObject = (InfoObject) dirtyInfos.get(o);
          tmpObject.setSourceFile(dirtyInfoDestination);
          tmpMap.put(tmpObject.getKeyName(), tmpObject);
      }

    resetWorkingCollections();
    // Write out the changes to the user file.
    writeOutToDestinationFile(userFilename);
    userDescriptionChanged=false;
  }

  /**
   * This method opens the destination file, and then with the help of the
   * formatOutput method, writes out ALL info objects that belong to the
   * destination file old and new.
   */
  protected void writeOutToDestinationFile(String destinationFile) {
    if (DEBUG) System.out.println("Writing out to file: "+destinationFile);
    File outputFile = new File(destinationFile);
    FileWriter writer;
    if (!outputFile.canWrite()) {
      handleOutputWriteError();
      return;
    }
    try {
      writer = new FileWriter(outputFile);
      if (DEBUG) System.out.println("Formatting output.");
      userFileDescription = userFileDescription.replace('\n', ' ');
      writer.write(DESCRIPTION+" "+userFileDescription+"\n\n");
      writeOutAllCollections(writer, destinationFile);
      writer.flush();
      writer.close();
    }
    catch (Exception ex) { handlePrefException(ex); }
  }


  /**
   * This method loops through the working collection passed in, and if source
   * file = destination file, asks the Info Object for its property output
   * and writes the key/value pairs to the destination file.
   */
  protected void addCollectionToOutput(FileWriter writer, TreeMap workingCollection, String destinationFile) {
    try {
        for (Object o1 : workingCollection.keySet()) {
            InfoObject tmpObject = (InfoObject) workingCollection.get(o1);
            if (tmpObject.getSourceFile().equals(destinationFile)) {
                Properties tmpProperties = tmpObject.getPropertyOutput();
                for (Object o : tmpProperties.keySet()) {
                    String tmpKey = (String) o;
                    String tmpValue = tmpProperties.getProperty(tmpKey);
                    if (DEBUG) System.out.println("Writing out: " + tmpKey + " " + tmpValue);
                    writer.write(tmpKey + " " + tmpValue + "\n");
                }
                writer.write("\n");
            }
        }
    }
    catch (Exception ex) {
      if (DEBUG) System.out.println("Dumping formatting of output.");
      handlePrefException(ex);
      return;
    }
  }


  public void registerPrefMgrListener(PrefMgrListener listener) {
    if (listener!=null) listeners.add(listener);
  }
  public void removePrefMgrListener(PrefMgrListener listener) {
    if (listeners.contains(listener)) listeners.remove(listener);
  }


  /**
   * This area of the code is reserved for inner classes that serve this class.
   */

  public class MyStringComparator implements Comparator {
    public int compare(Object key1, Object key2) {
      String keyName1, keyName2;
      try {
        keyName1 = (String)key1;
        keyName2 = (String)key2;
        if (keyName1==null || keyName2==null) return 0;
      }
      catch (Exception ex) {
        return 0;
      }
      return keyName1.compareToIgnoreCase(keyName2);
    }
  }


  private class MyFilenameFilter implements FilenameFilter {
    public boolean accept(File dir, String name) {
        return name.endsWith(filenameFilter);
    }
  }
}