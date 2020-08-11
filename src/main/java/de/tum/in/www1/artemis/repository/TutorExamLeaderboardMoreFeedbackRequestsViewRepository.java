package de.tum.in.www1.artemis.repository;

import de.tum.in.www1.artemis.domain.leaderboard.tutor.TutorExamLeaderboardMoreFeedbackRequestsView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TutorExamLeaderboardMoreFeedbackRequestsViewRepository extends JpaRepository<TutorExamLeaderboardMoreFeedbackRequestsView, Long> {

    List<TutorExamLeaderboardMoreFeedbackRequestsView> findAllByExamId(long examId);

    List<TutorExamLeaderboardMoreFeedbackRequestsView> findAllByLeaderboardId_ExerciseId(long exerciseId);
}
