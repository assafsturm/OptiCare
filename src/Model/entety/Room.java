package Model.entety;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Room {
    // --- זיהוי ---
    private String id;              // מספר חדר (למשל "Room-102")
    private String departmentId;    // שיוך למחלקה (למשל "Internal-A")

    // --- קיבולת ותכולה ---
    private int capacity;           // מקסימום מיטות
    private List<Bed> beds = new ArrayList<>();

    // --- תכונות פיזיות (קריטי לאופטימיזציה) ---
    private double distanceFromNurseStation; // במטרים. חולים קשים צריכים מרחק נמוך.
    private boolean hasNegativePressure;     // לחץ שלילי (חובה לחולי שחפת/קורונה קשה)
    private boolean hasBathroom;             // האם יש שירותים בחדר? (נוחות)

    // --- לוגיקה ---
    // פונקציות עזר (כמו קודם)
    public void addBed(Bed bed) {
        if (beds.size() < capacity) {
            beds.add(bed);
        } else {
            throw new RuntimeException("Room full");
        }
    }
}
