package de.tum.in.www1.artemis.domain;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonView;
import de.tum.in.www1.artemis.domain.view.QuizView;

/**
 * Base abstract class for entities which have an id that is generated automatically (basically all domain objects).
 */
@MappedSuperclass
public abstract class DomainObject implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView(QuizView.Before.class)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * this methods checks for database equality based on the id
     * @param o another object
     * @return whether this and the other object are equal based on the database id
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DomainObject domainObject = (DomainObject) o;
        if (domainObject.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), domainObject.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}
