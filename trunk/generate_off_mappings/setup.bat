@echo off

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

REM
REM   Use this if running the ant build outside of the IDE, but first modify below to make the locations specific
REM   to your machine.
REM

set B=.
set L=%B%\lib
set C=%B%\conf
set M=..\common\JBoss-Server-4.2.1.GA
set P=..\common\JBoss-Lib-4.2.1.GA

set CLASSPATH=%B%;%M%\cglib.jar;%L%\commons-collections-2.1.1.jar;%M%\commons-logging.jar;%P%\dom4j.jar;%L%\postgresql-8.1-404.jdbc3.jar;%L%\hsqldb.jar;%M%\log4j.jar;%L%\freemarker-2.3.6.jar;%L%\hibernate-tools.jar;%L%\hibernate3-3.2.0.jar
set PATH=c:\tools\apache-ant-1.6.5\bin;%PATH%
