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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.git.ChangeCache;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.git.TagCache;
import com.google.gerrit.server.git.VisibleRefFilter;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.gerrit.server.project.ProjectControl;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.eclipse.jgit.internal.storage.file.ReflogReader;
import org.eclipse.jgit.lib.ObjectDatabase;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.RefRename;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.lib.StoredConfig;

import java.io.IOException;
import java.util.List;
import java.util.Map;

class FilteredRepository extends Repository {
  static class Factory {
    private final Provider<ReviewDb> db;
    private final ProjectControl.GenericFactory projectControlFactory;
    private final Provider<CurrentUser> userProvider;
    private final GitRepositoryManager repoManager;
    private final TagCache tagCache;
    private final ChangeCache changeCache;

    @Inject
    Factory(Provider<ReviewDb> db,
        ProjectControl.GenericFactory projectControlFactory,
        Provider<CurrentUser> userProvider,
        GitRepositoryManager repoManager,
        TagCache tagCache,
        ChangeCache changeCache) {
      this.db = db;
      this.projectControlFactory = projectControlFactory;
      this.userProvider = userProvider;
      this.repoManager = repoManager;
      this.tagCache = tagCache;
      this.changeCache = changeCache;
    }

    FilteredRepository create(Project.NameKey name)
        throws NoSuchProjectException, IOException {
      ProjectControl ctl = projectControlFactory.controlFor(name, userProvider.get());
      Repository repo = repoManager.openRepository(name);
      return new FilteredRepository(ctl, repo,
          new VisibleRefFilter(tagCache, changeCache, repo, ctl, db.get(), true));
    }
  }

  private final Repository delegate;
  private final RefDatabase refdb;

  private FilteredRepository(ProjectControl ctl, Repository delegate,
      VisibleRefFilter refFilter) {
    super(toBuilder(delegate));
    this.delegate = delegate;
    if (ctl.allRefsAreVisible()) {
      this.refdb = delegate.getRefDatabase();
    } else {
      this.refdb = new FilteredRefDatabase(delegate.getRefDatabase(), refFilter);
    }
  }

  private static RepositoryBuilder toBuilder(Repository repo) {
    RepositoryBuilder b = new RepositoryBuilder().setGitDir(repo.getDirectory())
      .setFS(repo.getFS());
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
    delegate.scanForRepoChanges();
  }

  @Override
  public void notifyIndexChanged() {
    delegate.notifyIndexChanged();
  }

  @Override
  public ReflogReader getReflogReader(String refName) throws IOException {
    return getRef(refName) != null ? delegate.getReflogReader(refName) : null;
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
      delegate.close();
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
    public RefRename newRename(String fromName, String toName)
        throws IOException {
      throw new UnsupportedOperationException(); // Gitiles is read-only.
    }

    @Override
    public Ref getRef(String name) throws IOException {
      return refFilter.filter(ImmutableMap.of(name, delegate.getRef(name)), true).get(name);
    }

    @Override
    public Map<String, Ref> getRefs(String prefix) throws IOException {
      return refFilter.filter(delegate.getRefs(prefix), true);
    }

    @Override
    public List<Ref> getAdditionalRefs() throws IOException {
      List<Ref> refs = delegate.getAdditionalRefs();
      Map<String, Ref> refsMap = Maps.newHashMapWithExpectedSize(refs.size());
      for (Ref ref : refs) {
        refsMap.put(ref.getName(), ref);
      }
      return ImmutableList.copyOf(refFilter.filter(refsMap, true).values());
    }

    @Override
    public Ref peel(Ref ref) throws IOException {
      return delegate.peel(ref);
    }
  }
}
