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

import com.google.gerrit.server.config.AllProjectsName;
import com.google.gerrit.server.config.AllProjectsNameProvider;
import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.ssh.SshAdvertisedAddresses;
import com.google.gitiles.DefaultUrls;
import com.google.gitiles.GitilesAccess;
import com.google.gitiles.GitilesUrls;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.ProvisionException;
import com.google.inject.TypeLiteral;

import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.transport.resolver.RepositoryResolver;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

class Module extends AbstractModule {
  @Override
  protected void configure() {
    bind(GitilesUrls.class).to(GerritGitilesUrls.class);
    bind(GitilesAccess.Factory.class).to(GerritGitilesAccess.Factory.class);
    bind(new TypeLiteral<RepositoryResolver<HttpServletRequest>>() {}).to(Resolver.class);
    bind(AllProjectsName.class).toProvider(AllProjectsNameProvider.class);
  }

  private String getLocalHostName() throws UnknownHostException {
    return InetAddress.getLocalHost().getCanonicalHostName();
  }
}
