package pl.ug.virtualofficebackend.domain.user.internal;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pl.ug.virtualofficebackend.domain.user.entity.User;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {
}