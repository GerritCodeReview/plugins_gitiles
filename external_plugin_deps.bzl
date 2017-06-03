load("//tools/bzl:maven_jar.bzl", "maven_jar", "GERRIT", "MAVEN_CENTRAL")

COMMONMARK_VERSION = '0.6.0'

def external_plugin_deps():
  maven_jar(
    name = 'gitiles_servlet',
    artifact = 'com.google.gitiles:gitiles-servlet:0.2-1',
    sha1 = '4b83471c65e110a08d99570fafcdaf5f59a0b9ce',
    repository = GERRIT,
  )

  # Uncomment to route gitiles-servlet dependency to local repository
  #native.local_repository(
  #  name = "gitiles",
  #  path = "/home/<user>/projects/gitiles",
  #)

  # prettify must match the version used in Gitiles
  maven_jar(
    name = 'prettify',
    artifact = 'prettify:java-prettify:1.2.1',
    sha1 = '29ad8d072f9d0b83d1a2e9aa6ccb0905e6d543c6',
    repository = GERRIT,
  )

  maven_jar(
    name = 'commonmark',
    artifact = 'com.atlassian.commonmark:commonmark:' + COMMONMARK_VERSION,
    sha1 = '5df3f6fa3073966620685924aa22d08ece7213f2',
  )

  maven_jar(
    name = 'cm_autolink',
    artifact = 'com.atlassian.commonmark:commonmark-ext-autolink:' + COMMONMARK_VERSION,
    sha1 = '4d7e828a4651e2f590b4a059925991be58e62da6',
  )

  maven_jar(
    name = 'autolink',
    artifact = 'org.nibor.autolink:autolink:0.4.0',
    sha1 = '764f7b0147a0675d971a34282dce9ec76b8307c9',
  )

  maven_jar(
    name = 'gfm_strikethrough',
    artifact = 'com.atlassian.commonmark:commonmark-ext-gfm-strikethrough:' + COMMONMARK_VERSION,
    sha1 = '75a95aaec77810496de41239bcc773adfb13285f',
  )

  maven_jar(
    name = 'gfm_tables',
    artifact = 'com.atlassian.commonmark:commonmark-ext-gfm-tables:' + COMMONMARK_VERSION,
    sha1 = 'ae1c701517e8116bc205b561b9b215a53df8abc7',
  )
