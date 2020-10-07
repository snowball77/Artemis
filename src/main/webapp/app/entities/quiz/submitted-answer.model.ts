import { BaseEntity } from 'app/shared/model/base-entity';
import { QuizSubmission } from 'app/entities/quiz/quiz-submission.model';
import { QuizQuestion, QuizQuestionType } from 'app/entities/quiz/quiz-question.model';

export abstract class SubmittedAnswer implements BaseEntity {
    public id?: number;
    public scoreInPoints?: number;
    public quizQuestion?: QuizQuestion;
    public submission?: QuizSubmission;

    public type?: QuizQuestionType;

    protected constructor(type: QuizQuestionType) {
        this.type = type;
    }
}
