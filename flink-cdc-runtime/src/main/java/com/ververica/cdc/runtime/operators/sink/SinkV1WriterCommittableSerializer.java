/*
 * Copyright 2023 Ververica Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ververica.cdc.runtime.operators.sink;

import org.apache.flink.core.io.SimpleVersionedSerializer;
import org.apache.flink.core.memory.DataInputDeserializer;
import org.apache.flink.streaming.runtime.operators.sink.committables.SinkV1CommittableDeserializer;

import com.ververica.cdc.common.annotation.Internal;

import java.io.IOException;
import java.util.List;

import static org.apache.flink.util.Preconditions.checkNotNull;

@Internal
class SinkV1WriterCommittableSerializer<CommT> implements SimpleVersionedSerializer<List<CommT>> {

    private final SimpleVersionedSerializer<CommT> committableSerializer;

    public SinkV1WriterCommittableSerializer(
            SimpleVersionedSerializer<CommT> committableSerializer) {
        this.committableSerializer = checkNotNull(committableSerializer);
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public byte[] serialize(List<CommT> obj) throws IOException {
        throw new UnsupportedOperationException(
                "This serializer should only be used to deserialize legacy committable state.");
    }

    @Override
    public List<CommT> deserialize(int version, byte[] serialized) throws IOException {
        return SinkV1CommittableDeserializer.readVersionAndDeserializeList(
                committableSerializer, new DataInputDeserializer(serialized));
    }
}