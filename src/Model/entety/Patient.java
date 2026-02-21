package Model.entety;

import java.util.Objects;

public class Patient {
    private String id;
    private PersonalDetails personalDetails;
    private ClinicalData clinicalData;

    public Patient() {
    }

    public Patient(String id, PersonalDetails personalDetails, ClinicalData clinicalData) {
        this.id = id;
        this.personalDetails = personalDetails;
        this.clinicalData = clinicalData;
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
}
