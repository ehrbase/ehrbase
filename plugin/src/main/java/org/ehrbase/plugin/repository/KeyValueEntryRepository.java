package org.ehrbase.plugin.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

public interface KeyValueEntryRepository extends CrudRepository<KeyValueEntry, UUID> {
  public List<KeyValueEntry> findByPluginId(UUID uid);
  public Optional<KeyValueEntry> findByKey(String key);
}
