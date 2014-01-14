This file is included in SPLAT to ensure that the directory is
included on the classpath - unfortunately Lein/Java doesn't include
missing directories, so if the directory is created later (while the
application is still running), resources in those directories cannot
be found.

I'd recommend checking this into your Git repo (you may need to `git
add -f` to convince Git that you want to check in a file in an ignored
directory) so that people who clone your repo don't get hit with this
type of bug!