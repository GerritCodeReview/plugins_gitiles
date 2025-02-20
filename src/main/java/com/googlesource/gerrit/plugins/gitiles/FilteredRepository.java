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

import static java.util.stream.Collectors.toMap;

import com.google.common.collect.ImmutableList;
import com.google.gerrit.common.Nullable;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.gerrit.server.permissions.PermissionBackend.RefFilterOptions;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gerrit.server.permissions.ProjectPermission;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectState;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.eclipse.jgit.annotations.NonNull;
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
import org.eclipse.jgit.transport.ServiceMayNotContinueException;

class FilteredRepository extends Repository {
  static class Factory {
    private final Provider<CurrentUser> userProvider;
    private final ProjectCache projectCache;
    private final GitRepositoryManager repoManager;
    private final PermissionBackend permissionBackend;

    @Inject
    Factory(
        ProjectCache projectCache,
        Provider<CurrentUser> userProvider,
        GitRepositoryManager repoManager,
        PermissionBackend permissionBackend) {
      this.userProvider = userProvider;
      this.projectCache = projectCache;
      this.repoManager = repoManager;
      this.permissionBackend = permissionBackend;
    }

    FilteredRepository create(Project.NameKey name)
        throws NoSuchProjectException, IOException, PermissionBackendException {
      Optional<ProjectState> projectState = projectCache.get(name);
      if (!projectState.isPresent() || !projectState.get().statePermitsRead()) {
        throw new NoSuchProjectException(name);
      }
      try {
        permissionBackend.currentUser().project(name).check(ProjectPermission.ACCESS);
      } catch (AuthException e) {
        throw new NoSuchProjectException(name, e);
      } catch (PermissionBackendException e) {
        throw new ServiceMayNotContinueException(e);
      }
      return new FilteredRepository(
          projectState.get(),
          userProvider.get(),
          repoManager.openRepository(name),
          permissionBackend);
    }
  }

  private final Repository delegate;
  private final RefDatabase refdb;

  private FilteredRepository(
      ProjectState projectState,
      CurrentUser user,
      Repository delegate,
      PermissionBackend permissionBackend)
      throws PermissionBackendException {
    super(toBuilder(delegate));
    this.delegate = delegate;
    boolean visible = true;
    try {
      permissionBackend.user(user).project(projectState.getNameKey()).check(ProjectPermission.READ);
    } catch (AuthException e) {
      visible = false;
    }

    if (visible) {
      this.refdb = delegate.getRefDatabase();
    } else {

      this.refdb =
          new FilteredRefDatabase(
              delegate, permissionBackend.user(user).project(projectState.getNameKey()));
    }
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
  public String getIdentifier() {
    return delegate.getIdentifier();
  }

  @Override
  public void scanForRepoChanges() throws IOException {
    throw new UnsupportedOperationException(); // Gitiles is read-only.
  }

  @Override
  public void notifyIndexChanged(boolean internal) {
    throw new UnsupportedOperationException(); // Gitiles is read-only.
  }

  @Nullable
  @Override
  public ReflogReader getReflogReader(String refName) throws IOException {
    return exactRef(refName) != null ? delegate.getReflogReader(refName) : null;
  }

  @Override
  public void close() {
    delegate.close();
  }

  private static class FilteredRefDatabase extends RefDatabase {
    private final Repository git;
    private final PermissionBackend.ForProject perm;

    private FilteredRefDatabase(Repository git, PermissionBackend.ForProject perm) {
      this.git = git;
      this.perm = perm;
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
      return git.getRefDatabase().isNameConflicting(name);
    }

    @Override
    public RefUpdate newUpdate(String name, boolean detach) throws IOException {
      throw new UnsupportedOperationException(); // Gitiles is read-only.
    }

    @Override
    public RefRename newRename(String fromName, String toName) throws IOException {
      throw new UnsupportedOperationException(); // Gitiles is read-only.
    }

    @Nullable
    @Override
    public Ref exactRef(String name) throws IOException {
      Ref ref = git.getRefDatabase().exactRef(name);
      if (ref == null) {
        return null;
      }
      try {
        return perm.filter(ImmutableList.of(ref), git, RefFilterOptions.defaults()).stream()
            .filter(r -> r.getName().equals(name))
            .findAny()
            .orElse(null);
      } catch (PermissionBackendException e) {
        throw new IOException(e);
      }
    }

    @Override
    public Map<String, Ref> getRefs(String prefix) throws IOException {
      Collection<Ref> refs;
      try {
        refs =
            perm.filter(
                git.getRefDatabase().getRefsByPrefix(prefix), git, RefFilterOptions.defaults());
      } catch (PermissionBackendException e) {
        throw new IOException(e);
      }
      // VisibleRefFilter adds the prefix to the keys, re-strip it.
      return refs.stream().collect(toMap(r -> r.getName().substring(prefix.length()), r -> r));
    }

    @Override
    @NonNull
    public ReflogReader getReflogReader(@NonNull Ref ref) throws IOException {
      return git.getReflogReader(ref);
    }

    @Override
    public List<Ref> getAdditionalRefs() throws IOException {
      return ImmutableList.copyOf(git.getRefDatabase().getAdditionalRefs());
    }

    @Override
    public Ref peel(Ref ref) throws IOException {
      return git.getRefDatabase().peel(ref);
    }
  }
}
