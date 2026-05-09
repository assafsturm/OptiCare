package Persistence;

import Algorithm.AssignmentState;
import Model.entety.Bed;
import Model.entety.ClinicalData;
import Model.entety.Department;
import Model.entety.Patient;
import Model.entety.PersonalDetails;
import Model.entety.Room;
import Persistence.dto.PersistedBed;
import Persistence.dto.PersistedClinicalData;
import Persistence.dto.PersistedDepartment;
import Persistence.dto.PersistedPatient;
import Persistence.dto.PersistedPersonalDetails;
import Persistence.dto.PersistedRoom;
import Persistence.dto.WardStateDocument;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// bidirectional mapper between persisted JSON envelope and ward runtime objects
public final class WardStateMapper {

    private WardStateMapper() {
    }

    // record for the ward hydration (for deserialization)
    //main output of class
    public record WardHydration(
            List<Department> departments,
            LinkedHashMap<String, LinkedHashMap<String, Patient>> patientsByDepartmentId,
            LinkedHashMap<String, AssignmentState> assignmentStates,
            long persistVersionLoaded
    ) {}

    // capture the objects to be persisted (for serialization)

    public static WardStateDocument captureDraft(
            List<Department> departments,
            Map<String, Map<String, Patient>> patientByDept,
            Map<String, AssignmentState> assignmentByDeptId
    ) {
        WardStateDocument doc = new WardStateDocument();
        doc.setSchemaVersion(WardStateDocument.CURRENT_SCHEMA_VERSION);
        LinkedHashMap<String, Map<String, PersistedPatient>> pmap = new LinkedHashMap<>();// map of depatment to patients
        LinkedHashMap<String, Map<String, String>> amap = new LinkedHashMap<>();// map of depatment to assignments
        List<PersistedDepartment> pdepts = new ArrayList<>();// list of departments to be persisted

        for (Department d : departments) {// iterate over departments
            pdepts.add(toPersistedDepartment(d));// convert department to persisted department

            LinkedHashMap<String, PersistedPatient> rowPatients = new LinkedHashMap<>();// map of patient to persisted patient
            Map<String, Patient> src = patientByDept == null ? null : patientByDept.get(d.getId());// get the patients of the department
            if (src != null) {
                for (Patient patient : src.values()) {// iterate over patients
                    if (patient != null && patient.getId() != null) {
                        rowPatients.put(patient.getId(), toPersistedPatient(patient));// convert patient to persisted patient
                    }
                }
            }
            pmap.put(d.getId(), rowPatients);// add the patients to the map

            LinkedHashMap<String, String> rowAssign = new LinkedHashMap<>();
            AssignmentState ast = assignmentByDeptId == null ? null : assignmentByDeptId.get(d.getId());// get the assignments of the department
            if (ast != null) {
                for (Map.Entry<String, Bed> e : ast.getAssignments().entrySet()) {// iterate over assignments
                    Bed bed = e.getValue();
                    if (e.getKey() != null && bed != null && bed.getId() != null) {
                        rowAssign.put(e.getKey(), bed.getId());
                    }
                }
            }
            amap.put(d.getId(), rowAssign);
        }
        //set the departments, patients, and assignments to the document
        doc.setDepartments(new ArrayList<>(pdepts));
        doc.setPatientsByDepartmentId(pmap);
        doc.setAssignmentsByDepartmentId(amap);
        return doc;
    }

    public static WardHydration hydrate(WardStateDocument doc) {
        Objects.requireNonNull(doc, "doc");// check if the document is null

        if (doc.getSchemaVersion() != WardStateDocument.CURRENT_SCHEMA_VERSION) {// check if the schema version is supported
            throw new IllegalArgumentException(
                    "Unsupported ward state schema version " + doc.getSchemaVersion()
                            + "; expected " + WardStateDocument.CURRENT_SCHEMA_VERSION);
        }

        LinkedHashMap<String, LinkedHashMap<String, Patient>> patientsByDept = new LinkedHashMap<>();// map of department id to patients
        LinkedHashMap<String, Department> departmentsById = new LinkedHashMap<>();// map of department id to department object

        Map<String, Map<String, PersistedPatient>> rawPatients = doc.getPatientsByDepartmentId();// get the patients from the document

        List<Department> departmentListOrdered = new ArrayList<>();
        for (PersistedDepartment pd : doc.getDepartments()) {// iterate over the departments

            LinkedHashMap<String, Patient> builtPatients =
                    hydratePatientRegistry(pd.getId(), rawPatients != null ? rawPatients.get(pd.getId()) : null);
            patientsByDept.put(pd.getId(), builtPatients);
            Department d = hydrateDepartmentShell(pd, builtPatients);// convert the persisted department to the department object
            departmentsById.put(d.getId(), d);
            departmentListOrdered.add(d);
        }
        // rawAssign is a map of department id to map of patient id to bed id (no real objects)
        // so we need to get the objects by id
        LinkedHashMap<String, AssignmentState> assigns = new LinkedHashMap<>();// map of department id to assignment state
        Map<String, Map<String, String>> rawAssign = doc.getAssignmentsByDepartmentId();// get the assignments from the document (ids only)
        if (rawAssign != null) {
            for (Map.Entry<String, Map<String, String>> e : rawAssign.entrySet()) {// iterate over the assignments
                Department department = departmentsById.get(e.getKey());// get the department by id
                LinkedHashMap<String, Patient> pmap = patientsByDept.get(e.getKey());// get the patients by department id
                AssignmentState st = rebuildAssignmentState(e.getValue(), department, pmap);// rebuild the assignment state
                assigns.put(e.getKey(), st);// add the assignment state to the map
            }
        }
        long version = doc.getPersistVersion();
        return new WardHydration(departmentListOrdered, patientsByDept, assigns, version);
    }

    // convert the department to the persisted department
    // called from captureDraft
    private static PersistedDepartment toPersistedDepartment(Department d) {
        PersistedDepartment pd = new PersistedDepartment();
        pd.setId(d.getId());
        pd.setName(d.getName());
        List<String> wids = new ArrayList<>();// list of waiting patient ids
        if (d.getWaitingList() != null) {
            for (Patient p : d.getWaitingList()) {// iterate over the waiting list
                if (p != null && p.getId() != null) {
                    wids.add(p.getId());
                }
            }
        }

        pd.setWaitingPatientIds(wids);
        List<PersistedRoom> rooms = new ArrayList<>();// list of rooms to be persisted
        if (d.getRooms() != null) {
            for (Room r : d.getRooms()) {// iterate over the rooms
                rooms.add(toPersistedRoom(r));// convert the room to the persisted room
            }
        }
        pd.setRooms(rooms);
        return pd;
    }

    // convert the room to the persisted room
    // called from toPersistedDepartment
    private static PersistedRoom toPersistedRoom(Room r) {
        PersistedRoom pr = new PersistedRoom();
        pr.setId(r.getId());
        pr.setDepartmentId(r.getDepartmentId());
        pr.setCapacity(r.getCapacity());
        pr.setDistanceFromNurseStation(r.getDistanceFromNurseStation());
        pr.setHasNegativePressure(r.isHasNegativePressure());
        List<PersistedBed> beds = new ArrayList<>();
        if (r.getBeds() != null) {
            for (Bed b : r.getBeds()) {// iterate over the beds
                beds.add(toPersistedBed(b));// convert the bed to the persisted bed
            }
        }
        pr.setBeds(beds);
        return pr;
    }

    // convert the bed to the persisted bed
    // called from toPersistedRoom
    private static PersistedBed toPersistedBed(Bed b) {
        PersistedBed x = new PersistedBed();
        x.setId(b.getId());
        x.setRoomId(b.getRoomId());
        x.setType(b.getType());
        x.setHasVentilator(b.isHasVentilator());
        x.setBroken(b.isBroken());
        return x;
    }



    // convert the patient to the persisted patient
    // called from captureDraft
    private static PersistedPatient toPersistedPatient(Patient p) {
        PersistedPatient pp = new PersistedPatient();
        pp.setId(p.getId());
        pp.setPersonalDetails(copyPersonalDetailsForDto(p.getPersonalDetails()));
        pp.setClinicalData(copyClinicalForDto(p.getClinicalData()));
        pp.setStatus(p.getStatus());
        pp.setAdmittedAt(p.getAdmittedAt());
        pp.setTemporarilyUnavailable(p.isTemporarilyUnavailable());
        return pp;
    }

    // convert the personal details to the persisted personal details
    // called from toPersistedPatient
    private static PersistedPersonalDetails copyPersonalDetailsForDto(PersonalDetails pd) {
        if (pd == null) {
            return null;
        }
        PersistedPersonalDetails x = new PersistedPersonalDetails();
        x.setFirstName(pd.getFirstName());
        x.setLastName(pd.getLastName());
        x.setDateOfBirth(pd.getDateOfBirth());
        x.setGender(pd.getGender());
        return x;
    }
    // convert the clinical data to the persisted clinical data
    // called from toPersistedPatient
    private static PersistedClinicalData copyClinicalForDto(ClinicalData c) {
        if (c == null) {
            return null;
        }
        PersistedClinicalData x = new PersistedClinicalData();
        x.setRiskLevel(c.getRiskLevel());
        x.setSeverityScore(c.getSeverityScore());
        x.setNeedsVentilator(c.isNeedsVentilator());
        x.setRequiredBedType(c.getRequiredBedType());
        x.setWeightKg(c.getWeightKg());
        return x;
    }

    // convert the patients to the patient objects
    // called from hydrate
    private static LinkedHashMap<String, Patient> hydratePatientRegistry(
            String departmentIdIgnored,
            Map<String, PersistedPatient> deptBlob
    ) {

        LinkedHashMap<String, Patient> outById = new LinkedHashMap<>();// map of patient id to patient object
        if (deptBlob == null) {
            return outById;
        }
        for (PersistedPatient raw : deptBlob.values()) {// iterate over the patients
            Patient rebuilt = patientFromPersisted(raw);// convert the persisted patient to the patient object
            if (rebuilt != null && rebuilt.getId() != null) {
                outById.put(rebuilt.getId(), rebuilt);// add the patient to the map
            }
        }
        return outById;
    }

    // convert the persisted patient to the patient object
    // called from hydratePatientRegistry
    private static Patient patientFromPersisted(PersistedPatient pp) {
        if (pp == null || pp.getId() == null) {
            return null;
        }
        PersonalDetails pdetails = patientDetailsFromPersisted(pp.getPersonalDetails());// get the personal details from the persisted patient
        ClinicalData clinical = clinicalFromPersisted(pp.getClinicalData());// get the clinical data from the persisted patient
        Patient p = new Patient(pp.getId(), pdetails, clinical, pp.getAdmittedAt(), pp.isTemporarilyUnavailable());// create the patient object
        if (pp.getStatus() != null) {
            p.setStatus(pp.getStatus());
        }
        return p;
    }
    // convert the persisted clinical data to the clinical data object
    // called from patientFromPersisted
    private static ClinicalData clinicalFromPersisted(PersistedClinicalData c) {
        if (c == null) {
            return null;
        }
        return new ClinicalData(c.getRiskLevel(), c.getSeverityScore(),
                c.isNeedsVentilator(),
                c.getRequiredBedType(),
                c.getWeightKg());
    }

    // convert the persisted personal details to the personal details object (called from patientFromPersisted)
    private static PersonalDetails patientDetailsFromPersisted(PersistedPersonalDetails p) {
        if (p == null) {
            return null;
        }
        return new PersonalDetails(p.getFirstName(), p.getLastName(),
                p.getDateOfBirth(),
                p.getGender());
    }

    // convert the persisted department to the department object
    // called from hydrate
    private static Department hydrateDepartmentShell(PersistedDepartment pd, LinkedHashMap<String, Patient> knownPatientsById) {
        ArrayList<Room> roomsBuilt = new ArrayList<>();
        if (pd.getRooms() != null) {
            for (PersistedRoom pr : pd.getRooms()) {
                roomsBuilt.add(roomFromPersisted(pr));
            }
        }
        Department d = new Department(pd.getId(), pd.getName(), roomsBuilt, new ArrayList<>());
        List<Patient> waiting = new ArrayList<>();
        for (String pid : pd.getWaitingPatientIds()) {
            Patient px = pid == null ? null : knownPatientsById.get(pid);
            if (px != null) {
                waiting.add(px);
            }
        }
        d.setWaitingList(waiting);
        return d;
    }
    // convert the persisted room to the room object
    // called from hydrateDepartmentShell
    private static Room roomFromPersisted(PersistedRoom pr) {
        ArrayList<Bed> bb = new ArrayList<>();
        if (pr.getBeds() != null) {
            for (PersistedBed pb : pr.getBeds()) {
                bb.add(bedFromPersisted(pb));
            }
        }
        return new Room(pr.getId(), pr.getDepartmentId(), pr.getCapacity(), bb,
                pr.getDistanceFromNurseStation(),
                pr.isHasNegativePressure());
    }
    // convert the persisted bed to the bed object
    // called from roomFromPersisted
    private static Bed bedFromPersisted(PersistedBed pb) {
        return new Bed(pb.getId(), pb.getRoomId(), pb.getType(), pb.isHasVentilator(), pb.isBroken());
    }
    // places the objects by ids
    // convert the persisted assignment state to the assignment state object
    // called from hydrate
    private static AssignmentState rebuildAssignmentState(
            Map<String, String> patientIdToBedId,
            Department department,
            LinkedHashMap<String, Patient> patients) 
    {
        AssignmentState st = new AssignmentState();
        if (patientIdToBedId == null || department == null) {
            return st;
        }
        for (Map.Entry<String, String> a : patientIdToBedId.entrySet()) {// iterate over the assignments
            Patient p = patients == null ? null : patients.get(a.getKey());// get the patient by id
            Bed bed = department.findBedById(a.getValue());// get the bed by id
            if (p != null && bed != null) {
                st.assign(p, bed);// assign the patient to the bed
            }
        }
        return st;
    }

}
