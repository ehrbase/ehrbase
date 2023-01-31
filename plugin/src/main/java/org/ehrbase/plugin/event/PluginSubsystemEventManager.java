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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;

import org.ehrbase.api.plugin.PluginEvent;
import org.ehrbase.api.plugin.PluginEventListener;
import org.ehrbase.api.plugin.PluginEventing;


public class PluginSubsystemEventManager implements PluginEventing {
  private static final int NUM_OF_PROC = Runtime.getRuntime().availableProcessors();

  private final ExecutorService execService = Executors.newFixedThreadPool(Math.max(2, NUM_OF_PROC));
  private final LinkedBlockingQueue<PluginEvent> eventQueue = new LinkedBlockingQueue<>();
  
  private final List<PluginEventListener> allListener = new ArrayList<>();
  
  private volatile boolean shutdown = false;
  
  private final ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
  private final BiConsumer<Runnable,Lock> lockedConsumer = (runMe, lock) -> {
    lock.lock();
    try {
      runMe.run();
    } finally {
      lock.unlock();
    }
  };
  
  public PluginSubsystemEventManager() {
    execService.submit(new MainEventLoop());
  }
  
  private class MainEventLoop implements Runnable {
    public void run() {
      while(!PluginSubsystemEventManager.this.shutdown) {
        try {
          PluginEvent event = PluginSubsystemEventManager.this.eventQueue.take();
          lockedConsumer.accept(() -> {
            PluginSubsystemEventManager.this.allListener.forEach(listener -> {
              if(listener.accept(event))
                execService.submit(() -> listener.handle(event));
            });
          }, rw.readLock());
        } catch(InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }
  
  public int enqueuedEvents() {
    return eventQueue.size();
  }
  
  public synchronized void registerListener(PluginEventListener listener) {
    lockedConsumer.accept(() -> allListener.add(listener), rw.writeLock());
  } 
  
  public void sendAsync(PluginEvent event) {
    eventQueue.add(event);
  }
  
  public void sendSync(PluginEvent event) {
    lockedConsumer.accept(() -> {
        allListener.forEach(listener -> {
            if(listener.accept(event))
              listener.handle(event);
        });
      }, rw.readLock());
  }
}
