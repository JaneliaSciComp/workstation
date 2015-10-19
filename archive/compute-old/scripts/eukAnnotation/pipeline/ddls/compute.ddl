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

create table job (
job_id text not null,
job_name text not null,
program_name text not null,
program_version text not null,
program_options text,
project_code text not null,
subject_db_id integer,
query_db_id integer,
status text not null,
date_submitted datetime not null,
submitted_by text not null,
date_completed datetime,
result_type text,
result_message text,
num_results integer,
is_obsolete boolean not null,
primary key(job_id) );

create index ix_job_job_name on job(job_name,is_obsolete);

create view compute as
select j.*,
	s.dataset_name as subject_db_name, s.dataset_version as subject_db_version,
	s.subject_node as subject_db_node,
	q.dataset_name as query_db_name, q.dataset_version as query_db_version,
	q.query_node as query_db_node
from job j, dataset_version s, dataset_version q
where s.version_id=j.subject_db_id and q.version_id=j.query_db_id;

create table btab (
job_id text not null,
rank integer not null,
rank_vs_subject integer not null,
rank_vs_query integer not null,
subject_seq_id text not null, 		--btab[5]
subject_left integer not null,		--btab[8]
subject_right integer not null,		--btab[9]
subject_frame integer,
orientation integer not null,		--btab[17], minus=-1, plus=1
query_seq_id text not null,			--btab[0]
query_end5 integer not null,		--btab[6/7]
query_end3 integer not null,		--btab[6/7]
query_frame integer,				--btab[16]
num_identical integer,
num_similar integer,
num_gaps integer,
alignment_length integer,
pct_identity float not null,		--btab[10]
pct_similarity float not null,		--btab[11]
pct_length float not null,			--computed
hsp_score float not null,			--btab[12]
bit_score float not null,			--btab[13]
evalue float not null,				--btab[19]
pvalue float not null,				--btab[20]
primary key(job_id,rank) );

create index ix_btab_query_seq_id on btab(query_seq_id);
create index ix_btab_subject_seq_id on btab(subject_seq_id);

create view compute_btab as
select
	j.job_name as job_name, j.is_obsolete as is_obsolete, j.query_db_id as query_db_id, j.subject_db_id as subject_db_id,
	b.*,
	s.seq_acc as subject_id, s.seq_definition as subject_definition, s.seq_length as subject_length,
	q.seq_acc as query_id, q.seq_definition as query_definition, q.seq_length as query_length
from btab b, job j, dataset_seq s, dataset_seq q
where j.job_id=b.job_id
and s.seq_id=b.subject_seq_id
and q.seq_id=b.query_seq_id;

create view compute_btab_jcvi as
select
	j.job_name as job_name, j.is_obsolete as is_obsolete,
	q.seq_acc as query_id, q.seq_length as query_length,
	j.program_name as method, v.dataset_name as database,
	s.seq_acc as subject_id,
	b.query_end5 as query_end5, b.query_end3 as query_end3, b.subject_left as subject_left, b.subject_right as subject_right,
	b.pct_identity as pct_identity, b.pct_similarity as pct_similarity, b.hsp_score as hsp_score, b.bit_score as bit_score,
	s.seq_definition as subject_definition, s.seq_length as subject_length,
	q.seq_definition as query_definition,
	b.query_frame as query_frame,
    substr(case when query_frame<0 then '-' when query_frame>0 then '+' else null end,1,1) as query_strand,
	b.evalue as evalue, b.pvalue as pvalue
from btab b, job j, dataset_version v, dataset_seq s, dataset_seq q
where j.job_id=b.job_id
and v.version_id=j.subject_db_id
and s.seq_id=b.subject_seq_id
and q.seq_id=b.query_seq_id;

create view compute_btab_ncbi as
select job_name, is_obsolete, query_id, subject_id,
	pct_identity, alignment_length, alignment_length - num_identical - num_gaps as num_mismatches,
	num_gaps, query_end5, query_end3, subject_left, subject_right, evalue, bit_score
from compute_btab;	

create table htab (
job_id text not null,
rank integer not null,
rank_vs_hmm integer not null,
rank_vs_query integer not null,
subject_hmm_id integer not null,	--htab[0 & 15]
query_seq_id integer not null,		--htab[5]
hmm_begin integer not null,			--htab[6]
hmm_end integer not null,			--htab[7]
query_begin integer not null,		--htab[8]
query_end integer not null,			--htab[9]
domain_score float,					--htab[11]
total_score float,					--htab[12]
domain_index integer,				--htab[13]
domain_count integer,				--htab[14]
trusted_cutoff float,				--htab[17]
noise_cutoff float,					--htab[18]
total_evalue float,					--htab[19]
domain_evalue float,				--htab[20]
primary key (job_id,rank) );

create index ix_htab_query_seq_id on htab(query_seq_id);
create index ix_htab_subject_hmm_id on htab(subject_hmm_id);

create view compute_htab as
select 
	j.job_name as job_name, j.is_obsolete as is_obsolete, j.query_db_id as query_db_id, j.subject_db_id as subject_db_id,
	h.*,
	s.seq_acc as hmm_acc, s.seq_definition as hmm_description, s.seq_length as hmm_len,
	q.seq_acc as query_id, q.seq_definition as query_definition, q.seq_length as query_length
from htab h, job j, dataset_seq s, dataset_seq q
where j.job_id=h.job_id
and s.seq_id=h.subject_hmm_id
and q.seq_id=h.query_seq_id;

create table tmhmm (
job_id text not null,
query_seq_id integer not null,
exp_aa float not null,
exp_first60 float not null,
num_predicted_helixes integer not null,
topology text not null,
primary key(job_id,query_seq_id) );

create index ix_tmhmm_query_seq_id on tmhmm(query_seq_id);

create view compute_tmhmm as
select
	j.job_name as job_name, j.is_obsolete as is_obsolete, j.query_db_id as query_db_id,
	t.*,
	q.seq_acc as query_id, q.seq_definition as query_definition, q.seq_length as query_length
from tmhmm t, job j, dataset_seq q
where j.job_id=t.job_id
and q.seq_id=t.query_seq_id;

create table pepstats (
job_id text not null,
query_seq_id integer not null,
molecular_weight float not null,
isoelectric_Point float,
primary key(job_id,query_seq_id) );

create index ix_pepstats_query_seq_id on pepstats(query_seq_id);

create view compute_pepstats as
select
	j.job_name as job_name, j.is_obsolete as is_obsolete, j.query_db_id as query_db_id,
	p.*,
	q.seq_acc as query_id, q.seq_definition as query_definition, q.seq_length as query_length
from pepstats p, job j, dataset_seq q
where j.job_id=p.job_id
and q.seq_id=p.query_seq_id;

create table signalp (
job_id text not null,
query_seq_id integer not null,
hmm_flag char(1) not null,
hmm_cmax numeric not null,
hmm_cmax_pos numeric not null,
hmm_cmax_flag char(1) not null,
hmm_sprob numeric not null,
hmm_sprob_flag char(1) not null,
primary key(job_id,query_seq_id) );

create index ix_signalp_query_seq_id on signalp(query_seq_id);

create view compute_signalp as
select
	j.job_name as job_name, j.is_obsolete as is_obsolete, j.query_db_id as query_db_id,
	p.*,
	q.seq_acc as query_id, q.seq_definition as query_definition, q.seq_length as query_length
from signalp p, job j, dataset_seq q
where j.job_id=p.job_id
and q.seq_id=p.query_seq_id;

create table priam (
job_id text not null,
query_seq_id integer not null,
ec_num text not null,
ec_definition text,
evalue numeric not null,
bit_score numeric not null,
query_begin integer not null,
query_end integer not null,
primary key(job_id,query_seq_id,query_begin) );

create index ix_priam_query_seq_id on priam(query_seq_id);

create view compute_priam as
select
	j.job_name as job_name, j.is_obsolete as is_obsolete, j.query_db_id as query_db_id, j.subject_db_id as subject_db_id,
	p.*,
	q.seq_acc as query_id, q.seq_definition as query_definition, q.seq_length as query_length
from priam p, job j, dataset_seq q
where j.job_id=p.job_id
and q.seq_id=p.query_seq_id;

create table iprscan (			-- col indexes and descriptions from raw format
job_id text not null,		
query_seq_id integer not null,	-- in leiu of col[0]
analysis_method text not null,	-- col[3] 	"the analysis method launched"
db_member_id text not null,		-- col[4]	"database members entry for this match"
db_member_desc text not null,	-- col[5]	"database member description for the entry"
dm_start integer not null,		-- col[6]	"start of the domain match"
dm_end integer not null,		-- col[7]	"end of the domain match"
evalue numeric not null,		-- col[8]	"evalue of the match (reported by member database anayling method)."
status char(1) not null,		-- col[9]	"status of the match (T: true, ?: unknown)."
interpro_id text,				-- col[11]	"corresponding InterPro entry (if iprlookup requested by the user)."
interpro_desc text,				-- col[12]	"description of the InterPro entry"
go_terms text,					-- col[13]	"GO (gene ontology) description for the InterPro entry"
primary key(job_id,query_seq_id,analysis_method,db_member_id,dm_start) );

create index ix_iprscan_query_seq_id on iprscan(query_seq_id);

create view compute_iprscan as
select
	j.job_name as job_name, j.is_obsolete as is_obsolete, j.query_db_id as query_db_id, j.subject_db_id as subject_db_id,
	i.*,
	q.seq_acc as query_id, q.seq_definition as query_definition, q.seq_length as query_length
from iprscan i, job j, dataset_seq q
where j.job_id=i.job_id
and q.seq_id=i.query_seq_id;
