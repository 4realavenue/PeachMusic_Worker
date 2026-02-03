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

    public List<Long> findSongIdListByJobStatus(JobStatus jobStatus, int limit) {
        return jdbcTemplate.queryForList("""
                select song_id from streaming_jobs
                where job_status = ?
                ORDER BY song_id
                LIMIT ?
                """, Long.class, jobStatus.name(), limit);
    }

    public boolean claimStatus(Long songId, JobStatus from, JobStatus to) {
        int updated = jdbcTemplate.update("""
                    UPDATE streaming_jobs
                    SET job_status = ?
                    WHERE song_id = ?
                      AND job_status = ?
                """, to.name(), songId, from.name());

        return updated == 1; // true면 내가 가져간 거
    }

    public void updateStatus(Long songId, JobStatus status) {
        jdbcTemplate.update("""
                    UPDATE streaming_jobs
                    SET job_status = ?
                    WHERE song_id = ?
                """, status.name(), songId);
    }


}
