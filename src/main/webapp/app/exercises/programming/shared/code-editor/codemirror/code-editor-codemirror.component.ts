import { AfterViewInit, Component, Input, Output, EventEmitter } from '@angular/core';
import 'codemirror/mode/markdown/markdown';
import 'codemirror/mode/swift/swift';
import 'codemirror/mode/clike/clike';
import 'codemirror/mode/python/python';
import 'codemirror/addon/edit/closebrackets';
import 'codemirror/addon/edit/matchbrackets';
import 'codemirror/addon/edit/closetag';
import * as codeMirror from 'codemirror';
import { CodemirrorComponent } from '@ctrl/ngx-codemirror/codemirror.component';
import { Subscription, of, fromEvent } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { CodeEditorRepositoryFileService } from 'app/exercises/programming/shared/code-editor/service/code-editor-repository.service';
import { TextChange } from 'app/entities/text-change.model';
import { AnnotationArray } from 'app/entities/annotation.model';

@Component({
    selector: 'jhi-code-editor-codemirror',
    templateUrl: './code-editor-codemirror.component.html',
    styleUrls: ['./code-editor-codemirror.scss'],
})
export class CodeEditorCodemirrorComponent implements AfterViewInit {
    public content: string;

    ngAfterViewInit(): void {
        var editor = codeMirror.fromTextArea(document.getElementById('editor-textarea') as HTMLTextAreaElement, {
            lineNumbers: true,
            mode: 'text/x-java',
            lineWrapping: true,
            matchBrackets: true,
            autoCloseBrackets: true,
        });
    }
}
