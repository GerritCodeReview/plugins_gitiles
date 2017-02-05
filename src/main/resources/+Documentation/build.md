Build
=====

This plugin can be built with Buck.

Buck
----

Two build modes are supported: Standalone and in Gerrit tree.
The standalone build mode is recommended, as this mode doesn't require
the Gerrit tree to exist locally.


### Build standalone

Clone bucklets library:

```
  git clone https://gerrit.googlesource.com/bucklets

```
and link it to gitiles plugin directory:

```
  cd gitiles && ln -s ../bucklets .
```

Add link to the .buckversion file:

```
  cd gitiles && ln -s bucklets/buckversion .buckversion
```

Add link to the .watchmanconfig file:

```
  cd gitiles && ln -s bucklets/watchmanconfig .watchmanconfig
```

To build the plugin, issue the following command:

```
  buck build plugin
```

The output is created in

```
  buck-out/gen/gitiles.jar
```

### Build in Gerrit tree

Clone or link this plugin to the plugins directory of Gerrit's source
tree, and issue the command:

```
  buck build plugins/gitiles
```

The output is created in

```
  buck-out/gen/plugins/gitiles/gitiles/gitiles.jar
```

This project can be imported into the Eclipse IDE:

```
  ./tools/eclipse/project.py
```

How to build the Gerrit Plugin API is described in the [Gerrit
documentation](../../../Documentation/dev-buck.html#_extension_and_plugin_api_jar_files).
