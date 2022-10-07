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
package org.ehrbase.api.tenant;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ThreadLocalSupplierTest {

    @Test
    @SuppressWarnings("rawtypes")
    public void test() throws InterruptedException {
        String currentThread = Thread.currentThread().toString();

        ThreadLocalSupplier<String> tl = ThreadLocalSupplier.supplyFor(String.class);
        tl.accept(currentThread);

        Thread thread1 = new Thread(() -> {
            String currentThread2 = Thread.currentThread().toString();
            ThreadLocalSupplier<String> tl1 = ThreadLocalSupplier.supplyFor(String.class);
            Assertions.assertTrue(tl == tl1);
            Assertions.assertTrue(null == tl1.get());
            tl1.accept(currentThread2);
            Assertions.assertTrue(currentThread2.equals(tl1.get()));
        });

        Thread thread2 = new Thread(() -> {
            Integer currentThread2 = Thread.currentThread().hashCode();
            ThreadLocalSupplier<Integer> tl2 = ThreadLocalSupplier.supplyFor(Integer.class);
            Assertions.assertTrue((ThreadLocalSupplier) tl != (ThreadLocalSupplier) tl2);
            Assertions.assertTrue(null == tl2.get());
            tl2.accept(currentThread2);
            Assertions.assertTrue(currentThread2.equals(tl2.get()));
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        Assertions.assertTrue(currentThread.equals(tl.get()));
    }
}
