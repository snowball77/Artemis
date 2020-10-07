package de.tum.in.www1.artemis.domain.notification;

import java.time.ZonedDateTime;

import javax.persistence.*;

import de.tum.in.www1.artemis.domain.enumeration.SystemNotificationType;

/**
 * A SystemNotification.
 */
@Entity
@DiscriminatorValue(value = "S")
public class SystemNotification extends Notification {

    @Column(name = "expire_date")
    private ZonedDateTime expireDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "jhi_type")
    private SystemNotificationType type;

    public ZonedDateTime getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(ZonedDateTime expireDate) {
        this.expireDate = expireDate;
    }

    public SystemNotificationType getType() {
        return type;
    }

    public void setType(SystemNotificationType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "SystemNotification{" + "id=" + getId() + ", expireDate='" + getExpireDate() + "'" + ", type='" + getType() + "'" + "}";
    }
}
