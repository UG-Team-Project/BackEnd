package pl.ug.virtualofficebackend.domain.item.internal;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pl.ug.virtualofficebackend.domain.item.entity.Item;

@Repository
public interface ItemRepository extends CrudRepository<Item, Long> {
}