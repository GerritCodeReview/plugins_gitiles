load("//tools/bzl:genrule2.bzl", "genrule2")
load("//tools/bzl:junit.bzl", "junit_tests")
load("@rules_java//java:defs.bzl", "java_library")
load(
    "@com_googlesource_gerrit_bazlets//:gerrit_plugin.bzl",
    "gerrit_plugin",
    "gerrit_plugin_tests",
)
load(
    "@com_googlesource_gerrit_bazlets//tools:runtime_jars_overlap.bzl",
    "runtime_jars_overlap_test",
)
load(
    "@com_googlesource_gerrit_bazlets//tools:in_gerrit_tree.bzl",
    "in_gerrit_tree_enabled",
)

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
    deps = [":gitiles_lib"],
)

genrule2(
    name = "gitiles-servlet-resources",
    srcs = ["@gitiles_plugin_deps//:com_google_gitiles_gitiles_servlet"],
    outs = ["gitiles-servlet-resources.jar"],
    cmd = " && ".join([
        "unzip -qd $$TMP $(location @gitiles_plugin_deps//:com_google_gitiles_gitiles_servlet) \"com/google/gitiles/static/*\"",
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
        ":gitiles_lib",
    ],
)

java_library(
    name = "gitiles_lib",
    visibility = ["//visibility:public"],
    exports = [
        ":cm-autolink",
        ":commonmark",
        ":gfm-strikethrough",
        ":gfm-tables",
        ":gitiles-servlet",
        ":prettify",
    ],
)

java_library(
    name = "cm-autolink",
    data = ["//lib:LICENSE-commonmark"],
    visibility = ["//visibility:public"],
    exports = ["@gitiles_plugin_deps//:org_commonmark_commonmark_ext_autolink"],
)

java_library(
    name = "commonmark",
    data = ["//lib:LICENSE-commonmark"],
    visibility = ["//visibility:public"],
    exports = ["@gitiles_plugin_deps//:org_commonmark_commonmark"],
)

java_library(
    name = "gfm-strikethrough",
    data = ["//lib:LICENSE-Apache2.0"],
    visibility = ["//visibility:public"],
    exports = ["@gitiles_plugin_deps//:org_commonmark_commonmark_ext_gfm_strikethrough"],
)

java_library(
    name = "gfm-tables",
    data = ["//lib:LICENSE-Apache2.0"],
    visibility = ["//visibility:public"],
    exports = ["@gitiles_plugin_deps//:org_commonmark_commonmark_ext_gfm_tables"],
)

java_library(
    name = "gitiles-servlet",
    data = ["//lib:LICENSE-Apache2.0"],
    visibility = ["//visibility:public"],
    exports = ["@gitiles_plugin_deps//:com_google_gitiles_gitiles_servlet"],
)

java_library(
    name = "prettify",
    visibility = ["//visibility:public"],
    exports = ["@java-prettify"],
)

runtime_jars_overlap_test(
    name = "gitiles_no_overlap_with_gerrit",
    against = "//:release.war.jars.txt",
    hint = "Exclude overlaps via maven.install(excluded_artifacts=[...]) and re-run this test.",
    target = ":gitiles__plugin",
    target_compatible_with = in_gerrit_tree_enabled(),
)
