load(
    "@com_googlesource_gerrit_bazlets//tools:maven_jar.bzl",
    _gerrit = "GERRIT",
    _maven_central = "MAVEN_CENTRAL",
    _maven_jar = "maven_jar",
)

GERRIT = _gerrit
MAVEN_CENTRAL = _maven_central
maven_jar = _maven_jar
