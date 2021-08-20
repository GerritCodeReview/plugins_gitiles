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

import static com.google.common.base.Preconditions.checkState;

import com.google.gerrit.entities.Project;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.ServiceMayNotContinueException;
import org.eclipse.jgit.transport.resolver.RepositoryResolver;
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException;

public class Resolver implements RepositoryResolver<HttpServletRequest> {
  private static final String NAME_KEY_ATTRIBUTE = Resolver.class.getName() + "/NameKey";
  private final Provider<CurrentUser> userProvider;

  static Project.NameKey getNameKey(HttpServletRequest req) {
    return (Project.NameKey) req.getAttribute(NAME_KEY_ATTRIBUTE);
  }

  private final FilteredRepository.Factory repoFactory;

  @Inject
  Resolver(FilteredRepository.Factory repoFactory, Provider<CurrentUser> userProvider) {
    this.repoFactory = repoFactory;
    this.userProvider = userProvider;
  }

  @Override
  public Repository open(HttpServletRequest req, String name)
      throws RepositoryNotFoundException, ServiceMayNotContinueException,
          ServiceNotAuthorizedException {
    Project.NameKey oldName = getNameKey(req);
    checkState(
        oldName == null,
        "Resolved multiple repositories on %s: %s, %s",
        req.getRequestURL(),
        oldName,
        name);
    Project.NameKey nameKey = Project.nameKey(name);
    req.setAttribute(NAME_KEY_ATTRIBUTE, nameKey);
    try {
      return repoFactory.create(nameKey);
    } catch (NoSuchProjectException e) {
      if (userProvider.get().isIdentifiedUser()) {
        throw new RepositoryNotFoundException(name, e);
      }
      // Allow anonymous users a chance to login.
      // Avoid leaking information by not distinguishing between
      // project not existing and no access rights.
      throw new ServiceNotAuthorizedException();
    } catch (IOException | PermissionBackendException e) {
      ServiceMayNotContinueException err =
          new ServiceMayNotContinueException("error opening repository " + name);
      err.initCause(e);
      throw err;
    }
  }
}
