/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.util;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Non-blocking generation of random V4 UUIDs.
 *
 * UUIDs are prepared in batches, which increases throughput (while introducing a bit of jitter).
 *
 * Usually UUID::randomUUID synchronizes on the shared SecureRandom instance,
 * which can lead to blocked threads.
 *
 */
public class UuidGenerator {

    private static final int UUID_BYTECOUNT = 16;
    private static final ThreadLocal<UuidGenerator> GENERATORS = ThreadLocal.withInitial(UuidGenerator::new);

    private final SecureRandom numberGenerator = new SecureRandom();

    /**
     * Prepare 32 UUIDs in one call
     */
    private final byte[] data = new byte[UUID_BYTECOUNT * 32];

    private int nextUuid = data.length;

    private UuidGenerator() {}

    /**
     *
     * UUID.randomUUID() with
     *
     * @return a random UUID
     */
    public static UUID randomUUID() {
        return GENERATORS.get().randomUUIDInternal();
    }

    private UUID randomUUIDInternal() {
        ensureBatchAvailable();
        int pos = nextUuid;
        nextUuid += UUID_BYTECOUNT;
        return createUuid(data, pos);
    }

    private void ensureBatchAvailable() {
        if (nextUuid >= data.length) {
            // prepare new batch
            numberGenerator.nextBytes(data);
            nextUuid = 0;
        }
    }

    private static UUID createUuid(byte[] data, int pos) {
        engraveV4Bytes(data, pos);
        long msb = 0;
        long lsb = 0;
        int i = pos;
        for (int m = i + 8; i < m; i++) msb = (msb << 8) | (data[i] & 0xff);
        for (int m = i + 8; i < m; i++) lsb = (lsb << 8) | (data[i] & 0xff);

        return new UUID(msb, lsb);
    }

    private static void engraveV4Bytes(byte[] data, int offset) {
        int p6 = offset + 6;
        data[p6] &= 0x0f; /* clear version        */
        data[p6] |= 0x40; /* set to version 4     */
        int p8 = offset + 8;
        data[p8] &= 0x3f; /* clear variant        */
        data[p8] |= 0x80; /* set to IETF variant  */
    }
}
