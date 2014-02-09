// Copyright (C) 2014 The Android Open Source Project
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

import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.client.AccountGeneralPreferences.DownloadScheme;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.ssh.SshAdvertisedAddresses;
import com.google.gitiles.GitilesUrls;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;

import org.eclipse.jgit.lib.Config;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

class GerritGitilesUrls implements GitilesUrls {
  private final String canonicalHostName;
  private final String baseGerritUrl;
  private final String gitHttpUrl;
  private final String sshAddress;
  private final Provider<CurrentUser> userProvider;

  @Inject
  public GerritGitilesUrls(@GerritServerConfig Config gerritConfig,
      @Nullable @CanonicalWebUrl String canonicalWebUrl,
      @SshAdvertisedAddresses List<String> sshAdvertisedAddresses,
      Provider<CurrentUser> userProvider)
      throws MalformedURLException, UnknownHostException {

    this.userProvider = userProvider;
    this.baseGerritUrl = canonicalWebUrl;

    // Host Name
    URL gerritUrl;
    String hostName;
    if (canonicalWebUrl == null) {
      gerritUrl = null;
      hostName = getLocalHostName();
    } else {
      gerritUrl = new URL(canonicalWebUrl);
      hostName = gerritUrl.getHost();
      if (hostName == null) {
        hostName = getLocalHostName();
      }
    }
    canonicalHostName = hostName;

    // Initialize the sshAddress
    if (sshAdvertisedAddresses == null || sshAdvertisedAddresses.isEmpty()) {
      sshAddress = null;
    } else {
      final StringBuilder r = new StringBuilder();
      final String configSshAddress = sshAdvertisedAddresses.get(0);
      if (configSshAddress.startsWith("*:") || "".equals(configSshAddress)) {
        r.append(hostName);
      }
      if (configSshAddress.startsWith("*")) {
        r.append(configSshAddress.substring(1));
      } else {
        r.append(configSshAddress);
      }
      sshAddress = r.toString();
    }

    // Initialize the git http url
    String gitUrl = gerritConfig.getString("gerrit", null, "gitHttpUrl");
    if (gitUrl == null) {
      gitUrl = gerritConfig.getString("gerrit", null, "canonicalGitUrl");
      if (gitUrl == null) {
        gitUrl = canonicalWebUrl;
        if (gitUrl == null) {
          throw new ProvisionException("Unable to determine any canonical git URL from gerrit.config");
        }
      }
    }
    gitHttpUrl = gitUrl;
  }

  private String getLocalHostName() throws UnknownHostException {
    return InetAddress.getLocalHost().getCanonicalHostName();
  }

  @Override
  public String getHostName(HttpServletRequest req) {
    return canonicalHostName;
  }

  @Override
  public String getBaseGerritUrl(HttpServletRequest req) {
    return baseGerritUrl;
  }

  @Override
  public String getBaseGitUrl(HttpServletRequest req) {
    CurrentUser currentUser = userProvider.get();
    if (!currentUser.isIdentifiedUser()) {
      return getBaseGitUrlAnonHttp();
    }

    IdentifiedUser user = (IdentifiedUser) currentUser;
    Account account = user.getAccount();
    DownloadScheme scheme = account.getGeneralPreferences().getDownloadUrl();

    switch (scheme) {
    case ANON_HTTP:
      return getBaseGitUrlAnonHttp();
    case HTTP:
      return getBaseGitUrlHttp(account);
    case SSH:
      if (sshAddress != null) {
        return getBaseGitUrlSsh(account);
      } else {
        // SSH access is disabled.  Fallback to HTTP.
        return getBaseGitUrlHttp(account);
      }
    default:
      // Treat HTTP as the default.
      return getBaseGitUrlHttp(account);
    }
  }

  private String getBaseGitUrlAnonHttp() {
    return gitHttpUrl;
  }

  private String getBaseGitUrlHttp(Account account) {
    final StringBuilder r = new StringBuilder();

    int p = gitHttpUrl.indexOf("://");
    int s = gitHttpUrl.indexOf('/', p + 3);
    if (s < 0) {
      s = gitHttpUrl.length();
    }
    String host = gitHttpUrl.substring(p + 3, s);
    if (host.contains("@")) {
      host = host.substring(host.indexOf('@') + 1);
    }

    r.append(gitHttpUrl.substring(0, p + 3));
    r.append(account.getUserName());
    r.append('@');
    r.append(host);
    r.append(gitHttpUrl.substring(s));
    return r.toString();
  }

  private String getBaseGitUrlSsh(Account account) {
    final StringBuilder r = new StringBuilder();
    r.append("ssh://");
    r.append(account.getUserName());
    r.append("@");
    r.append(sshAddress);
    r.append("/");
    return r.toString();
  }
}
