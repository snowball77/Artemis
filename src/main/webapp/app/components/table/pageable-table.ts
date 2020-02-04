export interface SearchResult<T> {
    resultsOnPage: T[];
    numberOfPages: number;
    totalElements: number;
}

export enum SortingOrder {
    ASCENDING = 'ASCENDING',
    DESCENDING = 'DESCENDING',
}

export interface PageableSearch {
    page: number;
    pageSize: number;
    searchTerm: string;
    sortingOrder: SortingOrder;
    sortedColumn: string;
}
