package org.clinica.booking.schedule.repository;

import org.clinica.booking.schedule.entity.BlockSource;
import org.clinica.booking.schedule.entity.BlockedDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BlockedDateRepository extends JpaRepository<BlockedDate, Long> {

    @Query("""
            SELECT b FROM BlockedDate b
            WHERE b.startDate <= :windowEnd AND b.endDate >= :windowStart
            ORDER BY b.startDate ASC
            """)
    List<BlockedDate> findOverlapping(@Param("windowStart") LocalDate windowStart,
                                      @Param("windowEnd") LocalDate windowEnd);

    List<BlockedDate> findBySourceAndStartDateBetween(BlockSource source, LocalDate from, LocalDate to);
}
