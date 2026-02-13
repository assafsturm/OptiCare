package Model.entety;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Department {
    // --- זיהוי ---
    private String id;              // מזהה (למשל: "INT-A")
    private String name;            // שם (למשל: "Internal Medicine A")

    // --- המבנה הפיזי (היררכיה) ---
    // המחלקה מכילה את רשימת החדרים
    @Builder.Default
    private List<Room> rooms = new ArrayList<>();

    // --- המטופלים (התור) ---
    // רשימת הממתינים לשיבוץ במחלקה הזו ספציפית
    @Builder.Default
    private List<Patient> waitingList = new ArrayList<>();

    // --- פונקציות עזר (נוחות לאלגוריתם) ---

    /**
     * מחזירה את כל המיטות במחלקה ברשימה אחת שטוחה
     * (חוסך לאלגוריתם לולאות כפולות כל הזמן)
     */
    public List<Bed> getAllBeds() {
        List<Bed> allBeds = new ArrayList<>();
        for (Room room : rooms) {
            allBeds.addAll(room.getBeds());
        }
        return allBeds;
    }

    /**
     * מחזירה את סך הקיבולת של המחלקה
     */
    public int getTotalCapacity() {
        return rooms.stream().mapToInt(Room::getCapacity).sum();
    }

    public void addRoom(Room room) {
        this.rooms.add(room);
    }
}