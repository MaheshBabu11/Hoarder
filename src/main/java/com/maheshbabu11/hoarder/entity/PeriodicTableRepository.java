package com.maheshbabu11.hoarder.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PeriodicTableRepository extends CrudRepository<PeriodicTable, Integer> {
}
