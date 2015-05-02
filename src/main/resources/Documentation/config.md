Setup
=====

The Gitiles plugin for Gerrit allows you to list and browse all your Gerrit
repositories using [Gitiles](http://code.google.com/p/gitiles).

Once you have installed the plugin, run `java -jar gerrit.war init -d my-site`
to configure Gitiles as your source browser for Gerrit, replacing gitweb. The
plugin points Gitiles back at the running Gerrit server for things like
Change-Id links, so you can be up and running with no additional configuration.
If necessary, though, you can put additional Gitiles configuration in
`etc/gitiles.config`.

Access Controls
---------------

Gitiles uses Gerrit access controls behind the scenes: the only repositories
listed are those that are visible to the current user, logged in or not, and
branch-level access controls are applied as well. Objects can only be looked up
by SHA-1 if they are reachable from a visible branch.
