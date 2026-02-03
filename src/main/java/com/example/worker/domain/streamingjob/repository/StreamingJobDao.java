package com.example.worker.domain.streamingjob.repository;

import com.example.worker.common.enums.JobStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class StreamingJobDao {

    private final JdbcTemplate jdbcTemplate;

    public StreamingJobDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 작업 상태로 songId를 List로 받아오기
    public List<Long> findSongIdListByJobStatus(JobStatus jobStatus, int limit) {
        return jdbcTemplate.queryForList("""
                SELECT song_id 
                FROM streaming_jobs
                WHERE job_status = ?
                ORDER BY song_id
                LIMIT ?
                """, Long.class, jobStatus.name(), limit);
    }

    // 중복 작업 방지
    public boolean claimStatus(Long songId, JobStatus from, JobStatus to) {
        int updated = jdbcTemplate.update("""
                    UPDATE streaming_jobs
                    SET job_status = ?
                    WHERE song_id = ?
                      AND job_status = ?
                """, to.name(), songId, from.name());

        return updated == 1; // true면 내가 가져간 거
    }

    // 작업 상태 변경 (후 설정)
    public void updateStatus(Long songId, JobStatus status) {
        jdbcTemplate.update("""
                    UPDATE streaming_jobs
                    SET job_status = ?
                    WHERE song_id = ?
                """, status.name(), songId);
    }
}
