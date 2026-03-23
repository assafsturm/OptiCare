package Model.entety;

import Model.enums.PatientStatus;

import java.time.Instant;
import java.util.Objects;

public class Patient {
    private String id;
    private PersonalDetails personalDetails;
    private ClinicalData clinicalData;
    private PatientStatus status = PatientStatus.WAITING;
    /** Admission time for deterministic waiting-list ordering; null sorts last in tie-breaks. */
    private Instant admittedAt;
    /** When true, patient is excluded from Assign / Move / Swap (persisted). */
    private boolean temporarilyUnavailable;

    public Patient() {
    }

    public Patient(String id, PersonalDetails personalDetails, ClinicalData clinicalData) {
        this.id = id;
        this.personalDetails = personalDetails;
        this.clinicalData = clinicalData;
    }

    public Patient(String id, PersonalDetails personalDetails, ClinicalData clinicalData,
                   Instant admittedAt, boolean temporarilyUnavailable) {
        this.id = id;
        this.personalDetails = personalDetails;
        this.clinicalData = clinicalData;
        this.admittedAt = admittedAt;
        this.temporarilyUnavailable = temporarilyUnavailable;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Patient other = (Patient) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public PersonalDetails getPersonalDetails() { return personalDetails; }
    public void setPersonalDetails(PersonalDetails personalDetails) { this.personalDetails = personalDetails; }

    public ClinicalData getClinicalData() { return clinicalData; }
    public void setClinicalData(ClinicalData clinicalData) { this.clinicalData = clinicalData; }

    public PatientStatus getStatus() { return status; }
    public void setStatus(PatientStatus status) { this.status = status; }

    public Instant getAdmittedAt() { return admittedAt; }
    public void setAdmittedAt(Instant admittedAt) { this.admittedAt = admittedAt; }

    public boolean isTemporarilyUnavailable() { return temporarilyUnavailable; }
    public void setTemporarilyUnavailable(boolean temporarilyUnavailable) {
        this.temporarilyUnavailable = temporarilyUnavailable;
    }
}
