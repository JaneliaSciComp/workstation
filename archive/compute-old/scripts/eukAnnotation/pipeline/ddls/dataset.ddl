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

create table dataset (
dataset_name text not null,
current_version integer not null,
is_obsolete boolean not null,
primary key(dataset_name) );

create table dataset_version (
version_id integer not null,
content_type text not null
constraint chk_content_type check ( content_type in ('seq','hmm','rps','n/a') ),
source_type text not null
constraint chk_source_type check ( source_type in ('file','node','n/a') ),
content_path text not null,
md5 text not null,
content_count text not null,
content_length text not null,
description text,
query_node text,
date_uploaded datetime,
uploaded_by text,
seq_attr_list text,
subject_node text,
date_formatted datetime,
formatted_by text,
dataset_name text not null,
dataset_version integer not null,
date_released datetime not null,
released_by text not null,
date_retired datetime,
retired_by datetime,
primary key(version_id),
unique(dataset_name,dataset_version) );

create table dataset_seq (
version_id integer not null,
seq_id integer not null,
seq_acc text not null,
seq_definition text not null,
seq_length integer not null,
primary key(seq_id),
unique(seq_acc,version_id) );

create index ix_dataset_seq_version_id on dataset_seq(version_id);

create table dataset_seq_attr (
version_id integer not null,
seq_id integer not null,
attr_name text not null,
attr_value text,
primary key(seq_id,attr_name) );

create index ix_dataset_seq_attr_version_id on dataset_seq_attr(version_id);
create index ix_dataset_seq_attr_value on dataset_seq_attr(attr_value,version_id);

create table hmm_annotation (
version_id integer,
hmm_acc text,
hmm_len integer,
noise_cutoff numeric,
trusted_cutoff numeric,
definition text,
primary key(version_id,hmm_acc) );

create view dataset_detail as 
select f.*, d.is_obsolete as is_obsolete
from dataset d, dataset_version f
where f.dataset_name=d.dataset_name and f.dataset_version=d.current_version;

create view dataset_history as 
select v.*, case when d.is_obsolete<>0 then d.is_obsolete when d.current_version=v.dataset_version then 0 else 1 end as is_obsolete
from dataset d, dataset_version v
where v.dataset_name=d.dataset_name;

insert into dataset(dataset_name,current_version,is_obsolete)
values('no dataset',0,0);

insert into dataset_seq(version_id,seq_id,seq_acc,seq_definition,seq_length)
values(0,0,'no sequence','no sequence',-1);

insert into dataset_version(version_id,content_type,source_type,
content_path,md5,content_count,content_length,dataset_name,dataset_version,date_released,released_by)
values(0,'n/a','n/a','dev/null','n/a',0,0,'no dataset',0,datetime('now'),'n/a');

create view dataset_sequence as
select d.dataset_name as dataset_name, v.dataset_version as dataset_version,
       case when d.current_version=v.dataset_version then 0 else 1 end as is_obsolete,
       s.*
from dataset d, dataset_version v, dataset_seq s
where v.dataset_name=d.dataset_name and s.version_id=v.version_id;
