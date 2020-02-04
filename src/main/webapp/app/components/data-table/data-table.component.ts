import { Component, OnInit, OnChanges, SimpleChanges, ViewEncapsulation, Input } from '@angular/core';
import { debounceTime, distinctUntilChanged, map, tap } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { ColumnMode, SortType } from '@swimlane/ngx-datatable';
import { SortByPipe } from 'app/components/pipes';
import { compose, filter } from 'lodash/fp';
import { get } from 'lodash';
import { BaseEntity } from 'app/shared';
import { GenericDatatable, PagingValue, SortOrder } from 'app/components/data-table/generic-datatable';

@Component({
    selector: 'jhi-data-table',
    templateUrl: './data-table.component.html',
    styleUrls: ['data-table.component.scss'],
    encapsulation: ViewEncapsulation.None,
})
export class DataTableComponent extends GenericDatatable implements OnInit, OnChanges {
    @Input() allEntities: BaseEntity[] = [];

    constructor(private sortByPipe: SortByPipe) {
        super();
        this.entities = [];
        this.entityCriteria = {
            textSearch: [],
            sortProp: { field: 'id', order: SortOrder.ASC },
        };
    }

    ngOnChanges(changes: SimpleChanges) {
        super.ngOnChanges(changes);
        if (changes.allEntities) {
            this.updateEntities();
        }
    }

    protected get context() {
        return {
            settings: {
                limit: this.pageLimit,
                sortType: SortType.multi,
                columnMode: ColumnMode.force,
                headerHeight: 50,
                footerHeight: 50,
                rowHeight: 'auto',
                rows: this.entities,
                rowClass: '',
                scrollbarH: true,
            },
            controls: {
                iconForSortPropField: this.iconForSortPropField,
                onSort: this.onSort,
            },
        };
    }

    set numberOfRowsPerPage(paging: PagingValue) {
        this.isLoading = true;
        setTimeout(() => {
            this.pagingValue = paging;
            this.isLoading = false;
        }, 500);
    }

    protected updateEntities() {
        const filteredEntities = compose(
            filter((entity: BaseEntity) => this.filterEntityByTextSearch(this.entityCriteria.textSearch, entity, this.searchFields)),
            filter(this.customFilter),
        )(this.allEntities);
        this.entities = this.sortByPipe.transform(filteredEntities, this.entityCriteria.sortProp.field, this.entityCriteria.sortProp.order === SortOrder.ASC);
        // defer execution of change emit to prevent ExpressionChangedAfterItHasBeenCheckedError, see explanation at https://blog.angular-university.io/angular-debugging/
        setTimeout(() => this.entitiesSizeChange.emit(this.entities.length));
    }

    /**
     * Filter the given entities by the provided search words.
     * Returns entities that match any of the provides search words, if searchWords is empty returns all entities.
     *
     * @param searchWords list of student logins or names
     * @param entity BaseEntity
     * @param searchFields list of paths in entity to search
     */
    private filterEntityByTextSearch = (searchWords: string[], entity: BaseEntity, searchFields: string[]) => {
        // When no search word is inputted, we return all entities.
        if (!searchWords.length) {
            return true;
        }
        // Otherwise we do a fuzzy search on the inputted search words.
        const containsSearchWord = (fieldValue: string) => searchWords.some(this.foundIn(fieldValue));
        return this.entityFieldValues(entity, searchFields).some(containsSearchWord);
    };

    /**
     * Returns the values that the given entity has in the given fields
     *
     * @param entity Entity whose field values are extracted
     * @param fields Fields to extract from entity (can be paths such as "student.login")
     */
    private entityFieldValues = (entity: BaseEntity, fields: string[]) => {
        const getEntityFieldValue = (field: string) => get(entity, field, false);
        return fields.map(getEntityFieldValue).filter(Boolean) as string[];
    };

    /**
     * Performs a case-insensitive search of "word" inside of "text".
     * If "word" consists of multiple segments separated by a space, each one of them must appear in "text".
     * This relaxation has the benefit that searching for "Max Mustermann" will still find "Max Gregor Mustermann".
     * Additionally, the wildcard symbols "*" and "?" are supported.
     *
     * @param text string that is searched for param "word"
     */
    private foundIn = (text: string) => (word: string) => {
        const segments = word.toLowerCase().split(' ');
        return (
            text &&
            word &&
            segments.every(segment => {
                const regex = segment
                    .replace(/\*/g, '.*') // multiple characters
                    .replace(/\?/g, '.'); // single character
                return new RegExp(regex).test(text.toLowerCase());
            })
        );
    };

    onSearch = (text$: Observable<string>) => {
        return text$.pipe(
            debounceTime(200),
            distinctUntilChanged(),
            map(text => {
                const searchWords = text.split(',').map(word => word.trim());
                // When the entity field is cleared, we translate the resulting empty string to an empty array (otherwise no entities would be found).
                return searchWords.length === 1 && !searchWords[0] ? [] : searchWords;
            }),
            // For available entities in table.
            tap(searchWords => {
                this.entityCriteria.textSearch = searchWords;
                this.updateEntities();
            }),
            // For autocomplete.
            map((searchWords: string[]) => {
                // We only execute the autocomplete for the last keyword in the provided list.
                const lastSearchWord = searchWords.length ? searchWords[searchWords.length - 1] : null;
                // Don't execute autocomplete for less then two inputted characters.
                if (!lastSearchWord || lastSearchWord.length < 3) {
                    return false;
                }
                return this.entities.filter(entity => {
                    const fieldValues = this.entityFieldValues(entity, this.searchFields);
                    return fieldValues.some(fieldValue => this.foundIn(fieldValue)(lastSearchWord));
                });
            }),
        );
    };
}
