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

import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.inject.Inject;

import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.ServiceMayNotContinueException;
import org.eclipse.jgit.transport.resolver.RepositoryResolver;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

class Resolver implements RepositoryResolver<HttpServletRequest> {
  private static final String NAME_KEY_ATTRIBUTE = Resolver.class.getName()
      + "/NameKey";

  static Project.NameKey getNameKey(HttpServletRequest req) {
    return (Project.NameKey) req.getAttribute(NAME_KEY_ATTRIBUTE);
  }

  private final FilteredRepository.Factory repoFactory;

  @Inject
  Resolver(FilteredRepository.Factory repoFactory) {
    this.repoFactory = repoFactory;
  }

  @Override
  public Repository open(HttpServletRequest req, String name)
      throws RepositoryNotFoundException, ServiceMayNotContinueException {
    Project.NameKey oldName = getNameKey(req);
    checkState(oldName == null, "Resolved multiple repositories on %s: %s, %s",
        req.getRequestURL(), oldName, name);
    Project.NameKey nameKey = new Project.NameKey(name);
    req.setAttribute(NAME_KEY_ATTRIBUTE, nameKey);
    try {
      return repoFactory.create(nameKey);
    } catch (NoSuchProjectException e) {
      throw new RepositoryNotFoundException(name, e);
    } catch (IOException e) {
      ServiceMayNotContinueException err =
          new ServiceMayNotContinueException("error opening repository " + name);
      err.initCause(e);
      throw err;
    }
  }
}
