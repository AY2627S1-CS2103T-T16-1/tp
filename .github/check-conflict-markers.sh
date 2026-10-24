#!/bin/sh
# Checks for git merge-conflict markers left in tracked files.
# Matches the markers git writes: <<<<<<< , ||||||| , ======= and >>>>>>>

git grep --cached -I -n --no-color -P '^(<{7}|\|{7}|={7}|>{7})( |$)' -- ':/' |
awk '
    BEGIN {
        FS = ":"
        OFS = ":"
        ret = 0
    }
    {
        ret = 1
        print "ERROR", $1, $2, " git conflict marker."
    }
    END {
        exit ret
    }
'
