// Copyright (C) 2026 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.gitiles.cache;

import com.google.gerrit.server.cache.CacheModule;
import com.google.gitiles.IntraLineDiffKey;
import com.google.gitiles.diff.IntraLineDiffResult;

/**
 * Installs the persistent cache that backs the Gitiles intraline diff when running as a plugin.
 *
 * <p>The registered cache name is {@code intraline_diff} (exposed as {@code gitiles.intraline_diff}
 * because Gerrit namespaces plugin caches by plugin name); the operator-facing config key is the
 * explicit, stable {@code gitiles_intraline_diff}, sized with {@code [cache
 * "gitiles_intraline_diff"] memoryLimit/diskLimit}. {@code memoryLimit} is a byte budget (weighed
 * by {@link IntraLineDiffWeigher}), not an entry count. No {@code diskLimit} is set here, so
 * Gerrit's persistent default (128 MiB on disk) applies; {@code diskLimit = 0} makes it
 * memory-only.
 *
 * <p>The plugin installs this module (and binds the cache) only when enabled — {@code [gitiles]
 * intralineDiffCache = false} in {@code etc/gitiles.config} skips it entirely. Only successful
 * ({@code EDIT_LIST}) results are stored; bump {@code version()} whenever the algorithm output or
 * the serialized format changes.
 */
public class IntraLineDiffCacheModule extends CacheModule {
  public static final String NAME = "intraline_diff";

  @Override
  protected void configure() {
    persist(NAME, IntraLineDiffKey.class, IntraLineDiffResult.class)
        .version(1)
        .maximumWeight(10 << 20)
        .weigher(IntraLineDiffWeigher.class)
        .keySerializer(IntraLineDiffKeySerializer.INSTANCE)
        .valueSerializer(IntraLineDiffResultSerializer.INSTANCE)
        .configKey("gitiles_intraline_diff");
  }
}
