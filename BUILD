load("//tools/bzl:plugin.bzl", "gerrit_plugin")

genrule(
    name = "gitiles",
    srcs = [
        ":gitiles__base",
        "@gitiles_servlet//jar",
    ],
    outs = ["gitiles.jar"],
    cmd = " && ".join([
        "ROOT=$$PWD",
        "TMP=$$(mktemp -d || mktemp -d -t bazel-tmp)",
        "cp $(location :gitiles__base) $@",
        "chmod +w $@",
        "unzip -qd $$TMP $(location @gitiles_servlet//jar) \"com/google/gitiles/static/*\"",
        "cd $$TMP/com/google/gitiles",
        "mv static +static",
        "zip -Drq $$ROOT/$@ -g . -i \"+static/*\"",
    ]),
    visibility = ["//plugins:__subpackages__"],
)

gerrit_plugin(
    name = "gitiles",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: gitiles",
        "Gerrit-Module: com.googlesource.gerrit.plugins.gitiles.Module",
        "Gerrit-HttpModule: com.googlesource.gerrit.plugins.gitiles.HttpModule",
        # Gitiles uses /repo to access a repo, so the default plugin layout would
        # disallow repos named "static" or "Documentation". Paths starting with +
        # are reserved by Gitiles and can't match repos.
        "Gerrit-HttpStaticPrefix: +static",
        "Gerrit-HttpDocumentationPrefix: +Documentation",
    ],
    resources = glob(["src/main/resources/**/*"]),
    target_suffix = "__base",
    deps = [
        ":gitiles__plugin_deps",
        "//java/com/google/gerrit/server/restapi",
    ],
)

java_library(
    name = "gitiles__plugin_deps",
    visibility = ["//visibility:public"],
    exports = [
        "@autolink//jar",
        "@cm_autolink//jar",
        "@commonmark//jar",
        "@gfm_strikethrough//jar",
        "@gfm_tables//jar",
        "@gitiles_servlet//jar",
        "@prettify//jar",
    ],
)

java_library(
    name = "gitiles__classpath_deps",
    testonly = 1,
    visibility = ["//visibility:public"],
    exports = [
        ":gitiles__plugin",
        ":gitiles__plugin_deps",
    ],
)
