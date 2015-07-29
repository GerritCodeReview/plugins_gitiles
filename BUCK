include_defs('//bucklets/gerrit_plugin.bucklet')
include_defs('//bucklets/maven_jar.bucklet')

genrule(
  name = 'gitiles_jar',
  cmd = ' && '.join([
    'cp $(location :gitiles) $OUT',
    'unzip -qd $TMP $(location :gitiles-servlet) "com/google/gitiles/static/*"',
    'cd $TMP/com/google/gitiles',
    'mv static +static',
    'zip -Drq $OUT -g . -i "+static/*"',
  ]),
  out = 'gitiles.jar',
  deps = [
    ':gitiles-servlet',
    ':gitiles',
  ],
)

gerrit_plugin(
  name = 'gitiles',
  srcs = glob(['src/main/java/**/*.java']),
  deps = [
    ':gitiles-servlet',

    # Deps only needed by Gitiles.
    ':commons-lang3',
    ':guice-multibindings',
    ':nullable-jsr305',
    ':soy',
  ],
  # Deps shared with Gerrit but not in the plugin API.
  provided_deps = [
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

java_library(
  name = 'classpath',
  deps = [':gitiles__plugin'],
)

maven_jar(
  name = 'gitiles-servlet',
  id = 'com.google.gitiles:gitiles-servlet:0.1-4',
  sha1 = 'a7f33ef3685838ff9379e83b7ba41db6007a4fc8',
  deps = [':prettify'],
  license = 'Apache2.0',
  repository = GERRIT,
  visibility = [],
)

# prettify must match the version used in Gitiles
maven_jar(
  name = 'prettify',
  id = 'prettify:java-prettify:1.2.1',
  sha1 = '29ad8d072f9d0b83d1a2e9aa6ccb0905e6d543c6',
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

# soy must match closure-template version used in Gitiles
maven_jar(
  name = 'soy',
  id = 'com.google.template:soy:e74fcfa284a1e31d42ac93e53cb84a71f638c70b',
  sha1 = '1c75a007218f29d6124c46c8b18f4158cba4839c',
  deps = [':icu4j'],
  license = 'Apache2.0',
  repository = GERRIT,
  visibility = [],
)

maven_jar(
  name = 'icu4j',
  id = 'com.ibm.icu:icu4j:51.1',
  sha1 = '8ce396c4aed83c0c3de9158dc72c834fd283d5a4',
  license = 'Apache2.0',
)

# guice-multibindings must match Guice version used in Gerrit
maven_jar(
  name = 'guice-multibindings',
  id = 'com.google.inject.extensions:guice-multibindings:4.0',
  sha1 = 'f4509545b4470bbcc865aa500ad6fef2e97d28bf',
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

maven_jar(
  name = 'nullable-jsr305',
  id = 'com.google.code.findbugs:jsr305:3.0.0',
  sha1 = '5871fb60dc68d67da54a663c3fd636a10a532948',
  license = 'Apache2.0',
  exclude_java_sources = True,
  visibility = [],
)
