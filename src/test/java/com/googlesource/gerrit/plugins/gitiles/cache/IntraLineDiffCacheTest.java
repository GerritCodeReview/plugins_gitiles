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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.gitiles.IntraLineDiffKey;
import com.google.gitiles.diff.IntraLineDiffResult;
import com.google.gitiles.diff.ReplaceEdit;
import java.util.concurrent.Callable;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class IntraLineDiffCacheTest {
  private static final ObjectId A = ObjectId.fromString("0000000000000000000000000000000000000001");
  private static final ObjectId B = ObjectId.fromString("0000000000000000000000000000000000000002");
  private final IntraLineDiffKey key = new IntraLineDiffKey(A, B);

  @Test
  public void keyRoundTrip() {
    IntraLineDiffKey restored =
        IntraLineDiffKeySerializer.INSTANCE.deserialize(
            IntraLineDiffKeySerializer.INSTANCE.serialize(key));
    assertThat(restored).isEqualTo(key);
    assertThat(restored.blobA()).isEqualTo(A);
    assertThat(restored.blobB()).isEqualTo(B);
  }

  @Test
  public void valueRoundTripPreservesReplaceEditInternals() {
    ReplaceEdit re = new ReplaceEdit(0, 2, 0, 2, ImmutableList.of(new Edit(1, 2, 1, 2)));
    IntraLineDiffResult result =
        IntraLineDiffResult.editList(ImmutableList.of(new Edit(3, 4, 3, 4), re));

    IntraLineDiffResult restored = roundTrip(result);

    assertThat(restored.status()).isEqualTo(IntraLineDiffResult.Status.EDIT_LIST);
    assertThat(restored.edits()).hasSize(2);
    assertThat(restored.edits().get(0).getBeginA()).isEqualTo(3);
    assertThat(restored.edits().get(1)).isInstanceOf(ReplaceEdit.class);
    ReplaceEdit restoredRe = (ReplaceEdit) restored.edits().get(1);
    assertThat(restoredRe.getEndB()).isEqualTo(2);
    assertThat(restoredRe.getInternalEdits()).hasSize(1);
    assertThat(restoredRe.getInternalEdits().get(0).getBeginA()).isEqualTo(1);
  }

  @Test
  public void valueRoundTripPreservesEveryStatus() {
    ImmutableList<Edit> edits = ImmutableList.of(new Edit(0, 1, 0, 1));
    for (IntraLineDiffResult.Status status : IntraLineDiffResult.Status.values()) {
      assertThat(roundTrip(forStatus(status, edits)).status()).isEqualTo(status);
    }
  }

  private static IntraLineDiffResult forStatus(
      IntraLineDiffResult.Status status, ImmutableList<Edit> edits) {
    switch (status) {
      case TIMEOUT:
        return IntraLineDiffResult.timeout(edits);
      case ERROR:
        return IntraLineDiffResult.error(edits);
      case DISABLED:
        return IntraLineDiffResult.disabled(edits);
      case EDIT_LIST:
      default:
        return IntraLineDiffResult.editList(edits);
    }
  }

  @Test
  public void hitServesSecondCallWithoutRecomputing() throws Exception {
    GerritPersistentIntraLineDiffCache cache = newCache();
    int[] calls = {0};
    Callable<IntraLineDiffResult> loader =
        counting(calls, IntraLineDiffResult.editList(ImmutableList.of(new Edit(0, 1, 0, 1))));

    cache.get(key, loader);
    IntraLineDiffResult second = cache.get(key, loader);

    assertThat(calls[0]).isEqualTo(1);
    assertThat(second.status()).isEqualTo(IntraLineDiffResult.Status.EDIT_LIST);
  }

  @Test
  public void nonCacheableResultsAreNotStored() throws Exception {
    ImmutableList<Edit> edits = ImmutableList.of(new Edit(0, 1, 0, 1));
    for (IntraLineDiffResult.Status status : IntraLineDiffResult.Status.values()) {
      if (status != IntraLineDiffResult.Status.EDIT_LIST) {
        assertNotCached(forStatus(status, edits));
      }
    }
  }

  private void assertNotCached(IntraLineDiffResult nonCacheable) throws Exception {
    GerritPersistentIntraLineDiffCache cache = newCache();
    int[] calls = {0};
    Callable<IntraLineDiffResult> loader = counting(calls, nonCacheable);

    cache.get(key, loader);
    cache.get(key, loader);

    assertThat(calls[0]).isEqualTo(2);
  }

  private static IntraLineDiffResult roundTrip(IntraLineDiffResult result) {
    return IntraLineDiffResultSerializer.INSTANCE.deserialize(
        IntraLineDiffResultSerializer.INSTANCE.serialize(result));
  }

  private static GerritPersistentIntraLineDiffCache newCache() {
    Cache<IntraLineDiffKey, IntraLineDiffResult> backing = CacheBuilder.newBuilder().build();
    return new GerritPersistentIntraLineDiffCache(backing);
  }

  private static Callable<IntraLineDiffResult> counting(int[] calls, IntraLineDiffResult result) {
    return () -> {
      calls[0]++;
      return result;
    };
  }
}
