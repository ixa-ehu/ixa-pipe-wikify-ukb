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

package UpperMatchSQLite;
use strict;
use warnings;
use DBI;
use URI::Escape;


my $dbh;
my $UPPER = 1;

my $db_file;
my %stopwords ;

init_stopwords() ;



sub setUPPER
{
    $UPPER = $_[0];
}


sub connect_database
{
    $db_file = $_[0];
    $dbh = DBI->connect("dbi:SQLite:dbname=$db_file","","",{RaiseError => 1},) or die $DBI::errstr;
}


sub disconnect_database
{
    $dbh->disconnect;
}


sub create_database
{
    my $dict_file = $_[0];
    my $lang = $_[1];
    my $db_file = $_[2];

    my %first_word = ();
    $dbh = DBI->connect("dbi:SQLite:dbname=$db_file","","",{ RaiseError => 1, AutoCommit => 0},);
    if (!$dbh)
    {
	die "Create UTF-8 database with $db_file name.\n";	
    }
    if ($dbh)
    {    
	print "$db_file: do you want to (re)write all? this will take 2-3 hours. yes/no\n";
	my $yesno =  <STDIN>;
	chomp ($yesno);
	if ($yesno eq "yes")
	{
	    my $creation_stamp = gmtime(time());
	    $dbh->do("DROP TABLE IF EXISTS metadata");
	    $dbh->do("DROP TABLE IF EXISTS dictionary");
	    $dbh->do("DROP TABLE IF EXISTS firstword");
	    $dbh->do("CREATE TABLE metadata(dbname TEXT PRIMARY KEY, date TEXT, dictfile TEXT, lang TEXT)");
	    $dbh->commit(); 
	    my $sth = $dbh->prepare("INSERT INTO metadata(dbname,date,dictfile,lang) VALUES (?, ?, ?, ?)");
	    $sth->execute($db_file,$creation_stamp,$dict_file,$lang);
	    $dbh->do("CREATE TABLE dictionary(entry TEXT PRIMARY KEY, entities TEXT, linkedprob TEXT)");
	    $dbh->commit(); 
	    $sth = $dbh->prepare("INSERT INTO dictionary(entry,entities,linkedprob) VALUES (?, ?, ?)");

	    open (I,"-|:encoding(UTF-8)", "bzcat $dict_file") || die $! ; 
	    my ($anch,@ent,$prob);
	    while (<I>) 
	    {
		chomp;
		($anch,$prob, @ent) = split(/\s/,$_) ;
		$anch =~ s/\'/''/g;
		if ($anch =~ m/^([^_]*)_/)
		{
		    if (defined $first_word {$1})
		    {
			my $aurrekoluzera = $first_word {$1};
			if ($aurrekoluzera < length($anch))
			{
			    $first_word {$1} = length($anch);
			}
		    }
		    else
		    {
			$first_word {$1} = length($anch);
		    }
		}
		if ($anch =~ m/^([^_]*)$/)
		{
		    if (defined $first_word {$1})
		    {
			my $aurrekoluzera = $first_word {$1};
			if ($aurrekoluzera < length($anch))
			{
			    $first_word {$1} = length($anch);
			}
		    }
		    else
		    {
			$first_word {$1} = length($anch);
		    }
		}
		foreach my $a (@ent)
		{
		    $a =~ s/\'/''/g;
		}		
		$sth->execute(uri_escape_utf8($anch),uri_escape_utf8("@ent"),$prob);
	    }

	    $dbh->do("CREATE TABLE firstword(firstword TEXT PRIMARY KEY, maximunlength INTEGER)");
	    $dbh->commit(); 
	    $sth = $dbh->prepare("INSERT INTO firstword(firstword,maximunlength) VALUES (?, ?)");

	    foreach my $firstword (keys %first_word)
	    {
		$sth->execute(uri_escape_utf8($firstword), uri_escape_utf8($first_word{$firstword}));
	    }
	    $dbh->commit(); 
	    $dbh->disconnect();
	    warn "$db_file created ...\n";
	}
    }
}


sub merge_match
{
    my $text1 = $_[0]; ## input text1: token1_1@@offset1_1 token1_2@@offset1_2...
    my $text2 = $_[1]; ## input text2: token2_1@@offset2_1 token2_2@@offset2_2...

    my $match_text1 = join(" ", match($text1)); #garbitu($text1)); 
    my $match_text2 = join(" ", match($text2)); #garbitu($text2));

    return merge($match_text1, $match_text2);
}


sub merge
{
    my $text1 = $_[0]; ## token1@@offset1 token2@@offset2 ...
    my $text2 = $_[1]; ## token1@@offset1 token2@@offset2 ...

    my %w_hasierak;
    my %text1_h;
    my %text2_h;
    my @emaitza;

    my @t1 = split(" ", $text1);
    foreach my $m1_offset (@t1){
	next if (substr($m1_offset,-1,1) eq "@" || $m1_offset !~ /@@/);
	my($m1,$offset) = split("@@",$m1_offset);
	$text1_h{$offset}=$m1;
	$w_hasierak{$offset}++;
    }

    my @t2 = split(" ",$text2);
    foreach my $m2_offset (@t2){
	next if (substr($m2_offset,-1,1) eq "@" || $m2_offset !~ /@@/);
	my($m2,$offset) = split("@@",$m2_offset);
	$text2_h{$offset}=$m2;
	$w_hasierak{$offset}++;
    }

    my $off=0;
    foreach my $w_s (sort {$a <=> $b} keys %w_hasierak)
    {
	next if $w_s < $off;
	if ($w_hasierak{$w_s} eq 2)
	{
	    if(length($text1_h{$w_s})>=length($text2_h{$w_s}) )
	    {
		$text1_h{$w_s} =~ s/,$//g;
		push(@emaitza,$text1_h{$w_s}."@@".$w_s);
		$off = $w_s + length($text1_h{$w_s});
	    }
	    else
	    {
		push(@emaitza,$text2_h{$w_s}."@@".$w_s);
		$off = $w_s + length($text2_h{$w_s});
	    }
	}
	else
	{
	    if (defined $text1_h{$w_s})
	    {
		$text1_h{$w_s} =~ s/,$//g;
		push(@emaitza,$text1_h{$w_s}."@@".$w_s); 
		$off = $w_s + length($text1_h{$w_s});
	    }
	    if (defined $text2_h{$w_s})
	    {
		push(@emaitza,$text2_h{$w_s}."@@".$w_s); 
		$off = $w_s + length($text2_h{$w_s});
	    }
	}
    }    
    return @emaitza; ## @emaitza: {token1@@offset1, token2@@offset2...}
}


sub match
{
    ## input: token1@@offset1 token2@@offset2...
    my %firstword_maximunlength = ();
    my @emaitza;
    my $text = $_[0];

    my @tokens_offsets = split(" ", $text);
    foreach my $token_offset (@tokens_offsets)
    {

	my($token,$offset) = split("@@", $token_offset);

	next if (!defined $offset);
	next if (defined($stopwords{lc($token)}));
	$token =~ s/\'/''/g;
	my $sth = $dbh->prepare("SELECT * FROM firstword WHERE firstword = ?");
	$sth->execute(uri_escape_utf8(lc($token)));;
	while(my $tupla = $sth->fetchrow_hashref()) 
	{
	    $firstword_maximunlength{uri_unescape($tupla->{'firstword'})}=$tupla->{'maximunlength'};
	}    
    }   
    my $i;
    for($i = 0; $i <= @tokens_offsets; $i++)
    {
	next if !defined($tokens_offsets[$i]);

	my($token,$offset) = split("@@", $tokens_offsets[$i]);
	next if !defined($token);
	my $uneko_lehen_hitza = $token;
	my $luzeena;
	my $uneko_hitz_zerrenda = $uneko_lehen_hitza;
	next if !defined($firstword_maximunlength{lc($uneko_lehen_hitza)});
	
	for(my $ii = $i+1; $ii <= @tokens_offsets; $ii++)
	{
	    next if !defined($tokens_offsets[$ii]);

	    $uneko_hitz_zerrenda =~ s/\'/''/g;
	    my $sth = $dbh->prepare("SELECT * FROM dictionary WHERE entry = ?");
	    $sth->execute(uri_escape_utf8(lc($uneko_hitz_zerrenda)));
	    if(my $tupla = $sth->fetchrow_hashref())
	    {
		$luzeena = $uneko_hitz_zerrenda; 
	    }
	    last if (length($uneko_hitz_zerrenda) >= $firstword_maximunlength{lc($uneko_lehen_hitza)});
	    if(defined $tokens_offsets[$ii]){
		my($token2,$offset2) = split("@@", $tokens_offsets[$ii]);
		$uneko_hitz_zerrenda =  $uneko_hitz_zerrenda."_".$token2;
	    }
	}
	if(defined($luzeena))
	{
	    if ($UPPER){
		push (@emaitza,$luzeena."@@".$offset) if $luzeena =~ /[A-Z]/;
	    }
	    else{
		push (@emaitza,$luzeena."@@".$offset);
	    }
	    foreach my $t (split("_",$luzeena))
	    {
		$i ++; 
	    }
	    $i--;
	}
	
    }
    return @emaitza; ## @emaitza: {token1@@offset1, token2@@offset2...}
}


sub get_most_likely_BGlink
{
    ## input: token
    my $entry = &to_anchor($_[0]);
    my $result = "";
    $entry =~ s/\'/''/g;
    my @ent;
    my $winner;
    my $frekMax = 0;
    if ($dbh)
    {
	my $sth = $dbh->prepare("SELECT * FROM dictionary WHERE entry = ?");
	$sth->execute(uri_escape_utf8($entry));
	while(my $tupla = $sth->fetchrow_hashref()) 
	{    
	    @ent = split(" ",uri_unescape($tupla->{'entities'}));
	    foreach my $ent_frek (@ent)
	    {
		my($enti,$frek)= &parse_ent_freq($ent_frek);
		if ($frekMax < $frek)
		{
		    $winner = $enti;
		    $frekMax = $frek;
		}		
	    }
	    $result = $winner;
	}
    }
    else
    {
	die "database not working\n";		
    }
    return $result;
}


sub get_linked_prob
{
    ## input: token
    my $entry = $_[0];

    my $result = "";
    $entry =~ s/\'/''/g;

    if ($dbh)
    {
	my $sth = $dbh->prepare("SELECT * FROM dictionary WHERE entry = ?");
	$sth->execute(uri_escape_utf8($entry));

	if(my $tupla = $sth->fetchrow_hashref())
	{
	    $result = uri_unescape($tupla->{'linkedprob'});
	}
    }
    else
    {
	die "database not working\n";		
    }

    return $result;
}


sub parse_ent_freq
{
    my $str = shift;
    my ($ent, $freq);
    $freq = 0;
    my @aux = split(/:/, $str);
    if (@aux > 1 && $aux[-1] =~ /\d+/) 
    {
	$freq = pop @aux;
    }
    $ent = join(":", @aux);
    return ($ent, $freq);
}


sub to_anchor
{
    my $str = $_[0];
    $str =~ s/ /\_/g;
    return lc($str);
}


sub garbitu 
{
    my $zatia = $_[0];
    $zatia =~ s/(\w\w)\./$1 # /g;
    $zatia =~ s/,/ # /g;
    $zatia =~ s/:/ # /g;
    $zatia =~ s/!/ # /g;
    $zatia =~ s/\?/ # /g;
    $zatia =~ s/;/ # /g;
    $zatia =~ s/"/ /g;
    $zatia =~ s/\(/ # /g;
    $zatia =~ s/\)/ # /g;
    $zatia =~ s/ \@\@(\d+)/\@\@$1 /g;

    return $zatia;
}




sub init_stopwords
{    

    my $heredoc = << 'ENDHEREDOC' ;
a
adigai
al
arabera
ari
arte
artean
b
bai
baina
bat
batean
baten
batera
beste
beti
bezala
bi 
bidez
d
da
dago
dut
duzu
dira
du
dugu
dut
duzu
e
edo
egiten
era
ere
eta
ez
ezin
f
g
gaur
gauza
gero
gisa
gure
h
har
hartu
hau
hemen
hori
hots
i
ideia
izan
j
k
l
m
n
nahiz
naiz
nire
ni
nik
o
objektu
ondo
orain
ordea
oro
oso
p
r
s
soilik
t
ta
u
x
z
ze 
zer
zu
zuk
zure
urtarril
otsail
martxo
apiril
maiatz
ekain
uztail
abuztu
irail
urri
azaro
abendu
.
,
;
:
!
?
..
...
``
''
"
)
(
[
]
{
}
1
2
3
4
5
6
7
8
9
&amp;
&lt;
&gt;
'
ln
$
'_'
's
&
ENDHEREDOC

foreach (split(/\n/,$heredoc)) {
	chomp;
	$stopwords{"$_"} = 1;
    }
}


1;





