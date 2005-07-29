#!/usr/bin/perl -w
#
# $Id$
#
# Appends the contents of a form field to a file.

use CGI;

my $query = new CGI;
my $email = $query->param('email');

if (open(OUT, ">>/export/bang/emails.txt")) {
    print OUT "$email\n";
    close(OUT);
    print $query->redirect("thanks.html");
} else {
    print $query->header(-status=>500),
    $query->start_html("Internal Error"),
    $query->h2("Dadgummit!"),
    $query->strong("Sorry pardner. We're having some technical difficulties. " .
                   "If ya could try again later, we'd be might appreciative.");
}
exit 0;
