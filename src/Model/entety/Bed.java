package Model.entety;


import Model.enums.BedType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bed {
    // --- זיהוי ---
    private String id;              // ברקוד המיטה
    private String roomId;          // איפה היא נמצאת

    // --- יכולות ---
    private BedType type;           // סוג המיטה (רגילה/ICU/בריאטרית)
    private boolean hasVentilator;  // האם יש לה חיבור לחמצן/מנשם?
    private boolean isBroken;       // סימולציה של תקלה - אי אפשר לשבץ לפה
}