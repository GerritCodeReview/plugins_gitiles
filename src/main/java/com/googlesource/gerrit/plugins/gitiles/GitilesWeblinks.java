// Copyright (C) 2015 The Android Open Source Project
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

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.common.WebLinkInfo;
import com.google.gerrit.extensions.webui.BranchWebLink;
import com.google.gerrit.extensions.webui.FileHistoryWebLink;
import com.google.gerrit.extensions.webui.FileWebLink;
import com.google.gerrit.extensions.webui.PatchSetWebLink;
import com.google.gerrit.extensions.webui.ProjectWebLink;

import com.google.inject.Inject;

public class GitilesWeblinks implements BranchWebLink, FileWebLink,
    PatchSetWebLink, ProjectWebLink, FileHistoryWebLink {
  private final String name;
  private final String baseUrl;

  @Inject
  private com.google.gerrit.server.config.PluginConfigFactory cfg;

  @Inject
  public GitilesWeblinks(@PluginName String pluginName) {
    name = cfg.getGlobalPluginConfig(pluginName)
              .getString("gerrit", "linkname", "browse");
    baseUrl = "plugins/" + pluginName;
  }

  @Override
  public WebLinkInfo getProjectWeblink(String projectName) {
    return new WebLinkInfo(name, null, String.format("%s/%s", baseUrl,
        projectName), Target.BLANK);
  }

  @Override
  public WebLinkInfo getPatchSetWebLink(String projectName, String commit) {
    return new WebLinkInfo(name, null, String.format("%s/%s/+/%s", baseUrl,
        projectName, commit), Target.BLANK);
  }

  @Override
  public WebLinkInfo getFileWebLink(String projectName, String revision,
      String fileName) {
    return new WebLinkInfo(name, null, String.format("%s/%s/+/%s/%s", baseUrl,
        projectName, revision, fileName), Target.BLANK);
  }

  @Override
  public WebLinkInfo getBranchWebLink(String projectName, String branchName) {
    return new WebLinkInfo(name, null, String.format("%s/%s/+/%s", baseUrl,
        projectName, branchName), Target.BLANK);
  }

  @Override
  public WebLinkInfo getFileHistoryWebLink(String projectName, String revision, String fileName) {
    return new WebLinkInfo(name, null, String.format("%s/%s/+log/%s/%s", baseUrl,
        projectName, revision, fileName), Target.BLANK);
  }
}
