
package org.janelia.it.jacs.compute.gcdml.gcdml;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Nov 12, 2007
 * Time: 12:37:53 PM
 */
public class GcdmlCameraClient {
//    COMMENTED OUT 03/31/2010 as we aren't using this interface.  Also turning off the JAXB.
//    static final String EXPORT_PROJECT_MODE="-export_project";
//    static final String OUTPUT_ARG="-output";
//    GenomeContextBeanRemote _gcBean;
//    GenomeContextDAO gcDao;
//    Session session;
//    String mode;
//    String exportProjectName;
//    JAXBContext jaxbContext;
//    File outputFile;
//    ObjectFactory factory;
//
//    Pattern ewPattern=Pattern.compile("\\s*(\\S.+\\S)\\s*[EeWw]\\s*");
//    Pattern nsPattern=Pattern.compile("\\s*(\\S.+\\S)\\s*[NnSs]\\s*");
//    Pattern simpleDegreePattern=Pattern.compile("([\\d\\.]+)\\s*d");
//    Pattern minutesDegreePattern=Pattern.compile("([\\d\\.]+)\\s*d\\s*([\\d\\.]+)\\s*\\'");
//    Pattern fullDegreePattern=Pattern.compile("([\\d\\.]+)\\s*d\\s*([\\d\\.]+)\\s*\\'\\s*([\\d\\.]+)\\s*\"");
//
//    public GcdmlCameraClient(String[] args) {
//        _gcBean= EJBFactory.getRemoteGenomeContextBean();
//        session=getHibernateSession();
//        gcDao=new GenomeContextDAO(session);
//        for (int i=0;i<args.length;i++) {
//            if (args[i].equals(EXPORT_PROJECT_MODE)) {
//                mode=EXPORT_PROJECT_MODE;
//                exportProjectName=args[++i].trim();
//            } else if (args[i].equals(OUTPUT_ARG)) {
//                outputFile=new File(args[++i]);
//            }
//        }
//        if (mode==null) {
//            usage();
//        } else if (mode!=null && outputFile==null) {
//            usage();
//        }
//    }
//
//    protected void process() {
//        try {
//            jaxbContext = JAXBContext.newInstance("org.janelia.it.jacs.shared.gcdml.jaxb");
//            factory = new ObjectFactory();
//            if (mode.equals(EXPORT_PROJECT_MODE)) {
//                exportProject();
//            }
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//    }
//
//    protected void exportProject() throws Exception {
//        // First step is to get the project
//        Project project=(Project)session.load(Project.class, exportProjectName);
//        // Next, get the samples for this project
//        List<Sample> samples=gcDao.getSamplesByProject(project.getSymbol());
//        // There are two sources for the sequences:
//        //   (1) samples (reads)
//        //   (2) assemblies (contigs)
//        // Now, we can cycle through the samples to get the set of reads from the
//        // base-sequence-entity table by using the sample_id. Then, we can use
//        // the project name to get the assembly accessions, which we can then use
//        // to get the contigs corresponding to project assemblies.
//        List<String> assemblyAccessions=gcDao.getAssemblyAccessionsByProjectName(project.getSymbol());
//        System.out.println("There are "+samples.size()+" samples and "+assemblyAccessions.size()+" assemblies for project="+project.getName());
//
//        // Create metagenome
//        MetagenomeReportType metagenome=new MetagenomeReportType();
//
//        // Sample information
//        SourceSampleMetagenomicType sourceSample=new SourceSampleMetagenomicType();
//        List<MetagenomePhysicalSampleType> physicalSamples=createPhysicalSamples(samples);
//        if (physicalSamples.size()==1) {
//            sourceSample.setPhysicalMaterial(physicalSamples.get(0));
//        } else if (physicalSamples.size()>1) {
//            SampleCollectionMetagenomicType physicalCollection=new SampleCollectionMetagenomicType();
//            physicalCollection.getOrganismalMaterialOrPhysicalMaterial().addAll(physicalSamples);
//            sourceSample.setCollection(physicalCollection);
//        }
//        metagenome.setOriginalSample(sourceSample);
//
//        // Sequencing results
//        MetagenomeSequencingType sequencingType=new MetagenomeSequencingType();
//        ScaffoldSetType scaffoldSet=new ScaffoldSetType();
//        sequencingType.setScaffoldSet(scaffoldSet);
//        ContigSetType contigSet=new ContigSetType();
//        sequencingType.setContigSet(contigSet);
//        ReadSetType readSet=new ReadSetType();
//        sequencingType.setReadSet(readSet);
//
//        // Metagenome clustering information
//        FragmentClassificationMethod fragmentClassification=new FragmentClassificationMethod();
//        fragmentClassification.setValue("fragment classification value");
//        sequencingType.setFragmentClassificationMethod(fragmentClassification);
//        metagenome.setSequencing(sequencingType);
//
//        // DNA preparation method
//        NucExtract nucExtract=new NucExtract();
//        nucExtract.getMethod().add("method");
//        metagenome.setNucExtract(nucExtract);
//
//        // Library information
//        MetagenomeLibraryType library=new MetagenomeLibraryType();
//        metagenome.setDnaLibrary(library);
//
//        // Marshall metagenome
//        Marshaller marshaller = jaxbContext.createMarshaller();
//        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT , Boolean.TRUE);
//        JAXBElement<?> metagenomeElement = factory.createMetagenome(metagenome);
//        marshaller.marshal(metagenomeElement, new FileOutputStream(outputFile.getAbsolutePath()));
//    }
//
//    public void jaxbExample() throws Exception {
//        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
//        System.out.println("Starting to unmarshall metagenome...");
//        MetagenomeReportType metagenome= (MetagenomeReportType)unmarshaller.unmarshal(new File("gcdmlMetagenomeExample.xml"));
//        System.out.println("Finished unmarshalling metagenome");
//
//        System.out.println("Starting to marshall metagenome to file");
//        Marshaller marshaller = jaxbContext.createMarshaller();
//        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT , Boolean.TRUE);
//        marshaller.marshal(metagenome, new FileOutputStream("gcdmlMetagenomeExampleOutput.xml"));
//        System.out.println("Finished with remarshall");
//
//        Unmarshaller unmarshaller2 = jaxbContext.createUnmarshaller();
//        System.out.println("Starting to unmarshall metagenome 2...");
//        MetagenomeReportType metagenome2= (MetagenomeReportType)unmarshaller2.unmarshal(new File("gcdmlMetagenomeExampleOutput.xml"));
//        System.out.println("Finished unmarshalling metagenome 2");
//
//        System.out.println("Starting to marshall metagenome to file");
//        Marshaller marshaller2 = jaxbContext.createMarshaller();
//        marshaller2.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT , Boolean.TRUE);
//        marshaller2.marshal(metagenome2, new FileOutputStream("gcdmlMetagenomeExampleOutput2.xml"));
//        System.out.println("Finished with remarshall 2");
//    }
//
//    public static void main(String[] args) {
//        //BasicConfigurator.configure();
//        Properties log4jProps=new Properties();
//        log4jProps.setProperty("log4j.rootLogger","DEBUG, A1");
//        log4jProps.setProperty("log4j.appender.A1","org.apache.log4j.ConsoleAppender");
//        log4jProps.setProperty("log4j.appender.A1.layout","org.apache.log4j.PatternLayout");
//        log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern","%d [%t] %-5p %c - %m%n");
//        log4jProps.setProperty("log4j.logger.org.jboss", "WARN");
//        log4jProps.setProperty("log4j.logger.org.hibernate","ERROR");
//        log4jProps.setProperty("log4j.logger.org.janelia.it.jacs.model","ERROR");
//        PropertyConfigurator.configure(log4jProps);
//        GcdmlCameraClient client=new GcdmlCameraClient(args);
//        client.process();
//        client.close();
//    }
//
//    public static void usage() {
//        System.err.println("Usage: "+EXPORT_PROJECT_MODE+" <project name> "+OUTPUT_ARG+" <output file>");
//        System.exit(1);
//    }
//
//    public void close() {
//        session.close();
//    }
//
//    public Session getHibernateSession() {
//        Configuration cfg = new Configuration();
//        cfg = cfg.configure(new File("hibernate.cfg.xml"));
//        SessionFactory factory = cfg.buildSessionFactory();
//        Session session = factory.getCurrentSession();
//        Transaction transaction = session.beginTransaction();
//        return session;
//    }
//
//    protected List<MetagenomePhysicalSampleType> createPhysicalSamples(List<Sample> samples) {
//        List<MetagenomePhysicalSampleType> physicalSamples=new ArrayList<MetagenomePhysicalSampleType>();
//        for (Sample sample : samples) {
//            Map<String,List<CollectionObservation>> sampleObservations=getAllSampleObservations(sample);
//            MetagenomePhysicalSampleType physicalSample=new MetagenomePhysicalSampleType();
//
//            // Sample time - we simply take the first time from BioMaterial
//            Set<BioMaterial> bioMaterials=sample.getBioMaterials();
//            BioMaterial firstBiomaterial=null;
//            if (bioMaterials!=null && bioMaterials.size()>0) {
//                firstBiomaterial=bioMaterials.iterator().next();
//            }
//            FuzzyTimeType sampleTime=null;
//            if (firstBiomaterial==null) {
//                sampleTime=new FuzzyTimeType();
//                sampleTime.setNotGiven("unknown");
//            } else {
//                sampleTime=new FuzzyTimeType();
//                FuzzyTimePositionType timePosition=new FuzzyTimePositionType();
//                Calendar collectionCalendar=new GregorianCalendar();
//                collectionCalendar.setTime(firstBiomaterial.getCollectionStartTime());
//                timePosition.getValue().add(collectionCalendar.toString());
//                sampleTime.setTime(timePosition);
//            }
//            physicalSample.setSamplingTime(sampleTime);
//
//            // Sample material
//            physicalSample.setMaterialType("water");
//
//            // Sample amount
//            List<CollectionObservation> volumeObservations=sampleObservations.get("volume filtered");
//            if (volumeObservations==null || volumeObservations.size()==0) {
//                physicalSample.setAmount(createUnknownMeasure());
//            } else if (volumeObservations.size()>=1) {
//                // We will just take the first since there is no way to specify multiple collections per sample in gcdml
//                CollectionObservation volumeObservation=volumeObservations.get(0);
//                ParameterType volumeMeasure=new ParameterType();
//                volumeMeasure.setMeasure(createMeasurementType(volumeObservation, physicalSample.getSamplingTime().getTime()));
//                physicalSample.setAmount(volumeMeasure);
//            }
//
//            // Sample location
//            SamplePointLocationType samplePoint=null;
//            CollectionSite firstCollectionSite=null;
//            if (firstBiomaterial!=null && firstBiomaterial.getCollectionSite()!=null) {
//                firstCollectionSite=firstBiomaterial.getCollectionSite();
//                samplePoint=createSamplePointFromCollectionSite(firstCollectionSite);
//            } else {
//                samplePoint=createUnknownSamplePoint();
//            }
//            //physicalSample.setSampleLocation(samplePoint);
//            JAXBElement<?> sampleLocationElement=factory.createSamplePointLocation(samplePoint);
//            physicalSample.setSampleLocation(sampleLocationElement);
//
//            // Habitat
//            MarineHabitatType marineHabitat=new MarineHabitatType();
//            WaterBodyType waterBody=new WaterBodyType();
//
//            // Chlorophyll
//            ParameterType chlorophyllMeasure=createMeasureFromObservation(sampleObservations, "chlorophyll density", physicalSample.getSamplingTime().getTime());
//            if (chlorophyllMeasure!=null) {
//                waterBody.setChlorophyl(chlorophyllMeasure);
//            } else {
//                waterBody.setChlorophyl(createUnknownMeasure());
//            }
//
//            // Depth
//            ParameterType depthMeasure=createMeasureFromObservation(sampleObservations, "water depth", physicalSample.getSamplingTime().getTime());
//            if (depthMeasure!=null) {
//                waterBody.setDepth(depthMeasure);
//            } else {
//                waterBody.setDepth(createUnknownMeasure());
//            }
//
//            // Temperature
//            ParameterType temperatureMeasure=createMeasureFromObservation(sampleObservations, "temperature", physicalSample.getSamplingTime().getTime());
//            if (temperatureMeasure!=null) {
//                waterBody.setTemperature(temperatureMeasure);
//            } else {
//                waterBody.setTemperature(createUnknownMeasure());
//            }
//
//            // Salinity
//            ParameterType salinityMeasure=createMeasureFromObservation(sampleObservations, "salinity", physicalSample.getSamplingTime().getTime());
//            if (salinityMeasure!=null) {
//                waterBody.setSalinity(salinityMeasure);
//            } else {
//                waterBody.setSalinity(createUnknownMeasure());
//            }
//
//            // Dissolved Organic Carbon - NOT IMPLEMENTED IN GCDML YET
////            ParameterType docMeasure=createMeasureFromObservation(sampleObservations, "dissolved organic carbon", physicalSample.getSamplingTime().getTime());
////            if (docMeasure!=null) {
////                waterBody.setDOC(docMeasure);
////            } else {
////                waterBody.setDOC(createUnknownMeasure());
////            }
//
//            // Dissolved Oxygen
//            ParameterType oxygenMeasure=createMeasureFromObservation(sampleObservations, "dissolved oxygen", physicalSample.getSamplingTime().getTime());
//            if (oxygenMeasure!=null) {
//                waterBody.setDissolvedOxygen(oxygenMeasure);
//            } else {
//                waterBody.setDissolvedOxygen(createUnknownMeasure());
//            }
//
//            // Add habitat
//            marineHabitat.setWaterBody(waterBody);
//            JAXBElement<?> marineHabitatElement=factory.createMarineHabitat(marineHabitat);
//            physicalSample.setHabitat(marineHabitatElement);
//
//            // Add to collection
//            physicalSamples.add(physicalSample);
//        }
//        return physicalSamples;
//    }
//
//    protected ParameterType createMeasureFromObservation(Map<String,List<CollectionObservation>> sampleObservations, String type, FuzzyTimePositionType timePosition) {
//        ParameterType measure=null;
//        List<CollectionObservation> observations=sampleObservations.get(type);
//        if (observations!=null && observations.size()>0) {
//            measure=new ParameterType();
//            measure.setMeasure(createMeasurementType(observations.get(0), timePosition));
//        }
//        return measure;
//    }
//
//    protected SamplePointLocationType createSamplePointFromCollectionSite(CollectionSite collectionSite) {
//        SamplePointLocationType location=new SamplePointLocationType();
//        location.setDeterminationMethod("gps");
//        SamplePointLocationType.Pos2D pos2D=new SamplePointLocationType.Pos2D();
//        GeoPoint gp=gcDao.getGeoPointByCollectionSiteId(collectionSite.getSiteId());
//        try {
//            double longitude=convertLongitudeStringToDouble(gp.getLongitude());
//            double latitude=convertLatitudeStringToDouble(gp.getLatitude());
//            pos2D.getAxisLabels().add("longitude");
//            pos2D.getAxisLabels().add("latitude");
//            pos2D.setSrsDimension(new BigInteger("2"));
//            pos2D.setSrsName("longitude latitude");
//            pos2D.getUomLabels().add("degrees");
//            pos2D.getValue().add(longitude);
//            pos2D.getValue().add(latitude);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//            System.exit(1);
//        }
//        location.setPos2D(pos2D);
//        return location;
//    }
//
//    protected SamplePointLocationType createUnknownSamplePoint() {
//        SamplePointLocationType unknownLocation=new SamplePointLocationType();
//        unknownLocation.setDeterminationMethod("unknown");
//        SamplePointLocationType.Pos2D pos2D=new SamplePointLocationType.Pos2D();
//        pos2D.getValue().add(new Double(0.0));
//        pos2D.getUomLabels().add("unknown");
//        pos2D.getAxisLabels().add("unknown");
//        pos2D.setSrsDimension(new BigInteger("1"));
//        pos2D.setSrsName("unknown");
//        unknownLocation.setPos2D(pos2D);
//        return unknownLocation;
//    }
//
//    protected Map<String, List<CollectionObservation>> getAllSampleObservations(Sample sample) {
//        Map<String, List<CollectionObservation>> observationMap=new HashMap<String,List<CollectionObservation>>();
//        java.util.Set<BioMaterial> biomaterials=(java.util.Set<BioMaterial>)sample.getBioMaterials();
//        for (BioMaterial bm : biomaterials) {
//            Map<String, CollectionObservation> bmMap=bm.getObservations();
//            for (String key : bmMap.keySet()) {
//                List<CollectionObservation> observationList=observationMap.get(key);
//                if (observationList==null) {
//                    observationList=new ArrayList<CollectionObservation>();
//                    observationMap.put(key, observationList);
//                }
//                observationList.add(bmMap.get(key));
//            }
//        }
//        return observationMap;
//    }
//
//    protected ParameterType createUnknownMeasure() {
//        ParameterType unknown=new ParameterType();
//        unknown.setNotGiven("unknown");
//        return unknown;
//    }
//
//    protected Double createSafeDouble(String doubleString) {
//        Double d = 0.0;
//        try {
//            d=new Double(doubleString);
//        } catch (Exception e) {}
//        return d;
//    }
//
//    protected MeasurementType createRangeMeasure(CollectionObservation observation, FuzzyTimePositionType timePosition) {
//        MeasurementType measure=new MeasurementType();
//        measure.setMeasure(new GcdMeasureListType());
//        measure.getMeasure().setUom(observation.getUnits());
//        String[] vlist=observation.getValue().split("-");
//        double minValue=createSafeDouble(vlist[0].trim());
//        double maxValue=createSafeDouble(vlist[1].trim());
//        measure.getMeasure().getValue().add(minValue);
//        measure.getMeasure().getValue().add(maxValue);
//        measure.setMin(minValue);
//        measure.setMax(maxValue);
//        measure.setComment(observation.getComment());
//        measure.setDate(timePosition);
//        return measure;
//    }
//
//    protected MeasurementType createMeasure(CollectionObservation observation, FuzzyTimePositionType timePosition) {
//        MeasurementType measure=new MeasurementType();
//        measure.setMeasure(new GcdMeasureListType());
//        measure.getMeasure().setUom(observation.getUnits());
//        measure.getMeasure().getValue().add(createSafeDouble(observation.getValue()));
//        measure.setComment(observation.getComment());
//        measure.setMin(createSafeDouble(observation.getValue()));
//        measure.setMax(createSafeDouble(observation.getValue()));
//        measure.setDate(timePosition);
//        return measure;
//    }
//
//    protected MeasurementType createMeasurementType(CollectionObservation observation, FuzzyTimePositionType timePosition) {
//        // Depending on observation value format we will create a different type
//        // of measurement element
//        if (observation.getValue().matches("\\s*[\\d\\.]+\\s*-\\s*[\\d\\.]+\\s*")) {
//            return createRangeMeasure(observation, timePosition);
//        } else if (observation.getValue().matches("\\s*[\\d\\.]+\\s*")) {
//            return createMeasure(observation, timePosition);
//        } else {
//            return null;
//        }
//    }
//
//    protected double convertLongitudeStringToDouble(String longitudeString) throws Exception {
//        double degrees=0.0;
//        double minutes=0.0;
//        double seconds=0.0;
//        double westOrEast=0.0;
//
//        // Handle East or West
//        if (longitudeString.trim().endsWith("w") || longitudeString.trim().endsWith("W")) {
//            westOrEast=-1.0; // west
//        } else if (longitudeString.trim().endsWith("e") || longitudeString.trim().endsWith("E")) {
//            westOrEast=1.0; // east
//        } else {
//            throw new Exception("Could not find W or E in longitude string="+longitudeString);
//        }
//
//        // Strip off E or W
//        String degreeString=null;
//        Matcher ewMatcher=ewPattern.matcher(longitudeString);
//        if (ewMatcher.matches()) {
//            degreeString=ewMatcher.group(1);
//        } else {
//            throw new Exception("Could not find match to ewPattern for="+longitudeString);
//        }
//
//        Matcher simpleDegree=simpleDegreePattern.matcher(degreeString);
//        Matcher minuteDegree=minutesDegreePattern.matcher(degreeString);
//        Matcher fullDegree=fullDegreePattern.matcher(degreeString);
//        if (simpleDegree.matches()) {
//            degrees=new Double(simpleDegree.group(1));
//        } else if (minuteDegree.matches()) {
//            degrees=new Double(minuteDegree.group(1));
//            minutes=new Double(minuteDegree.group(2));
//        } else if (fullDegree.matches()) {
//            degrees=new Double(fullDegree.group(1));
//            minutes=new Double(fullDegree.group(2));
//            seconds=new Double(fullDegree.group(3));
//        } else {
//            throw new Exception("Could not find any degree match to="+degreeString);
//        }
//        double longitude=westOrEast*(degrees+(minutes/60.0)+(seconds/3600.0));
//        return longitude;
//    }
//
//    protected double convertLatitudeStringToDouble(String latitudeString) throws Exception {
//        double degrees=0.0;
//        double minutes=0.0;
//        double seconds=0.0;
//        double northOrSouth=0.0;
//
//        // Handle North or South
//        if (latitudeString.trim().endsWith("n") || latitudeString.trim().endsWith("N")) {
//            northOrSouth=1.0; // north
//        } else if (latitudeString.trim().endsWith("s") || latitudeString.trim().endsWith("S")) {
//            northOrSouth=-1.0; // south
//        } else {
//            throw new Exception("Could not find N or S in longitude string="+latitudeString);
//        }
//
//        // Strip off N or S
//        String degreeString=null;
//        Matcher nsMatcher=nsPattern.matcher(latitudeString);
//        if (nsMatcher.matches()) {
//            degreeString=nsMatcher.group(1);
//        } else {
//            throw new Exception("Could not find match to nsPattern for="+latitudeString);
//        }
//
//        Matcher simpleDegree=simpleDegreePattern.matcher(degreeString);
//        Matcher minuteDegree=minutesDegreePattern.matcher(degreeString);
//        Matcher fullDegree=fullDegreePattern.matcher(degreeString);
//        if (simpleDegree.matches()) {
//            degrees=new Double(simpleDegree.group(1));
//        } else if (minuteDegree.matches()) {
//            degrees=new Double(minuteDegree.group(1));
//            minutes=new Double(minuteDegree.group(2));
//        } else if (fullDegree.matches()) {
//            degrees=new Double(fullDegree.group(1));
//            minutes=new Double(fullDegree.group(2));
//            seconds=new Double(fullDegree.group(3));
//        } else {
//            throw new Exception("Could not find any degree match to="+degreeString);
//        }
//        double latitude=northOrSouth*(degrees+(minutes/60.0)+(seconds/3600.0));
//        return latitude;
//    }
//
}
