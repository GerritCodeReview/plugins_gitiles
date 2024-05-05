// Copyright (C) 2025 The Android Open Source Project
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

import com.google.gitiles.GitilesUrls;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Singleton
public class LoginRedirectErrorHandler implements Filter {
  private final GitilesUrls urls;

  @Inject
  LoginRedirectErrorHandler(GitilesUrls urls) {
    this.urls = urls;
  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest httpReq = (HttpServletRequest) req;
    HttpServletResponse httpRes = (HttpServletResponse) res;

    try {
      chain.doFilter(req, res);
    } catch (LoginRedirectRequiredException e) {
      String loginUrl = LoginRedirectUtil.getLoginRedirectUrl(httpReq, urls);
      httpRes.sendRedirect(loginUrl);
    }
  }

  @Override
  public void init(FilterConfig cfg) throws ServletException {}

  @Override
  public void destroy() {}
}
