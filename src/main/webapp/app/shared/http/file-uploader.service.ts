import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { MAX_FILE_SIZE } from 'app/shared/constants/input.constants';

export interface FileUploadResponse {
    path?: string;
}

type Options = {
    keepFileName: boolean;
};

@Injectable({ providedIn: 'root' })
export class FileUploaderService {
    // NOTE: this list has to be the same as in FileResource.java
    acceptedFileExtensions = 'png,jpg,jpeg,svg,pdf,zip';

    constructor(private http: HttpClient) {}

    /**
     * Function which uploads a file. It checks for supported file extensions and file size.
     * Options must be passed as a dictionary. E.g: { keepFileName: true }
     * @param {Blob | File} file
     * @param {string} fileName
     * @param options
     */
    uploadFile(file: Blob | File, fileName?: string, options?: Options): Promise<FileUploadResponse> {
        /** Check file extension **/
        const fileExtension = fileName ? fileName.split('.').pop()!.toLocaleLowerCase() : file['name'].split('.').pop().toLocaleLowerCase();
        if (this.acceptedFileExtensions.split(',').indexOf(fileExtension) === -1) {
            return Promise.reject(
                new Error(
                    'Unsupported file type! Only files of type ' +
                        this.acceptedFileExtensions
                            .split(',')
                            .map((extension) => `".${extension}"`)
                            .join(', ') +
                        ' allowed.',
                ),
            );
        }

        /** Check file size **/
        if (file.size > MAX_FILE_SIZE) {
            return Promise.reject(new Error('File is too big! Maximum allowed file size: ' + MAX_FILE_SIZE / (1024 * 1024) + ' MB.'));
        }

        const formData = new FormData();
        formData.append('file', file, fileName);
        const keepFileName: boolean = !!options && options.keepFileName;
        const url = `/api/fileUpload?keepFileName=${keepFileName}`;
        return this.http.post<FileUploadResponse>(url, formData).toPromise();
    }

    /**
     * Function which uploads a file. It checks for supported file extensions and file size.
     * Options must be passed as a dictionary. E.g: { keepFileName: true }
     * @param {Blob | File} file
     * @param {string} fileName
     * @param options
     */
    uploadMarkdownFile(file: Blob | File, fileName?: string, options?: Options): Promise<FileUploadResponse> {
        /** Check file extension **/
        const fileExtension = fileName ? fileName.split('.').pop()!.toLocaleLowerCase() : file['name'].split('.').pop().toLocaleLowerCase();
        if (this.acceptedFileExtensions.split(',').indexOf(fileExtension) === -1) {
            return Promise.reject(
                new Error(
                    'Unsupported file type! Only files of type ' +
                        this.acceptedFileExtensions
                            .split(',')
                            .map((extension) => `".${extension}"`)
                            .join(', ') +
                        ' allowed.',
                ),
            );
        }

        /** Check file size **/
        if (file.size > MAX_FILE_SIZE) {
            return Promise.reject(new Error('File is too big! Maximum allowed file size: ' + MAX_FILE_SIZE / (1024 * 1024) + ' MB.'));
        }

        const formData = new FormData();
        formData.append('file', file, fileName);
        const keepFileName: boolean = !!options && options.keepFileName;
        const url = `/api/markdown-file-upload?keepFileName=${keepFileName}`;
        return this.http.post<FileUploadResponse>(url, formData).toPromise();
    }

    /**
     * Duplicates file in the server.
     * @param filePath Path of the file which needs to be duplicated
     */
    async duplicateFile(filePath: string): Promise<FileUploadResponse> {
        // Get file from the server using filePath,
        const file = await this.http.get(filePath, { responseType: 'blob' }).toPromise();
        // Generate a temp file name with extension. File extension is necessary as server stores only specific kind of files,
        const tempFilename = 'temp' + filePath.split('/').pop()!.split('#')[0].split('?')[0];
        const formData = new FormData();
        formData.append('file', file, tempFilename);
        // Upload the file to server. This will make a new file in the server in the temp folder
        // and will return path of the file,
        return await this.http.post<FileUploadResponse>(`/api/fileUpload?keepFileName=${false}`, formData).toPromise();
    }
}
