import { AfterViewInit, Component, Input, Output, EventEmitter, ViewChild, ElementRef } from '@angular/core';
import { EditorView } from '@codemirror/next/view';
import { EditorState } from '@codemirror/next/state';

@Component({
    selector: 'jhi-code-editor-codemirror-next',
    templateUrl: './code-editor-codemirror-next.component.html',
    styleUrls: ['./code-editor-codemirror.scss'],
})
export class CodeEditorCodemirrorNextComponent {
    editor = new EditorView({ state: EditorState.create({ doc: 'hello' }) });
    @ViewChild('editorRef') editorRef: ElementRef;

    ngAfterViewInit(): void {
        this.editorRef.nativeElement.innerHTML = this.editor.dom.outerHTML;
    }
}
