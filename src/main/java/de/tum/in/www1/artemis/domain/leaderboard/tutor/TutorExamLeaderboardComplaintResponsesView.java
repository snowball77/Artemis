package de.tum.in.www1.artemis.domain.leaderboard.tutor;

import org.hibernate.annotations.Immutable;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "view_tutor_exam_leaderboard_complaint_responses")
@Immutable
public class TutorExamLeaderboardComplaintResponsesView {

    @EmbeddedId
    private LeaderboardId leaderboardId;

    @Column(name = "complaint_responses")
    private long complaintResponses;

    @Column(name = "points")
    private Long points;

    @Column(name = "exam_id")
    private long examId;

    @Column(name = "first_name")
    private String userFirstName;

    public long getComplaintResponses() {
        return complaintResponses;
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

    public TutorExamLeaderboardComplaintResponsesView() {
    }

    public TutorExamLeaderboardComplaintResponsesView(LeaderboardId leaderboardId, long complaintResponses, Long points, long examId, String userFirstName) {
        this.leaderboardId = leaderboardId;
        this.complaintResponses = complaintResponses;
        this.points = points;
        this.examId = examId;
        this.userFirstName = userFirstName;
    }
}
