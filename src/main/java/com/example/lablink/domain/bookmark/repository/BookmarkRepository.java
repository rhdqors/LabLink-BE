package com.example.lablink.domain.bookmark.repository;

import com.example.lablink.domain.bookmark.entity.Bookmark;
import com.example.lablink.domain.company.entity.Company;
import com.example.lablink.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    void deleteByStudyIdAndUser(Long studyId, User user);
    void deleteByStudyId(Long studyId);
    boolean existsByStudyIdAndUser(Long studyId, User user);

    List<Bookmark> findAllByUser(User user);

    void deleteByStudyIdAndCompany(Long studyId, Company company);

    List<Bookmark> findAllByCompany(Company company);

    boolean existsByStudyIdAndCompany(Long studyId, Company company);

    @Query("SELECT b.studyId FROM Bookmark b WHERE b.user = :user AND b.studyId IN :studyIds")
    List<Long> findStudyIdsByUserAndStudyIdIn(@Param("user") User user, @Param("studyIds") List<Long> studyIds);

    @Query("SELECT b.studyId FROM Bookmark b WHERE b.company = :company AND b.studyId IN :studyIds")
    List<Long> findStudyIdsByCompanyAndStudyIdIn(@Param("company") Company company, @Param("studyIds") List<Long> studyIds);
}
