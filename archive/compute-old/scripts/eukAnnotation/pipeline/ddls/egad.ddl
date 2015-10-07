/**************************************************************************
  Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.

  This file is part of JCVI VICS.

  JCVI VICS is free software; you can redistribute it and/or modify it 
  under the terms and conditions of the Artistic License 2.0.  For 
  details, see the full text of the license in the file LICENSE.txt.  
  No other rights are granted.  Any and all third party software rights 
  to remain with the original developer.

  JCVI VICS is distributed in the hope that it will be useful in 
  bioinformatics applications, but it is provided "AS IS" and WITHOUT 
  ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to implied 
  warranties of merchantability or fitness for any particular purpose.  
  For details, see the full text of the license in the file LICENSE.txt.

  You should have received a copy of the Artistic License 2.0 along with 
  JCVI VICS.  If not, the license can be obtained from 
  "http://www.perlfoundation.org/artistic_license_2_0."
***************************************************************************/

CREATE TABLE rfam(
	id numeric,
	accession varchar,
	feat_type varchar,
	feat_class varchar,
	com_name varchar,
	gene_sym varchar,
	window_size numeric,
	noise_cutoff numeric,
	gathering_thresh numeric,
	trusted_cutoff numeric,
	euk tinyint,
	prok tinyint,
	vir tinyint,
	iscurrent bit,
	date_refreshed datetime,
	primary key(accession),
	unique(id));
CREATE TABLE hmm2 (
 	id numeric,
 	hmm_acc varchar,
 	hmm_len integer,
	iso_type varchar,
 	hmm_com_name varchar,
 	gene_sym varchar,
 	ec_num varchar,
 	noise_cutoff numeric,
 	gathering_cutoff numeric,
 	trusted_cutoff numeric,
 	trusted_cutoff2 numeric,
 	is_current bit,
	date_refreshed datetime,
 	primary key(hmm_acc),
 	unique(id));
