package com.NetflixClone.dao;

import com.NetflixClone.entity.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VideoRepository extends JpaRepository<Video,Long> {

    Optional<Video> findByTitle(String title);

    @Query("SELECT v FROM Video v WHERE " +
            "LOWER(v.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(v.description) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Video> searchVideos(@Param("search") String search, Pageable pageable);

    Page<Video> findByPublishedTrue(Pageable pageable);

    @Query("SELECT v FROM Video v WHERE v.published = true AND (" +
            "LOWER(v.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(v.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Video> searchPublishedVideos(@Param("search") String search, Pageable pageable);

    @Query(value = "SELECT * FROM videos WHERE published = true ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<Video> findRandomPublishedVideos(@Param("limit") int limit);

    long countByPublishedTrue();

    @Query("SELECT COALESCE(SUM(v.duration), 0) FROM Video v")
    long getTotalDuration();
}
