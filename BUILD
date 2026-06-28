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

# The default (javax.servlet) jar. With the ee11 twin below, the plugin is
# explicitly flavour-managed: `flavour = "ee8"` stamps `Gerrit-Flavour: ee8`
# (loader-equivalent to the unmarked jar) and guards the target to the ee8
# configuration, so the ee11 wildcard pass skips the javax side instead of
# failing -- both wildcard passes stay green at every stage:
#   bazelisk test plugins/gitiles/...
#   bazelisk test --@com_googlesource_gerrit_bazlets//flags:flavour=ee11 \
#       plugins/gitiles/...
gerrit_plugin(
    name = "gitiles",
    srcs = SRCS,
    flavour = "ee8",
    manifest_entries = MANIFEST_ENTRIES,
    resource_jars = [":gitiles-servlet-resources"],
    resources = RESOURCES,
    deps = ["//lib/gitiles"],
)

# EE11 (jakarta.servlet) flavour. The shared bazlets `flavour = "ee11"` rewrites
# the plugin's javax.servlet imports to jakarta.servlet, injects
# `Gerrit-Flavour: ee11`, compiles against the jakarta plugin API, and wraps the
# target in a flavour=ee11 transition -- so building :gitiles-ee11 self-selects
# the jakarta config with no command-line flag. Under that transition
# //plugins:plugin-lib-neverlink, //lib/gitiles (jakarta gitiles-servlet 2.0.0)
# and the resource jar below all flip together. Build with:
#   bazelisk build //plugins/gitiles:gitiles-ee11
# (or both flavours at once:
#   `bazelisk build //plugins/gitiles:gitiles \
#       //plugins/gitiles:gitiles-ee11`).
# `dir_name = "gitiles"` keeps stamping/versioning shared with the default jar.
gerrit_plugin(
    name = "gitiles-ee11",
    srcs = SRCS,
    dir_name = "gitiles",
    flavour = "ee11",
    manifest_entries = MANIFEST_ENTRIES,
    resource_jars = [":gitiles-servlet-resources"],
    resources = RESOURCES,
    deps = ["//lib/gitiles"],
)

# Static resources are extracted from the gitiles-servlet jar of the active
# flavour, via the single flavour seam in //lib/gitiles: javax by default,
# jakarta under the ee11 transition (i.e. when built as part of :gitiles-ee11).
# The CSS munging below is flavour-neutral.
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

# EE8 tests, guarded to the ee8 configuration (the default).
gerrit_plugin_tests(
    name = "gitiles_tests",
    srcs = glob(["src/test/java/**/*Test.java"]),
    flavour = "ee8",
    tags = ["gitiles"],
    visibility = ["//visibility:public"],
    runtime_deps = [":gitiles__plugin"],
    deps = [
        ":gitiles__plugin",
        "//javatests/com/google/gerrit/util/http/testutil",
        "//lib/gitiles",
    ],
)

# EE11 test twin: the same canonical test sources through the shared
# `to_jakarta` transform, executed against :gitiles-ee11__plugin; the
# in-tree deps (testutil, //lib/gitiles) flip with the ee11 configuration.
# A test target cannot self-transition the flavour, so the twin is guarded
# to the ee11 configuration and runs under the flag pass; the plain pass
# skips it and runs gitiles_tests above.
gerrit_plugin_tests(
    name = "gitiles_tests-ee11",
    srcs = glob(["src/test/java/**/*Test.java"]),
    flavour = "ee11",
    tags = ["gitiles"],
    visibility = ["//visibility:public"],
    runtime_deps = [":gitiles-ee11__plugin"],
    deps = [
        ":gitiles-ee11__plugin",
        "//javatests/com/google/gerrit/util/http/testutil:testutil-ee11",
        "//lib/gitiles",
    ],
)
