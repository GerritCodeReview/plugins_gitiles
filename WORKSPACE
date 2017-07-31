workspace(name = "gitiles")

load("//:bazlets.bzl", "load_bazlets")

load_bazlets(
    commit = "35a4ca1ccbe200cf9f480c5d299533bce67e91f1",
    local_path = "/home/ehecabo/workspace/bazlets",
)

#Snapshot Plugin API
# load(
#    "@com_googlesource_gerrit_bazlets//:gerrit_api_maven_local.bzl",
#    "gerrit_api_maven_local",
# )

# Load snapshot Plugin API
# gerrit_api_maven_local()

# Release Plugin API
load(
    "@com_googlesource_gerrit_bazlets//:gerrit_api.bzl",
    "gerrit_api",
)

# Load release Plugin API
gerrit_api()

load("//:external_plugin_deps.bzl", "external_plugin_deps")

external_plugin_deps()
