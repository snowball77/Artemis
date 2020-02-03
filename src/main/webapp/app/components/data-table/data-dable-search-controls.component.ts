import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { isNumber } from 'lodash';
import { BaseEntity } from 'app/shared';
import { LocalStorageService } from 'ngx-webstorage';
import { Observable } from 'rxjs';
import { entityToString, PagingValue } from 'app/components/data-table/generic-datatable';

@Component({
    selector: 'jhi-data-table-search-controls',
    templateUrl: 'data-table-search-controls.html',
    styleUrls: ['data-table.component.scss'],
})
export class DataDableSearchControlsComponent implements OnInit {
    @Input() entityType = 'entity';
    @Input() entitiesPerPageTranslation: string;
    @Input() searchPlaceholderTranslation: string;
    @Input() searchTextFromEntity: (entity: BaseEntity) => string = entityToString;
    @Input() searchResultFormatter: (entity: BaseEntity) => string = entityToString;
    @Input() showAllEntitiesTranslation: string;
    @Input() onSearch: (text$: Observable<string>) => Observable<false | BaseEntity[]>;

    @Output() onAutocompleteSelected = new EventEmitter<void>();
    @Output() onPagingValueSelected = new EventEmitter<PagingValue>();

    pagingValue: PagingValue;
    textSearch: string[];

    constructor(private localStorage: LocalStorageService) {}

    ngOnInit(): void {
        this.pagingValue = this.getCachedEntitiesPerPage();
        this.onPagingValueSelected.emit(this.pagingValue);
    }

    /**
     * @property PAGING_VALUES Possible values for the number of entities shown per page of the table
     * @property DEFAULT_PAGING_VALUE Default number of entities shown per page if the user has no value set for this yet in local storage
     */
    readonly PAGING_VALUES: PagingValue[] = [10, 20, 50, 100, 200, 500, 1000, 'all'];
    readonly DEFAULT_PAGING_VALUE = 50;

    /**
     * Get "items per page" setting from local storage. If it does not exist, use the default.
     */
    private getCachedEntitiesPerPage = () => {
        const cachedValue = this.localStorage.retrieve(this.perPageCacheKey);
        if (cachedValue) {
            const parsedValue = parseInt(cachedValue, 10) || cachedValue;
            if (this.PAGING_VALUES.includes(parsedValue as any)) {
                return parsedValue as PagingValue;
            }
        }
        return this.DEFAULT_PAGING_VALUE;
    };

    /**
     * Returns the translation based on whether a limited number of entities is displayed or all
     *
     * @param quantifier Number of entities per page or 'all'
     */
    perPageTranslation(quantifier: PagingValue) {
        return isNumber(quantifier) ? this.entitiesPerPageTranslation : this.showAllEntitiesTranslation;
    }

    /**
     * Set the number of entities shown per page (and persist it in local storage).
     *
     * @param paging Number of entities per page
     */
    setEntitiesPerPage = (paging: number) => {
        this.onPagingValueSelected.emit(paging);
        this.pagingValue = paging;
        this.localStorage.store(this.perPageCacheKey, paging.toString());
    };

    /**
     * Method is called when user clicks on an autocomplete suggestion. The input method
     * searchTextFromEntity determines how the entity is converted to a searchable string.
     *
     * @param entity Entity that was selected via autocomplete
     */
    onAutocompleteSelect = (entity: BaseEntity) => {
        this.textSearch[this.textSearch.length - 1] = this.searchTextFromEntity(entity);
        this.onAutocompleteSelected.emit();
    };

    /**
     * Key that is used for storing this "items per page" setting in local storage
     */
    private get perPageCacheKey() {
        return `${this.entityType}-items-per-page`;
    }

    /**
     * Formats the search input.
     */
    searchInputFormatter = () => {
        return this.textSearch.join(', ');
    };
}
