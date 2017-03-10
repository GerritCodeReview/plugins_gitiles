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
import com.google.gerrit.extensions.webui.ParentWebLink;
import com.google.gerrit.extensions.webui.ProjectWebLink;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.Inject;
import com.google.common.base.MoreObjects;

import org.eclipse.jgit.lib.Config;

public class GitilesWeblinks implements BranchWebLink, FileWebLink,
    PatchSetWebLink, ProjectWebLink, FileHistoryWebLink, ParentWebLink {
  private final String name;
  private final String baseUrl;
  private final String target;

  private static final Logger log = LoggerFactory
          .getLogger(GitilesWeblinks.class);

  @Inject
  public GitilesWeblinks(@PluginName String pluginName,
                         PluginConfigFactory configFactory) {
    Config config = configFactory.getGlobalPluginConfig("gitiles");
    name = MoreObjects.firstNonNull(
                          config.getString("gerrit", null, "linkname"),
                          "browse");
    baseUrl = "plugins/" + pluginName;
    target = MoreObjects.firstNonNull(
                          config.getString("gerrit", null, "target"),
                          Target.SELF);
  }

  @Override
  public WebLinkInfo getProjectWeblink(String projectName) {
    return new WebLinkInfo(name, null, String.format("%s/%s", baseUrl,
        projectName), target);
  }

  @Override
  public WebLinkInfo getPatchSetWebLink(String projectName, String commit) {
    return new WebLinkInfo(name, null, String.format("%s/%s/+/%s", baseUrl,
        projectName, commit), target);
  }

  @Override
  public WebLinkInfo getParentWeblink(String projectName, String commit) {
    return new WebLinkInfo(name, null, String.format("%s/%s/+/%s", baseUrl,
        projectName, commit), target);
  }

  @Override
  public WebLinkInfo getFileWebLink(String projectName, String revision,
      String fileName) {
    return new WebLinkInfo(name, null, String.format("%s/%s/+/%s/%s", baseUrl,
        projectName, revision, fileName), target);
  }

  @Override
  public WebLinkInfo getBranchWebLink(String projectName, String branchName) {
    return new WebLinkInfo(name, null, String.format("%s/%s/+/%s", baseUrl,
        projectName, branchName), target);
  }

  @Override
  public WebLinkInfo getFileHistoryWebLink(String projectName, String revision, String fileName) {
    return new WebLinkInfo(name, null, String.format("%s/%s/+log/%s/%s", baseUrl,
        projectName, revision, fileName), target);
  }
}
