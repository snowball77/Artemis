import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { ArtemisShowdownExtensionWrapper } from 'app/shared/markdown-editor/extensions/artemis-showdown-extension-wrapper';
import * as DOMPurify from 'dompurify';
import { ArtemisMarkdownService } from 'app/shared/markdown.service';
import { escapeStringForUseInRegex } from 'app/shared/util/global.utils';

@Injectable({ providedIn: 'root' })
export class MarkdownPlantImageExtensionWrapper implements ArtemisShowdownExtensionWrapper {
    private injectableElementsFoundSubject = new Subject<() => void>();

    // unique index, even if multiple plant images are shown from different editors on the same page (in different tabs)
    private plantImageIndex = 0;

    constructor(private markdownService: ArtemisMarkdownService) {}

    /**
     * Subscribes to injectableElementsFoundSubject.
     */
    subscribeForInjectableElementsFound() {
        return this.injectableElementsFoundSubject.asObservable();
    }

    /**
     * For the stringified plantImage provided, load the plantImage from the server and inject it into the html.
     * @param plantImage a stringified version of one plantImage.
     * @param index the index of the plantUml in html
     */
    private loadAndInjectPlantUml(plantImage: string, index: number) {
        this.markdownService
            .getPlantMarkdownImage(plantImage)
            .pipe(
                tap((plantImageImg) => {
                    const plantImageHtmlContainer = document.getElementById(`plantImage-${index}`);
                    if (plantImageHtmlContainer) {
                        // We need to sanitize the received img as it could contain malicious code in a script tag.
                        plantImageHtmlContainer.innerHTML = DOMPurify.sanitize(plantImageImg);
                    }
                }),
            )
            .subscribe();
    }

    /**
     * Creates and returns an extension to current exercise.
     */
    getExtension() {
        const extension: showdown.ShowdownExtension = {
            type: 'lang',
            filter: (text: string) => {
                const idPlaceholder = '%idPlaceholder%';
                const plantImageRegex = /@startimage([^@]*)@endimage/g;
                const plantImgContainer = `<div class="mb-4" id="plantImg-${idPlaceholder}"></div>`;
                const plantImages = text.match(plantImageRegex) || [];
                // Assign unique ids to images at the beginning.
                const plantImagesIndexed = plantImages.map((plantImage) => {
                    const nextIndex = this.plantImageIndex;
                    // increase the global unique index so that the next plantUml gets a unique global id
                    this.plantImageIndex++;
                    return { plantImageId: nextIndex, plantImage };
                });
                // custom markdown to html rendering: replace the plantImg in the markdown with a simple <div></div> container with a unique id placeholder
                // with the global unique id so that we can find the plantImg later on, when it was rendered, and then inject the 'actual' inner html (actually a svg image)
                const replacedText = plantImagesIndexed.reduce((acc: string, imgIndexed: { plantImgId: number; plantImg: string }): string => {
                    return acc.replace(new RegExp(escapeStringForUseInRegex(imgIndexed.plantImg), 'g'), plantImgContainer.replace(idPlaceholder, imgIndexed.plantImgId.toString()));
                }, text);
            },
        };
        return extension;
    }
}
