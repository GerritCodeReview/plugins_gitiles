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

import com.google.common.collect.ImmutableList;
import com.google.gerrit.server.cache.serialize.CacheSerializer;
import com.google.gitiles.diff.IntraLineDiffResult;
import com.google.gitiles.diff.ReplaceEdit;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.diff.Edit;

/** Serializes an {@link IntraLineDiffResult}: its status and (Replace)edits with internals. */
public enum IntraLineDiffResultSerializer implements CacheSerializer<IntraLineDiffResult> {
  INSTANCE;

  @Override
  public byte[] serialize(IntraLineDiffResult result) {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (DataOutputStream out = new DataOutputStream(bytes)) {
      out.writeByte(result.status().ordinal());
      ImmutableList<Edit> edits = result.edits();
      out.writeInt(edits.size());
      for (Edit e : edits) {
        writeEdit(out, e);
        if (e instanceof ReplaceEdit) {
          ImmutableList<Edit> internal = ((ReplaceEdit) e).getInternalEdits();
          out.writeBoolean(true);
          out.writeInt(internal.size());
          for (Edit ie : internal) {
            writeEdit(out, ie);
          }
        } else {
          out.writeBoolean(false);
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return bytes.toByteArray();
  }

  @Override
  public IntraLineDiffResult deserialize(byte[] in) {
    try (DataInputStream data = new DataInputStream(new ByteArrayInputStream(in))) {
      IntraLineDiffResult.Status status = IntraLineDiffResult.Status.values()[data.readByte()];
      int size = data.readInt();
      List<Edit> edits = new ArrayList<>(size);
      for (int i = 0; i < size; i++) {
        Edit edit = readEdit(data);
        if (data.readBoolean()) {
          int internalSize = data.readInt();
          List<Edit> internal = new ArrayList<>(internalSize);
          for (int j = 0; j < internalSize; j++) {
            internal.add(readEdit(data));
          }
          edits.add(
              new ReplaceEdit(
                  edit.getBeginA(), edit.getEndA(), edit.getBeginB(), edit.getEndB(), internal));
        } else {
          edits.add(edit);
        }
      }
      return fromStatus(status, edits);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void writeEdit(DataOutputStream out, Edit e) throws IOException {
    out.writeInt(e.getBeginA());
    out.writeInt(e.getEndA());
    out.writeInt(e.getBeginB());
    out.writeInt(e.getEndB());
  }

  private static Edit readEdit(DataInputStream in) throws IOException {
    return new Edit(in.readInt(), in.readInt(), in.readInt(), in.readInt());
  }

  private static IntraLineDiffResult fromStatus(
      IntraLineDiffResult.Status status, List<Edit> edits) {
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
}
