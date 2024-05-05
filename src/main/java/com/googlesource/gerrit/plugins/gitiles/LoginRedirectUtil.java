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

import com.google.gerrit.extensions.restapi.Url;
import com.google.gitiles.GitilesUrls;
import jakarta.servlet.http.HttpServletRequest;

public final class LoginRedirectUtil {
  private LoginRedirectUtil() {}

  public static String getLoginRedirectUrl(HttpServletRequest req, GitilesUrls urls) {
    String baseUrl = urls.getBaseGerritUrl(req);
    String token = getRequestUrl(req);
    if (baseUrl == null) {
      baseUrl = "";
    }
    if (!baseUrl.isEmpty() && token.startsWith(baseUrl)) {
      token = token.substring(baseUrl.length());
    }
    String queryString = req.getQueryString();
    if (queryString != null && !queryString.isEmpty()) {
      token = token.concat("?" + queryString);
    }
    String loginUrl = baseUrl + "login/";
    return loginUrl + Url.encode(token);
  }


  @SuppressWarnings("JdkObsolete")
  private static String getRequestUrl(HttpServletRequest req) {
    if (req.getRequestURL() == null) {
      return "";
    }
    return req.getRequestURL().toString();
  }
}
