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
    bind(GitilesAccess.Factory.class).to(GerritGitilesAccess.Factory.class);
    bind(new TypeLiteral<RepositoryResolver<HttpServletRequest>>() {}).to(Resolver.class);
    bind(AllProjectsName.class).toProvider(AllProjectsNameProvider.class);
  }

  @Provides
  GitilesUrls getGitilesUrls(@GerritServerConfig Config gerritConfig,
      @Nullable @CanonicalWebUrl String gerritUrl,
      @SshAdvertisedAddresses List<String> advertisedSshAddresses)
      throws MalformedURLException, UnknownHostException {
    URL u;
    String hostName;
    if (gerritUrl != null) {
      u = new URL(gerritUrl);
      if (u.getHost() == null) {
        throw new ProvisionException("Missing host in gerrit.canonicalWebUrl " + gerritUrl);
      }
      // Roughly matches Gerrit.initHostname();
      hostName = u.getHost();
      int d1 = hostName.indexOf('.');
      if (d1 >= 0) {
        int d2 = hostName.indexOf('.', d1 + 1);
        if (d2 >= 0) {
          hostName = hostName.substring(0, d2);
        }
      }
    } else {
      u = null;
      hostName = "Gerrit";
    }

    // Arbitrarily prefer SSH, then HTTP, then git.
    // TODO: Use user preferences.
    String gitUrl;
    if (!advertisedSshAddresses.isEmpty()) {
      String addr = advertisedSshAddresses.get(0);
      if (addr.startsWith("*:") || "".equals(addr)) {
        addr = u != null ? u.getHost() : InetAddress.getLocalHost().getCanonicalHostName();
      }
      gitUrl = "ssh://" + addr + "/";
    } else {
      gitUrl = gerritConfig.getString("gerrit", null, "gitHttpUrl");
      if (gitUrl == null) {
        gitUrl = gerritConfig.getString("gerrit", null, "canonicalGitUrl");
      }
    }
    if (gitUrl == null) {
      throw new ProvisionException("Unable to determine any canonical git URL from gerrit.config");
    }
    return new DefaultUrls(hostName, gitUrl, gerritUrl);
  }
}
