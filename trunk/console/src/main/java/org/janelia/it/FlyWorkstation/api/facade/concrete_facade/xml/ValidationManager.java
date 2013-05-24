package org.janelia.it.FlyWorkstation.api.facade.concrete_facade.xml;

/**
 * Title:        ValidationManager
 * Description:  Central object for doing validations of XML, and keeping
 *               track of validations if needed, as well as defining constants
 *               used globally for validating clients if needed.
 */

import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.FacadeManager;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.*;
import java.util.*;

/**
 * Manages XML DTD, Schema validation for the Workstation.
 */
public class ValidationManager {

    //----------------------------EXPOSED CONSTANTS
    public static final String VALIDATION_PROP_NAME = "XmlValidation";

    // XML Validation menu item strings.
    public static final String DO_NOT_VALIDATE = "None";
    public static final String VALIDATE_WITH_DTD = "Validate All XML Files against DTD Before Opening";
    public static final String VALIDATE_WITH_XML_SCHEMA = "Validate All XML Files against XML Schema Before Opening";
    public final String DTD_NAME = "/resource/client/XMLdata/dtd/GenomicsExchangeFormat-V4.dtd";
    public final String SCHEMA_NAME = "/resource/client/XMLdata/schema/CEF.xsd";

    //----------------------------INTERNAL CONSTANTS
    // Possible validation settings.
    private static final byte NO_VALIDATION = 'N';
    private static final byte DTD_VALIDATION = 'D';
    private static final byte XML_SCHEMA_VALIDATION = 'S';
    private static final byte UNSET_VALIDATION_SETTING = 'U';
    private static final String ALL_PARSE_REPORT_TITLE = "of Current Directories and Recently Opened";

    //----------------------------CLASS MEMBER VARIABLES
    private static ValidationManager validationManager = null;

    private static String fileSep = System.getProperty("file.separator");

    //----------------------------OBJECT MEMBER VARIABLES
    private File validationPrefFile = new File(SessionMgr.getSessionMgr().getApplicationOutputDirectory() + fileSep + "userPrefs." + VALIDATION_PROP_NAME);

    private List filesValidatedInSession = null;

    private byte cachedValidationSetting = UNSET_VALIDATION_SETTING;

    //----------------------------CONSTRUCTORS

    /**
     * Private constructor to enforce Singleton.
     */
    private ValidationManager() {
        filesValidatedInSession = new ArrayList();
    } // End constructor

    //----------------------------CLASS METHODS

    /**
     * Returns the one and only validation manager.
     */
    public synchronized static ValidationManager getInstance() {
        if (validationManager == null) validationManager = new ValidationManager();
        return validationManager;
    } // End method

    /**
     * Allows the instance to be changed, hence dropping all records of session.
     */
    public static void clearSession() {
        validationManager = null;
    } // End method

    //----------------------------INTERFACE METHODS

    /**
     * Gets the old val setting, in a form for human consumption.
     */
    public String getDisplayableValidationSetting() {
        return convertValidationSettingFromByteToString(getValidationSetting());
    } // End method

    /**
     * Gets the old val setting.
     */
    public byte getValidationSetting() {
        // Prevent re-reading the file each request.
        if (cachedValidationSetting != UNSET_VALIDATION_SETTING) return cachedValidationSetting;

        /** @todo when possible change this to use Model Property implementation. */
        // Get user's previous preference of whether to validate XML files or not.
        //
        byte returnValue = 0;
        try {

            if (validationPrefFile.canRead() && validationPrefFile.exists()) {
                FileInputStream fis = new FileInputStream(validationPrefFile);
                ObjectInputStream istream = new ObjectInputStream(fis);
                Byte inputByte = (Byte) istream.readObject();
                istream.close();
                byte val = inputByte.byteValue();
                switch (val) {
                    case DTD_VALIDATION:
                        returnValue = val;
                        break;
                    case XML_SCHEMA_VALIDATION:
                        returnValue = val;
                        break;
                    case NO_VALIDATION:
                        returnValue = val;
                        break;
                    default:
                        returnValue = NO_VALIDATION;
                } // Checking for reasonable values.

            } // Permission granted.
            else {
                returnValue = NO_VALIDATION;
            } // Permission denied.

        } // End try
        catch (Exception ex) {
            return NO_VALIDATION;
        } // End catch block for pref file open exceptions.

        cachedValidationSetting = returnValue;

        return returnValue;

    } // End method

    /**
     * Returns all choices for validation, in a readily readable manner.
     */
    public String[] getDisplayableValidationChoices() {
        return new String[]{DO_NOT_VALIDATE, VALIDATE_WITH_DTD, VALIDATE_WITH_XML_SCHEMA};
    } // End method

    /**
     * Convert validation setting from menu item into byte-code suitable for storage.
     */
    public byte convertValidationSettingFromStringToByte(String setting) {
        byte returnVal = NO_VALIDATION;
        if (setting.equals(DO_NOT_VALIDATE)) returnVal = NO_VALIDATION;
        else if (setting.equals(VALIDATE_WITH_DTD)) returnVal = DTD_VALIDATION;
        else if (setting.equals(VALIDATE_WITH_XML_SCHEMA)) returnVal = XML_SCHEMA_VALIDATION;

        return returnVal;
    } // Ene method

    /**
     * Convert validateion setting from byte code to menu item.
     */
    public String convertValidationSettingFromByteToString(byte setting) {
        String returnVal;
        switch (setting) {
            case NO_VALIDATION:
                returnVal = DO_NOT_VALIDATE;
                break;
            case DTD_VALIDATION:
                returnVal = VALIDATE_WITH_DTD;
                break;
            case XML_SCHEMA_VALIDATION:
                returnVal = VALIDATE_WITH_XML_SCHEMA;
                break;
            default:
                returnVal = DO_NOT_VALIDATE;
                break;
        } // End switch

        return returnVal;
    } // End method

    /**
     * Using a string that is meaningful to the user, set the val setting.
     */
    public void setDisplayableValidationSetting(String newVal) {
        setValidationSetting(convertValidationSettingFromStringToByte(newVal));
    } // End method

    /**
     * Sets the user's validation pref.
     */
    public void setValidationSetting(byte newVal) {
        /** @todo when possible change this to use Model Property implementation. */
        // Now attempt to writeback the user's choice of whether to validate xml or not.
        //
        try {
            ObjectOutputStream ostream = new ObjectOutputStream(new FileOutputStream(validationPrefFile));
            ostream.writeObject(new Byte(newVal));
            ostream.close();
        } // End try block.
        catch (Exception ex) {
            FacadeManager.handleException(new IllegalArgumentException("XML Validation Prefs file cannot be written"));
        } // End catch block for writeback of preferred directory.

        // Force re-read of file on next validation 'look'.
        cachedValidationSetting = UNSET_VALIDATION_SETTING;

        // Force re-scan of any XML files.
        filesValidatedInSession.clear();

    } // End method

    /**
     * Tests an entire directory's worth of files.
     */
    public void validateWholeDirectory(String directoryName, Set extensions) {

        // NOTE: no need to continue if the validation setting is nada.
        if (getValidationSetting() == NO_VALIDATION) return;

        try {
            StringBuffer overallValidationOutput = new StringBuffer();
            validateWholeDirectory(directoryName, extensions, overallValidationOutput);
            if ((overallValidationOutput != null) && (overallValidationOutput.length() > 0)) {
                FacadeManager.handleException(new InvalidXmlException(overallValidationOutput, directoryName, getParseTitle()));
            } // Found errors.

        } // End try block for val.
        catch (Exception ex) {
            // Unable to fully validate.
            FacadeManager.handleException(ex);
        } // End catch for val.

    } // End method: validateWholeDirectory

    /**
     * Goes through all directories in the iterator, validating files with extensions given.
     */
    public void validateAll(Iterator directoriesIterator, Iterator separateFilesIterator, Set extensions) {
        // NOTE: no need to continue if the validation setting is nada.
        byte validationSetting = getValidationSetting();
        if (validationSetting == NO_VALIDATION) return;

        try {
            String nextDirectory = null;
            StringBuffer overallValidationOutput = new StringBuffer();
            while (directoriesIterator.hasNext()) {
                nextDirectory = (String) directoriesIterator.next();
                if (nextDirectory != null) validateWholeDirectory(nextDirectory, extensions, overallValidationOutput);
            } // For all directories

            String nextFilename = null;
            StringBuffer nextErrorSet = null;
            while (separateFilesIterator.hasNext()) {
                nextFilename = (String) separateFilesIterator.next();
                if (nextFilename != null) nextErrorSet = validateInputFile(nextFilename, validationSetting);
                if ((nextErrorSet != null) && (nextErrorSet.length() > 0)) {
                    appendErrorHeader(overallValidationOutput, nextFilename);
                    overallValidationOutput.append(nextErrorSet.toString());
                } // Got errors.
            } // For all separate paths.

            if ((overallValidationOutput != null) && (overallValidationOutput.length() > 0)) {
                FacadeManager.handleException(new InvalidXmlException(overallValidationOutput, ALL_PARSE_REPORT_TITLE, getParseTitle()));
            } // Found errors.
        } // End try block
        catch (Exception ex) {
            FacadeManager.handleException(ex);
        } // End catch

    } // End method: validateAll

    /**
     * Build and invoke a validating parser.
     */
    public StringBuffer validateInputFile(String filePath) {
        // Do not repeat 'user setting' validations.
        if (filesValidatedInSession.contains(filePath)) return null;
        else return validateInputFile(filePath, getValidationSetting());
    } // End method

    /**
     * Build and invoke a validating parser.
     */
    public StringBuffer validateInputFile(String filePath, byte validationRequestType) {
        // Do not repeat 'user setting' validations.
        if (filesValidatedInSession.contains(filePath)) return null;

        return revalidateInputFile(filePath, validationRequestType);
    } // End method

    /**
     * Invoke validatng parser, without regard to whether the file has been validated this session before.
     */
    public StringBuffer revalidateInputFile(String filePath, byte validationRequestType) {
        StringBuffer outputBuffer = new StringBuffer(1000);
        byte validationSetting = validationRequestType;
//    try {
//      if (validationSetting == XML_SCHEMA_VALIDATION) {
//        String schemaPath = getClass().getResource(SCHEMA_NAME).toString();
//        NonRefCheckingErrorHandler errorHandler = new NonRefCheckingErrorHandler(filePath, outputBuffer);
//        FeatureHandlerBase featureHandler = new NonRefCheckingErrorHandler.ParserForContext();
//        ElementStacker stacker = new ArrayListElementStacker();
//        GenomicsExchangeHandler handler = new GenomicsExchangeHandler(stacker, featureHandler);
//        errorHandler.setElementContext(handler.getElementContext());
//        new SchemaValidatingParser( filePath, handler, errorHandler, schemaPath );
//      } // Validate with XML Schema
//      else if (validationSetting == DTD_VALIDATION) {
//        String dtdPath = getClass().getResource(DTD_NAME).toString();
//        NonRefCheckingErrorHandler errorHandler = new NonRefCheckingErrorHandler(filePath, outputBuffer, true);
//        ElementStacker stacker = new ArrayListElementStacker();
//        FeatureHandlerBase featureHandler = new NonRefCheckingErrorHandler.ParserForContext();
//        GenomicsExchangeHandler handler = new GenomicsExchangeHandler(stacker, featureHandler);
//        errorHandler.setElementContext(handler.getElementContext());
//
//        new ValidatingParser( "game", dtdPath, filePath, handler, errorHandler );
//      } // Validate with DTD
//    } // End try block for val.
//    catch (Exception ex) {
//      if (NonRefCheckingErrorHandler.TOO_MANY_ERRORS.equals(ex.getMessage())) {
//        outputBuffer.append(ex.getMessage());
//      } // Got bad one.
//      else
//        FacadeManager.handleException(ex);
//    } // End catch block for val.

        if (!filesValidatedInSession.contains(filePath)) filesValidatedInSession.add(filePath);

        return outputBuffer;

    } // End method

    /**
     * Tests whether the input file is valid per user's standard, and issues report.
     * This method is to be called when one-and-only-one file is to be validated.
     */
    public void validateAndReportInputFile(String fileName) {
        // Check: nothing to do?
        if (getValidationSetting() == NO_VALIDATION) return;

        StringBuffer validationOutput = validateInputFile(fileName);
        if ((validationOutput != null) && (validationOutput.length() > 0)) {
            StringBuffer header = new StringBuffer();
            appendErrorHeader(header, fileName);
            validationOutput.insert(0, header.toString());
            FacadeManager.handleException(new InvalidXmlException(validationOutput, fileName, getParseTitle()));
        } // Got errors to report.
    } // End method

    /**
     * Run requested test and issues report.
     */
    public void validateAndReportInputFile(String fileName, String requestedValidationString) {

        byte requestedValidationType = convertValidationSettingFromStringToByte(requestedValidationString);

        // Check: nothing to do?
        if (requestedValidationType == NO_VALIDATION) return;

        StringBuffer validationOutput = null;
        validationOutput = validateInputFile(fileName, requestedValidationType);
        if ((validationOutput != null) && (validationOutput.length() > 0))
            FacadeManager.handleException(new InvalidXmlException(validationOutput, fileName, getParseTitle()));
    } // End method

    /**
     * Run requested test and issues report, w/o regard to whether val'd earlier or not.
     */
    public void revalidateAndReportInputFile(String fileName, String requestedValidationString) {

        byte requestedValidationType = convertValidationSettingFromStringToByte(requestedValidationString);

        // Check: nothing to do?
        if (requestedValidationType == NO_VALIDATION) return;

        StringBuffer validationOutput = null;
        validationOutput = revalidateInputFile(fileName, requestedValidationType);
        if ((validationOutput != null) && (validationOutput.length() > 0))
            FacadeManager.handleException(new InvalidXmlException(validationOutput, fileName, getParseTitle()));
    } // End method

    /**
     * Tests an entire directory's worth of files, creating a report string buffer.
     */
    private void validateWholeDirectory(String directoryName, Set extensions, StringBuffer overallValidationOutput) {

        final Set extensionsOfInterest = extensions;

        // Will open the directory and scan it for validation errors.
        //
        try {

            FilenameFilter filter = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    int extPos = name.lastIndexOf(".");
                    if (extPos < 0) return false;

                    File thisFile = new File(dir, name);
                    if (!thisFile.isFile()) return false;

                    String extension = name.substring(extPos, name.length());
                    if (extensionsOfInterest.contains(extension)) return true;
                    else return false;
                } // End method
            };

            // Open the directory/confirm it can be opened.
            File directoryFile = FileUtil.ensureDirExists(directoryName);

            // Get list of appropriately-named files.
            String[] filesInDirectory = directoryFile.list(filter);
            String nextFileName = null;
            StringBuffer nextValidationOutput = null;
            for (int i = 0; i < filesInDirectory.length; i++) {
                nextFileName = new File(directoryFile, filesInDirectory[i]).getAbsolutePath();
                nextValidationOutput = validateInputFile(nextFileName);
                if ((overallValidationOutput != null) && (nextValidationOutput.length() > 0)) {
                    appendErrorHeader(overallValidationOutput, nextFileName);
                    overallValidationOutput.append(nextValidationOutput);
                    overallValidationOutput.append("\n\n");
                } // Got errors.
            } // For all files in directory

            // Add the files to the collection of pre-val'd.
            Collections.addAll(filesValidatedInSession, filesInDirectory);

        } // End try block for val.
        catch (Exception ex) {
            // Unable to fully validate.
            FacadeManager.handleException(ex);
        } // End catch for val.

    } // End method: validateWholeDirectory

    /**
     * Appends standard error header to buffer.
     */
    private void appendErrorHeader(StringBuffer overallValidationOutput, String nextFileName) {
        overallValidationOutput.append("-------------------------------------------------");
        overallValidationOutput.append("-------------------------------------------------");
        overallValidationOutput.append("-------------------------------------------------");
        overallValidationOutput.append("-------------------------------------------------");
        overallValidationOutput.append("\nInput File: ");
        overallValidationOutput.append(nextFileName);
        overallValidationOutput.append("\n");
    } // End method: errorHeader

    /**
     * Presentation title for exceptions report to validation.
     */
    private String getParseTitle() {
        byte validationSetting = getValidationSetting();
        if (validationSetting == XML_SCHEMA_VALIDATION) return SchemaValidatingParser.getPresentationTitle();
        else if (validationSetting == DTD_VALIDATION) return ValidatingParser.getPresentationTitle();
        else return "";
    } // End method
} // End class: ValidationManager
