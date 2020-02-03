import { ContentChild, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges, TemplateRef } from '@angular/core';
import { BaseEntity, PageableSearch, SearchResult } from 'app/shared';
import { isNumber } from 'lodash';
import { Observable } from 'rxjs';

export enum SortOrder {
    ASC = 'asc',
    DESC = 'desc',
}

export enum SortIcon {
    NONE = 'sort',
    ASC = 'sort-up',
    DESC = 'sort-down',
}

export const SortOrderIcon = {
    [SortOrder.ASC]: SortIcon.ASC,
    [SortOrder.DESC]: SortIcon.DESC,
};

export type SortProp = {
    field: string;
    order: SortOrder;
};

export type PagingValue = number | 'all';

export const entityToString = (entity: BaseEntity) => entity.id.toString();

export abstract class GenericDatatable implements OnInit, OnChanges {
    /**
     * @property templateRef Ref to the content child of this component (which is ngx-datatable)
     */
    @ContentChild(TemplateRef, { read: TemplateRef, static: false })
    templateRef: TemplateRef<any>;

    /**
     * @property isLoading Loading state of the data that is fetched by the ancestral component
     * @property entityType Entity identifier (e.g. 'result' or 'participation') used as a key to differentiate from other tables
     * @property allEntities List of all entities that should be displayed in the table (one entity per row)
     * @property entitiesPerPageTranslation Translation string that has the variable { number } in it (e.g. 'artemisApp.exercise.resultsPerPage')
     * @property showAllEntitiesTranslation Translation string if all entities should be displayed (e.g. 'artemisApp.exercise.showAll')
     * @property searchPlaceholderTranslation Translation string that is used for the placeholder in the search input field
     * @property searchFields Fields of entity whose values will be compared to the user's search string (allows nested attributes, e.g. ['student.login', 'student.name'])
     * @function searchTextFromEntity Function that takes an entity and returns a text that is inserted into the search input field when clicking on an autocomplete suggestion
     * @function searchResultFormatter Function that takes an entity and returns the text for the autocomplete suggestion result row
     * @function customFilter Function that takes an entity and returns true or false depending on whether this entity should be shown (combine with customFilterKey)
     * @property customFilterKey Filter state of an ancestral component which triggers a table re-rendering if it changes
     */
    @Input() isLoading = false;
    @Input() entityType = 'entity';
    @Input() allEntities: BaseEntity[] = [];
    @Input() entitiesPerPageTranslation: string;
    @Input() showAllEntitiesTranslation: string;
    @Input() searchPlaceholderTranslation: string;
    @Input() searchFields: string[] = [];
    @Input() searchTextFromEntity: (entity: BaseEntity) => string = entityToString;
    @Input() searchResultFormatter: (entity: BaseEntity) => string = entityToString;
    @Input() customFilter: (entity: BaseEntity) => boolean = () => true;
    @Input() customFilterKey: any = {};
    @Input() pagedResultProvider: (search: PageableSearch) => SearchResult<BaseEntity>;

    /**
     * @property entitiesSizeChange Emits an event when the number of entities displayed changes (e.g. by filtering)
     */
    @Output() entitiesSizeChange = new EventEmitter<number>();

    /**
     * @property isRendering Rendering state of the table (used for conditional display of the loading indicator)
     * @property entities (Sorted) List of entities that are shown in the table (is a subset of allEntities after filters were applied)
     * @property pagingValue Current number (or 'all') of entities displayed per page (can be changed and saved to local storage by the user)
     * @property entityCriteria Contains a list of search terms
     */
    protected isRendering: boolean;
    protected entities: BaseEntity[];
    protected pagingValue: PagingValue;
    protected entityCriteria: {
        textSearch: string[];
        sortProp: SortProp;
    };

    ngOnInit() {
        // explicitly bind these callbacks to their current context
        // so that they can be used from child components
        this.onSort = this.onSort.bind(this);
        this.iconForSortPropField = this.iconForSortPropField.bind(this);
    }

    /**
     * Method is called when Inputs of this component have changed.
     *
     * @param changes List of Inputs that were changed
     */
    ngOnChanges(changes: SimpleChanges) {
        if (changes.allEntities || changes.customFilterKey) {
            this.updateEntities();
        }
    }

    /**
     * Set the number of entities shown per page (and persist it in local storage).
     * Since the rendering takes a bit, show the loading animation until it completes.
     *
     * @param paging Number of entities per page
     */
    protected abstract set numberOfRowsPerPage(paging: PagingValue);

    /**
     * Splits the provides search words by comma and updates the autocompletion overlay.
     * Also updates the available entities in the UI.
     *
     * @param text$ stream of text input.
     */
    protected abstract onSearch(search$: Observable<string>): Observable<boolean | BaseEntity[]>;

    protected abstract updateEntities(): void;

    /**
     * The component is preparing if the data is loading (managed by the parent component)
     * or rendering (managed by this component).
     */
    protected get isPreparing() {
        return this.isLoading || this.isRendering;
    }

    /**
     * Number of entities displayed per page. Can be undefined to show all entities without pagination.
     */
    protected get pageLimit() {
        return isNumber(this.pagingValue) ? this.pagingValue : undefined;
    }

    /**
     * Sets the selected sort field, then updates the available entities in the UI.
     * Toggles the order direction (asc, desc) when the field has not changed.
     *
     * @param field Entity field
     */
    protected onSort(field: string) {
        const sameField = this.entityCriteria.sortProp && this.entityCriteria.sortProp.field === field;
        const order = sameField ? this.invertSort(this.entityCriteria.sortProp.order) : SortOrder.ASC;
        this.entityCriteria.sortProp = { field, order };
        this.updateEntities();
    }

    /**
     * Returns the opposite sort order of the given sort order.
     *
     * @param order SortOrder
     */
    private invertSort = (order: SortOrder) => {
        return order === SortOrder.ASC ? SortOrder.DESC : SortOrder.ASC;
    };

    /**
     * Returns the Font Awesome icon name for a column header's sorting icon
     * based on the currently active sortProp field and order.
     *
     * @param field Entity field
     */
    protected iconForSortPropField(field: string) {
        if (this.entityCriteria.sortProp.field !== field) {
            return SortIcon.NONE;
        }
        return SortOrderIcon[this.entityCriteria.sortProp.order];
    }
}
