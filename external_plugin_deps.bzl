load("//tools/bzl:maven_jar.bzl", "maven_jar", "GERRIT", "MAVEN_CENTRAL")

COMMONMARK_VERSION = '0.9.0'

def external_plugin_deps():
  maven_jar(
    name = 'gitiles_servlet',
    artifact = 'com.google.gitiles:gitiles-servlet:0.2-3',
    sha1 = 'dfba4b00f748e579797aadb3fd3b2fd5c85c45ed',
    repository = GERRIT,
  )

  # prettify must match the version used in Gitiles
  maven_jar(
    name = 'prettify',
    artifact = 'prettify:java-prettify:1.2.1',
    sha1 = '29ad8d072f9d0b83d1a2e9aa6ccb0905e6d543c6',
    repository = GERRIT,
  )

  # commonmark must match the version used in Gitiles
  maven_jar(
    name = 'commonmark',
    artifact = 'com.atlassian.commonmark:commonmark:' + COMMONMARK_VERSION,
    sha1 = '4eb11e3f9aaecafc6073b84c15f66376ef8dc5d3',
  )

  maven_jar(
    name = 'cm_autolink',
    artifact = 'com.atlassian.commonmark:commonmark-ext-autolink:' + COMMONMARK_VERSION,
    sha1 = 'b81d7f0e2bdb987d3f447f3e92756bcdbb5ff537',
  )

  maven_jar(
    name = 'autolink',
    artifact = 'org.nibor.autolink:autolink:0.6.0',
    sha1 = '3986d016a14e8c81afeec752f19af29b20e8367b',
  )

  maven_jar(
    name = 'gfm_strikethrough',
    artifact = 'com.atlassian.commonmark:commonmark-ext-gfm-strikethrough:' + COMMONMARK_VERSION,
    sha1 = 'd6814aac1a6aaa473c0c3ef0a23bfff8836d1e67',
  )

  maven_jar(
    name = 'gfm_tables',
    artifact = 'com.atlassian.commonmark:commonmark-ext-gfm-tables:' + COMMONMARK_VERSION,
    sha1 = '4c8a93ef905ef8fc35d6379965641a980d67e304',
  )
