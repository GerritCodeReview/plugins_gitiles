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
`gerrit.linkname`. Defaults to `browse` if not set.

The link frame target name can be configured with `gerrit.target`. Valid values
are defined in the [W3C HTML Specification](https://www.w3.org/TR/1999/REC-html401-19991224/types.html#type-frame-target).
Defaults to `_blank` if not set.

```
  [gerrit]
    linkname = gitiles
    target = _self
```

The flag `gerrit.noWebLinks` can be set to `true` to run Gitiles without
adding links in the Gerrit UI. Defaults to `false` if not set, meaning the
links are added.

```
  [gerrit]
    noWebLinks = true
```

The flag `gerrit.baseUrl` can be set to a path other than the default location
that Gitiles is being exposed. By default, it's at the path to Gerrit + plugins/gitiles/

When Gerrit is behind a reverse proxy, or via rewrite rules, Gitiles can be placed
in easier-to-remember URLs.

```
  [gerrit]
    baseUrl = https://gerrit.example.com/gitiles/
```

Note: If using this setting you possibly want to set Gerrit's auth.cookiePath to "/"
if it's not running in document root already.

The flag `gerrit.cloneUrlType` can be set to either `ssh`, `http`, or `git`. This
will determine which protocol to be used to display repos' clone url to the user:
`git` will use value of `gerrit.canonicalGitUrl` and `http` will use the value of
`gerrit.gitHttpUrl`. If those values are not set then http/git format will fall
back to `gerrit.canonicalWebUrl`.
If it's not set, or it fails to get the set type of URL, it will automatically prefer
SSH, then HTTP, then Git.

```
  [gerrit]
    cloneUrlType = http
```
