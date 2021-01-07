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

import com.google.common.base.MoreObjects;
import com.google.gerrit.common.Nullable;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.common.WebLinkInfo;
import com.google.gerrit.extensions.webui.BranchWebLink;
import com.google.gerrit.extensions.webui.FileHistoryWebLink;
import com.google.gerrit.extensions.webui.FileWebLink;
import com.google.gerrit.extensions.webui.ParentWebLink;
import com.google.gerrit.extensions.webui.PatchSetWebLink;
import com.google.gerrit.extensions.webui.ProjectWebLink;
import com.google.gerrit.extensions.webui.TagWebLink;
import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.jgit.lib.Config;

@Singleton
public class GitilesWeblinks
    implements BranchWebLink,
        FileWebLink,
        PatchSetWebLink,
        ProjectWebLink,
        FileHistoryWebLink,
        ParentWebLink,
        TagWebLink {
  private final String name;
  private final String baseUrl;
  private final String target;

  @Inject
  public GitilesWeblinks(
      @PluginName String pluginName,
      @Nullable @CanonicalWebUrl String gerritUrl,
      PluginConfigFactory configFactory)
      throws MalformedURLException {

    String baseGerritUrl;
    if (gerritUrl != null) {
      URL u = new URL(gerritUrl);
      baseGerritUrl = u.getPath();
    } else {
      baseGerritUrl = "/";
    }

    Config config = configFactory.getGlobalPluginConfig("gitiles");
    name = MoreObjects.firstNonNull(config.getString("gerrit", null, "linkname"), "browse");
    baseUrl =
        MoreObjects.firstNonNull(
            config.getString("gerrit", null, "baseUrl"), baseGerritUrl + "plugins/" + pluginName);

    target = MoreObjects.firstNonNull(config.getString("gerrit", null, "target"), Target.BLANK);
  }

  @Override
  public WebLinkInfo getProjectWeblink(String projectName) {
    return new WebLinkInfo(name, null, String.format("%s/%s", baseUrl, projectName), target);
  }

  @Override
  public WebLinkInfo getPatchSetWebLink(
      String projectName, String commit, String commitMessage, String branchName) {
    return new WebLinkInfo(
        name, null, String.format("%s/%s/+/%s", baseUrl, projectName, commit), target);
  }

  @Override
  public WebLinkInfo getParentWebLink(
      String projectName, String commit, String commitMessage, String branchName) {
    return new WebLinkInfo(
        name, null, String.format("%s/%s/+/%s", baseUrl, projectName, commit), target);
  }

  @Override
  public WebLinkInfo getFileWebLink(String projectName, String revision, String fileName) {
    return new WebLinkInfo(
        name,
        null,
        String.format("%s/%s/+/%s/%s", baseUrl, projectName, revision, fileName),
        target);
  }

  @Override
  public WebLinkInfo getBranchWebLink(String projectName, String branchName) {
    return new WebLinkInfo(
        name, null, String.format("%s/%s/+/%s", baseUrl, projectName, branchName), target);
  }

  @Override
  public WebLinkInfo getTagWebLink(String projectName, String tagName) {
    return new WebLinkInfo(
        name, null, String.format("%s/%s/+/%s", baseUrl, projectName, tagName), target);
  }

  @Override
  public WebLinkInfo getFileHistoryWebLink(String projectName, String revision, String fileName) {
    return new WebLinkInfo(
        name,
        null,
        String.format("%s/%s/+log/%s/%s", baseUrl, projectName, revision, fileName),
        target);
  }
}
