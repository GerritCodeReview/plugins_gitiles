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

import static com.google.gerrit.extensions.client.ProjectState.HIDDEN;

import com.google.common.collect.Maps;
import com.google.gerrit.common.Nullable;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.common.ProjectInfo;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.Url;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.config.AnonymousCowardName;
import com.google.gerrit.server.config.SitePaths;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gerrit.server.permissions.ProjectPermission;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectJson;
import com.google.gerrit.server.project.ProjectState;
import com.google.gerrit.server.restapi.project.ListProjects;
import com.google.gitiles.GitilesAccess;
import com.google.gitiles.GitilesUrls;
import com.google.gitiles.RepositoryDescription;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException;
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException;
import org.eclipse.jgit.util.FS;

class GerritGitilesAccess implements GitilesAccess {
  // Assisted injection doesn't work with the overridden method, so write the
  // factory manually.
  static class Factory implements GitilesAccess.Factory {
    private final ProjectCache projectCache;
    private final ProjectJson projectJson;
    private final Provider<ListProjects> listProjects;
    private final GitilesUrls urls;
    private final SitePaths site;
    private final Provider<CurrentUser> userProvider;
    private final String anonymousCowardName;
    private final PermissionBackend permissionBackend;

    @Inject
    Factory(
        ProjectCache projectCache,
        ProjectJson projectJson,
        Provider<ListProjects> listProjects,
        GitilesUrls urls,
        SitePaths site,
        Provider<CurrentUser> userProvider,
        PermissionBackend permissionBackend,
        @AnonymousCowardName String anonymousCowardName) {
      this.projectCache = projectCache;
      this.projectJson = projectJson;
      this.listProjects = listProjects;
      this.urls = urls;
      this.site = site;
      this.userProvider = userProvider;
      this.permissionBackend = permissionBackend;
      this.anonymousCowardName = anonymousCowardName;
    }

    @Override
    public GerritGitilesAccess forRequest(HttpServletRequest req) {
      return new GerritGitilesAccess(this, req);
    }
  }

  private final ProjectCache projectCache;
  private final ProjectJson projectJson;
  private final Provider<ListProjects> listProjects;
  private final GitilesUrls urls;
  private final SitePaths site;
  private final Provider<CurrentUser> userProvider;
  private final String anonymousCowardName;
  private final HttpServletRequest req;
  private final PermissionBackend permissionBackend;

  @Inject
  GerritGitilesAccess(Factory factory, HttpServletRequest req) {
    this.projectCache = factory.projectCache;
    this.projectJson = factory.projectJson;
    this.listProjects = factory.listProjects;
    this.urls = factory.urls;
    this.site = factory.site;
    this.userProvider = factory.userProvider;
    this.anonymousCowardName = factory.anonymousCowardName;
    this.permissionBackend = factory.permissionBackend;
    this.req = req;
  }

  @Override
  public Map<String, RepositoryDescription> listRepositories(
      @Nullable String prefix, Set<String> branches)
      throws ServiceNotEnabledException, ServiceNotAuthorizedException, IOException {
    ListProjects lp = listProjects.get();
    lp.setShowDescription(true);
    lp.setAll(true);
    for (String branch : branches) {
      lp.addShowBranch(branch);
    }
    Map<String, ProjectInfo> projects;
    try {
      projects = lp.apply();
    } catch (BadRequestException | PermissionBackendException e) {
      throw new IOException(e);
    }
    Map<String, RepositoryDescription> result = Maps.newLinkedHashMap();
    projects.entrySet().removeIf(e -> e.getValue().state.equals(HIDDEN));
    CurrentUser currentUser = userProvider.get();
    PermissionBackend.WithUser withUser = permissionBackend.user(currentUser);
    for (Map.Entry<String, ProjectInfo> e : projects.entrySet()) {
      if (withUser
          .project(Project.nameKey(Url.decode(e.getValue().id)))
          .testOrFalse(ProjectPermission.ACCESS)) {
        result.put(e.getKey(), toDescription(e.getKey(), e.getValue()));
      }
    }
    return Collections.unmodifiableMap(result);
  }

  private RepositoryDescription toDescription(String name, ProjectInfo info) {
    RepositoryDescription desc = new RepositoryDescription();
    desc.name = name;
    desc.cloneUrl = urls.getBaseGitUrl(req) + name;
    desc.description = info.description;
    if (info.branches != null) {
      desc.branches = Collections.unmodifiableMap(info.branches);
    }
    return desc;
  }

  private Config getGlobalConfig() throws IOException {
    File cfgFile = site.etc_dir.resolve("gitiles.config").toFile();
    FileBasedConfig cfg = new FileBasedConfig(cfgFile, FS.DETECTED);
    try {
      if (cfg.getFile().exists()) {
        cfg.load();
      }
    } catch (ConfigInvalidException e) {
      throw new IOException(e);
    }

    return cfg;
  }

  @Override
  public Object getUserKey() {
    CurrentUser user = userProvider.get();
    if (user instanceof IdentifiedUser) {
      return ((IdentifiedUser) user).getAccountId();
    }
    return anonymousCowardName;
  }

  @Override
  public String getRepositoryName() {
    return Resolver.getNameKey(req).get();
  }

  @Override
  public RepositoryDescription getRepositoryDescription() throws IOException {
    Project.NameKey nameKey = Resolver.getNameKey(req);
    ProjectState state =
        projectCache.get(nameKey).orElseThrow(() -> new RepositoryNotFoundException(nameKey.get()));
    return toDescription(nameKey.get(), projectJson.format(state.getProject()));
  }

  @Override
  public Config getConfig() throws IOException {
    // Try to get a gitiles.config file from the refs/meta/config branch
    // of the project. For non-project access, use All-Projects as project.
    // If none of the above exists, use global gitiles.config.
    Project.NameKey nameKey = Resolver.getNameKey(req);
    Optional<ProjectState> state = projectCache.get(nameKey);
    if (state.isPresent()) {
      Config cfg = state.get().getConfig("gitiles.config").getWithInheritance();
      if (cfg != null && cfg.getSections().size() > 0) {
        return cfg;
      }
    } else {
      Config cfg = projectCache.getAllProjects().getConfig("gitiles.config").get();
      if (cfg != null && cfg.getSections().size() > 0) {
        return cfg;
      }
    }

    return getGlobalConfig();
  }
}
