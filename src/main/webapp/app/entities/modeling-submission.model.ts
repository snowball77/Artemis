import { Submission, SubmissionExerciseType } from 'app/entities/submission.model';

export class ModelingSubmission extends Submission {
    public model?: string;
    public explanationText?: string;
    public optimal?: boolean; // used by compass to determine whether a submission leads to the most learning possible

    constructor() {
        super(SubmissionExerciseType.MODELING);
    }
}
