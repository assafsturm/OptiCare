package Persistence.dto;

import java.util.LinkedHashMap;
import java.util.Map;

/** Root JSON envelope for Stage-6 ward persistence. */
public final class WardStateDocument {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    private int schemaVersion = CURRENT_SCHEMA_VERSION;
    /** Monotonic CAS token; incremented on each successful atomic save when file exists / first save assigns 1. */
    private long persistVersion;

    private java.util.List<PersistedDepartment> departments = new java.util.ArrayList<>();
    /** departmentId → (patientId → payload). Includes assigned and waiting patients. */
    private Map<String, Map<String, PersistedPatient>> patientsByDepartmentId = new LinkedHashMap<>();
    /** departmentId → (patientId → bedId). */
    private Map<String, Map<String, String>> assignmentsByDepartmentId = new LinkedHashMap<>();

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public long getPersistVersion() {
        return persistVersion;
    }

    public void setPersistVersion(long persistVersion) {
        this.persistVersion = persistVersion;
    }

    public java.util.List<PersistedDepartment> getDepartments() {
        return departments;
    }

    public void setDepartments(java.util.List<PersistedDepartment> departments) {
        this.departments = departments != null ? departments : new java.util.ArrayList<>();
    }

    public Map<String, Map<String, PersistedPatient>> getPatientsByDepartmentId() {
        return patientsByDepartmentId;
    }

    public void setPatientsByDepartmentId(Map<String, Map<String, PersistedPatient>> patientsByDepartmentId) {
        this.patientsByDepartmentId = patientsByDepartmentId != null ? patientsByDepartmentId : new LinkedHashMap<>();
    }

    public Map<String, Map<String, String>> getAssignmentsByDepartmentId() {
        return assignmentsByDepartmentId;
    }

    public void setAssignmentsByDepartmentId(Map<String, Map<String, String>> assignmentsByDepartmentId) {
        this.assignmentsByDepartmentId = assignmentsByDepartmentId != null ? assignmentsByDepartmentId : new LinkedHashMap<>();
    }
}
