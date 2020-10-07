import { BaseEntity } from 'app/shared/model/base-entity';
import { ShortAnswerQuestion } from 'app/entities/quiz/short-answer-question.model';
import { generate } from 'app/exercises/quiz/manage/temp-id';
import { CanBecomeInvalid } from 'app/entities/quiz/drop-location.model';

export class ShortAnswerSpot implements BaseEntity, CanBecomeInvalid {
    public id?: number;
    public width?: number;
    public spotNr?: number;
    public invalid?: boolean;
    public question?: ShortAnswerQuestion;
    public tempID?: number;

    // additionally added after database changes with Stephan
    public posX?: number;
    public posY?: number;

    constructor() {
        this.tempID = generate();
        this.invalid = false; // default
    }
}
