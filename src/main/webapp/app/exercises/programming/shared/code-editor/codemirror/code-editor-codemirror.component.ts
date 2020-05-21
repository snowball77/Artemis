import { Component } from '@angular/core';
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
export class CodeEditorCodemirrorComponent {
    public content: string;
}
