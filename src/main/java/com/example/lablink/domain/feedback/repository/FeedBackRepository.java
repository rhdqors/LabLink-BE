package com.example.lablink.domain.feedback.repository;

import com.example.lablink.domain.feedback.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedBackRepository extends JpaRepository<Feedback,Long> {

    List<Feedback> findAllByStudyId(Long id);

    @Query("SELECT f FROM Feedback f JOIN FETCH f.user u JOIN FETCH u.userinfo WHERE f.study.id = :studyId")
    List<Feedback> findAllByStudyIdWithUser(@Param("studyId") Long studyId);
}
