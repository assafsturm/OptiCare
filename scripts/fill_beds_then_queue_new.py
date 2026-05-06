"""
One-off: fill free beds in D28/D29 with new legal synthetic patients, then move only
those new patients to the waiting list (matches HardConstraints + RiskMatrix defaults).

Usage: py -3 scripts/fill_beds_then_queue_new.py [path/to/ward-state.json]
Default: %USERPROFILE%\\.opticare\\ward-state.json
"""
from __future__ import annotations

import json
import sys
from collections import defaultdict
from datetime import datetime, timezone
from pathlib import Path

BIG_M = 1_000_000.0

TARGET_DEPTS = ("D28", "D29")
ID_PREFIX = "NX"  # new synthetic batch — avoids collisions with P* ...

# (riskLevel, severity, needsVentilator, requiredBedType, weightKg)
# requiredBedType / weight None = no constraint
PROFILES: list[tuple[str, int, bool, str | None, int | None]] = [
    ("CLEAN", 5, False, None, None),
    ("IMMUNO_COMPROMISED", 4, False, None, None),
    ("RESPIRATORY", 4, False, None, None),
    ("UNKNOWN", 3, False, None, None),
    ("INFECTIOUS", 5, False, None, None),
]


def forbidden_pair(a: str | None, b: str | None) -> bool:
    if a is None or b is None:
        return False
    # Mirror RiskMatrix.getDefaultPenalty hard cases (penalty == bigM)
    fs = {
        frozenset({"INFECTIOUS", "IMMUNO_COMPROMISED"}),
        frozenset({"RESPIRATORY", "IMMUNO_COMPROMISED"}),
        frozenset({"INFECTIOUS", "CLEAN"}),
    }
    return frozenset({a, b}) in fs


def clinical_legal(risk: str, bed: dict, room: dict) -> bool:
    if bed.get("broken"):
        return False
    # PatientRiskPolicy.requiresNegativePressureRoom: only INFECTIOUS
    if risk == "INFECTIOUS" and not room.get("hasNegativePressure"):
        return False
    # No required bed type / bariatric / ventilator in our profiles
    if risk != "INFECTIOUS":  # all synthetic profiles have no requiredBedType
        pass
    return True


def cohort_ok(new_risk: str, existing_risks: list[str]) -> bool:
    for r in existing_risks:
        if forbidden_pair(new_risk, r):
            return False
    return True


def load_dept(doc: dict, dept_id: str) -> dict | None:
    for d in doc.get("departments", []):
        if d.get("id") == dept_id:
            return d
    return None


def main() -> None:
    path = Path(sys.argv[1]) if len(sys.argv) > 1 else Path.home() / ".opticare" / "ward-state.json"
    if not path.is_file():
        raise SystemExit(f"Missing file: {path}")

    data = json.loads(path.read_text(encoding="utf-8"))
    patients_root = data.setdefault("patientsByDepartmentId", {})
    assign_root = data.setdefault("assignmentsByDepartmentId", {})

    now = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.") + f"{datetime.now(timezone.utc).microsecond // 1000:03d}Z"

    id_counter = 0

    def next_id() -> str:
        nonlocal id_counter
        id_counter += 1
        return f"{ID_PREFIX}{id_counter:06d}"

    all_new_ids: dict[str, list[str]] = {d: [] for d in TARGET_DEPTS}

    for dept_id in TARGET_DEPTS:
        dept = load_dept(data, dept_id)
        if dept is None:
            print(f"Skip: department {dept_id} not found")
            continue

        bed_by_id: dict[str, dict] = {}
        room_by_id: dict[str, dict] = {}
        beds_in_room: dict[str, list[str]] = defaultdict(list)

        for room in dept.get("rooms", []):
            rid = room["id"]
            room_by_id[rid] = room
            for bed in room.get("beds", []):
                bid = bed["id"]
                bed_by_id[bid] = bed
                beds_in_room[rid].append(bid)

        for rid in beds_in_room:
            beds_in_room[rid].sort()

        assign = dict(assign_root.get(dept_id) or {})
        occupied_beds = set(assign.values())
        patients = dict(patients_root.get(dept_id) or {})

        free_beds: list[str] = []
        for rid in sorted(beds_in_room.keys()):
            for bid in beds_in_room[rid]:
                if bid not in occupied_beds:
                    free_beds.append(bid)

        patient_risk_cache: dict[str, str] = {}
        for pid, blob in patients.items():
            cd = blob.get("clinicalData") or {}
            patient_risk_cache[pid] = cd.get("riskLevel") or "UNKNOWN"

        def risks_in_room_for_state(room_id: str, pending_room: dict[str, str]) -> list[str]:
            out: list[str] = []
            for bid in beds_in_room[room_id]:
                pid = pending_room.get(bid)
                if pid and pid in patient_risk_cache:
                    out.append(patient_risk_cache[pid])
            return out

        pending_assign = dict(assign)
        pending_room_maps: dict[str, dict[str, str]] = defaultdict(dict)
        for pid, bid in pending_assign.items():
            room_id = bed_by_id[bid]["roomId"]
            pending_room_maps[room_id][bid] = pid

        placed = 0
        for bid in free_beds:
            bed = bed_by_id[bid]
            room_id = bed["roomId"]
            room = room_by_id[room_id]
            pr_map = pending_room_maps[room_id]

            chosen = None
            for risk, sev, nv, req_bt, wkg in PROFILES:
                existing = risks_in_room_for_state(room_id, pr_map)
                if not cohort_ok(risk, existing):
                    continue
                bcopy = {**bed, "type": bed.get("type")}
                if not clinical_legal(risk, bcopy, room):
                    continue
                chosen = (risk, sev, nv, req_bt, wkg)
                break

            if chosen is None:
                print(f"WARNING: could not place any legal synthetic patient on bed {bid} ({dept_id})")
                continue

            risk, sev, nv, req_bt, wkg = chosen
            pid = next_id()
            all_new_ids[dept_id].append(pid)

            patients[pid] = {
                "id": pid,
                "personalDetails": None,
                "clinicalData": {
                    "riskLevel": risk,
                    "severityScore": sev,
                    "needsVentilator": nv,
                    "requiredBedType": req_bt,
                    "weightKg": wkg,
                },
                "status": "ASSIGNED",
                "admittedAt": now,
                "temporarilyUnavailable": False,
            }
            patient_risk_cache[pid] = risk
            pending_assign[pid] = bid
            pr_map[bid] = pid
            placed += 1

        patients_root[dept_id] = patients
        assign_root[dept_id] = pending_assign
        print(f"{dept_id}: filled {placed} free beds (free list had {len(free_beds)})")

    # Move only new synthetic patients to waiting list
    for dept_id in TARGET_DEPTS:
        new_ids = all_new_ids[dept_id]
        if not new_ids:
            continue
        dept = load_dept(data, dept_id)
        if dept is None:
            continue
        waiting = list(dept.get("waitingPatientIds") or [])
        seen = set(waiting)
        assign = dict(assign_root.get(dept_id) or {})
        patients = dict(patients_root.get(dept_id) or {})

        for pid in new_ids:
            if pid in assign:
                del assign[pid]
            blob = patients.get(pid)
            if blob:
                blob["status"] = "WAITING"
            if pid not in seen:
                waiting.append(pid)
                seen.add(pid)

        dept["waitingPatientIds"] = waiting
        assign_root[dept_id] = assign
        patients_root[dept_id] = patients

    data["persistVersion"] = int(data.get("persistVersion") or 0) + 1

    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"Wrote {path} (persistVersion={data['persistVersion']})")
    for d in TARGET_DEPTS:
        print(f"  {d}: new patient IDs (now waiting): {len(all_new_ids[d])}")


if __name__ == "__main__":
    main()
