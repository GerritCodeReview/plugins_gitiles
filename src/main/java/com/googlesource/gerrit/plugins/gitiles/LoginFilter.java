// Copyright (C) 2015 The Android Open Source Project
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

import com.google.gerrit.extensions.restapi.Url;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gitiles.GitilesUrls;
import com.google.inject.Inject;
import com.google.inject.Provider;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

class LoginFilter implements Filter {
  private final Provider<CurrentUser> userProvider;
  private final GitilesUrls urls;

  @Inject
  LoginFilter(Provider<CurrentUser> userProvider, GitilesUrls urls) {
    this.userProvider = userProvider;
    this.urls = urls;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response,
      FilterChain chain) throws IOException, ServletException {
    final HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponseWrapper rsp = new HttpServletResponseWrapper((HttpServletResponse) response) {
      @Override
      public void sendError(int sc) throws IOException {
        CurrentUser user = userProvider.get();
        if (sc == SC_UNAUTHORIZED && !(user instanceof IdentifiedUser)) {
          sendRedirect(getLoginRedirectUrl(req));
          return;
        }
        super.sendError(sc);
      }

      @Override
      public void sendError(int sc, String msg) throws IOException {
        CurrentUser user = userProvider.get();
        if (sc == SC_UNAUTHORIZED && !(user instanceof IdentifiedUser)) {
          sendRedirect(getLoginRedirectUrl(req));
          return;
        }
        super.sendError(sc, msg);
      }
    };
    chain.doFilter(request, rsp);
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void destroy() {
  }

  private String getLoginRedirectUrl(HttpServletRequest req) {
    String baseUrl = urls.getBaseGerritUrl(req);
    String loginUrl = baseUrl + "login/";
    String token = req.getRequestURL().toString();
    if (!baseUrl.isEmpty()) {
      token = token.substring(baseUrl.length());
    }

    String queryString = req.getQueryString();
    if (queryString != null && !queryString.isEmpty()) {
      token = token.concat("?" + queryString);
    }
    return (loginUrl + Url.encode(token));
  }
}
