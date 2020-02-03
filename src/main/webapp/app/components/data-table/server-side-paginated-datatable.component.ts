import { Component } from '@angular/core';
import { BaseEntity } from 'app/shared';
import { GenericDatatable, PagingValue } from 'app/components/data-table/generic-datatable';
import { Observable, of } from 'rxjs';
import { ColumnMode, SortType } from '@swimlane/ngx-datatable';

@Component({
    selector: 'jhi-server-side-paginated-data-table',
    templateUrl: 'server-side-paginated-data-table.html',
    styleUrls: ['data-table.component.scss'],
})
export class ServerSidePaginatedDatatableComponent extends GenericDatatable {
    readonly SortType = SortType;
    readonly ColumnMode = ColumnMode;

    protected set numberOfRowsPerPage(paging: PagingValue) {}

    protected onSearch(search$: Observable<string>): Observable<boolean | BaseEntity[]> {
        return of(false);
    }

    protected updateEntities(): void {}
}
