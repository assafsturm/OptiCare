package Persistence.dto;

import Model.enums.PatientStatus;

import java.time.Instant;

public final class PersistedPatient {

    private String id;
    private PersistedPersonalDetails personalDetails;
    private PersistedClinicalData clinicalData;
    private PatientStatus status;
    private Instant admittedAt;
    private boolean temporarilyUnavailable;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public PersistedPersonalDetails getPersonalDetails() {
        return personalDetails;
    }

    public void setPersonalDetails(PersistedPersonalDetails personalDetails) {
        this.personalDetails = personalDetails;
    }

    public PersistedClinicalData getClinicalData() {
        return clinicalData;
    }

    public void setClinicalData(PersistedClinicalData clinicalData) {
        this.clinicalData = clinicalData;
    }

    public PatientStatus getStatus() {
        return status;
    }

    public void setStatus(PatientStatus status) {
        this.status = status;
    }

    public Instant getAdmittedAt() {
        return admittedAt;
    }

    public void setAdmittedAt(Instant admittedAt) {
        this.admittedAt = admittedAt;
    }

    public boolean isTemporarilyUnavailable() {
        return temporarilyUnavailable;
    }

    public void setTemporarilyUnavailable(boolean temporarilyUnavailable) {
        this.temporarilyUnavailable = temporarilyUnavailable;
    }
}
