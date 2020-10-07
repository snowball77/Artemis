package de.tum.in.www1.artemis.domain.quiz;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * A ShortAnswerQuestionStatistic.
 */
@Entity
@DiscriminatorValue(value = "SA")
@JsonTypeName("short-answer")
public class ShortAnswerQuestionStatistic extends QuizQuestionStatistic {

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "shortAnswerQuestionStatistic")
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private Set<ShortAnswerSpotCounter> shortAnswerSpotCounters = new HashSet<>();

    public Set<ShortAnswerSpotCounter> getShortAnswerSpotCounters() {
        return shortAnswerSpotCounters;
    }

    public ShortAnswerQuestionStatistic addShortAnswerSpotCounters(ShortAnswerSpotCounter shortAnswerSpotCounter) {
        this.shortAnswerSpotCounters.add(shortAnswerSpotCounter);
        shortAnswerSpotCounter.setShortAnswerQuestionStatistic(this);
        return this;
    }

    public ShortAnswerQuestionStatistic removeShortAnswerSpotCounters(ShortAnswerSpotCounter shortAnswerSpotCounter) {
        this.shortAnswerSpotCounters.remove(shortAnswerSpotCounter);
        shortAnswerSpotCounter.setShortAnswerQuestionStatistic(null);
        return this;
    }

    public void setShortAnswerSpotCounters(Set<ShortAnswerSpotCounter> shortAnswerSpotCounters) {
        this.shortAnswerSpotCounters = shortAnswerSpotCounters;
    }

    @Override
    public String toString() {
        return "ShortAnswerQuestionStatistic{" + "id=" + getId() + "}";
    }

    /**
     * 1. creates the ShortAnswerSpotCounter for the new spot if where is already an ShortAnswerSpotCounter with the given spot -> nothing happens
     *
     * @param spot the spot-object which will be added to the ShortAnswerQuestionStatistic
     */
    public void addSpot(ShortAnswerSpot spot) {
        if (spot == null) {
            return;
        }

        for (ShortAnswerSpotCounter counter : shortAnswerSpotCounters) {
            if (spot.equals(counter.getSpot())) {
                return;
            }
        }
        ShortAnswerSpotCounter spotCounter = new ShortAnswerSpotCounter();
        spotCounter.setSpot(spot);
        addShortAnswerSpotCounters(spotCounter);
    }

    @Override
    public void addResult(SubmittedAnswer submittedAnswer, boolean rated) {
        changeStatisticBasedOnResult(submittedAnswer, rated, 1);
    }

    @Override
    public void removeOldResult(SubmittedAnswer submittedAnswer, boolean rated) {
        changeStatisticBasedOnResult(submittedAnswer, rated, -1);
    }

    @Override
    public void resetStatistic() {
        setParticipantsRated(0);
        setParticipantsUnrated(0);
        setRatedCorrectCounter(0);
        setUnRatedCorrectCounter(0);
        for (ShortAnswerSpotCounter spotCounter : shortAnswerSpotCounters) {
            spotCounter.setRatedCounter(0);
            spotCounter.setUnRatedCounter(0);
        }
    }

    /**
     * 1. check if the Result is rated or unrated 2. change participants, all the ShortAnswerSpotCounter if the ShortAnswerAssignment is correct and if the complete question is
     * correct, than change the correctCounter
     *
     * @param submittedAnswer the submittedAnswer object which contains all submittedTexts
     * @param rated           specify if the Result was rated ( participated during the releaseDate and the dueDate of the quizExercise) or unrated ( participated after the dueDate
     *                        of the quizExercise)
     * @param change          the int-value, which will be added to the Counter and participants
     */
    private void changeStatisticBasedOnResult(SubmittedAnswer submittedAnswer, boolean rated, int change) {
        if (submittedAnswer == null) {
            return;
        }

        ShortAnswerSubmittedAnswer shortAnswerSubmittedAnswer = (ShortAnswerSubmittedAnswer) submittedAnswer;

        if (rated) {
            // change the rated participants
            setParticipantsRated(getParticipantsRated() + change);

            if (shortAnswerSubmittedAnswer.getSubmittedTexts() != null) {
                ShortAnswerSubmittedText shortAnswerSubmittedText;
                Set<ShortAnswerSolution> shortAnswerSolutions;

                // change rated spotCounter if spot is correct
                for (ShortAnswerSpotCounter spotCounter : shortAnswerSpotCounters) {
                    shortAnswerSubmittedText = shortAnswerSubmittedAnswer.getSubmittedTextForSpot(spotCounter.getSpot());
                    shortAnswerSolutions = spotCounter.getSpot().getQuestion().getCorrectSolutionForSpot(spotCounter.getSpot());

                    // TODO Francisco: please double check if this makes sense: it definitely avoids a null pointer exception because the method getSubmittedTextForSpot(...) above
                    // can return null
                    if (shortAnswerSubmittedText == null) {
                        continue;
                    }
                    for (ShortAnswerSolution solution : shortAnswerSolutions) {
                        if (shortAnswerSubmittedText.isSubmittedTextCorrect(shortAnswerSubmittedText.getText(), solution.getText())
                                && Boolean.TRUE.equals(shortAnswerSubmittedText.isIsCorrect())) {
                            spotCounter.setRatedCounter(spotCounter.getRatedCounter() + change);
                        }
                    }
                }
            }
            // change rated correctCounter if answer is complete correct
            if (getQuizQuestion().isAnswerCorrect(shortAnswerSubmittedAnswer)) {
                setRatedCorrectCounter(getRatedCorrectCounter() + change);
            }
        }
        // Result is unrated
        else {
            // change the unrated participants
            setParticipantsUnrated(getParticipantsUnrated() + change);

            if (shortAnswerSubmittedAnswer.getSubmittedTexts() != null) {
                ShortAnswerSubmittedText shortAnswerSubmittedText;
                Set<ShortAnswerSolution> shortAnswerSolutions;
                // change unrated spotCounter if spot is correct
                for (ShortAnswerSpotCounter spotCounter : shortAnswerSpotCounters) {
                    shortAnswerSubmittedText = shortAnswerSubmittedAnswer.getSubmittedTextForSpot(spotCounter.getSpot());
                    shortAnswerSolutions = spotCounter.getSpot().getQuestion().getCorrectSolutionForSpot(spotCounter.getSpot());

                    if (shortAnswerSubmittedText == null) {
                        continue;
                    }
                    for (ShortAnswerSolution solution : shortAnswerSolutions) {
                        if (shortAnswerSubmittedText.isSubmittedTextCorrect(shortAnswerSubmittedText.getText(), solution.getText())
                                && Boolean.TRUE.equals(shortAnswerSubmittedText.isIsCorrect())) {
                            spotCounter.setUnRatedCounter(spotCounter.getUnRatedCounter() + change);
                        }
                    }
                }
            }
            // change unrated correctCounter if answer is complete correct
            if (getQuizQuestion().isAnswerCorrect(shortAnswerSubmittedAnswer)) {
                setUnRatedCorrectCounter(getUnRatedCorrectCounter() + change);
            }
        }
    }
}
