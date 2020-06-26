load("//tools/bzl:genrule2.bzl", "genrule2")
load("//tools/bzl:plugin.bzl", "gerrit_plugin")

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
        # 1. Strip out Google font CSS imports
        "sed -e '\\%^@import .//fonts\\.googleapis\\.com/%d' -i base.css",
        # 2. Add Gerrit's fonts CSS
        "sed -e 's%^\\(.*Common styles and definitions.*\\)$$%" +
          "\\1\\n\\n@import \"../../../styles/fonts.css\";%' -i base.css",
        # 3. Use Gerrit's Roboto Mono for Source Code Pro
        "sed -e 's/Source Code Pro/Roboto Mono/g' -i base.css",
        # Switching from `static` to `+static` (see comment in plugin definiton)
        "cd $$TMP/com/google/gitiles",
        "mv static +static",
        "zip -qr $$ROOT/$@ .",
    ]),
)
