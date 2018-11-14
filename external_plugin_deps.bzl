load("//tools/bzl:maven_jar.bzl", "GERRIT", "MAVEN_CENTRAL", "MAVEN_LOCAL", "maven_jar")

COMMONMARK_VERSION = "0.10.0"

def external_plugin_deps():
    maven_jar(
        name = "gitiles-servlet",
        artifact = "com.google.gitiles:gitiles-servlet:0.2-7",
        sha1 = "f23b22cb27fe5c4a78f761492082159d17873f57",
        repository = GERRIT,
    )

    # prettify must match the version used in Gitiles
    maven_jar(
        name = "prettify",
        artifact = "com.github.twalcari:java-prettify:1.2.2",
        sha1 = "b8ba1c1eb8b2e45cfd465d01218c6060e887572e",
    )

    maven_jar(
        name = "commons-lang3",
        artifact = "org.apache.commons:commons-lang3:3.7",
        sha1 = "557edd918fd41f9260963583ebf5a61a43a6b423",
    )

    maven_jar(
        name = "commons-text",
        artifact = "org.apache.commons:commons-text:1.2",
        sha1 = "74acdec7237f576c4803fff0c1008ab8a3808b2b",
    )

    # commonmark must match the version used in Gitiles
    maven_jar(
        name = "commonmark",
        artifact = "com.atlassian.commonmark:commonmark:" + COMMONMARK_VERSION,
        sha1 = "119cb7bedc3570d9ecb64ec69ab7686b5c20559b",
    )

    maven_jar(
        name = "cm-autolink",
        artifact = "com.atlassian.commonmark:commonmark-ext-autolink:" + COMMONMARK_VERSION,
        sha1 = "a6056a5efbd68f57d420bc51bbc54b28a5d3c56b",
    )

    maven_jar(
        name = "autolink",
        artifact = "org.nibor.autolink:autolink:0.7.0",
        sha1 = "649f9f13422cf50c926febe6035662ae25dc89b2",
    )

    maven_jar(
        name = "gfm-strikethrough",
        artifact = "com.atlassian.commonmark:commonmark-ext-gfm-strikethrough:" + COMMONMARK_VERSION,
        sha1 = "40837da951b421b545edddac57012e15fcc9e63c",
    )

    maven_jar(
        name = "gfm-tables",
        artifact = "com.atlassian.commonmark:commonmark-ext-gfm-tables:" + COMMONMARK_VERSION,
        sha1 = "c075db2a3301100cf70c7dced8ecf86b494458a2",
    )
