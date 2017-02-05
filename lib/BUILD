package(default_visibility = ["//visibility:public"])

java_library(
    name = "gitiles-servlet",
    runtime_deps = [":prettify"],
    exports = ["@gitiles-servlet//jar"],
)

java_library(
    name = "prettify",
    exports = ["@prettify//jar"],
)

java_library(
    name = "commons-lang3",
    exports = ["@commons-lang3//jar"],
)

java_library(
    name = "commonmark",
    exports = ["@commonmark//jar"],
)

java_library(
    name = "cm-autolink",
    runtime_deps = [
        ":commonmark",
        ":autolink",
    ],
    exports = ["@cm-autolink//jar"],
)

java_library(
    name = "autolink",
    exports = ["@autolink//jar"],
)

java_library(
    name = "gfm-strikethrough",
    runtime_deps = [":commonmark"],
    exports = ["@gfm-strikethrough//jar"],
)

java_library(
    name = "gfm-tables",
    runtime_deps = [":commonmark"],
    exports = ["@gfm-tables//jar"],
)
