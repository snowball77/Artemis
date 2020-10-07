export class ExamScoreDTO {
    constructor(
        public examId: number,
        public title: string,
        public maxPoints: number,
        public averagePointsAchieved: number,
        public exerciseGroups: ExerciseGroup[],
        public studentResults: StudentResult[],
    ) {}
}

export class ExerciseGroup {
    constructor(public id: number, public title: string, public maxPoints: number, public numberOfParticipants: number, public containedExercises: ExerciseInfo[]) {}
}

export class ExerciseInfo {
    constructor(public exerciseId: number, public title: string, public maxPoints: number, public numberOfParticipants: number) {}
}

export class StudentResult {
    constructor(
        public userId: number,
        public name: string,
        public login: string,
        public eMail: string,
        public registrationNumber: string,
        public overallPointsAchieved: number,
        public overallScoreAchieved: number,
        public submitted: boolean,
        public exerciseGroupIdToExerciseResult: { [key: number]: ExerciseResult },
    ) {}
}

export class ExerciseResult {
    constructor(
        public exerciseId: number,
        public title: string,
        public maxScore: number,
        public achievedScore: number,
        public achievedPoints: number,
        public hasNonEmptySubmission: boolean,
    ) {}
}

export class AggregatedExamResult {
    public meanPoints: number;
    public meanPointsRelative: number;
    public meanPointsTotal: number;
    public meanPointsRelativeTotal: number;
    public median: number;
    public medianRelative: number;
    public medianTotal: number;
    public medianRelativeTotal: number;
    public standardDeviation: number;
    public standardDeviationTotal: number;
    public noOfExamsFiltered = 0;
    public noOfRegisteredUsers = 0;

    constructor() {}
}

export class AggregatedExerciseGroupResult {
    public exerciseGroupId: number;
    public title: string;
    public maxPoints: number;
    public totalParticipants: number;
    public noOfParticipantsWithFilter = 0;
    public totalPoints = 0;
    public averagePoints?: number;
    public averagePercentage?: number;
    public exerciseResults: AggregatedExerciseResult[] = [];

    constructor(exerciseGroupId: number, title: string, maxPoints: number, totalParticipants: number) {
        this.exerciseGroupId = exerciseGroupId;
        this.title = title;
        this.maxPoints = maxPoints;
        this.totalParticipants = totalParticipants;
    }
}

export class AggregatedExerciseResult {
    public exerciseId: number;
    public title: string;
    public maxPoints: number;
    public totalParticipants: number;
    public noOfParticipantsWithFilter = 0;
    public totalPoints = 0;
    public averagePoints?: number;
    public averagePercentage?: number;

    constructor(exerciseId: number, title: string, maxPoints: number, totalParticipants: number) {
        this.exerciseId = exerciseId;
        this.title = title;
        this.maxPoints = maxPoints;
        this.totalParticipants = totalParticipants;
    }
}
