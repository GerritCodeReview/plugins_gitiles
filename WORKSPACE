workspace(name = "gitiles")

load("//:bazlets.bzl", "load_bazlets")

load_bazlets(
    commit = "e15ad03897f040435d6c5e808b697b1125b964c1",
    local_path = "/home/ehecabo/workspace/bazlets",
)

#Snapshot Plugin API
load(
   "@com_googlesource_gerrit_bazlets//:gerrit_api_maven_local.bzl",
   "gerrit_api_maven_local",
)

# Load snapshot Plugin API
gerrit_api_maven_local()

# Release Plugin API
# load(
#     "@com_googlesource_gerrit_bazlets//:gerrit_api.bzl",
#     "gerrit_api",
# )

# Load release Plugin API
# gerrit_api()

load("//:external_plugin_deps.bzl", "external_plugin_deps")

external_plugin_deps()
