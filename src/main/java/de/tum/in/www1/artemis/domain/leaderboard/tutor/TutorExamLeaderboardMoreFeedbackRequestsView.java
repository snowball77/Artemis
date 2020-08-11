package de.tum.in.www1.artemis.domain.leaderboard.tutor;

import org.hibernate.annotations.Immutable;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "view_tutor_exam_leaderboard_more_feedback_requests")
@Immutable
public class TutorExamLeaderboardMoreFeedbackRequestsView {

    @EmbeddedId
    private LeaderboardId leaderboardId;

    @Column(name = "all_requests")
    private long allRequests;

    @Column(name = "not_answered_requests")
    private long notAnsweredRequests;

    @Column(name = "points")
    private Long points;

    @Column(name = "exam_id")
    private long examId;

    @Column(name = "first_name")
    private String userFirstName;

    public long getAllRequests() {
        return allRequests;
    }

    public long getNotAnsweredRequests() {
        return notAnsweredRequests;
    }

    public Long getPoints() {
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

    public TutorExamLeaderboardMoreFeedbackRequestsView() {
    }

    public TutorExamLeaderboardMoreFeedbackRequestsView(LeaderboardId leaderboardId, long allRequests, long notAnsweredRequests, Long points, long examId, String userFirstName) {
        this.leaderboardId = leaderboardId;
        this.allRequests = allRequests;
        this.notAnsweredRequests = notAnsweredRequests;
        this.points = points;
        this.examId = examId;
        this.userFirstName = userFirstName;
    }
}
