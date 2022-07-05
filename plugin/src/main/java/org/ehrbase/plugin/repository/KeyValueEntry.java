package org.ehrbase.plugin.repository;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "plugin")
public class KeyValueEntry {
  @Column(name = "pluginid")
  private UUID pluginId;
  @Column(name = "key")
  private String key;
  @Column(name = "value")
  private String value;
  
  @Id
  @GeneratedValue(strategy=GenerationType.AUTO)
  private UUID id;
  public UUID getPluginId() {
    return pluginId;
  }
  public void setPluginId(UUID pluginId) {
    this.pluginId = pluginId;
  }
  public String getKey() {
    return key;
  }
  public void setKey(String key) {
    this.key = key;
  }
  public String getValue() {
    return value;
  }
  public void setValue(String value) {
    this.value = value;
  }
}
