##
#  Copyright (C) 2016 IXA Taldea, University of the Basque Country UPV/EHU
#
#   This file is part of ixa-pipe-wikify-ukb.
#
#   ixa-pipe-wikify-ukb is free software: you can redistribute it and/or modify
#   it under the terms of the GNU General Public License as published by
#   the Free Software Foundation, either version 3 of the License, or
#   (at your option) any later version.
#
#   ixa-pipe-wikify-ukb is distributed in the hope that it will be useful,
#   but WITHOUT ANY WARRANTY; without even the implied warranty of
#   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#   GNU General Public License for more details.
#
#   You should have received a copy of the GNU General Public License
#   along with ixa-pipe-wikify.  If not, see <http://www.gnu.org/licenses/>.
##

use FindBin qw($Bin);
use lib $Bin;
use UpperMatchSQLite;
use strict;
use warnings;
use DBI;
use Getopt::Long qw(GetOptions);


my $db_file;

die "Usage: $0 --db DB_FILE TOKEN" if ( @ARGV < 2 or
          ! GetOptions('db|d=s' => \$db_file));


UpperMatchSQLite::connect_database ($db_file);
UpperMatchSQLite::setUPPER(0);

my $text = ""; ##text: token
while(<>){
    chomp;
    $text .= $_;
}

my $prob = "";
$prob = UpperMatchSQLite::get_linked_prob($text);
if($prob){
    print $prob;
}
else{
    print "NILL";
}

UpperMatchSQLite::disconnect_database();
