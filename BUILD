load("//tools/bzl:genrule2.bzl", "genrule2")
    ],
    srcs = ["@gitiles-servlet//jar"],
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
