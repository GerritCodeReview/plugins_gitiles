package(default_visibility = ["//visibility:public"])

java_library(
    name = "jgit",
    runtime_deps = [":ewah"],
    exports = ["@jgit//jar"],
)

java_library(
    name = "jgit-servlet",
    runtime_deps = [":jgit"],
    exports = ["@jgit-servlet//jar"],
)

java_library(
    name = "ewah",
    exports = ["@ewah//jar"],
)
