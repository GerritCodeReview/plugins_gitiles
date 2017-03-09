Setup
=====

The Gitiles plugin for Gerrit allows you to list and browse all your Gerrit
repositories using [Gitiles](http://code.google.com/p/gitiles).

The plugin points Gitiles back at the running Gerrit server for things like
Change-Id links, so you can be up and running with no additional configuration.
If necessary, though, you can put additional Gitiles configuration in
`etc/gitiles.config`.

Gitiles now provides its own links through the weblinks extensions in Gerrit.
If you upgrade from an older version of the Gitiles plugin, that uses the gitweb
configuration in 'etc/gerrit.config', you will have to remove this section or
the links will be in duplicate.

Access Controls
---------------

Gitiles uses Gerrit access controls behind the scenes: the only repositories
listed are those that are visible to the current user, logged in or not, and
branch-level access controls are applied as well. Objects can only be looked up
by SHA-1 if they are reachable from a visible branch.

File `gitiles.config`
---------------------

The optional file `$site_path/etc/gitiles.config` is a Git-style config file
that controls the settings for the core gitiles servlet. Some settings specific
to the gitiles plugin can also be configured.

The name of the link that appears in Gerrit can be configured with
`gerrit.linkname`.

The link frame target name can be configured with `gerrit.target`. Valid values
are defined in the [W3C HTML Specification](https://www.w3.org/TR/1999/REC-html401-19991224/types.html#type-frame-target).
Defaults to `_blank` if not set.

```
  [gerrit]
    linkname = browse
    target = _self
```
