package Persistence.dto;

import java.util.LinkedHashMap;
import java.util.Map;


// json envelope for the ward state, the single file that contains all the data, json writes and reads this file
public final class WardStateDocument {

    public static final int CURRENT_SCHEMA_VERSION = 1;// no changing mid run
    // the schema version of the file
    private int schemaVersion = CURRENT_SCHEMA_VERSION; // current mode expectition

    private long persistVersion;
// list of all departments
    private java.util.List<PersistedDepartment> departments = new java.util.ArrayList<>();
    // departmentId → (bedtId → patient) map of department id to map of patient id to Ppatient
    private Map<String, Map<String, PersistedPatient>> patientsByDepartmentId = new LinkedHashMap<>();
    // departmentId → (patientId → bedId) depatmet id to map of patient id to bed id
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
