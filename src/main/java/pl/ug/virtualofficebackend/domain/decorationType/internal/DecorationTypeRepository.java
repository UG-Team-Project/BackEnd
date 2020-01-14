package pl.ug.virtualofficebackend.domain.decorationType.internal;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pl.ug.virtualofficebackend.domain.decorationType.entity.DecorationType;

@Repository
public interface DecorationTypeRepository extends CrudRepository<DecorationType, Long> {
}