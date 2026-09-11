package com.adnaan525.medme.model;

/**
 * scheduledDateTime / actualTakenDateTime are ISO-8601 "yyyy-MM-dd'T'HH:mm" strings
 * (see DateTimeUtils) so the JSON stays plain-text and portable across export/import.
 */
public class DoseLog {
    private String id;
    private String scheduledDateTime;
    private String actualTakenDateTime;
    private DoseStatus status;

    public DoseLog() {
    }

    public DoseLog(String id, String scheduledDateTime) {
        this.id = id;
        this.scheduledDateTime = scheduledDateTime;
        this.status = DoseStatus.PENDING;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getScheduledDateTime() {
        return scheduledDateTime;
    }

    public void setScheduledDateTime(String scheduledDateTime) {
        this.scheduledDateTime = scheduledDateTime;
    }

    public String getActualTakenDateTime() {
        return actualTakenDateTime;
    }

    public void setActualTakenDateTime(String actualTakenDateTime) {
        this.actualTakenDateTime = actualTakenDateTime;
    }

    public DoseStatus getStatus() {
        return status;
    }

    public void setStatus(DoseStatus status) {
        this.status = status;
    }
}
