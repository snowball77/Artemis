import * as chai from 'chai';
import * as sinonChai from 'sinon-chai';
import { ArtemisMarkdownService } from 'app/shared/markdown.service';
import { MultipleChoiceQuestion } from 'app/entities/quiz/multiple-choice-question.model';
import { ShortAnswerQuestion } from 'app/entities/quiz/short-answer-question.model';

chai.use(sinonChai);
const expect = chai.expect;

describe('Markdown Service', () => {
    const hintText = 'Add an explanation here (only visible in feedback after quiz has ended)';
    const markdownHint = '[hint] ' + hintText;
    const explanationText = 'Add an explanation here (only visible in feedback after quiz has ended)';
    const markdownExplanation = '[exp] ' + explanationText;

    it('should return correct parsed text hint and explanation for MC questions', () => {
        let markdownElement = new MultipleChoiceQuestion();
        const markdownString = '[ ] Enter a correct answer option here';

        ArtemisMarkdownService.parseTextHintExplanation(markdownString, markdownElement);
        expect(markdownElement.text).to.equal(markdownString);
        expect(markdownElement.hint).to.be.undefined;
        expect(markdownElement.explanation).to.be.undefined;

        markdownElement = new MultipleChoiceQuestion();
        ArtemisMarkdownService.parseTextHintExplanation(`${markdownHint}`, markdownElement);
        expect(markdownElement.text).to.equal('');
        expect(markdownElement.hint).to.equal(hintText);
        expect(markdownElement.explanation).to.be.undefined;

        markdownElement = new MultipleChoiceQuestion();
        ArtemisMarkdownService.parseTextHintExplanation(`${markdownExplanation}`, markdownElement);
        expect(markdownElement.text).to.equal('');
        expect(markdownElement.hint).to.be.undefined;
        expect(markdownElement.explanation).to.equal(explanationText);

        markdownElement = new MultipleChoiceQuestion();
        ArtemisMarkdownService.parseTextHintExplanation(`${markdownString} ${markdownHint} ${markdownExplanation}`, markdownElement);
        expect(markdownElement.text).to.equal(markdownString);
        expect(markdownElement.hint).to.equal(hintText);
        expect(markdownElement.explanation).to.equal(explanationText);
    });

    it('should return correct parsed text hint and explanation for SA questions', () => {
        let markdownElement = new ShortAnswerQuestion();
        const markdownString =
            'Enter your long question if needed\n' +
            'Select a part of the text and click on Add Spot to automatically create an input field and the corresponding mapping\n' +
            'You can define a input field like this: This [-spot 1] an [-spot 2] field.\n' +
            'To define the solution for the input fields you need to create a mapping (multiple mapping also possible):';

        ArtemisMarkdownService.parseTextHintExplanation(markdownString, markdownElement);
        expect(markdownElement.text).to.equal(markdownString);
        expect(markdownElement.hint).to.be.undefined;
        expect(markdownElement.explanation).to.be.undefined;

        markdownElement = new ShortAnswerQuestion();
        ArtemisMarkdownService.parseTextHintExplanation(`${markdownHint}`, markdownElement);
        expect(markdownElement.text).to.equal('');
        expect(markdownElement.hint).to.equal(hintText);
        expect(markdownElement.explanation).to.be.undefined;

        markdownElement = new ShortAnswerQuestion();
        ArtemisMarkdownService.parseTextHintExplanation(`${markdownExplanation}`, markdownElement);
        expect(markdownElement.text).to.equal('');
        expect(markdownElement.hint).to.be.undefined;
        expect(markdownElement.explanation).to.equal(explanationText);

        markdownElement = new ShortAnswerQuestion();
        ArtemisMarkdownService.parseTextHintExplanation(`${markdownString} ${markdownHint} ${markdownExplanation}`, markdownElement);
        expect(markdownElement.text).to.equal(markdownString);
        expect(markdownElement.hint).to.equal(hintText);
        expect(markdownElement.explanation).to.equal(explanationText);
    });
});
