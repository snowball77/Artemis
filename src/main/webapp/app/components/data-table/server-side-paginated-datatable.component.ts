import { Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { BaseEntity, PageableSearch, SearchResult, SortingOrder } from 'app/shared';
import { GenericDatatable, PagingValue, SortOrder } from 'app/components/data-table/generic-datatable';
import { Observable, of } from 'rxjs';
import { ColumnMode, SortType } from '@swimlane/ngx-datatable';

@Component({
    selector: 'jhi-server-side-paginated-data-table',
    templateUrl: 'server-side-paginated-data-table.html',
    styleUrls: ['data-table.component.scss'],
})
export class ServerSidePaginatedDatatableComponent extends GenericDatatable implements OnInit, OnChanges {
    @Input() searchResult: SearchResult<BaseEntity>;

    @Output() search = new EventEmitter<PageableSearch>();

    totalNumberOfRows = 0;

    ngOnInit() {
        super.ngOnInit();
        this.entities = [];
        this.entityCriteria = {
            textSearch: [],
            sortProp: { field: 'id', order: SortOrder.ASC },
        };
    }

    ngOnChanges(changes: SimpleChanges) {
        super.ngOnChanges(changes);
        if (changes.searchResult) {
            this.onUpdatedSearchResult();
        }
    }

    protected get context() {
        return {
            settings: {
                count: this.totalNumberOfRows,
                limit: this.pageLimit,
                sortType: SortType.multi,
                columnMode: ColumnMode.force,
                headerHeight: 50,
                footerHeight: 50,
                rowHeight: 'auto',
                rows: this.entities,
                rowClass: '',
                scrollbarH: true,
                externalSorting: true,
                externalPaging: true,
            },
            controls: {
                iconForSortPropField: this.iconForSortPropField,
                onSort: this.onSort,
            },
        };
    }

    protected set numberOfRowsPerPage(paging: PagingValue) {
        this.pagingValue = paging;
        if (this.pagingValue) {
            this.updateEntities();
        }
    }

    protected onSearch(search$: Observable<string>): Observable<boolean | BaseEntity[]> {
        return of(false);
    }

    protected updateEntities(): void {
        this.isLoading = true;
        const search = {
            pageSize: this.pagingValue === 'all' ? -1 : this.pagingValue,
            searchTerm: this.entityCriteria.textSearch.join(','),
            sortedColumn: this.entityCriteria.sortProp.field,
            sortingOrder: this.convertSortOrder(this.entityCriteria.sortProp.order),
        } as PageableSearch;

        this.search.emit(search);
    }

    private onUpdatedSearchResult() {
        if (this.searchResult) {
            this.entities = this.searchResult.resultsOnPage;
            this.totalNumberOfRows = this.searchResult.totalElements;
            // defer execution of change emit to prevent ExpressionChangedAfterItHasBeenCheckedError, see explanation at https://blog.angular-university.io/angular-debugging/
            setTimeout(() => this.entitiesSizeChange.emit(this.entities.length));
            this.isLoading = false;
        }
    }

    private convertSortOrder(order: SortOrder): SortingOrder {
        switch (order) {
            case SortOrder.ASC:
                return SortingOrder.ASCENDING;
            case SortOrder.DESC:
                return SortingOrder.DESCENDING;
        }
    }
}
