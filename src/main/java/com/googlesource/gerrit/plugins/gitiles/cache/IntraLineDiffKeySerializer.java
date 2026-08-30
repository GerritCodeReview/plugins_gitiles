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

import com.google.gerrit.server.cache.serialize.CacheSerializer;
import com.google.gitiles.IntraLineDiffKey;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;

/** Serializes an {@link IntraLineDiffKey} as its two raw 20-byte blob ids. */
public enum IntraLineDiffKeySerializer implements CacheSerializer<IntraLineDiffKey> {
  INSTANCE;

  private static final int LEN = Constants.OBJECT_ID_LENGTH;

  @Override
  public byte[] serialize(IntraLineDiffKey key) {
    byte[] buf = new byte[2 * LEN];
    key.blobA().copyRawTo(buf, 0);
    key.blobB().copyRawTo(buf, LEN);
    return buf;
  }

  @Override
  public IntraLineDiffKey deserialize(byte[] in) {
    if (in == null || in.length != 2 * LEN) {
      throw new IllegalArgumentException("Failed to deserialize IntraLineDiffKey");
    }
    return new IntraLineDiffKey(ObjectId.fromRaw(in, 0), ObjectId.fromRaw(in, LEN));
  }
}
