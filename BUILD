load("//tools/bzl:genrule2.bzl", "genrule2")
load("//tools/bzl:plugin.bzl", "PLUGIN_DEPS", "PLUGIN_TEST_DEPS", "gerrit_plugin")
load("//tools/bzl:junit.bzl", "junit_tests")

gerrit_plugin(
    name = "gitiles",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: gitiles",
        "Gerrit-Module: com.googlesource.gerrit.plugins.gitiles.PluginModule",
        "Gerrit-HttpModule: com.googlesource.gerrit.plugins.gitiles.HttpModule",
        # Gitiles uses /repo to access a repo, so the default plugin layout would
        # disallow repos named "static" or "Documentation". Paths starting with +
    resource_jars = [":gitiles-servlet-resources"],
    resources = glob(["src/main/resources/**/*"]),
    deps = ["//lib/gitiles"],
)

genrule2(
    name = "gitiles-servlet-resources",
    srcs = ["@gitiles-servlet//jar"],
    outs = ["gitiles-servlet-resources.jar"],
    cmd = " && ".join([
        "unzip -qd $$TMP $(location @gitiles-servlet//jar) \"com/google/gitiles/static/*\"",
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

junit_tests(
    name = "gitiles_tests",
    srcs = glob(["src/test/java/**/*Test.java"]),
    tags = ["gitiles"],
    visibility = ["//visibility:public"],
    runtime_deps = [":gitiles__plugin"],
    deps = PLUGIN_TEST_DEPS + PLUGIN_DEPS + [
        ":gitiles__plugin",
        "//javatests/com/google/gerrit/util/http/testutil",
        "//lib/gitiles",
    ],
)
