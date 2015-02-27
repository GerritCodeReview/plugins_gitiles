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

import com.google.gerrit.server.config.SitePaths;
import com.google.gitiles.GitilesAccess;
import com.google.gitiles.GitilesServlet;
import com.google.gitiles.GitilesUrls;
import com.google.gitiles.GitilesView;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.servlet.ServletModule;

import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.transport.resolver.RepositoryResolver;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

class HttpModule extends ServletModule {
  protected Filter createPathFilter() {
    return new Filter() {
      @Override
      public void doFilter(ServletRequest request, ServletResponse response,
          FilterChain chain) throws IOException, ServletException {
        HttpServletRequestWrapper wrappedRequest =
            new HttpServletRequestWrapper((HttpServletRequest) request) {
              @Override
              public String getRequestURI() {
                try {
                  // Note: URLDecoder.decode() will decode "+" to a space.
                  // This doesn't work, so use URI.getPath() instead.
                  URI uri = new URI(super.getRequestURI());
                  return uri.getPath();
                } catch (URISyntaxException e) {
                  return super.getRequestURI();
                }
              }
            };
        chain.doFilter(wrappedRequest, response);
      }

      @Override
      public void destroy() {}

      @Override
      public void init(FilterConfig config) throws ServletException {}
    };
  }

  private static final Logger log = LoggerFactory
      .getLogger(ServletModule.class);

  @Override
  protected void configureServlets() {
    // Filter all paths so we can decode escaped entities in the URI
    filter("/*").through(createPathFilter());
    // Let /+static, /+Documentation, etc. fall through to default servlet, but
    // handle everything else.
    serveRegex("^(/)$", "^(/[^+].*)").with(GitilesServlet.class);
  }

  @Provides
  @Singleton
  GitilesServlet getServlet(@Named("gitiles") Config cfg, GitilesUrls urls,
      GitilesAccess.Factory accessFactory,
      RepositoryResolver<HttpServletRequest> resolver,
      MenuFilter menuFilter) throws ServletException {
    GitilesServlet s = new GitilesServlet(cfg, null, urls, accessFactory, resolver, null, null, null, null);
    for (GitilesView.Type view : GitilesView.Type.values()) {
      s.addFilter(view, menuFilter);
    }
    return s;
  }

  @Provides
  @Named("gitiles")
  Config getConfig(SitePaths site) throws IOException, ConfigInvalidException {
    File cfgFile = site.etc_dir.resolve("gitiles.config").toFile();
    FileBasedConfig cfg = new FileBasedConfig(cfgFile, FS.DETECTED);
    if (cfg.getFile().exists()) {
      cfg.load();
    } else {
      log.info("No " + cfgFile.getAbsolutePath() + "; assuming defaults");
    }
    return cfg;
  }
}
