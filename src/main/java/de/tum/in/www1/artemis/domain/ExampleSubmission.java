package de.tum.in.www1.artemis.domain;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.tum.in.www1.artemis.domain.participation.TutorParticipation;

/**
 * A ExampleSubmission.
 */
@Entity
@Table(name = "example_submission")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class ExampleSubmission extends DomainObject {

    @Column(name = "used_for_tutorial")
    private Boolean usedForTutorial;

    @ManyToOne
    private Exercise exercise;

    @OneToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(unique = true)
    private Submission submission;

    @ManyToMany(mappedBy = "trainedExampleSubmissions")
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    @JsonIgnoreProperties({ "trainedExampleSubmissions", "assessedExercise" })
    private Set<TutorParticipation> tutorParticipations = new HashSet<>();

    @Column(name = "assessment_explanation")
    private String assessmentExplanation;

    public Boolean isUsedForTutorial() {
        return usedForTutorial;
    }

    public void setUsedForTutorial(Boolean usedForTutorial) {
        this.usedForTutorial = usedForTutorial;
    }

    public Exercise getExercise() {
        return exercise;
    }

    public void setExercise(Exercise exercise) {
        this.exercise = exercise;
    }

    public Submission getSubmission() {
        return submission;
    }

    public void setSubmission(Submission submission) {
        this.submission = submission;
    }

    public Set<TutorParticipation> getTutorParticipations() {
        return tutorParticipations;
    }

    public ExampleSubmission addTutorParticipations(TutorParticipation tutorParticipation) {
        this.tutorParticipations.add(tutorParticipation);
        tutorParticipation.getTrainedExampleSubmissions().add(this);
        return this;
    }

    public ExampleSubmission removeTutorParticipations(TutorParticipation tutorParticipation) {
        this.tutorParticipations.remove(tutorParticipation);
        tutorParticipation.getTrainedExampleSubmissions().remove(this);
        return this;
    }

    public void setTutorParticipations(Set<TutorParticipation> tutorParticipations) {
        this.tutorParticipations = tutorParticipations;
    }

    public String getAssessmentExplanation() {
        return assessmentExplanation;
    }

    public ExampleSubmission assessmentExplanation(String assessmentExplanation) {
        this.assessmentExplanation = assessmentExplanation;
        return this;
    }

    public void setAssessmentExplanation(String assessmentExplanation) {
        this.assessmentExplanation = assessmentExplanation;
    }

    @Override
    public String toString() {
        return "ExampleSubmission{" + "id=" + getId() + ", usedForTutorial='" + isUsedForTutorial() + "'" + "}";
    }
}
