#!/usr/bin/perl -w
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

use Getopt::Long;
use File::Basename;

my $prog = basename($0);

my $usage = "
  Program to read a CSV file of (a,x) values and create a maxima
  batch script to generate the expected gamma_p(a, x) derivative.

  The output maxima script is named '[input]_p_derivative.mac'.

Usage:

  $prog input [...]

Options:

  input     CSV file(s)

  -exec     Execute maxima as a batch process on the script.
            The output maxima result file is named '[input]_p_derivative.csv'.

  -help     Print this help and exit

";

my ($help, $exec);
GetOptions(
  "help" => \$help,
  "exec" => \$exec,
);

die $usage if $help;

@ARGV or die $usage;

my @files;
for (@ARGV) {
  if (m/\*/) {
    push @files, glob "$_";
  } else {
    push @files, $_;
  }
}

# Process each file
for $input (@files) {
  open (IN, $input) or die "Failed to open '$input': $!\n";
  # strip file extension
  my $out = $input;
  $out =~ s/\.[^\.]+$//;
  $out = "${out}_p_derivative";
  open (OUT, ">$out.mac") or die "Failed to open '$out.mac': $!\n";

  #
  #  Start the maxima script
  #
  print OUT <<__END;
kill(all);
fpprec : 128;

/* Function to open a file and add the Apache license header. */
header(s) :=
  block(out : openw(s),
    printf(out, "#~%"),
    printf(out, "# Licensed to the Apache Software Foundation (ASF) under one or more~%"),
    printf(out, "# contributor license agreements.  See the NOTICE file distributed with~%"),
    printf(out, "# this work for additional information regarding copyright ownership.~%"),
    printf(out, "# The ASF licenses this file to You under the Apache License, Version 2.0~%"),
    printf(out, "# (the \\"License\\"); you may not use this file except in compliance with~%"),
    printf(out, "# the License.  You may obtain a copy of the License at~%"),
    printf(out, "#~%"),
    printf(out, "#     http://www.apache.org/licenses/LICENSE-2.0~%"),
    printf(out, "#~%"),
    printf(out, "# Unless required by applicable law or agreed to in writing, software~%"),
    printf(out, "# distributed under the License is distributed on an \\"AS IS\\" BASIS,~%"),
    printf(out, "# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.~%"),
    printf(out, "# See the License for the specific language governing permissions and~%"),
    printf(out, "# limitations under the License.~%"),
    printf(out, "~%"),
    printf(out, "# Generated using maxima from a script built by $prog~%"),
    printf(out, "# Input data: $input~%"),
    printf(out, "# a, x, gamma_p_derivative(a, x), log(gamma_p_derivative(a, x)),~%"),
    return (out) );

/* Input data. */
l: [
__END

  #
  # Program to read the CSV file and create a list variable in maxima
  #

  my $i = 0;
  while (<IN>) {
    # Skip comments and lines without a comma
    next if (m/^#/);
    next unless m/,/;
    # Get the first two fields
    my ($a,$x) = split /[, ]+/, $_;
    # Remove trailing space
    chomp($x);
    # Print a trailing comma for the previous field
    print OUT ",\n" if $i++;
    # Print the fields for the maxima list data
    print OUT "[$a,$x]";
  }

  #
  #  Finish the maxima script
  #
  print OUT <<__END;

];

/* Formatting function to convert big float exponent to e. */
str(x) := ssubst("e","b",string(x));

xlogy(x, y) := if x = 0 then 0 else x * log(y);

out : header("$out.csv");
for pair in l do
 (a : bfloat(pair[1]),
  x : bfloat(pair[2]),
  printf(out, "~f,~f,~a,~a~%", a, x, str(exp(-x) * x^(a - 1) / gamma(a)), str(xlogy(a-1, x) - x - log_gamma(a)) )), fpprintprec:30;
close(out);
__END
  close IN;
  close OUT;

  if ($exec) {
    $filename = "$out.mac";
    `maxima -b $filename` or die "Failed to execute 'maxima': $!";
    unlink($filename) or die "Can't delete $filename: $!\n";
  }
}
