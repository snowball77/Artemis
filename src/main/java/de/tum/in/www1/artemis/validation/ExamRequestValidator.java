package de.tum.in.www1.artemis.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import de.tum.in.www1.artemis.validation.constraints.ExamRequestConstraint;
import de.tum.in.www1.artemis.web.rest.dto.request.ExamRequestDTO;

public class ExamRequestValidator implements ConstraintValidator<ExamRequestConstraint, ExamRequestDTO> {

    @Override
    public boolean isValid(ExamRequestDTO examRequestDTO, ConstraintValidatorContext context) {
        if (examRequestDTO.visibleDate != null && examRequestDTO.startDate != null && examRequestDTO.endDate != null) {
            return examRequestDTO.visibleDate.isBefore(examRequestDTO.startDate) && examRequestDTO.startDate.isBefore(examRequestDTO.endDate);
        }
        else {
            return true;
        }
    }

}
