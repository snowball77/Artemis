package de.tum.in.www1.artemis.repository;

import de.tum.in.www1.artemis.domain.leaderboard.tutor.TutorExamLeaderboardComplaintResponsesView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TutorExamLeaderboardComplaintResponsesViewRepository extends JpaRepository<TutorExamLeaderboardComplaintResponsesView, Long> {

    List<TutorExamLeaderboardComplaintResponsesView> findAllByExamId(long examId);

    List<TutorExamLeaderboardComplaintResponsesView> findAllByLeaderboardId_ExerciseId(long exerciseId);
}
