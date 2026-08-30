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

import com.google.common.cache.Weigher;
import com.google.gitiles.IntraLineDiffKey;
import com.google.gitiles.diff.IntraLineDiffResult;
import com.google.gitiles.diff.ReplaceEdit;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.lib.Constants;

/**
 * Approximates the retained memory of a cached intraline result in bytes, so {@code memoryLimit} is
 * a byte budget (like Gerrit core's {@code diff_intraline} cache) rather than an entry count.
 * Values can be large, so size-based bounding matters for a persistent cache.
 *
 * <p>The estimate is the two blob-id key plus wrapper overhead, plus a fixed per-edit cost for each
 * edit and each {@link ReplaceEdit} internal edit.
 */
public class IntraLineDiffWeigher implements Weigher<IntraLineDiffKey, IntraLineDiffResult> {
  private static final int OBJECT_OVERHEAD = 16;
  private static final int EDIT_BYTES = 16 + 4 * 4;

  @Override
  public int weigh(IntraLineDiffKey key, IntraLineDiffResult value) {
    int bytes = 2 * OBJECT_OVERHEAD + 2 * Constants.OBJECT_ID_LENGTH;
    for (Edit e : value.edits()) {
      bytes += EDIT_BYTES;
      if (e instanceof ReplaceEdit) {
        bytes += EDIT_BYTES * ((ReplaceEdit) e).getInternalEdits().size();
      }
    }
    return bytes;
  }
}
