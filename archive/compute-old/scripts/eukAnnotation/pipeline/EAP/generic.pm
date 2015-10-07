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

use Exporter 'import';
@EXPORT_OK = qw(rpad);
use strict;
use warnings;
our $errorMessage;
###################################################################
# generic utilities
###################################################################
#
# extract values from hash and return as array
sub extractArrayFromHash{

	my ( $inhash, @keys ) = @_;
	$errorMessage = undef;
	
	my @outarray = ();
	for my $key ( @keys ) {
		if ( defined $inhash->{$key} ) {
			push(@outarray,$inhash->{$key});
		} else {
			push(@outarray,undef);
		}
	}
	return @outarray;
}

#
# convert an array of hashes to a hash of hashes
sub arrayHashToHashHash {
	my ( $array, $key ) = @_;
	$errorMessage = undef;
	
	if ( !defined $array ) {
		$errorMessage = "ArrayHashToHashHash: input array is undefined";
		return undef
	};
	
	if ( !defined $key ) {
		$errorMessage = "ArrayHashToHashHash: key is undefined";
		return undef
	};
		
	my %hash;
	foreach my $item ( @$array ) {
		if ( !defined $$item{$key} ) {
			$errorMessage = "ArrayHashToHashHash: keyless item";
			return undef;
		}
		$hash{ $$item{$key} } = $item;
	}

	return \%hash;
}

sub findArrayValue {
	my ( $value, $array ) = @_;
	
	foreach my $i ( 0..scalar @$array -1 ) {
		if ( $$array[$i] eq $value ) { return $i }
	}
	return -1;
}

#
# right pad string to fixed length
sub rpad {
	my ( $text, $pad_len, $pad_char) = @_;

	if ( !defined $pad_char ) {
		$pad_char = " ";
	} elsif ( length($pad_char)>1 ) {
		$pad_char = substr($pad_char,0,1);
	}

    $text = $pad_char unless defined $text;
	
	if ( $pad_len<=0 ) {
		return "";
	} elsif ( $pad_len<=length($text) ) {
		return substr($text,0,$pad_len);
	}

	if ( $pad_len>length($text) ) {
		$text .= $pad_char x ( $pad_len - length( $text ) );
	}
	
	return "$text"; 
}

#
# left pad string to fixed length
sub lpad {
	my ( $text, $pad_len, $pad_char) = @_;

	if ( !defined $pad_char ) {
		$pad_char = " ";
	} elsif ( length($pad_char)>1 ) {
		$pad_char = substr($pad_char,0,1);
	}

    $text = $pad_char unless defined $text;

	if ( $pad_len<=0 ) {
		return "";
	} elsif ( $pad_len<length($text) ) {
		return substr($text,0,$pad_len);
	}

	if ( $pad_len>length($text) ) {
		$text = $pad_char x ( $pad_len - length( $text ) ). $text;
	}
	
	return "$text"; 
}

#
# center string in fixed length
sub cpad {
	my ( $text, $pad_len, $pad_char) = @_;

	if ( !defined $pad_char ) {
		$pad_char = " ";
	} elsif ( length($pad_char)>1 ) {
		$pad_char = substr($pad_char,0,1);
	}

    $text = $pad_char unless defined $text;
	
	if ( $pad_len<=0 ) {
		return "";
	} elsif ( $pad_len<length($text) ) {
		return substr($text,0,$pad_len);
	}

	my $margin = int( ( $pad_len - length($text) ) / 2. );
	if ( $margin>0 ) {
		$text = &lpad($pad_char,$margin,$pad_char) . $text;
	}

	$margin = $pad_len - length($text);
	if ( $margin>0 ) {
		$text .= &rpad($pad_char,$margin,$pad_char);
	}

	return "$text"; 
}

#
# return current date and time YYYY-MM-DD:HH:MI:SS
sub now {
	my ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdst) = localtime(time);
	
	$sec = &lpad($sec,2,"0");
	$min = &lpad($min,2,"0");
	$hour = &lpad($hour,2,"0");
	$mday = &lpad($mday,2,"0");
	$mon = &lpad($mon+1,2,"0");
	$year = &lpad($year+1900,4,"0");
	my $now = "$year-$mon-$mday $hour:$min:$sec";
	return $now;
}

#
# return current date YYYY-MM-DD
sub today {
	my ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdst) = localtime(time);
	
	$mday = &lpad($mday,2,"0");
	$mon = &lpad($mon+1,2,"0");
	$year = &lpad($year+1900,4,"0");
	$year+=100;
	my $today = "$year-$mon-$mday";
	return $today;
}
1;
