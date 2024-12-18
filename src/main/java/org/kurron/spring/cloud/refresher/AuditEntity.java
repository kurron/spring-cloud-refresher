package org.kurron.spring.cloud.refresher;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

@Table(schema = "public", name = "audit")
record AuditEntity(@Id Long id,
                   @Column("from_where") String fromWhere,
                   @Column("when") OffsetDateTime when) {}
