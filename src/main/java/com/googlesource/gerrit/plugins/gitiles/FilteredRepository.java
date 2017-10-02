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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.git.VisibleRefFilter;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gerrit.server.permissions.ProjectPermission;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.gerrit.server.project.ProjectControl;
import com.google.gerrit.server.project.ProjectState;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.eclipse.jgit.attributes.AttributesNodeProvider;
import org.eclipse.jgit.lib.ObjectDatabase;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.RefRename;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.ReflogReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.lib.StoredConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FilteredRepository extends Repository {
  private static final Logger log = LoggerFactory.getLogger(FilteredRepository.class);

  static class Factory {
    private final ProjectControl.GenericFactory projectControlFactory;
    private final Provider<CurrentUser> userProvider;
    private final GitRepositoryManager repoManager;
    private final VisibleRefFilter.Factory visibleRefFilterFactory;
    private final PermissionBackend permissionBackend;
    private final ProjectState projectState;

    @Inject
    Factory(
        ProjectControl.GenericFactory projectControlFactory,
        Provider<CurrentUser> userProvider,
        GitRepositoryManager repoManager,
        VisibleRefFilter.Factory visibleRefFilterFactory,
        PermissionBackend permissionBackend,
        ProjectState projectState) {
      this.projectControlFactory = projectControlFactory;
      this.userProvider = userProvider;
      this.repoManager = repoManager;
      this.visibleRefFilterFactory = visibleRefFilterFactory;
      this.permissionBackend = permissionBackend;
      this.projectState = projectState;
    }

    FilteredRepository create(Project.NameKey name) throws NoSuchProjectException, IOException {
      ProjectControl ctl = projectControlFactory.controlFor(name, userProvider.get());
      if (ctl.getProject().getState().equals(HIDDEN)) {
        throw new NoSuchProjectException(name);
      }
      return new FilteredRepository(ctl, repoManager.openRepository(name), visibleRefFilterFactory,
          userProvider, permissionBackend, projectState);
    }
  }

  private final Repository delegate;
  private final RefDatabase refdb;

  private FilteredRepository(
      ProjectControl ctl, Repository delegate, VisibleRefFilter.Factory refFilterFactory,
      Provider<CurrentUser> userProvider, PermissionBackend permissionBackend,
      ProjectState projectState) {
    super(toBuilder(delegate));
    this.delegate = delegate;

    PermissionBackend.WithUser withUser = permissionBackend.user(userProvider);
    PermissionBackend.ForProject forProject = withUser.project(projectState.getNameKey());
    if (checkProjectPermission(forProject, ProjectPermission.READ, userProvider, projectState)) {
      this.refdb = delegate.getRefDatabase();
    } else {
      this.refdb =
          new FilteredRefDatabase(
              delegate.getRefDatabase(), refFilterFactory.create(ctl.getProjectState(), delegate));
    }
  }

  private boolean checkProjectPermission(
      PermissionBackend.ForProject forProject, ProjectPermission perm, Provider<CurrentUser> userProvider,
      ProjectState projectState) {
    try {
      forProject.check(perm);
    } catch (AuthException e) {
      return false;
    } catch (PermissionBackendException e) {
      log.error(
          String.format(
              "Can't check permission for user %s on project %s",
              userProvider.get(), projectState.getName()),
          e);
      return false;
    }
    return true;
  }

  private static RepositoryBuilder toBuilder(Repository repo) {
    RepositoryBuilder b =
        new RepositoryBuilder().setGitDir(repo.getDirectory()).setFS(repo.getFS());
    if (!repo.isBare()) {
      b.setWorkTree(repo.getWorkTree()).setIndexFile(repo.getIndexFile());
    }
    return b;
  }

  @Override
  public void create(boolean bare) throws IOException {
    throw new UnsupportedOperationException(); // Gitiles is read-only.
  }

  @Override
  public AttributesNodeProvider createAttributesNodeProvider() {
    return delegate.createAttributesNodeProvider();
  }

  @Override
  public ObjectDatabase getObjectDatabase() {
    // No access control on object database; Gitiles ensures objects are only
    // accessed if they are visible by the user according to ACLs on the refs.
    return delegate.getObjectDatabase();
  }

  @Override
  public RefDatabase getRefDatabase() {
    return refdb;
  }

  @Override
  public StoredConfig getConfig() {
    return delegate.getConfig();
  }

  @Override
  public void scanForRepoChanges() throws IOException {
    throw new UnsupportedOperationException(); // Gitiles is read-only.
  }

  @Override
  public void notifyIndexChanged() {
    throw new UnsupportedOperationException(); // Gitiles is read-only.
  }

  @Override
  public ReflogReader getReflogReader(String refName) throws IOException {
    return exactRef(refName) != null ? delegate.getReflogReader(refName) : null;
  }

  @Override
  public void close() {
    delegate.close();
  }

  private static class FilteredRefDatabase extends RefDatabase {
    private final RefDatabase delegate;
    private final VisibleRefFilter refFilter;

    private FilteredRefDatabase(RefDatabase delegate, VisibleRefFilter refFilter) {
      this.delegate = delegate;
      this.refFilter = refFilter;
    }

    @Override
    public void create() throws IOException {
      throw new UnsupportedOperationException(); // Gitiles is read-only.
    }

    @Override
    public void close() {
      // Closed by FilteredRepository.
    }

    @Override
    public boolean isNameConflicting(String name) throws IOException {
      return delegate.isNameConflicting(name);
    }

    @Override
    public RefUpdate newUpdate(String name, boolean detach) throws IOException {
      throw new UnsupportedOperationException(); // Gitiles is read-only.
    }

    @Override
    public RefRename newRename(String fromName, String toName) throws IOException {
      throw new UnsupportedOperationException(); // Gitiles is read-only.
    }

    @Override
    public Ref exactRef(String name) throws IOException {
      Ref ref = delegate.exactRef(name);
      if (ref == null) {
        return null;
      }
      return refFilter.filter(ImmutableMap.of(name, ref), true).get(name);
    }

    @Override
    public Ref getRef(String name) throws IOException {
      Ref ref = delegate.getRef(name);
      if (ref == null) {
        return null;
      }
      return refFilter.filter(ImmutableMap.of(ref.getName(), ref), true).get(ref.getName());
    }

    @Override
    public Map<String, Ref> getRefs(String prefix) throws IOException {
      Map<String, Ref> refs = refFilter.filter(delegate.getRefs(prefix), true);
      Map<String, Ref> result = Maps.newHashMapWithExpectedSize(refs.size());
      for (Ref ref : refs.values()) {
        // VisibleRefFilter adds the prefix to the keys, re-strip it.
        result.put(ref.getName().substring(prefix.length()), ref);
      }
      return refs;
    }

    @Override
    public List<Ref> getAdditionalRefs() throws IOException {
      List<Ref> refs = delegate.getAdditionalRefs();
      Map<String, Ref> result = Maps.newHashMapWithExpectedSize(refs.size());
      for (Ref ref : refs) {
        result.put(ref.getName(), ref);
      }
      return ImmutableList.copyOf(result.values());
    }

    @Override
    public Ref peel(Ref ref) throws IOException {
      return delegate.peel(ref);
    }
  }
}
