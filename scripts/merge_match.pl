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
#   along with ixa-pipe-wikify-ukb.  If not, see <http://www.gnu.org/licenses/>.
##

use FindBin qw($Bin);
use lib $Bin;
use UpperMatchSQLite;
use strict;
use warnings;
use DBI;
use Getopt::Long qw(GetOptions);

my $db_file;
my $text1; ## input text1: token1_1@@offset1_1 token1_2@@offset1_2...
my $text2; ## input text2: token2_1@@offset2_1 token2_2@@offset2_2...


die "Usage: $0 --db DB_FILE --t1 TEXT1 --t2 TEXT2\n\tTEXT: token1\@\@offset1 token2\@\@offset2..." if ( @ARGV < 4 or
          ! GetOptions('db|d=s' => \$db_file,
		       't1=s' => \$text1,
		       't2=s' => \$text2));
 

UpperMatchSQLite::connect_database ($db_file);
UpperMatchSQLite::setUPPER(0);

my @output = UpperMatchSQLite::merge_match($text1, $text2);
print "@output";

UpperMatchSQLite::disconnect_database();

