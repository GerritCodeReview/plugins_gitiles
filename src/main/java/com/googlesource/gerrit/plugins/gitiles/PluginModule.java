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

import com.google.gerrit.common.Nullable;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.extensions.webui.BranchWebLink;
import com.google.gerrit.extensions.webui.FileHistoryWebLink;
import com.google.gerrit.extensions.webui.FileWebLink;
import com.google.gerrit.extensions.webui.ParentWebLink;
import com.google.gerrit.extensions.webui.PatchSetWebLink;
import com.google.gerrit.extensions.webui.ProjectWebLink;
import com.google.gerrit.extensions.webui.TagWebLink;
import com.google.gerrit.lifecycle.LifecycleModule;
import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.ssh.SshAdvertisedAddresses;
import com.google.gitiles.ArchiveFormat;
import com.google.gitiles.DefaultUrls;
import com.google.gitiles.GitilesAccess;
import com.google.gitiles.GitilesUrls;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.ProvisionException;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.transport.resolver.RepositoryResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PluginModule extends LifecycleModule {
  private final boolean noWebLinks;
  private final String cloneUrlType;
  private static final Logger log = LoggerFactory.getLogger(Module.class);

  @Inject
  PluginModule(PluginConfigFactory configFactory) {
    Config config = configFactory.getGlobalPluginConfig("gitiles");
    this.noWebLinks = config.getBoolean("gerrit", null, "noWebLinks", false);
    this.cloneUrlType = config.getString("gerrit", null, "cloneUrlType");
  }

  @Override
  protected void configure() {
    if (!noWebLinks) {
      DynamicSet.bind(binder(), BranchWebLink.class).to(GitilesWeblinks.class);
      DynamicSet.bind(binder(), FileHistoryWebLink.class).to(GitilesWeblinks.class);
      DynamicSet.bind(binder(), FileWebLink.class).to(GitilesWeblinks.class);
      DynamicSet.bind(binder(), ParentWebLink.class).to(GitilesWeblinks.class);
      DynamicSet.bind(binder(), PatchSetWebLink.class).to(GitilesWeblinks.class);
      DynamicSet.bind(binder(), ProjectWebLink.class).to(GitilesWeblinks.class);
      DynamicSet.bind(binder(), TagWebLink.class).to(GitilesWeblinks.class);
    }
    bind(GitilesAccess.Factory.class).to(GerritGitilesAccess.Factory.class);
    bind(new TypeLiteral<RepositoryResolver<HttpServletRequest>>() {}).to(Resolver.class);
    listener().to(Lifecycle.class);
  }

  private Optional<String> getSshCloneUrl(URL gerritUrl, List<String> advertisedSshAddresses) {
    try {
      if (!advertisedSshAddresses.isEmpty()) {
        String addr = advertisedSshAddresses.get(0);
        int index = addr.indexOf(":");
        String port = "";
        if (index != -1) {
          port = addr.substring(index);
        }
        if (addr.startsWith("*:") || "".equals(addr)) {
          if (gerritUrl != null && gerritUrl.getHost() != null) {
            addr = gerritUrl.getHost();
          } else {
            addr = getLocalHostName();
          }
        } else {
          if (index != -1) {
            addr = addr.substring(0, index);
          }
        }
        return Optional.of("ssh://" + addr + port + "/");
      }
    } catch (UnknownHostException e) {
      log.error("Unable to get SSH clone url.", e);
      return Optional.empty();
    }
    return Optional.empty();
  }

  private Optional<String> getHttpCloneUrl(Config gerritConfig) {
    Optional<String> httpUrl =
        Optional.ofNullable(gerritConfig.getString("gerrit", null, "gitHttpUrl"));
    if (httpUrl.isEmpty()) {
      return getDefaultCloneUrl(gerritConfig);
    }
    return httpUrl;
  }

  private Optional<String> getGitCloneUrl(Config gerritConfig) {
    Optional<String> gitUrl =
        Optional.ofNullable(gerritConfig.getString("gerrit", null, "canonicalGitUrl"));
    if (gitUrl.isEmpty()) {
      return getDefaultCloneUrl(gerritConfig);
    }
    return gitUrl;
  }

  private Optional<String> getDefaultCloneUrl(Config gerritConfig) {
    return Optional.ofNullable(gerritConfig.getString("gerrit", null, "canonicalWebUrl"));
  }

  private Optional<String> getUserConfig(
      Config gerritConfig, URL u, @SshAdvertisedAddresses List<String> advertisedSshAddresses) {
    Optional<String> gitUrl = Optional.empty();
    // Try to use user's config first.
    if (this.cloneUrlType != null) {
      switch (this.cloneUrlType) {
        case "ssh":
          gitUrl = getSshCloneUrl(u, advertisedSshAddresses);
          break;
        case "http":
          gitUrl = getHttpCloneUrl(gerritConfig);
          break;
        case "git":
          gitUrl = getGitCloneUrl(gerritConfig);
          break;
      }
      if (gitUrl.isEmpty()) {
        log.info(
            "Failed to use clone url type configuration."
                + " Using default type (prefer SSH, then HTTP, then Git).");
      }
    }
    return gitUrl;
  }

  @Provides
  GitilesUrls getGitilesUrls(
      @GerritServerConfig Config gerritConfig,
      @Nullable @CanonicalWebUrl String gerritUrl,
      @SshAdvertisedAddresses List<String> advertisedSshAddresses)
      throws MalformedURLException, UnknownHostException {
    URL u;
    String hostName;
    if (gerritUrl != null) {
      u = URI.create(gerritUrl).toURL();
      hostName = u.getHost() != null ? u.getHost() : getLocalHostName();
    } else {
      u = null;
      hostName = "Gerrit";
    }

    // If no config is set, or we can't get the chosen type of URL determined in the config,
    // arbitrarily prefer SSH, then HTTP, then git.
    String gitUrl =
        Stream.of(
                getUserConfig(gerritConfig, u, advertisedSshAddresses),
                getSshCloneUrl(u, advertisedSshAddresses),
                getHttpCloneUrl(gerritConfig),
                getGitCloneUrl(gerritConfig))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst()
            .orElseThrow(
                () ->
                    new ProvisionException(
                        "Unable to determine any canonical git URL from gerrit.config"));

    return new DefaultUrls(hostName, gitUrl, gerritUrl);
  }

  private String getLocalHostName() throws UnknownHostException {
    return InetAddress.getLocalHost().getCanonicalHostName();
  }

  @Singleton
  static class Lifecycle implements LifecycleListener {
    @Override
    public void start() {
      // Do nothing.
    }

    @Override
    public void stop() {
      ArchiveFormat.unregisterAll();
    }
  }
}
