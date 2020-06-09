package de.tum.in.www1.artemis.web.rest.dto.request;

import java.time.ZonedDateTime;

import de.tum.in.www1.artemis.validation.constraints.ExamRequestConstraint;

@ExamRequestConstraint
public class ExamRequestDTO {

    public Long id;

    public String title;

    public ZonedDateTime visibleDate;

    public ZonedDateTime startDate;

    public ZonedDateTime endDate;

    public String startText;

    public String endText;

    public String confirmationStartText;

    public String confirmationEndText;

    public Integer maxPoints;

    public Boolean randomizeExerciseOrder;

    public Integer numberOfExercisesInExam;
}
