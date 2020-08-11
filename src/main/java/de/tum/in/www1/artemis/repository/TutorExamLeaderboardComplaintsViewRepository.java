package de.tum.in.www1.artemis.repository;

import de.tum.in.www1.artemis.domain.leaderboard.tutor.TutorExamLeaderboardComplaintsView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TutorExamLeaderboardComplaintsViewRepository extends JpaRepository<TutorExamLeaderboardComplaintsView, Long> {

    List<TutorExamLeaderboardComplaintsView> findAllByExamId(long examId);

    List<TutorExamLeaderboardComplaintsView> findAllByLeaderboardId_ExerciseId(long exerciseId);
}
