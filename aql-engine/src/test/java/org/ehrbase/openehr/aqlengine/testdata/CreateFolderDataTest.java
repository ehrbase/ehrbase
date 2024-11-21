/*
 * Copyright (c) 2024 vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.openehr.aqlengine.testdata;

import java.net.URI;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.java.quickcheck.generator.PrimitiveGenerators;
import org.ehrbase.openehr.sdk.client.openehrclient.OpenEhrClientConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
class CreateFolderDataTest {

    interface CreateEhr<T> {
        T createEhr();
    }

    interface CreateFolder<T> {
        T createRootFolder();
    }

    interface CreateEncounterFolder<T> {
        T createEncounterFolder(int num);
    }

    interface CreateSubEncounterFolder<T> {
        T createSubEncounterFolder(int num);
    }

    interface CreateItemsPerSubEncounterFolder<T> {
        T createItemsperSubEncounterFolder(int num);
    }

    private static final String RESULT = "Created EHR[%s] With Root-Folder[%s]";
    private static final String FOLDER_ITEMS = "Creating %ds Items For Folder[%s]";

    public class TestDataGenerator {
        private final OpenEhrClientConfig cfg = new OpenEhrClientConfig(URI.create("http://localhost:8080/ehrbase/"));
        private final CompositionSupport compositionSupport = new CompositionSupport(cfg);
        private final FolderSupport folderSupport = new FolderSupport(cfg);
        private final EhrSupport ehrSupport = new EhrSupport(cfg);

        CreateEhr<
                        CreateFolder<
                                CreateEncounterFolder<
                                        CreateSubEncounterFolder<CreateItemsPerSubEncounterFolder<String>>>>>
                create() {
            return () -> () -> numEncFolder -> numSubEncFolder -> numItems -> {
                EhrSpec ehrSpec = new EhrSpec(ehrSupport) {
                    String build() {
                        UUID ehrId = this.ehrSupport.create(UUID.randomUUID(), "namespace_8480722");

                        String allEncounterFolder = IntStream.range(0, anyNumberLessThan(numEncFolder))
                                .mapToObj(i -> {
                                    String allSubFolder = IntStream.range(0, anyNumberLessThan(numSubEncFolder))
                                            .mapToObj(i1 -> {
                                                int itemNum = anyNumberLessThan(numItems);
                                                System.out.println(
                                                        FOLDER_ITEMS.formatted(itemNum, "subEncounterFolder" + i1));
                                                return createFolder(
                                                        "subEncounterFolder" + i1,
                                                        NO_FOLDER,
                                                        createItems(compositionSupport, ehrId, itemNum));
                                            })
                                            .collect(Collectors.joining(","));
                                    return createFolder("encounter" + i, allSubFolder, NO_ITEMS);
                                })
                                .collect(Collectors.joining(","));

                        UUID folderUUID =
                                folderSupport.create(ehrId, createFolder("rootFolder", allEncounterFolder, NO_ITEMS));
                        System.out.println(RESULT.formatted(ehrId, folderUUID));
                        return ehrId.toString();
                    }
                };

                return ehrSpec.build();
            };
        }
    }

    @Test
    void generateTestData() {
        String ehrId = new TestDataGenerator()
                .create()
                .createEhr()
                .createRootFolder()
                .createEncounterFolder(1)
                .createSubEncounterFolder(anyNumberLessThan(10))
                .createItemsperSubEncounterFolder(anyNumberLessThan(50));

        System.out.println("Ehr[%s] Created".formatted(ehrId));
    }

    private static final String NO_ITEMS = "";
    private static final String NO_FOLDER = "";

    private static int anyNumberLessThan(int num) {
        return PrimitiveGenerators.integers(1, num).next();
    }

    private String createFolder(String name, String subFolder, String items) {
        FolderSpec folderSpec = new FolderSpec(name, UUID.randomUUID()) {
            String build() {
                return FolderSupport.render(UUID.randomUUID(), this.name, subFolder, items);
            }
        };
        return folderSpec.build();
    }

    private String createItems(CompositionSupport compositionSupport, UUID ehrId, int numItems) {
        return IntStream.range(0, numItems)
                .mapToObj(i2 -> {
                    ItemSpec item = new ItemSpec(compositionSupport, "localNS") {
                        String build() {
                            return ItemSupport.create(
                                    compositionSupport.create(ehrId).getRoot().getValue(), this.ns);
                        }
                    };

                    return item.build();
                })
                .collect(Collectors.joining(","));
    }

    abstract static class EhrSpec {
        final EhrSupport ehrSupport;

        public EhrSpec(EhrSupport ehrSupport) {
            this.ehrSupport = ehrSupport;
        }

        abstract String build();
    }

    abstract static class FolderSpec {
        final String name;
        final UUID uuid;

        public FolderSpec(String name, UUID uuid) {
            this.name = name;
            this.uuid = uuid;
        }

        abstract String build();
    }

    abstract static class ItemSpec {
        final CompositionSupport compositionSupport;
        final String ns;

        public ItemSpec(CompositionSupport compositionSupport, String ns) {
            this.compositionSupport = compositionSupport;
            this.ns = ns;
        }

        abstract String build();
    }
}
