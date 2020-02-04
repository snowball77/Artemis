package de.tum.in.www1.artemis.domain.enumeration;

import org.springframework.data.domain.Sort;

public enum SortingOrder {

    ASCENDING, DESCENDING;

    public Sort.Direction toJpaOrder() {
        switch (this) {
        case ASCENDING:
            return Sort.Direction.ASC;
        default:
            return Sort.Direction.DESC;
        }
    }
}
