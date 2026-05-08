package Model.entety;

import java.time.Instant;
import java.util.Objects; // for the admission time

import Model.enums.PatientStatus; // for equals and hashCode

public class Patient {
    private String id;
    private PersonalDetails personalDetails;
    private ClinicalData clinicalData;
    private PatientStatus status = PatientStatus.WAITING;
    // admission time for waiting list ordering null sorts last in tie breaks (see watinglistcomparatorfactory)
    private Instant admittedAt;
    // When true patient is excluded from Assign/Move/Swap
    private boolean temporarilyUnavailable;

    public Patient() {
    }

    public Patient(String id, PersonalDetails personalDetails, ClinicalData clinicalData) {
        this.id = id;
        this.personalDetails = personalDetails;
        this.clinicalData = clinicalData;
    } // constructor for patient when admission time and temporarily unavailable are not provided

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
    public int hashCode() {// hash by id
        return Objects.hash(id);
    }
// getters and setters for the patient
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
