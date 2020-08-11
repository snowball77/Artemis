package de.tum.in.www1.artemis.domain.leaderboard.tutor;

import org.hibernate.annotations.Immutable;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "view_tutor_exam_leaderboard_complaints")
@Immutable
public class TutorExamLeaderboardComplaintsView {

    @EmbeddedId
    private LeaderboardId leaderboardId;

    @Column(name = "all_complaints")
    private long allComplaints;

    @Column(name = "accepted_complaints")
    private long acceptedComplaints;

    @Column(name = "points")
    private Long points;

    @Column(name = "exam_id")
    private long examId;

    @Column(name = "first_name")
    private String userFirstName;

    public long getAllComplaints() {
        return allComplaints;
    }

    public long getAcceptedComplaints() {
        return acceptedComplaints;
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

    public TutorExamLeaderboardComplaintsView() {
    }

    public TutorExamLeaderboardComplaintsView(LeaderboardId leaderboardId, long allComplaints, long acceptedComplaints, Long points, long examId, String userFirstName) {
        this.leaderboardId = leaderboardId;
        this.allComplaints = allComplaints;
        this.acceptedComplaints = acceptedComplaints;
        this.points = points;
        this.examId = examId;
        this.userFirstName = userFirstName;
    }
}
