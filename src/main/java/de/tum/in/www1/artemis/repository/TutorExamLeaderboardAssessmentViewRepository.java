package de.tum.in.www1.artemis.repository;

import de.tum.in.www1.artemis.domain.leaderboard.tutor.TutorExamLeaderboardAssessmentView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TutorExamLeaderboardAssessmentViewRepository extends JpaRepository<TutorExamLeaderboardAssessmentView, Long> {

    List<TutorExamLeaderboardAssessmentView> findAllByExamId(long examId);

    List<TutorExamLeaderboardAssessmentView> findAllByLeaderboardId_ExerciseId(long exerciseId);
}
