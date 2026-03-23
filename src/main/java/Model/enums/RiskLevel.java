package Model.enums;

public enum RiskLevel {
    /**
     * ירוק: חולה "נקי".
     * מיועד למחלקות כירורגיות, אורתופדיה, או פנימית רגילה.
     * אין סכנת הדבקה.
     */
    CLEAN,

    /**
     * כתום: חשד נשימתי.
     * חולה שמשתעל או עם חום, אבל עוד לא קיבל תשובה של בדיקה.
     * צריך להפריד אותו מ-CLEAN, אבל אולי אפשר עם אחרים כמוהו.
     */
    RESPIRATORY,

    /**
     * אדום: זיהומי ודאי.
     * חולה קורונה, שפעת, או חיידק עמיד.
     * חייב להיות בחדר מבודד או עם חולים שיש להם בדיוק אותו דבר (Cohort).
     */
    INFECTIOUS,

    /**
     * כחול: מדוכא חיסון (Immuno-Compromised).
     * חולה אונקולוגי או מושתל איברים.
     * הוא לא מדבק אחרים, אבל אחרים יהרגו אותו.
     * חייב להיות בחדר סטרילי לגמרי (אסור לשים לידו RESPIRATORY).
     */
    IMMUNO_COMPROMISED,

    /**
     * Unknown / missing risk data. Used for null-risk policy: conservative finite cohorting penalties,
     * not treated as infectious for hard isolation rules unless explicitly set.
     */
    UNKNOWN;

    /**
     * Priority for global waiting-list min-heap (smaller = higher priority).
     * IMMUNO_COMPROMISED=0, INFECTIOUS=1, RESPIRATORY=2, CLEAN=3, UNKNOWN=4.
     */
    public int waitingQueuePriority() {
        return switch (this) {
            case IMMUNO_COMPROMISED -> 0;
            case INFECTIOUS -> 1;
            case RESPIRATORY -> 2;
            case CLEAN -> 3;
            case UNKNOWN -> 4;
        };
    }
}