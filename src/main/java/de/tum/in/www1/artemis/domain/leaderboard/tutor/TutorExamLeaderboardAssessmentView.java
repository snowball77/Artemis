package de.tum.in.www1.artemis.domain.leaderboard.tutor;

import org.hibernate.annotations.Immutable;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "view_tutor_exam_leaderboard_assessments")
@Immutable
public class TutorExamLeaderboardAssessmentView {

    @EmbeddedId
    private LeaderboardId leaderboardId;

    @Column(name = "assessments")
    private long assessments;

    @Column(name = "points")
    private Long points;

    @Column(name = "exam_id")
    private long examId;

    @Column(name = "first_name")
    private String userFirstName;

    public long getAssessments() {
        return assessments;
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

    public TutorExamLeaderboardAssessmentView() {
    }

    public TutorExamLeaderboardAssessmentView(LeaderboardId leaderboardId, long assessments, Long points, long examId, String userFirstName) {
        this.leaderboardId = leaderboardId;
        this.assessments = assessments;
        this.points = points;
        this.examId = examId;
        this.userFirstName = userFirstName;
    }
}
