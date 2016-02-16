// Copyright (C) 2013 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.gitiles;

import com.google.gerrit.pgm.init.api.ConsoleUI;
import com.google.gerrit.pgm.init.api.InitFlags;
import com.google.gerrit.pgm.init.api.InitStep;
import com.google.gerrit.pgm.init.api.Section;
import com.google.gerrit.server.config.SitePaths;
import com.google.inject.Inject;

import org.eclipse.jgit.storage.file.FileBasedConfig;

import java.io.File;
import java.io.IOException;

class InitGitiles implements InitStep {
  private final ConsoleUI ui;
  private final Section gitweb;
  private final FileBasedConfig cfg;
  private final File gitilesConfig;

  @Inject
  InitGitiles(InitFlags flags, ConsoleUI ui, Section.Factory sections,
      SitePaths sitePaths) {
    this.ui = ui;
    this.gitweb = sections.get("gitweb", null);
    this.cfg = flags.cfg;
    this.gitilesConfig = new File(sitePaths.etc_dir, "gitiles.config");
  }

  @Override
  public void run() throws IOException {
    ui.header("Gitiles");
    if (!confirm()) {
      return;
    }

    gitweb.set("type", "custom");
    gitweb.set("linkname", "gitiles");
    gitweb.unset("cgi");
    gitweb.unset("pathSeparator");
    gitweb.set("url", "plugins/gitiles/");
    gitweb.set("revision", "${project}/+/${commit}");
    gitweb.set("project", "${project}");
    gitweb.set("branch", "${project}/+/${branch}");
    gitweb.set("filehistory", "${project}/+log/${branch}/${file}");
    gitweb.set("file", "${project}/+/${commit}/${file}");
    gitweb.set("roottree", "${project}/+/${commit}/");

    // Configuration is mainly done in code. Create an empty config file so the
    // user knows where to put additional configuration.
    gitilesConfig.createNewFile();
  }

  @Override
  public void postRun() {
    // Do nothing.
  }

  private boolean confirm() {
    if (!cfg.getSections().contains("gitweb")) {
      return ui.yesno(true, "Configure Gitiles source browser");
    }
    if ("custom".equalsIgnoreCase(cfg.getString("gitweb", null, "type"))
        && "gitiles".equalsIgnoreCase(cfg.getString("gitweb", null, "linkname"))) {
      return ui.yesno(false, "Restore default Gitiles config");
    }
    return ui.yesno(false, "Override existing gitweb config with Gitiles");
  }
}
