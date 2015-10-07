#
# Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
# 
# This file is part of JCVI VICS.
# 
# JCVI VICS is free software; you can redistribute it and/or modify it 
# under the terms and conditions of the Artistic License 2.0.  For 
# details, see the full text of the license in the file LICENSE.txt.  
# No other rights are granted.  Any and all third party software rights 
# to remain with the original developer.
# 
# JCVI VICS is distributed in the hope that it will be useful in 
# bioinformatics applications, but it is provided "AS IS" and WITHOUT 
# ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to implied 
# warranties of merchantability or fitness for any particular purpose.  
# For details, see the full text of the license in the file LICENSE.txt.
# 
# You should have received a copy of the Artistic License 2.0 along with 
# JCVI VICS.  If not, the license can be obtained from 
# "http://www.perlfoundation.org/artistic_license_2_0."
# 

use warnings;
use strict;
our $errorMessage;
our $filestore = "Xruntime-shared/filestore";
use lib ('/usr/local/devel/ANNOTATION/EAP/pipeline');
use EAP::generic;
use EAP::db;
use EAP::vics;
$|++;
###################################################################
# dataset utilities
###################################################################
#
# add dataset from file
sub addFile {
	my ( $dbh, $name, $description, $filepath, $as_query_node, $as_subject_node, $content_type, $taglist ) = @_;
	if ( !defined $content_type ) { $content_type = "seq" }
	my $source_type = "file";
	my $oldset;

# check role
	if ( !$as_query_node && !$as_subject_node ) {
		$errorMessage = "addFile: no role defined.";
		return undef;
	}
	
# make sure dataset is new or obsolete
	my $exists = &_checkDatasetNameExists( $dbh, $name );
	if ( !defined $exists ) {
		$errorMessage = "addFile: " . $errorMessage;
		return undef;
	} elsif ( $exists ) {
		$oldset = &getDatasetByName( $dbh, $name );
		if ( !defined $oldset ) { 
			$errorMessage = "addFile: " . $errorMessage;
			return undef;
		} elsif ( $$oldset{is_obsolete} == 0 ) {
			$errorMessage = "addFile: dataset already exists.";
			return undef;
		}
	}

# build dataset
	my $dataset = &_makeDataset( $name, $description, $content_type, $source_type, $filepath );
	if ( !defined $dataset ) {
		$errorMessage = "addFile: " . $errorMessage;
		return undef;
	}

# upload to vics
	$dataset = &_uploadDataset( $dataset, $as_query_node, $as_subject_node );
	if ( !defined $dataset ) {
		$errorMessage = "addFile: " . $errorMessage;
		return undef;
	}
	
# for QUERY sequence datasets, load sequence descriptions
	if ( defined $taglist && $$dataset{content_type} eq "seq" && $$dataset{query_node} > 0 ) {
		$$dataset{seq_attr_list} = $taglist;
	}

# save to db
	$dataset = &_saveDataset( $dbh, $dataset, $oldset );
	if ( !defined $dataset ) {
		$errorMessage = "addFile: " . $errorMessage;
		return undef;
	}
	
	return $dataset;
}

#
# import existing VICS blast db node as dataset
sub addNode {
	my ( $dbh, $name, $description, $node_id, $node_owner, $content_type ) = @_;
	if ( !defined $node_owner ) { $node_owner = "system" }
	if ( !defined $content_type ) { $content_type = "seq" }
	my $source_type = "node";
	
	my $oldset;

# make sure dataset is new or obsolete
	my $exists = &_checkDatasetNameExists( $dbh, $name );
	if ( !defined $exists ) {
		$errorMessage = "addNode: " . $errorMessage;
		return undef;
	} elsif ( $exists ) {
		$oldset = &getDatasetByName( $dbh, $name );
		if ( !defined $oldset ) { 
			$errorMessage = "addNode: " . $errorMessage;
			return undef;
		} elsif ( $$oldset{is_obsolete} == 0 ) {
			$errorMessage = "addNode: dataset already exists.";
			return undef;
		}
	}

# get path for node content
	my $nodepath;
	if ( $content_type eq "seq" ) {
		$nodepath = &_blastNodePartitionPath( $node_id, $node_owner );
	} elsif ( $content_type eq "hmm" ) {
		$nodepath = &_hmmDBPath( $node_id, $node_owner );		
	} elsif ( $content_type eq "rps" ) {
		$nodepath = &_rpsDBPath( $node_id, $node_owner );		
	} else {
		$errorMessage = "addNode: unknown content type: \"" . $content_type . "\".";
		return undef;
	}
	if ( !defined $nodepath ) {
		$errorMessage = "addNode: " . $errorMessage;
		return undef;
	}

# build imported dataset
	my $dataset = &_makeDataset( $name, $description, $content_type, $source_type, $nodepath );
	if ( !defined $dataset ) {
		$errorMessage = "addNode: " . $errorMessage;
		return undef;
	}

# save upload info
	$$dataset{date_formatted} = &now;
	$$dataset{formatted_by} = $node_owner;
	$$dataset{subject_node} = $node_id;

# save to db
	$dataset = &_saveDataset( $dbh, $dataset, $oldset );
	if ( !defined $dataset ) {
		$errorMessage = "addNode: " . $errorMessage;
		return undef;
	}
	
	return $dataset;
}

#
# update dataset from file
sub updateFile {
	my ( $dbh, $name, $description, $filepath, $content_type ) = @_;
	if ( !defined $content_type ) { $content_type = "seq" }
	my $source_type = "file";
	
# get old version of dataset
	my $oldset = &getDatasetByName( $dbh, $name );
	if ( !defined $oldset ) {
		$errorMessage = "updateFile: " . $errorMessage;
		return undef;
	} elsif ( $$oldset{is_obsolete} ) {
		$errorMessage = "updateFile: dataset is obsolete.";
		return undef;
	} elsif ( $$oldset{source_type} ne $source_type ) {
		$errorMessage = "updateFile: dataset is $$oldset{source_type}-based.";
		return undef;
	}

# build new dataset
	my $newset = &_makeDataset( $name, $description, $content_type, $source_type, $filepath );
	if ( !defined $newset ) {
		$errorMessage = "updateFile: " . $errorMessage;
		return undef;
	}

# if content has not changed return the existing dataset version
	if ( $$oldset{md5} eq $$newset{md5}
			&& $$oldset{content_type} eq $$newset{content_type}
			&& $$oldset{content_count} eq $$newset{content_count}
			&& $$oldset{content_length} eq $$newset{content_length} ) {

# update existing dataset version 
		$oldset = &_updateContentDescription( $dbh, $oldset,
			$$newset{description}, $$newset{content_type}, $$newset{source_type}, $$newset{content_path} );
		if ( !defined $oldset ) {
			$errorMessage = "updateFile: " . $errorMessage;
			return undef;
		}
		$oldset = &_updateVICSNodes( $dbh, $oldset,
			$$newset{query_node}, $$newset{seq_attr_list}, $$newset{uploaded_by},
			$$newset{date_uploaded}, $$newset{subject_node}, $$newset{formatted_by},
			$$newset{date_formatted} );
		if ( !defined $oldset ) {
			$errorMessage = "updateFile: " . $errorMessage;
			return undef;
		}
		return $oldset;
	}
	
# return a new  version	
# upload new dataset
	my $as_query_node = ( $$oldset{query_node} > 0 );
	my $as_subject_node = ( $$oldset{subject_node} > 0 );
	$newset = &_uploadDataset( $newset, $as_query_node, $as_subject_node );
	if ( !defined $newset ) {
		$errorMessage = "updateFile: " . $errorMessage;
		return undef;
	}
	
# for QUERY sequence datasets, load sequence descriptions
if ( $content_type eq "seq" && $$newset{query_node} > 0 ) {
	$$newset{seq_attr_list} = $$oldset{seq_attr_list};
}

# save to db
	$newset = &_saveDataset( $dbh, $newset, $oldset );
	if ( !defined $newset ) {
		$errorMessage = "updateFile: " . $errorMessage;
		return undef;
	}
	
	return $newset;
}

#
# update dataset from VICS node
sub updateNode {
	my ( $dbh, $name, $description, $node_id, $node_owner, $content_type ) = @_;
	if ( !defined $content_type ) { $content_type = "seq" }
	my $source_type = "node";
	
# get old version of dataset
	my $oldset = &getDatasetByName( $dbh, $name );
	if ( !defined $oldset ) {
		$errorMessage = "updateNode: " . $errorMessage;
		return undef;
	} elsif ( $$oldset{is_obsolete} ) {
		$errorMessage = "updateNode: dataset is obsolete.";
		return undef;
	} elsif ( $$oldset{source_type} ne $source_type ) {
		$errorMessage = "updateFile: dataset is $$oldset{source_type}-based.";
		return undef;
	}

# default node/owner if not specified
	if ( !defined $node_id ) {
		$node_id = $$oldset{subject_node};
		$node_owner = $$oldset{formatted_by};
	} else {
		if ( !defined $node_owner ) { $node_owner = "system"}
	}

# get path for blast node partitions
	my $nodepath;
	if ( $content_type eq "seq" ) {
		$nodepath = &_blastNodePartitionPath( $node_id, $node_owner );
	} elsif ( $content_type eq "hmm") {
		$nodepath = &_hmmDBPath( $node_id, $node_owner );
	} elsif ( $content_type eq "rps") {
		$nodepath = &_rpsDBPath( $node_id, $node_owner );
	} else {
		$errorMessage = "updateNode: unknown content type: \"" . $content_type . "\".";
		return undef;
	}
	if ( !defined $nodepath ) {
		$errorMessage = "updateNode: " . $errorMessage;
		return undef;
	}

# build imported dataset
	my $newset = &_makeDataset( $name, $description, $content_type, $source_type, $nodepath );
	if ( !defined $newset ) {
		$errorMessage = "updateNode: " . $errorMessage;
		return undef;
	}

# if content has not changed return the existing dataset version
	if ( $$oldset{md5} eq $$newset{md5}
			&& $$oldset{content_type} eq $$newset{content_type}
			&& $$oldset{content_count} eq $$newset{content_count}
			&& $$oldset{content_length} eq $$newset{content_length} ) {

# update existing dataset version 
		$oldset = &_updateContentDescription( $dbh, $oldset,
			$$newset{description}, $$newset{content_type}, $$newset{source_type}, $$newset{content_path} );
		if ( !defined $oldset ) {
			$errorMessage = "updateNode: " . $errorMessage;
			return undef;
		}
		if ( $node_id ne $$oldset{subject_node} ) {
print "UPDATE NODE\n";
			$oldset = &_updateVICSNodes( $dbh, $oldset, undef, undef, undef, undef, $node_id, $node_owner, $$oldset{date_formatted} );
			if ( !defined $oldset ) {
				$errorMessage = "updateNode: " . $errorMessage;
				return undef;
			}
		}
		return $oldset;
	}
	
# return a new version
# save upload info
	$$newset{date_formatted} = &now;
	$$newset{formatted_by} = $node_owner;
	$$newset{subject_node} = $node_id;

# save to db
	$newset = &_saveDataset( $dbh, $newset, $oldset );
	if ( !defined $newset ) {
		$errorMessage = "updateNode: " . $errorMessage;
		return undef;
	}
	
	return $newset;
}

#
# import dataset from external library
sub importExternalDataset {
	my ( $dbh, $name, $externalLibrary, $externalName ) = @_;

# get existing version dataset if it exists
	my $oldset;
	my $exists = &_checkDatasetNameExists( $dbh, $name );
	if ( !defined $exists ) {
		$errorMessage = "importExternalDataset: " . $errorMessage;
		return undef;
	} elsif ( $exists ) {
		$oldset = &getDatasetByName( $dbh, $name );
		if ( !defined $oldset ) { 
			$errorMessage = "importExternalDataset: " . $errorMessage;
			return undef;
		}
	}
	
# get external version of dataset
	my $libdbh = &connectSQLite( $externalLibrary );
	if ( !defined $libdbh ) {
		$errorMessage = "importExternalDataset: " . $errorMessage;
		return undef;
	}

	my $newset = &getDatasetByName( $libdbh, $externalName );
	$libdbh->disconnect;
	if ( !defined $newset ) {
		$errorMessage = "importExternalDataset: " . $errorMessage;
		return undef;
	}
	$$newset{dataset_name} = $name;

# if content has not changed return the existing dataset version
	if ( defined $oldset
			&& $$oldset{md5} eq $$newset{md5}
			&& $$oldset{content_type} eq $$newset{content_type}
			&& $$oldset{content_count} eq $$newset{content_count}
			&& $$oldset{content_length} eq $$newset{content_length} ) {

# update existing dataset version 
		$oldset = &_updateContentDescription( $dbh, $oldset, $$newset{description}, $$newset{content_type}, $$newset{source_type}, $$newset{content_path});
		if ( !defined $oldset ) {
			$errorMessage = "importExternalDataset: " . $errorMessage;
			return undef;
		}
		$oldset = &_updateVICSNodes( $dbh, $oldset,
			$$newset{query_node}, $$newset{seq_attr_list}, $$newset{uploaded_by},
			$$newset{date_uploaded}, $$newset{subject_node}, $$newset{formatted_by},
			$$newset{date_formatted} );
		if ( !defined $oldset ) {
			$errorMessage = "importExternalDataset: " . $errorMessage;
			return undef;
		}
		return $oldset;
	}

# return a new version
# save new version to db
	$newset = &_saveDataset( $dbh, $newset, $oldset );
	if ( !defined $newset ) {
		$errorMessage = "updateFile: " . $errorMessage;
		return undef;
	}
	
	return $newset;
}

#
# retrieve all dataset versions from db
sub getAllDatasetVersions {
	my ( $dbh ) = @_;
	
	my $result = &querySQLHashHash( $dbh, "select * from dataset_history");
	if ( !defined $result ) {
		$errorMessage = "getAllDatasetVersions: " . $errorMessage;
		return undef;
	}

	foreach my $dataset ( @$result ) {
		$dataset = &_getDatasetAnnotation( $dbh, $dataset );
		if ( !defined $dataset ) {
			$errorMessage = "getAllDatasetVersions: " . $errorMessage;
			return undef;
		}
	}
	
	
	return $result;
}
		
#
# delete dataset (mark obsolete)
sub deleteDataset {
	my ( $dbh, $dataset ) = @_;
	
	$$dataset{is_obsolete} = 1;
	$dbh->begin_work;
	my $delete = &executeSQL( $dbh, "update dataset set is_obsolete=? where dataset_name=?", 1, $$dataset{dataset_name} );
	if ( !defined $delete ) {
		$errorMessage = "deleteDataset: " . $errorMessage;
		return undef;
	}
	$delete = &executeSQL( $dbh, "update dataset_version set date_retired=?, retired_by=? where version_id=?",
		&now, getlogin || getpwuid($>) || 'ccbuild', $$dataset{version_id} );
	if ( !defined $delete ) {
		$errorMessage = "deleteDataset: " . $errorMessage;
		return undef;
	}
	$dbh->commit;
	
	return $dataset;
}
#
# retrieve all current datasets from db
sub getAllCurrentDatasets {
	my ( $dbh ) = @_;
	
	my $result = &querySQLArrayHash( $dbh, "select * from dataset_detail where is_obsolete=0");
	if ( !defined $result ) {
		$errorMessage = "getAllCurrentDatasets: " . $errorMessage;
		return undef;
	}
	
	foreach my $dataset ( @$result ) {
		$dataset = &_getDatasetAnnotation( $dbh, $dataset );
		if ( !defined $dataset ) {
			$errorMessage = "getAllCurrentDatasets: " . $errorMessage;
			return undef;
		}
	}
	
	return $result;
}

#
# retrieve current version of dataset from db by name
sub getDatasetByName {
	my ( $dbh, $name ) = @_;
	
# get dataset description
	my $result = &querySQLArrayHash( $dbh, "select * from dataset_detail where dataset_name=?", $name );
	if ( !defined $result ) {
		$errorMessage = "getDatasetByName: " . $errorMessage;
		return undef;
	} elsif ( scalar @$result == 0 ) {
		$errorMessage = "getDatasetByName: no such dataset: \"" . $name . "\".";
		return undef;
	}

	my $dataset = $$result[0];
	$dataset = &_getDatasetAnnotation( $dbh, $dataset );
	if ( !defined $dataset ) {
		$errorMessage = "getDatasetByName: " . $errorMessage;
		return undef;
	}
	
# return dataset
	return $dataset;
}

#
# retrieve specific version of dataset from db
sub getDatasetVersion {
	my ( $dbh, $id ) = @_;
	
# get dataset description
	my $result = &querySQLArrayHash( $dbh, "select * from dataset_history where version_id=?", $id );
	if ( !defined $result ) {
		$errorMessage = "getDatasetVersion (dataset): " . $errorMessage;
		return undef;
	} elsif ( scalar @$result == 0 ) {
		$errorMessage = "getDatasetVersion: no such dataset version.";
		return undef;
	}

	my $dataset = &_getDatasetAnnotation( $dbh, $$result[0] );
	if ( !defined $dataset ) {
		$errorMessage = "getDatasetVersion: " . $errorMessage;
		return undef;
	}
	
# return dataset
	return $dataset;
}

#
# return the path to the dataset's content
sub getDatasetVICSFasta {
	my ( $dataset ) = @_;
	
	my $path = &getDatasetNodePath( $dataset );
	if ( !defined $path ) {
		$errorMessage = "getDatasetVICSFasta: " . $errorMessage;
		return undef;
	}

#print "\nPEPSTATS content type: $$dataset{content_type} query node: $$dataset{query_node}\n";
	if ( $$dataset{content_type} eq "seq" ) {
		if ( $$dataset{query_node} > 0 ) {
			if ( -e $path . "/peptide.fasta" ) { return $path . "/peptide.fasta" }
			else { return $path . "/nucleotide.fasta" };
		} elsif ( $$dataset{subject_node} > 0 ) {
			if ( $$dataset{content_type} eq "seq" ) {
				return "cat $path/p_*.fasta |";
			} elsif ( $$dataset{content_type} eq "rps" ) {
				my $db = `ls -1 $path/*.rps`;
				$db =~ s/[\r\n]//;
				$db =~ s/.rps$//;
				return "fastacmd -d $db -D 1 | sed 's/>\\S* />/g' |";
			} else {
				$errorMessage = "getDatasetVICSFasta: no VICS fasta for \"$$dataset{content_type}\" datasets.";
				return undef;
			}
		} else {
			$errorMessage = "getDatasetVICSFasta: no VICS fasta defined for dataset.";
			return undef;
		}
	} else {
		$errorMessage = "getDatasetVICSFasta: not  implemented for content type $$dataset{content_type}.";
		return undef;
	}
}

sub getDatasetNodePath {
	my ( $dataset ) = @_;
	
	if ( $$dataset{query_node} > 0 ) {
		return $filestore . "/" . $$dataset{uploaded_by} . "/FastaFiles/" . $$dataset{query_node};
	} elsif ( $$dataset{subject_node} > 0 ) {
		if ( $$dataset{content_type} eq "seq" ) {
			return $filestore . "/" . $$dataset{formatted_by} . "/BlastDatabases/" . $$dataset{subject_node};
		} elsif ( $$dataset{content_type} eq "rps" ) {
			return $filestore . "/" . $$dataset{formatted_by} . "/ReversePsiBlastDatabase/" . $$dataset{subject_node};
		} elsif ( $$dataset{content_type} eq "hmm" ) {
			return $filestore . "/" . $$dataset{formatted_by} . "/HmmerPfamDatabase/" . $$dataset{subject_node};
		} else {
			$errorMessage = "getDatasetNodePath: no cannot find path for \"$$dataset{content_type}\" node.";
			return undef;
		}
	} else {
		$errorMessage = "getDatasetNodePath: no VICS node defined for dataset.";
		return undef;
	}
}

#
# get sequence data for given dataset version
sub getDatasetSeqs {
	my ( $dbh, $version_id ) = @_;
	
	my $tmp = &querySQLArrayHash( $dbh, "select * from dataset_seq where version_id=?", $version_id );
	if ( !defined $tmp ) {
		$errorMessage = "getDatasetSeqs: " . $errorMessage;
		return undef;
	}
	
	my $seqs = &arrayHashToHashHash( $tmp, "seq_acc" );
	return $seqs;
}

#
# get sequence data for given dataset version
sub getDatasetSeqAccs {
	my ( $dbh, $version_id ) = @_;
	
	my $tmp = &querySQLArrayHash( $dbh, "select seq_id, seq_acc from dataset_seq where version_id=?", $version_id );
	if ( !defined $tmp ) {
		$errorMessage = "getDatasetSeqAccs: " . $errorMessage;
		return undef;
	}
	
	my $seqs = &arrayHashToHashHash( $tmp, "seq_acc" );
	return $seqs;
}

#
# add a sequence to a dataset version
sub addDatasetSeq{
	my ( $dbh, $version_id, $id, $acc, $definition, $length ) = @_;
	if ( !defined $definition || length($definition) == 0 ) { $definition = $acc }
	if ( !defined $length || length($length) == 0 ) { $length = -1 }

	my $add = &executeSQL( $dbh,
		"insert into dataset_seq(version_id,seq_id,seq_acc,seq_definition,seq_length) values(?,?,?,?,?)",
		$version_id, $id, $acc, $definition, $length );
	if ( !defined $add ) {
		$errorMessage = "addDatasetSeq: " . $errorMessage;
		return undef;
	}
	
	my $seq;
	$$seq{version_id} = $version_id;
	$$seq{seq_id} = $id;
	$$seq{seq_acc} = $acc;
	$$seq{seq_definition} = $definition;
	$$seq{seq_length} = $length; 
	
	return $seq;
}

###################################################################
# internal subroutines
###################################################################
#
# compare blast db's fasta partition names
sub _comparePartitions {
	my ( $a, $b ) = @_;
	
	$a =~ s/\n//g;
	my $apart = basename($a);
	$apart =~ s/.fasta$//;
	$apart =~ s/^p\_//;
	
	$b =~ s/\n//g;
	my $bpart = basename($b);
	$bpart =~ s/.fasta$//;
	$bpart =~ s/^p\_//;
	
	if ( int($apart) < int($bpart) ) {
		return -1;
	} elsif ( int($apart) == int($bpart) ) {
		return 0;
	} else {
		return 1;
	}
}

#
# update descriptive dataset details without changing version
sub _updateContentDescription {
	my ( $dbh, $dataset, $description, $content_type, $source_type, $filepath ) = @_;

	$$dataset{description} = $description;
	$$dataset{content_type} = $content_type;
	$$dataset{source_type} = $source_type;
	$$dataset{content_path} = $filepath;
	my $update = &executeSQL( $dbh,
		"update dataset_version set content_path=?, description=?, content_type=?, source_type=? where version_id=?",
		$filepath, $description, $content_type, $source_type, $$dataset{version_id} );
	if ( !defined $update ) {
		$errorMessage = "_updateContentDescription: " . $errorMessage;
		return undef;
	}
	
	return $dataset;
}

#
# update VICS node data
sub _updateVICSNodes{
	my ( $dbh, $dataset, $querynode, $seqattrlist, $uploadedby, $dateuploaded, $subjectnode, $formattedby, $dateformatted) = @_;
	$$dataset{query_node} = $querynode;
	$$dataset{seq_attr_list} = $seqattrlist;
	$$dataset{uploaded_by} = $uploadedby;
	$$dataset{date_uploaded} = $dateuploaded;
	$$dataset{subject_node} = $subjectnode;
	$$dataset{formatted_by} = $formattedby;
	$$dataset{date_formatted} = $dateformatted;
	my $update = &executeSQL( $dbh,
		"update dataset_version set query_node=?, seq_attr_list=?, uploaded_by=?, date_uploaded=?, subject_node=?, formatted_by=?, date_formatted=? where version_id=?",
		$querynode, $seqattrlist, $uploadedby, $dateuploaded, $subjectnode, $formattedby, $dateformatted, $$dataset{version_id} );
	if ( !defined $update ) {
		$errorMessage = "_updateVICSNodes: " . $errorMessage;
		return undef;
	}
	
	return $dataset;
}

#
# upload dataset to vics
sub _uploadDataset {
	my ( $dataset, $as_query_node, $as_subject_node ) = @_;
	if ( $$dataset{source_type} ne "file" ) {
		$errorMessage = "_uploadDataset: cannot upload $$dataset{source_type}-based content.";
		return undef;
	}
	
# check roles
	if ( !$as_query_node && !$as_subject_node ) {
		$errorMessage = "_uploadDataset: no role provided.";
		return undef;
	}
# create VICS blast database from fasta
	if ( $as_subject_node ) {
		if ( $$dataset{content_type} eq "seq" ) {
			$$dataset{subject_node} = &vicsBlastIndexFasta( $$dataset{dataset_name}, $$dataset{description}, $$dataset{content_path}, getlogin || getpwuid($>) || 'ccbuild');
		} else {
			$errorMessage = "_uploadDataset: cannot upload $$dataset{content_type} content as subject node.";
			return undef;
		}
		if ( !defined $$dataset{subject_node} ) {
			$errorMessage = "_uploadDataset: " . $errorMessage;
			return undef;
		}
		$$dataset{formatted_by} = getlogin || getpwuid($>) || 'ccbuild';
		$$dataset{date_formatted} = &now;
		
	}

# save fasta file?
	if ( $as_query_node > 0 ) {
		if ( $$dataset{content_type} eq "seq" ) {
			$$dataset{query_node} = &vicsUploadFasta( $$dataset{content_path}, getlogin || getpwuid($>) || 'ccbuild');
		} else {
			$errorMessage = "_uploadDataset: cannot upload $$dataset{content_type} content as query node.";
			return undef;
		}
		if ( !defined $$dataset{query_node} ) {
			$errorMessage = "_uploadDataset: " . $errorMessage;
			return undef;
		}
		$$dataset{uploaded_by} = getlogin || getpwuid($>) || 'ccbuild';
		$$dataset{date_uploaded} = &now;
	}

	return $dataset;
}

#
# does dataset name exist?
sub _checkDatasetNameExists {
	my ( $dbh, $name ) = @_;
	
	my $check = &firstRowSQL( $dbh, "select count(*) from dataset where dataset_name=?", $name );
	if ( !defined $check ) {
		$errorMessage = "_checkDatasetNameExists: " . $errorMessage;
		return undef;
	} else {
		return $$check[0];
	}
}

#
# populate the dataset data hash
sub _makeDataset {
	my ( $name, $description, $content_type, $source_type, $datapath ) = @_;
	
# initialize new dataset
	my $dataset;
	
	$$dataset{dataset_name} = $name;
	$$dataset{description} = $description;
	$$dataset{content_type} = $content_type;
	$$dataset{source_type} = $source_type;

	$$dataset{dataset_version} = undef;
	$$dataset{is_obsolete} = 0;

# fill in data attributes
	if ( $content_type eq "seq" ) {
		if ( $source_type eq "file" ) {
			$dataset = &_describeFasta( $dataset, $datapath );
		} else {
			$dataset = &_describeBlastPartitions( $dataset, $datapath );
		}
	} elsif ( $content_type eq "hmm" ) {
		$dataset = &_describeHmmDB( $dataset, $datapath );
	} elsif ( $content_type eq "rps" ) {
		$dataset = &_describeRpsDB( $dataset, $datapath );
	} else {
		$errorMessage = "_makeDataset: unknown content type: \"$content_type\".";
		return undef;
	}
	if ( !defined $dataset ) {
		$errorMessage = "_makeDataset: " . $errorMessage;
		return undef;
	}
		
# return dataset
	return $dataset
}

#
# populate the fasta data hash
sub _describeFasta {
	my ( $dataset, $filepath ) = @_;

# validate path
	if ( !-e $filepath ) {
		$errorMessage = "_describeFasta: path does not exist: \"" . $filepath . "\".";
		return undef;
	}

	my $fullpath = realpath($filepath);
	if ( ! -T $fullpath ) {
		$errorMessage = "_describeFasta: not a text file.";
		return undef;
	}

# get file statistics
	my ( $md5 ) = split( /\s/, `md5sum $fullpath`);
		
	my $content_count = `grep ">" $fullpath | wc -l`;
	$content_count =~ s/\n//g;
	
	my $defsize = `grep ">" $fullpath | wc -c`;
	$defsize -= $content_count;
	my $totlines = `wc -l $fullpath | cut -f 1 -d ' '`;
	my $totsize = `wc -c $fullpath | cut -f 1 -d ' '`;
	$totsize -= $totlines;
	my $content_length = $totsize - $defsize;

# return results in hash
	$$dataset{content_path} = $fullpath;
	$$dataset{md5} = $md5;
	$$dataset{content_count} = $content_count;
	$$dataset{content_length} = $content_length;
	return $dataset;
}

sub _describeBlastPartitions {
	my ( $dataset, $partitionpath ) = @_;
	my $numpartitions = `ls -1 $partitionpath | wc -l`;
	$numpartitions =~ s/\n//g;
	
	my $estimatedflag = "";
	my $samplepath = $partitionpath;
	my $samplesize = `ls -1 $samplepath | wc -l`;
	$samplesize =~ s/\n//g;
	while ( $samplesize > 99 ) {
		$estimatedflag = "~";
		$samplepath =~ s/\*/\*0/;
		$samplesize = `ls -1 $samplepath | wc -l`;
		$samplesize =~ s/\n//g;
	}
	if ( int($samplesize) <= 0 ) {
		$errorMessage = "_describeBlastPartitions: could not sample partitions \"$partitionpath\".";
		return undef;
	}

	my ( $md5 ) = split( /\s/, `cat $samplepath | md5sum` );

	my $content_count = `cat $samplepath | grep ">" | wc -l`;
	$content_count =~ s/\n//g;

	my $defsize = `cat $samplepath | grep ">" | wc -c`;
	$defsize -= $content_count;
	my $totlines = `cat $samplepath | wc -l`;
	my $totsize = `cat $samplepath | wc -c`;
	$totsize -= $totlines;
	my $content_length = $totsize - $defsize;

	my $scale = $numpartitions / $samplesize;
	$content_count = int ( $scale * $content_count + 0.5 );
	$content_length = int ( $scale * $content_length + 0.5 );

	$$dataset{content_path} = $partitionpath;
	$$dataset{md5} = $md5;
	$$dataset{content_count} = $estimatedflag . $content_count;
	$$dataset{content_length} = $estimatedflag . $content_length;
	return $dataset;
	
}

sub _describeHmmDB {
	my ( $dataset, $dbpath ) = @_;
	my ( $md5 ) = split( /\s/, `md5sum $dbpath` );
	my $content_count;
	if ( -T $dbpath ) {
		$content_count = `grep ALPH $dbpath | wc -l`;
		$content_count =~ s/\n//g;
	} else {
		$content_count = "n/a";		
	}
	my ( $content_length ) = split( /\s/, `ls -s --block-size=1 $dbpath` );

	$$dataset{content_path} = $dbpath;
	$$dataset{md5} = $md5;
	$$dataset{content_count} = $content_count;
	$$dataset{content_length} = $content_length;
	
	return $dataset;	
}

sub _describeRpsDB {
	my ( $dataset, $dbpath ) = @_;

	my ( $md5 ) = split( /\s/, `cat $dbpath/*.rps $dbpath/*.phr | md5sum` );
	
	my $content_count = `cat $dbpath/*.aux | wc -l`;
	$content_count =~ s/\n//g;
	$content_count = ( $content_count - 8 ) / 2;
	my $content_length = `cat $dbpath/*.psq | wc -c`;
	$content_length =~ s/\n//g;
	$content_length  = $content_length - $content_count - 1;

	$$dataset{content_path} = $dbpath;
	$$dataset{md5} = $md5;
	$$dataset{content_count} = $content_count;
	$$dataset{content_length} = $content_length;
	
	return $dataset;	
}

#	
# write dataset to db
sub _saveDataset {
	my ( $dbh, $dataset, $obsdataset ) = @_;
	$dbh->begin_work;
	
# updated version of existing dataset?
	if ( defined $obsdataset ) {
		$$dataset{dataset_name} = $$obsdataset{dataset_name};
		$$dataset{dataset_version} = $$obsdataset{dataset_version} + 1;

# or first version of new dataset
	} else {
		$$dataset{dataset_version} = 0;		
	}
	$$dataset{is_obsolete} = 0;
	
# assign a version id
	my $seq = &openSequence( $dbh, "version_id");
	if ( !defined $seq ) {
		$errorMessage = "_saveDataset (version_id): " . $errorMessage;
		return undef;
	}
	$$dataset{version_id} = &nextSequenceValue( "version_id" );
	if ( !defined $$dataset{version_id} ) {
		$errorMessage = "_saveDataset (version_id): " . $errorMessage;
		return undef;
	}
	&closeSequence( "version_id" );

# for query nodes, save sequence data to db
	if ( $$dataset{content_type} eq "seq" && (defined $$dataset{query_node}) && $$dataset{query_node} > 0 ) {
		$dataset = &_loadDatasetFasta( $dbh, $dataset );
		if ( !defined $dataset ) {
			$errorMessage = "_saveDataset: " . $errorMessage;
			return undef;
		}
	}
	
# for hmm databases, save hmm annotation (if any)
if ( defined $$dataset{hmm_annotation} ) {
	my $delete = &executeSQL( $dbh, "delete from hmm_annotation where version_id=?", $$dataset{version_id} );
	if ( !defined $delete ) {
		$errorMessage = "_saveDataset (delete hmm anno): " . $errorMessage;
		return undef;
	}
	
	my $annotation = $$dataset{hmm_annotation};
	foreach my $anno ( values %$annotation ) {
		my $insert = &executeSQL( $dbh,
			"insert into hmm_annotation(version_id,hmm_acc,hmm_len,noise_cutoff,trusted_cutoff,definition) values(?,?,?,?,?,?)",
			$$dataset{version_id}, $$anno{hmm_acc}, $$anno{hmm_len}, $$anno{noise_cutoff}, $$anno{trusted_cutoff}, $$anno{definition} );
		if ( !defined $insert ) {
			$errorMessage = "_saveDataset (insert hmm anno): " . $errorMessage;
			return undef;
		}
	}
}
	
# save version details to db
	$$dataset{date_released} = &now;
	$$dataset{released_by} = getlogin || getpwuid($>) || 'ccbuild';

	my $details = &_getVersionDetails( $dataset );
	my @cols = keys %$details;
	my $columns = join( ",", @cols );
	my $values = join( ",", split( //, &rpad( "?", scalar @cols, "?" ) ) );
	my @row = &extractArrayFromHash( $details, @cols );
	my $add = &executeSQL( $dbh, "insert into dataset_version($columns) values($values)", @row );
	if ( !defined $add ) {
		$errorMessage = "_saveDataset (add): " . $errorMessage;
		return undef;
	}

# redirect existing dataset to new version
	if ( defined $obsdataset ) {
		my $redirect = &executeSQL( $dbh,
			"update dataset set current_version=?, is_obsolete=? where dataset_name=?",
			$$dataset{dataset_version}, $$dataset{is_obsolete}, $$dataset{dataset_name} );	
		if ( !defined $redirect ) {
			$errorMessage = "updateFile (redirect): " . $errorMessage;
			return undef;
		}

		my $obsolete = &executeSQL( $dbh,
			"update dataset_version set date_retired=?, retired_by=? "
				. "where version_id=?",
			&now, getlogin || getpwuid($>) || 'ccbuild', $$obsdataset{version_id} );
		if ( !defined $obsolete ) {
			$errorMessage = "updateFile (obsolete): " . $errorMessage;
			return undef;
		}

# create new dataset
	} else {
		my $add = &executeSQL( $dbh,
			"insert into dataset(dataset_name,current_version,is_obsolete)"
				."values(?,?,?)",
			$$dataset{dataset_name}, $$dataset{dataset_version}, $$dataset{is_obsolete} );
		if ( !defined $add ) {
			$errorMessage = "updateFile (add): " . $errorMessage;
			return undef;
		}
	}

# return dataset
	$dbh->commit;
	return $dataset
}	

sub _blastNodeFastaFile {
	my ( $node_id, $node_owner, $workspace ) = @_;
	
# default workspace is current directory
	if ( !defined $workspace ) {
		$workspace = ".";
	} elsif ( $workspace =~ /\/\s*$/ ) {
		 $workspace =~ s/\/\s*$//;
	}

# concatenate blast fasta partitions
	my $blastpath = $filestore . "/" . $node_owner. "/" . $node_id;
	my @partitions = `ls -1 $blastpath/p_*.fasta`;
	if ( scalar @partitions == 0 ) {
		$errorMessage = "_blastNodeFastaFile: could not find fasta partitions.";
		return undef;
	}
	@partitions = sort { int( &_comparePartitions( $a, $b ) ) } @partitions;
	my $tmpfasta = $workspace . "/" . "tmp$$.fasta";
	unlink $tmpfasta;

	foreach my $partition ( @partitions ) {
		$partition =~ s/\n//g;
		my $error = system("cat $partition >> $tmpfasta");
		if ( $error ) {
			$errorMessage = "_blastNodeFastaFile: could not concatenate partitions: $error";
			unlink $tmpfasta;
			return undef;
		}
	}
	
	return $tmpfasta;
}

sub _blastNodePartitionPath {
	my ( $node_id, $node_owner ) = @_;

	my $path = $filestore . "/" . $node_owner. "/BlastDatabases/" . $node_id . "/p_*.fasta";
	return $path;
}

sub _hmmDBPath {
	my ( $node_id, $node_owner ) = @_;

	my $path = $filestore . "/" . $node_owner. "/HmmerPfamDatabase/" . $node_id . "/pfam_db_file";
	return $path;
}

sub _rpsDBPath {
	my ( $node_id, $node_owner ) = @_;

	my $path = $filestore . "/" . $node_owner. "/ReversePsiBlastDatabase/" . $node_id;
	return $path;
}

#
# get version detail attributes
sub _getVersionDetails {
	my ( $dataset ) = @_;
	
	my $fasta;
	for my $key ("version_id","dataset_name","dataset_version","description","content_type","source_type",
					"content_path","md5","content_count","content_length","date_released",
					"released_by","date_retired","retired_by",
					"query_node","date_uploaded","uploaded_by","seq_attr_list",
					"subject_node","date_formatted","formatted_by") {
		$$fasta{$key} = $$dataset{$key};
	}
	return $fasta;
}

#
# bulk insert dataset sequences
sub _bulkAddSequences {
	my ( $dbh, $sequences, $tags ) = @_;
	
	my $attrs = ();
	my $seqs = ();
	
	foreach my $seq ( @$sequences ) {
		my @seq = ( $$seq{version_id}, $$seq{seq_id}, $$seq{seq_acc}, $$seq{seq_definition}, $$seq{seq_length} );
		push ( @$seqs, \@seq );
		
		foreach my $tag ( @$tags ) {
			my @attr = ( $$seq{version_id}, $$seq{seq_id}, $tag, $$seq{$tag} );
			push ( @$attrs, \@attr );			
		}		
	}
	
	my $insert = &bulkInsertData( $dbh,
		"insert into dataset_seq(version_id,seq_id,seq_acc,seq_definition,seq_length) values(?,?,?,?,?)",
		$seqs );
	if ( !defined $insert ) {
		$errorMessage = "_bulkAddSequences: ". $errorMessage;
		return undef;
	}
	
	$insert = &bulkInsertData( $dbh,
		"insert into dataset_seq_attr(version_id,seq_id,attr_name,attr_value) values(?,?,?,?)",
		$attrs );
	if ( !defined $insert ) {
		$errorMessage = "_bulkAddSequences: ". $errorMessage;
		return undef;
	}
	
	return scalar @$sequences;
}

sub _loadDatasetFasta {
# read fasta file and return array of hashes with tags defline and seq
	my ( $dbh, $dataset ) = @_;

	my @tags;
    @tags = sort split( /,/, $$dataset{seq_attr_list} ) if (defined $$dataset{seq_attr_list});
	
	if ( $$dataset{content_type} ne "seq" ) {
		$errorMessage = "_loadDatasetFasta: no fasta for content type $$dataset{content_type}.";
		return undef;
	}
	my $fastapath = $$dataset{content_path};
	my $version_id = $$dataset{version_id};
	
	my @sequences = ();
	my $defline = "";
	my $sequence = "";
	my $cnt = 0;
	my $len = 0;
	
# open seq identifier
	my $idseq = &openSequence( $dbh, "seq_id", 100000 );
	if ( !defined $idseq ) {
		$errorMessage = "_loadDatasetFasta: " . $errorMessage;
		return undef;
	}

# open file
	if ( !open(FASTA,"cat $fastapath |") ) {
		$errorMessage = "_loadDatasetFasta: could not open content.";
		return undef;
	}

# ">" flags defline
	while ( my $line = <FASTA> ) {
		$line =~ s/\n//g;
		$line =~ s/\r//g;

# new defline
		if ( substr($line,0,1) eq ">" ) {

# save previous sequence
			if ( length($defline)>0 && length($sequence)>0 ) {
				$sequence =~ s/\s+//g;
				my $entry = _parseFastaEntry( $defline, $sequence, \@tags );
				$$entry{version_id} = $version_id;
				$$entry{seq_id} = &nextSequenceValue( "seq_id" );
				push ( @sequences, $entry );
				$cnt++;
				$len += $$entry{seq_length};
				if ( scalar @sequences >= 100000 ) {
					if ( !defined &_bulkAddSequences( $dbh, \@sequences, \@tags ) ) {
						$errorMessage = "_loadDatasetFasta: ". $errorMessage;
						return undef;
					}
					@sequences = ();
				}
			}
# start new sequence
			$defline = substr($line,1);
			$defline =~ s/\s+/ /g;
			$defline =~ s/ $//;
			$sequence = "";

# append to sequence
		} else {
			$sequence .= $line;
		}
	}
	close(FASTA);

# save last sequence
	if ( length($defline)>0 && length($sequence)>0 ) {
		my $entry = _parseFastaEntry( $defline, $sequence, \@tags );
		$$entry{version_id} = $version_id;
		$$entry{seq_id} = &nextSequenceValue( "seq_id" );
		$cnt++;
		$len += $$entry{seq_length};
		push ( @sequences, $entry );
	}
	if ( !defined &_bulkAddSequences( $dbh, \@sequences, \@tags ) ) {
		$errorMessage = "_loadDatasetFasta: ". $errorMessage;
		return undef;
	}

# return modified dataset
	$$dataset{content_count} = $cnt;
	$$dataset{content_length} = $len;
	$$dataset{seq_attr_list} = join( ",", sort @tags );
	return $dataset;
}

#
# parse fasta file entry
sub _parseFastaEntry {
	my ( $defline, $sequence, $tags ) = @_;
	
	$defline =~ s/^\s+//;

	my ( $seq_acc ) = split( /\s/, $defline );
	if ( index($seq_acc,"\|")>=0 ) {
	    	my @tmp = split ( /\|/, $seq_acc );
    		$seq_acc = $tmp[0] . "|" . $tmp[1];
	}
	
	my $seq_length = length($sequence);
	
	my $entry;
	$$entry{seq_acc} = $seq_acc;
	$$entry{seq_definition} = $defline;
	$$entry{seq_length} = $seq_length;

	foreach my $tag ( @$tags ) {
		my $tmp = $defline;
		my $tagstart = index( $tmp, "/" . $tag . "=" );
		if ( $tagstart >=0 ) {
			$tmp = substr( $tmp, $tagstart + length($tag) + 2 ); 
			my $tagend;
			if ( substr( $tmp, 0, 1 ) eq "\"" ) {
				$tmp = substr( $tmp, 1 );
				$tagend = index( $tmp, "\"" );
			} else {
				$tagend = index( $tmp, " " );
			}
			$$entry{$tag} = substr( $tmp, 0, $tagend);
		}
	}
	
	return $entry;
}

sub _getDatasetAnnotation {
	my ( $dbh, $dataset ) = @_;
	if ( $$dataset{content_type} eq "hmm" ) {
		my $hmm_annotation = &querySQLArrayHash( $dbh,
			"select hmm_acc, hmm_len, noise_cutoff, trusted_cutoff, definition from hmm_annotation where version_id=?",
			$$dataset{version_id} );
		if ( !defined $hmm_annotation ) {
			$errorMessage = "_getDatasetAnnotation: hmm anno: " . $errorMessage;
			return undef;
		}
		if ( scalar @$hmm_annotation > 0 ) { $$dataset{hmm_annotation} = &arrayHashToHashHash( $hmm_annotation, "hmm_acc" ) }
	}

	return $dataset;
}
1;
