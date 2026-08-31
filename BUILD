load("@com_googlesource_gerrit_bazlets//:gerrit_plugin.bzl", "gerrit_plugin", "gerrit_plugin_tests")
load("@com_googlesource_gerrit_bazlets//tools:genrule2.bzl", "genrule2")

gerrit_plugin(
    name = "gitiles",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: gitiles",
        "Gerrit-Module: com.googlesource.gerrit.plugins.gitiles.PluginModule",
        "Gerrit-HttpModule: com.googlesource.gerrit.plugins.gitiles.HttpModule",
        # Gitiles uses /repo to access a repo, so the default plugin layout would
        # disallow repos named "static" or "Documentation". Paths starting with +
        # are reserved by Gitiles and can't match repos.
        "Gerrit-HttpStaticPrefix: +static",
        "Gerrit-HttpDocumentationPrefix: +Documentation",
    ],
    resource_jars = [":gitiles-servlet-resources"],
    resources = glob(["src/main/resources/**/*"]),
    deps = ["//lib/gitiles"],
)

genrule2(
    name = "gitiles-servlet-resources",
    srcs = ["@gitiles//resources/com/google/gitiles:webassets"],
    outs = ["gitiles-servlet-resources.jar"],
    cmd = " && ".join([
        "unzip -qd $$TMP $(location @gitiles//resources/com/google/gitiles:webassets) \"+static/*\"",
        "cd $$TMP/+static",
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
        "cd $$TMP",
        "zip -qr $$ROOT/$@ +static",
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
