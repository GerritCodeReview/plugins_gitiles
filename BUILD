load("@com_googlesource_gerrit_bazlets//:gerrit_plugin.bzl", "gerrit_plugin", "gerrit_plugin_tests")
load("@com_googlesource_gerrit_bazlets//tools:genrule2.bzl", "genrule2")

SRCS = glob(["src/main/java/**/*.java"])

RESOURCES = glob(["src/main/resources/**/*"])

MANIFEST_ENTRIES = [
    "Gerrit-PluginName: gitiles",
    "Gerrit-Module: com.googlesource.gerrit.plugins.gitiles.PluginModule",
    "Gerrit-HttpModule: com.googlesource.gerrit.plugins.gitiles.HttpModule",
    # Gitiles uses /repo to access a repo, so the default plugin layout would
    # disallow repos named "static" or "Documentation". Paths starting with +
    # are reserved by Gitiles and can't match repos.
    "Gerrit-HttpStaticPrefix: +static",
    "Gerrit-HttpDocumentationPrefix: +Documentation",
]

# The sources are jakarta (Servlet API 6.1) and build natively against the
# jakarta plugin API and gitiles-servlet 2.x through //lib/gitiles.
gerrit_plugin(
    name = "gitiles",
    srcs = SRCS,
    manifest_entries = MANIFEST_ENTRIES,
    resource_jars = [":gitiles-servlet-resources"],
    resources = RESOURCES,
    deps = ["//lib/gitiles"],
)

# Static resources are extracted from the gitiles-servlet jar (via
# //lib/gitiles). The CSS munging below does not touch class files.
genrule2(
    name = "gitiles-servlet-resources",
    srcs = ["//lib/gitiles:gitiles-servlet-jar"],
    outs = ["gitiles-servlet-resources.jar"],
    cmd = " && ".join([
        "unzip -qd $$TMP $(location //lib/gitiles:gitiles-servlet-jar) \"com/google/gitiles/static/*\"",
        "cd $$TMP/com/google/gitiles/static",
        # To avoid loading 3rd party resources, we adapt gitiles' CSS to
        # load fonts from Gerrit directly:
        # 1. Strip out Google font CSS imports to tmp file as in-place replace is OSX no-go
        "sed -e '\\%^@import .//fonts\\.googleapis\\.com/%d' base.css > $$TMP/base.css.tmp",
        # move tmp file back to base.css
        "mv $$TMP/base.css.tmp base.css",
        # 2. Add Gerrit's fonts CSS
        "sed -e 's%^\\(.*Common styles and definitions.*\\)$$%" +
        "\\1\\n\\n@import \"../../../styles/fonts.css\";%' base.css > $$TMP/base.css.tmp",
        "mv $$TMP/base.css.tmp base.css",
        # 3. Use Gerrit's Roboto Mono for Source Code Pro
        "sed -e 's/Source Code Pro/Roboto Mono/g' base.css > $$TMP/base.css.tmp",
        "mv $$TMP/base.css.tmp base.css",
        # Switching from `static` to `+static` (see comment in plugin definiton)
        "cd $$TMP/com/google/gitiles",
        "mv static +static",
        "zip -qr $$ROOT/$@ .",
    ]),
)

gerrit_plugin_tests(
    name = "gitiles_tests",
    srcs = glob(["src/test/java/**/*Test.java"]),
    tags = ["gitiles"],
    visibility = ["//visibility:public"],
    runtime_deps = [":gitiles__plugin"],
    deps = [
        ":gitiles__plugin",
        "//javatests/com/google/gerrit/util/http/testutil",
        "//lib/gitiles",
    ],
)
