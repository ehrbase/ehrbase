package org.ehrbase.api.plugin;

public interface PluginEventing {

  void registerListener(PluginEventListener listener);

  void sendAsync(PluginEvent event);

  void sendSync(PluginEvent event);

}