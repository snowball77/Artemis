import { AfterViewInit, Component, Input, Output, EventEmitter } from '@angular/core';
import 'codemirror/mode/markdown/markdown';
import 'codemirror/mode/swift/swift';
import 'codemirror/mode/clike/clike';
import 'codemirror/mode/python/python';
import 'codemirror/addon/edit/closebrackets';
import 'codemirror/addon/edit/matchbrackets';
import 'codemirror/addon/edit/closetag';
import * as codeMirror from 'codemirror';

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
            gutters: ['CodeMirror-linenumbers', 'breakpoints'],
        });

        editor.on('gutterClick', function (cm, n) {
            // var info = cm.lineInfo(n);
            var msg = document.createElement('div');
            msg.innerText = 'Hier könnte ihr Feedback and Assessment stehen';
            cm.getDoc().addLineWidget(n, msg, { above: true });
        });
    }
}
