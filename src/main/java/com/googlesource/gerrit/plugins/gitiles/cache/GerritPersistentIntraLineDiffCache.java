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

import com.google.common.cache.Cache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.gitiles.IntraLineDiffCache;
import com.google.gitiles.IntraLineDiffKey;
import com.google.gitiles.diff.IntraLineDiffResult;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * Gitiles {@link IntraLineDiffCache} backed by Gerrit's persistent cache.
 *
 * <p>Loads atomically per key and stores only cacheable ({@code EDIT_LIST}) results — a timeout or
 * error result bypasses the cache via {@link SkipCacheException} so it is never persisted.
 */
@Singleton
public class GerritPersistentIntraLineDiffCache implements IntraLineDiffCache {
  private final Cache<IntraLineDiffKey, IntraLineDiffResult> cache;

  @Inject
  GerritPersistentIntraLineDiffCache(
      @Named(IntraLineDiffCacheModule.NAME) Cache<IntraLineDiffKey, IntraLineDiffResult> cache) {
    this.cache = cache;
  }

  @Override
  public IntraLineDiffResult get(IntraLineDiffKey key, Callable<IntraLineDiffResult> loader)
      throws Exception {
    try {
      return cache.get(
          key,
          () -> {
            IntraLineDiffResult result = loader.call();
            if (!result.isCacheable()) {
              throw new SkipCacheException(result);
            }
            return result;
          });
    } catch (ExecutionException | UncheckedExecutionException e) {
      if (e.getCause() instanceof SkipCacheException) {
        return ((SkipCacheException) e.getCause()).result;
      }
      throw e;
    }
  }

  /** Thrown by the loader to return a non-cacheable result without storing it. */
  private static final class SkipCacheException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    final transient IntraLineDiffResult result;

    SkipCacheException(IntraLineDiffResult result) {
      super(null, null, false, false);
      this.result = result;
    }
  }
}
