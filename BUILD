load("//tools/bzl:plugin.bzl", "gerrit_plugin")

genrule(
  name = "gitiles_2",
  cmd = " && ".join([
    "ROOT=$$PWD",
    "cp $(location :gitiles__base) $$ROOT",
    "unzip -qd $$TMP $(location :gitiles-servlet) com/google/gitiles/static/*",
    'cd $$TMP/com/google/gitiles',
    'mv static +static',
    'zip -Drq $$ROOT -g . -i "+static/*"',
  ]),
  visibility = ["PUBLIC"],
  outs = ["gitiles_2.jar"],
)

gerrit_plugin(
    name = "gitiles",
    srcs = glob(["src/main/java/**/*.java"]),
    resources = glob(["src/main/resources/**/*"]),
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
    deps = [
        "//plugins/gitiles/lib:gitiles-servlet",
        "//plugins/gitiles/lib:commons-lang3",
        "//plugins/gitiles/lib:commonmark",
        "//plugins/gitiles/lib:cm-autolink",
        "//plugins/gitiles/lib:gfm-tables",
        "//plugins/gitiles/lib:gfm-strikethrough",
    ],
)
