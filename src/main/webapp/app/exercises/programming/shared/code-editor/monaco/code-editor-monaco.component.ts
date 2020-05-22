import { AfterViewInit, Component, ViewChild, ElementRef } from '@angular/core';
import * as monaco from 'monaco-editor';

@Component({
    selector: 'jhi-code-editor-monaco',
    templateUrl: './code-editor-monaco.component.html',
    styleUrls: ['./code-editor-monaco.scss'],
})
export class CodeEditorMonacoComponent implements AfterViewInit {
    @ViewChild('editor') editorContent: ElementRef;
    monaco: any;

    ngAfterViewInit(): void {
        const test = monaco.editor.create(this.editorContent.nativeElement, {
            value: ['function x() {', '\tconsole.log("Hello world!");', '}'].join('\n'),
            language: 'javascript',
        });
    }
}
