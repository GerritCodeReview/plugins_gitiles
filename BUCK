include_defs('//bucklets/gerrit_plugin.bucklet')
include_defs('//bucklets/maven_jar.bucklet')

genrule(
  name = 'gitiles',
  cmd = ' && '.join([
    'cp $(location :gitiles__base) $OUT',
    'unzip -qd $TMP $(location :gitiles-servlet) "com/google/gitiles/static/*"',
    'cd $TMP/com/google/gitiles',
    'mv static +static',
    'zip -Drq $OUT -g . -i "+static/*"',
  ]),
  visibility = ['PUBLIC'],
  out = 'gitiles.jar',
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
    ':commonmark',
    ':cm-autolink',
    ':gfm-tables',
    ':gfm-strikethrough',

  ],
  resources = glob(['src/main/resources/**/*']),
  manifest_entries = [
    'Gerrit-PluginName: gitiles',
    'Gerrit-Module: com.googlesource.gerrit.plugins.gitiles.Module',
    'Gerrit-HttpModule: com.googlesource.gerrit.plugins.gitiles.HttpModule',

    # Gitiles uses /repo to access a repo, so the default plugin layout would
    # disallow repos named "static" or "Documentation". Paths starting with +
    # are reserved by Gitiles and can't match repos.
    'Gerrit-HttpStaticPrefix: +static',
    'Gerrit-HttpDocumentationPrefix: +Documentation',
  ],
  visibility = [],
  target_suffix = '__base'
)

java_library(
  name = 'classpath',
  deps = [':gitiles__plugin'],
)

maven_jar(
  name = 'gitiles-servlet',
  id = 'com.google.gitiles:gitiles-servlet:0.1-9',
  sha1 = '653b054661425499b55400888c35b55089f8816b',
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

# soy version must match version used in Gitiles
maven_jar(
  name = 'soy',
  id = 'com.google.template:soy:2015-04-10',
  sha1 = 'f2a260c0eafbc5756ccec546efd2ffd5b0a583cb',
  deps = [':icu4j'],
  license = 'Apache2.0',
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

maven_jar(
  name = 'commonmark',
  id = 'com.atlassian.commonmark:commonmark:0.5.1',
  sha1 = 'b35ae2353871955674bbfa1a92394272b1dada45',
  license = 'commonmark',
)

maven_jar(
  name = 'cm-autolink',
  id = 'com.atlassian.commonmark:commonmark-ext-autolink:0.5.1',
  sha1 = '29bb9d22a7aaf5bd8f23d8cbdd9f438f07e26735',
  license = 'commonmark',
  deps = [
    ':commonmark',
    ':autolink',
  ],
  )

maven_jar(
  name = 'autolink',
  id = 'org.nibor.autolink:autolink:0.4.0',
  sha1 = '764f7b0147a0675d971a34282dce9ec76b8307c9',
  license = 'autolink',
)

maven_jar(
  name = 'gfm-strikethrough',
  id = 'com.atlassian.commonmark:commonmark-ext-gfm-strikethrough:0.5.1',
  sha1 = 'acc28d79c4e00a6e24017596dd22ce757df71db3',
  license = 'commonmark',
  deps = [':commonmark'],
)

maven_jar(
  name = 'gfm-tables',
  id = 'com.atlassian.commonmark:commonmark-ext-gfm-tables:0.5.1',
  sha1 = '5cdc350f7e498458e5ed6751771c5e8c3efc107e',
  license = 'commonmark',
  deps = [':commonmark'],
)
