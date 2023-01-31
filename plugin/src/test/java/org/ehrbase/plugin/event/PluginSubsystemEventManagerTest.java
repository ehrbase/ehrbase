/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.plugin.event;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.ehrbase.api.plugin.PluginEvent;
import org.ehrbase.api.plugin.PluginEventListener;
import org.ehrbase.api.plugin.PluginEventing;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class PluginSubsystemEventManagerTest {
  @Test
  void sendAsync() throws InterruptedException {
    AtomicInteger cntI = new AtomicInteger(0);
    AtomicInteger cntII = new AtomicInteger(0);
    
    PluginEventListener listenerI = new PluginEventListener() {
      public void handle(PluginEvent event) { cntI.incrementAndGet(); }
      public boolean accept(PluginEvent event) { return "type_I".equals(event.getType()); }
    };
    
    PluginEventListener listenerII = new PluginEventListener() {
      public void handle(PluginEvent event) { cntII.incrementAndGet(); }
      public boolean accept(PluginEvent event) { return "type_II".equals(event.getType()); }
    };
    
    PluginSubsystemEventManager eventManager = new PluginSubsystemEventManager();
      eventManager.registerListener(listenerI);
      eventManager.registerListener(listenerII);
    
    Set<PluginEvent> allEvents = new HashSet<>();
    
    int numOfTypeIEvents = anyInt();
    for(int i = 0; i < numOfTypeIEvents; i++)
      allEvents.add(PluginEvent.of("type_I"));
    
    int numOfTypeIIEvents = anyInt();
    for(int i = 0; i < numOfTypeIIEvents; i++)
      allEvents.add(PluginEvent.of("type_II"));
    
    allEvents.forEach(e -> eventManager.sendAsync(e));

    while(eventManager.enqueuedEvents() != 0) {
      Thread.sleep(500);
    }
    
    Assertions.assertTrue(cntI.get() + cntII.get() == numOfTypeIEvents + numOfTypeIIEvents);
  }
  
  @Test
  void sendSync() throws InterruptedException {
    AtomicInteger cntI = new AtomicInteger(0);
    AtomicInteger cntII = new AtomicInteger(0);
    
    PluginEventListener listenerI = new PluginEventListener() {
      public void handle(PluginEvent event) { cntI.incrementAndGet(); }
      public boolean accept(PluginEvent event) { return "type_I".equals(event.getType()); }
    };
    
    PluginEventListener listenerII = new PluginEventListener() {
      public void handle(PluginEvent event) { cntII.incrementAndGet(); }
      public boolean accept(PluginEvent event) { return "type_II".equals(event.getType()); }
    };
    
    PluginEventing eventManager = new PluginSubsystemEventManager();
      eventManager.registerListener(listenerI);
      eventManager.registerListener(listenerII);
    
    Set<PluginEvent> allEvents = new HashSet<>();
    
    int numOfTypeIEvents = anyInt();
    for(int i = 0; i < numOfTypeIEvents; i++)
      allEvents.add(PluginEvent.of("type_I"));
    
    int numOfTypeIIEvents = anyInt();
    for(int i = 0; i < numOfTypeIIEvents; i++)
      allEvents.add(PluginEvent.of("type_II"));
    
    allEvents.forEach(e -> eventManager.sendSync(e));
    
    Assertions.assertTrue((cntI.get() + cntII.get()) == (numOfTypeIEvents + numOfTypeIIEvents));
  }
  private int anyInt() {
    Random random = new Random(System.currentTimeMillis());
    return random.nextInt(1, 512);
  }
}
