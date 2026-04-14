package com.parkease.analytics.repository;

import com.parkease.analytics.entity.OccupancyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OccupancyLogRepository extends JpaRepository<OccupancyLog, Long> {

    List<OccupancyLog> findByLotIdAndTimestampBetweenOrderByTimestampAsc(
            Long lotId, LocalDateTime from, LocalDateTime to);

    OccupancyLog findTopByLotIdOrderByTimestampDesc(Long lotId);

    @Query("SELECT AVG(o.occupancyRate) FROM OccupancyLog o WHERE o.lotId = :lotId")
    Double avgOccupancyByLotId(@Param("lotId") Long lotId);

    @Query("SELECT o.hourOfDay, AVG(o.occupancyRate) FROM OccupancyLog o " +
           "WHERE o.lotId = :lotId " +
           "GROUP BY o.hourOfDay ORDER BY o.hourOfDay")
    List<Object[]> getHourlyOccupancy(@Param("lotId") Long lotId);

    @Query("SELECT o.hourOfDay, AVG(o.occupancyRate) as avgRate FROM OccupancyLog o " +
           "WHERE o.lotId = :lotId " +
           "GROUP BY o.hourOfDay ORDER BY avgRate DESC")
    List<Object[]> getPeakHours(@Param("lotId") Long lotId);

    @Query("SELECT COUNT(o) FROM OccupancyLog o " +
           "WHERE o.lotId = :lotId AND o.timestamp >= :startOfDay")
    int countTodayLogs(@Param("lotId") Long lotId,
                       @Param("startOfDay") LocalDateTime startOfDay);

    @Query("SELECT o.lotId, o.occupancyRate, o.occupiedSpots, o.totalSpots " +
           "FROM OccupancyLog o WHERE o.timestamp = " +
           "(SELECT MAX(o2.timestamp) FROM OccupancyLog o2 WHERE o2.lotId = o.lotId)")
    List<Object[]> getLatestOccupancyAllLots();
}
