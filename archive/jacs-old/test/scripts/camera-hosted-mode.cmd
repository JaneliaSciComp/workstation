REM 
REM  Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
REM  
REM  This file is part of JCVI VICS.
REM  
REM  JCVI VICS is free software; you can redistribute it and/or modify it 
REM  under the terms and conditions of the Artistic License 2.0.  For 
REM  details, see the full text of the license in the file LICENSE.txt.  
REM  No other rights are granted.  Any and all third party software rights 
REM  to remain with the original developer.
REM  
REM  JCVI VICS is distributed in the hope that it will be useful in 
REM  bioinformatics applications, but it is provided "AS IS" and WITHOUT 
REM  ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to implied 
REM  warranties of merchantability or fitness for any particular purpose.  
REM  For details, see the full text of the license in the file LICENSE.txt.
REM  
REM  You should have received a copy of the Artistic License 2.0 along with 
REM  JCVI VICS.  If not, the license can be obtained from 
REM  "http://www.perlfoundation.org/artistic_license_2_0."
REM  

rem
rem This script runs the GWT "hosted mode" browser.  It requires:
rem    * A running Tomcat containing a vics webapp at the URL defined below.
rem    * The environment variable JAVA_HOME defined, and pointing to any JDK (even 1.5)
rem
rem You can attach a debugger to the hosted mode browser to debug GWT client-side Java code
rem using the port defined at the end of the DEBUG_OPTS line below.
rem
rem @author Michael Press
rem

set JAVA_OPTS= -Xms128m -Xmx768m
set DEBUG_OPTS=-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5555
set CLASSPATH=..\..\src
set CLASSPATH=%CLASSPATH%;..\..\..\model\src
set CLASSPATH=%CLASSPATH%;..\..\..\shared\build\jars\jacs-shared.jar
set CLASSPATH=%CLASSPATH%;..\..\lib\googlemaps_gwt_2_2_1.jar
set CLASSPATH=%CLASSPATH%;..\..\..\common\GWT\gwt-user.jar
set CLASSPATH=%CLASSPATH%;..\..\..\common\GWT\gwt-dev.jar
set CLASSPATH=%CLASSPATH%;..\..\..\common\GWT\gwt-widgets-0.2.0.jar
set CLASSPATH=%CLASSPATH%;..\..\..\common\GWT\gwt-dnd-3.0.0.jar

rem ******* Use one of these entry points (make it the last URL) ********
rem set URL=http://localhost:8080/jacs/gwt/org.janelia.it.jacs.web.gwt.search.Search/Search.oa?keyword=Sargasso
rem set URL=http://localhost:8080/jacs/gwt/org.janelia.it.jacs.web.gwt.search.Search/Search.htm
rem set URL=http://localhost:8080/jacs/gwt/org.janelia.it.jacs.web.gwt.frv.Frv/Frv.htm
rem set URL=http://localhost:8080/jacs/gwt/org.janelia.it.jacs.web.gwt.download.BrowseProjectsPage/BrowseProjectsPage.oa
rem set URL=http://localhost:8080/jacs/gwt/org.janelia.it.jacs.web.gwt.download.BrowseProjectsPage/BrowseProjectsPage.oa?projectSymbol=CAM_PROJ_MarineMicrobes
rem set URL=http://localhost:8080/jacs/gwt/org.janelia.it.jacs.web.gwt.download.ProjectSamplesPage/ProjectSamplesPage.oa?projectSymbol=CAM_PROJ_MarineVirome
rem set URL=http://localhost:8080/jacs/gwt/org.janelia.it.jacs.web.gwt.download.DownloadByPubPage/DownloadByPubPage.oa?projectSymbol=CAM_PROJ_GOS
rem set URL=http://localhost:8080/jacs/gwt/org.janelia.it.jacs.web.gwt.status.Status/Status.htm
rem set URL=http://localhost:8080/jacs/gwt/org.janelia.it.jacs.web.gwt.status.Status/Status.htm
set URL=http://localhost:8080/jacs/gwt/org.janelia.it.jacs.web.gwt.advancedblast.AdvancedBlast/AdvancedBlast.htm
rem set URL=http://localhost:8080/jacs/gwt/org.janelia.it.jacs.web.gwt.advancedblast.AdvancedBlast/AdvancedBlast.htm?taskId=1206751584357188054
rem set URL=http://localhost:8080/jacs/gwt/org.janelia.it.jacs.web.gwt.blast.Blast/Blast.htm
rem set URL=http://localhost:8080/jacs/gwt/org.janelia.it.jacs.web.gwt.blast.Blast/Blast.htm#BlastWizardSubmitJobPage?taskId=1209621450823041494
rem set URL=http://localhost:8080/jacs/gwt/org.janelia.it.jacs.web.gwt.home.Home/Home.htm

"%JAVA_HOME%"\bin\java %JAVA_OPTS% %DEBUG_OPTS% -cp %CLASSPATH% com.google.gwt.dev.GWTShell -out www %* -noserver %URL%
