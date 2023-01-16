package com.brokencircuits.kissad.ui.repository;

import com.brokencircuits.kissad.table.ReadWriteTable;
import com.brokencircuits.kissad.util.KeyValue;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;

@RequiredArgsConstructor
public class RepositoryBasedTable<K, V, T, O> implements ReadWriteTable<K, V> {

  private final JpaRepository<T, O> repository;
  private final Function<KeyValue<K, V>, T> convertToEntity;
  private final Function<T, KeyValue<K, V>> convertToKeyValue;
  private final Function<K, O> convertKey;

  @Override
  public V get(K key) {
    return repository.findById(convertKey.apply(key))
        .map(convertToKeyValue)
        .map(KeyValue::getValue)
        .orElse(null);
  }

  @Override
  public void all(Consumer<KeyValue<K, V>> consumer) {
    repository.findAll().stream()
        .map(convertToKeyValue)
        .forEach(consumer);
  }

  @Override
  public void put(K key, V value) {
    if (value == null) {
      repository.deleteById(convertKey.apply(key));
    } else {
      repository.save(convertToEntity.apply(KeyValue.of(key, value)));
    }
  }
}
