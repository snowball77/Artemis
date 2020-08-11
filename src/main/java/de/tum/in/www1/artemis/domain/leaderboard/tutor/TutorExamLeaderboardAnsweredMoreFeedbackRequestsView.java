package de.tum.in.www1.artemis.domain.leaderboard.tutor;

import org.hibernate.annotations.Immutable;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "view_tutor_exam_leaderboard_answered_more_feedback_requests")
@Immutable
public class TutorExamLeaderboardAnsweredMoreFeedbackRequestsView {

    @EmbeddedId
    private LeaderboardId leaderboardId;

    @Column(name = "answered_requests")
    private long answeredRequests;

    @Column(name = "points")
    private Long points;

    @Column(name = "exam_id")
    private long examId;

    @Column(name = "first_name")
    private String userFirstName;

    public long getAnsweredRequests() {
        return answeredRequests;
    }

    public long getPoints() {
        return points;
    }

    public long getUserId() {
        return leaderboardId.getUserId();
    }

    public long getExamId() {
        return examId;
    }

    public long getExerciseId() {
        return leaderboardId.getExerciseId();
    }

    public String getUserFirstName() {
        return userFirstName;
    }

    public TutorExamLeaderboardAnsweredMoreFeedbackRequestsView() {
    }

    public TutorExamLeaderboardAnsweredMoreFeedbackRequestsView(LeaderboardId leaderboardId, long answeredRequests, Long points, long examId, String userFirstName) {
        this.leaderboardId = leaderboardId;
        this.answeredRequests = answeredRequests;
        this.points = points;
        this.examId = examId;
        this.userFirstName = userFirstName;
    }
}
