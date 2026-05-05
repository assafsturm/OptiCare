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

/** Bidirectional mapper between persisted JSON envelope and ward runtime graphs. */
public final class WardStateMapper {

    private WardStateMapper() {
    }

    public record WardHydration(
            List<Department> departments,
            LinkedHashMap<String, LinkedHashMap<String, Patient>> patientsByDepartmentId,
            LinkedHashMap<String, AssignmentState> assignmentStates,
            long persistVersionLoaded
    ) {}

    public static WardStateDocument captureDraft(
            List<Department> departments,
            Map<String, Map<String, Patient>> patientByDept,
            Map<String, AssignmentState> assignmentByDeptId
    ) {
        WardStateDocument doc = new WardStateDocument();
        doc.setSchemaVersion(WardStateDocument.CURRENT_SCHEMA_VERSION);
        LinkedHashMap<String, Map<String, PersistedPatient>> pmap = new LinkedHashMap<>();
        LinkedHashMap<String, Map<String, String>> amap = new LinkedHashMap<>();
        List<PersistedDepartment> pdepts = new ArrayList<>();

        for (Department d : departments) {
            pdepts.add(toPersistedDepartment(d));

            LinkedHashMap<String, PersistedPatient> rowPatients = new LinkedHashMap<>();
            Map<String, Patient> src = patientByDept == null ? null : patientByDept.get(d.getId());
            if (src != null) {
                for (Patient patient : src.values()) {
                    if (patient != null && patient.getId() != null) {
                        rowPatients.put(patient.getId(), toPersistedPatient(patient));
                    }
                }
            }
            pmap.put(d.getId(), rowPatients);

            LinkedHashMap<String, String> rowAssign = new LinkedHashMap<>();
            AssignmentState ast = assignmentByDeptId == null ? null : assignmentByDeptId.get(d.getId());
            if (ast != null) {
                for (Map.Entry<String, Bed> e : ast.getAssignments().entrySet()) {
                    Bed bed = e.getValue();
                    if (e.getKey() != null && bed != null && bed.getId() != null) {
                        rowAssign.put(e.getKey(), bed.getId());
                    }
                }
            }
            amap.put(d.getId(), rowAssign);
        }

        doc.setDepartments(new ArrayList<>(pdepts));
        doc.setPatientsByDepartmentId(pmap);
        doc.setAssignmentsByDepartmentId(amap);
        return doc;
    }

    public static WardHydration hydrate(WardStateDocument doc) {
        Objects.requireNonNull(doc, "doc");

        if (doc.getSchemaVersion() != WardStateDocument.CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported ward state schema version " + doc.getSchemaVersion()
                            + "; expected " + WardStateDocument.CURRENT_SCHEMA_VERSION);
        }

        LinkedHashMap<String, LinkedHashMap<String, Patient>> patientsByDept = new LinkedHashMap<>();
        LinkedHashMap<String, Department> departmentsById = new LinkedHashMap<>();

        Map<String, Map<String, PersistedPatient>> rawPatients = doc.getPatientsByDepartmentId();

        List<Department> departmentListOrdered = new ArrayList<>();
        for (PersistedDepartment pd : doc.getDepartments()) {

            LinkedHashMap<String, Patient> builtPatients =
                    hydratePatientRegistry(pd.getId(), rawPatients != null ? rawPatients.get(pd.getId()) : null);
            patientsByDept.put(pd.getId(), builtPatients);

            Department d = hydrateDepartmentShell(pd, builtPatients);

            departmentsById.put(d.getId(), d);
            departmentListOrdered.add(d);



        }



        LinkedHashMap<String, AssignmentState> assigns = new LinkedHashMap<>();

        Map<String, Map<String, String>> rawAssign = doc.getAssignmentsByDepartmentId();

        if (rawAssign != null) {

            for (Map.Entry<String, Map<String, String>> e : rawAssign.entrySet()) {


                Department department = departmentsById.get(e.getKey());

                LinkedHashMap<String, Patient> pmap = patientsByDept.get(e.getKey());

                AssignmentState st = rebuildAssignmentState(e.getValue(), department, pmap);



                assigns.put(e.getKey(), st);

            }



        }



        long version = doc.getPersistVersion();


        return new WardHydration(departmentListOrdered, patientsByDept, assigns, version);

    }



    private static PersistedDepartment toPersistedDepartment(Department d) {



        PersistedDepartment pd = new PersistedDepartment();



        pd.setId(d.getId());



        pd.setName(d.getName());

        List<String> wids = new ArrayList<>();

        if (d.getWaitingList() != null) {


            for (Patient p : d.getWaitingList()) {


                if (p != null && p.getId() != null) {


                    wids.add(p.getId());



                }







            }



        }



        pd.setWaitingPatientIds(wids);

        List<PersistedRoom> rooms = new ArrayList<>();

        if (d.getRooms() != null) {


            for (Room r : d.getRooms()) {


                rooms.add(toPersistedRoom(r));


            }


        }



        pd.setRooms(rooms);

        return pd;

    }



    private static PersistedRoom toPersistedRoom(Room r) {


        PersistedRoom pr = new PersistedRoom();


        pr.setId(r.getId());



        pr.setDepartmentId(r.getDepartmentId());

        pr.setCapacity(r.getCapacity());



        pr.setDistanceFromNurseStation(r.getDistanceFromNurseStation());



        pr.setHasNegativePressure(r.isHasNegativePressure());

        pr.setHasBathroom(r.isHasBathroom());



        List<PersistedBed> beds = new ArrayList<>();

        if (r.getBeds() != null) {


            for (Bed b : r.getBeds()) {



                beds.add(toPersistedBed(b));

            }







        }



        pr.setBeds(beds);

        return pr;

    }



    private static PersistedBed toPersistedBed(Bed b) {


        PersistedBed x = new PersistedBed();


        x.setId(b.getId());



        x.setRoomId(b.getRoomId());



        x.setType(b.getType());



        x.setHasVentilator(b.isHasVentilator());



        x.setBroken(b.isBroken());

        return x;

    }



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



    private static LinkedHashMap<String, Patient> hydratePatientRegistry(


            String departmentIdIgnored,


            Map<String, PersistedPatient> deptBlob





    ) {

        LinkedHashMap<String, Patient> outById = new LinkedHashMap<>();

        if (deptBlob == null) {


            return outById;

        }



        for (PersistedPatient raw : deptBlob.values()) {


            Patient rebuilt = patientFromPersisted(raw);

            if (rebuilt != null && rebuilt.getId() != null) {


                outById.put(rebuilt.getId(), rebuilt);

            }







        }



        return outById;



    }



    private static Patient patientFromPersisted(PersistedPatient pp) {



        if (pp == null || pp.getId() == null) {

            return null;

        }



        PersonalDetails pdetails = patientDetailsFromPersisted(pp.getPersonalDetails());

        ClinicalData clinical = clinicalFromPersisted(pp.getClinicalData());

        Patient p = new Patient(pp.getId(), pdetails, clinical, pp.getAdmittedAt(), pp.isTemporarilyUnavailable());

        if (pp.getStatus() != null) {


            p.setStatus(pp.getStatus());

        }






        return p;

    }



    private static ClinicalData clinicalFromPersisted(PersistedClinicalData c) {



        if (c == null) {

            return null;

        }



        return new ClinicalData(c.getRiskLevel(), c.getSeverityScore(),




                c.isNeedsVentilator(),




                c.getRequiredBedType(),





                c.getWeightKg());

    }



    private static PersonalDetails patientDetailsFromPersisted(PersistedPersonalDetails p) {


        if (p == null) {


            return null;

        }



        return new PersonalDetails(p.getFirstName(), p.getLastName(),




                p.getDateOfBirth(),





                p.getGender());

    }



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



    private static Room roomFromPersisted(PersistedRoom pr) {


        ArrayList<Bed> bb = new ArrayList<>();

        if (pr.getBeds() != null) {

            for (PersistedBed pb : pr.getBeds()) {


                bb.add(bedFromPersisted(pb));


            }



        }






        return new Room(pr.getId(), pr.getDepartmentId(), pr.getCapacity(), bb,

                pr.getDistanceFromNurseStation(),

                pr.isHasNegativePressure(),

                pr.isHasBathroom());

    }



    private static Bed bedFromPersisted(PersistedBed pb) {


        return new Bed(pb.getId(), pb.getRoomId(), pb.getType(), pb.isHasVentilator(), pb.isBroken());

    }



    private static AssignmentState rebuildAssignmentState(


            Map<String, String> patientIdToBedId,


            Department department,





            LinkedHashMap<String, Patient> patients




    ) {

        AssignmentState st = new AssignmentState();

        if (patientIdToBedId == null || department == null) {


            return st;

        }

        for (Map.Entry<String, String> a : patientIdToBedId.entrySet()) {


            Patient p = patients == null ? null : patients.get(a.getKey());

            Bed bed = department.findBedById(a.getValue());

            if (p != null && bed != null) {


                st.assign(p, bed);

            }







        }



        return st;

    }

}
