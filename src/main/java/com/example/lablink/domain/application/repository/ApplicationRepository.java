package com.example.lablink.domain.application.repository;

import com.example.lablink.domain.application.entity.Application;
import com.example.lablink.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application,Long> {

    List<Application> findByStudyId(Long studyId);

    @Query("SELECT a FROM Application a JOIN FETCH a.user u JOIN FETCH u.userinfo WHERE a.studyId = :studyId")
    List<Application> findByStudyIdWithUserAndUserInfo(@Param("studyId") Long studyId);

    boolean existsByStudyIdAndUser(Long studyId, User user);

    Optional<Application> findByIdAndStudyId(Long ApplicationId,Long StudyId);
//    List<Application> findByUser(User user); // 신청서 조회 JPA
}
