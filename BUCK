include_defs('//lib/maven.defs')

genrule(
  name = 'gitiles',
  cmd = ' && '.join([
    'cp $(location :gitiles_base) $OUT',
    'unzip -qd $TMP $(location :gitiles-servlet) "com/google/gitiles/static/*"',
    'cd $TMP/com/google/gitiles',
    'mv static +static',
    'zip -Drq $OUT -g . -i "+static/*"',
  ]),
  out = 'gitiles.jar',
  deps = [
    ':gitiles-servlet',
    ':gitiles_base',
  ],
)

gerrit_plugin(
  name = 'gitiles_base',
  srcs = glob(['src/main/java/**/*.java']),
  deps = [
    ':gitiles-servlet',

    # Deps only needed by Gitiles.
    ':guice-multibindings',
    ':soy',
    ':commons-lang3',
  ],
  # Deps shared with Gerrit but not in the plugin API.
  compile_deps = [
    '//lib/jgit:jgit-servlet',
  ],
  resources = glob(['src/main/resources/**/*']),
  manifest_entries = [
    'Gerrit-PluginName: gitiles',
    'Gerrit-Module: com.googlesource.gerrit.plugins.gitiles.Module',
    'Gerrit-HttpModule: com.googlesource.gerrit.plugins.gitiles.HttpModule',
    'Gerrit-InitStep: com.googlesource.gerrit.plugins.gitiles.InitGitiles',

    # Gitiles uses /repo to access a repo, so the default plugin layout would
    # disallow repos named "static" or "Documentation". Paths starting with +
    # are reserved by Gitiles and can't match repos.
    'Gerrit-HttpStaticPrefix: +static',
    'Gerrit-HttpDocumentationPrefix: +Documentation',
  ],
  visibility = [],
)

maven_jar(
  name = 'gitiles-servlet',
  id = 'com.google.gitiles:gitiles-servlet:0.1-2',
  sha1 = '31c84c6fdcde30174c70e4f1f5a5a8d71c57a19e',
  license = 'Apache2.0',
  repository = GERRIT,
  visibility = [],
)

maven_jar(
  name = 'commons-lang3',
  id = 'org.apache.commons:commons-lang3:3.1',
  sha1 = '905075e6c80f206bbe6cf1e809d2caa69f420c76',
  license = 'Apache2.0',
  visibility = [],
)

maven_jar(
  name = 'soy',
  id = 'com.google.template:soy:2012-12-21',
  sha1 = 'cc28da103845a0f08cfd3fa5abdd45899b0adae1',
  license = 'Apache2.0',
  visibility = [],
)

maven_jar(
  name = 'guice-multibindings',
  id = 'com.google.inject.extensions:guice-multibindings:4.0-beta',
  sha1 = '558a3dcfd203db33a5a96a70a18076c866723ee4',
  license = 'Apache2.0',
  exclude_java_sources = True,
  exclude = [
    'META-INF/DEPENDENCIES',
    'META-INF/LICENSE',
    'META-INF/NOTICE',
    'META-INF/maven/com.google.guava/guava/pom.properties',
    'META-INF/maven/com.google.guava/guava/pom.xml',
  ],
  visibility = [],
)
