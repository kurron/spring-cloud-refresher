package org.kurron.spring.cloud.refresher;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditRepository extends ListCrudRepository<AuditEntity,Long> {}
