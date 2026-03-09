package uz.ramazon.qarshi;

import java.util.Calendar;

/**
 * Qarshi shahri uchun namoz vaqtlari hisoblash
 * Koordinatalar: 38.8667°N, 65.7833°E, UTC+5
 * Usul: O'zbekiston Musulmonlar Idorasi (Hanafi, Fajr 18°, Isha 17°)
 */
public class PrayerTimes {

    static final double LAT = 38.8667;
    static final double LON = 65.7833;
    static final int TZ = 5;

    // Ramazon 2026 uchun aniq jadval (sajda.com dan tekshirilgan)
    static final String[][] RAMAZON = {
        {"2026-02-19","06:10","07:23","12:51","16:37","18:23","19:32"},
        {"2026-02-20","06:09","07:22","12:51","16:38","18:25","19:33"},
        {"2026-02-21","06:07","07:21","12:50","16:39","18:26","19:34"},
        {"2026-02-22","06:06","07:20","12:50","16:40","18:27","19:35"},
        {"2026-02-23","06:05","07:18","12:50","16:41","18:28","19:36"},
        {"2026-02-24","06:04","07:17","12:50","16:42","18:29","19:37"},
        {"2026-02-25","06:02","07:15","12:50","16:43","18:30","19:38"},
        {"2026-02-26","06:01","07:14","12:50","16:44","18:31","19:39"},
        {"2026-02-27","06:00","07:13","12:50","16:45","18:32","19:40"},
        {"2026-02-28","05:58","07:11","12:49","16:46","18:33","19:41"},
        {"2026-03-01","05:57","07:10","12:49","16:46","18:34","19:42"},
        {"2026-03-02","05:55","07:08","12:49","16:47","18:35","19:43"},
        {"2026-03-03","05:54","07:07","12:49","16:48","18:36","19:44"},
        {"2026-03-04","05:53","07:05","12:49","16:49","18:37","19:45"},
        {"2026-03-05","05:51","07:04","12:48","16:50","18:38","19:46"},
        {"2026-03-06","05:50","07:02","12:48","16:51","18:39","19:47"},
        {"2026-03-07","05:48","07:01","12:48","16:52","18:41","19:48"},
        {"2026-03-08","05:47","06:59","12:48","16:53","18:42","19:49"},
        {"2026-03-09","05:45","06:58","12:47","16:53","18:43","19:50"},
        {"2026-03-10","05:44","06:56","12:47","16:54","18:44","19:51"},
        {"2026-03-11","05:42","06:55","12:47","16:55","18:45","19:53"},
        {"2026-03-12","05:40","06:53","12:47","16:56","18:46","19:54"},
        {"2026-03-13","05:39","06:52","12:46","16:57","18:47","19:55"},
        {"2026-03-14","05:37","06:50","12:46","16:57","18:48","19:56"},
        {"2026-03-15","05:36","06:49","12:46","16:58","18:49","19:57"},
        {"2026-03-16","05:34","06:47","12:46","16:59","18:50","19:58"},
        {"2026-03-17","05:33","06:46","12:45","17:00","18:51","19:59"},
        {"2026-03-18","05:31","06:44","12:45","17:00","18:52","20:00"},
        {"2026-03-19","05:29","06:42","12:45","17:01","18:53","20:01"},
        {"2026-03-20","05:28","06:41","12:44","17:02","18:54","20:02"},
    };

    // Offset tuzatmasi (O'zbekiston usuli, Mart oyiga asoslangan)
    static final int[] OFFSETS = {16, -1, 0, 1, 6, -10};

    public static int[] getTodayTimes() {
        Calendar c = Calendar.getInstance();
        return getTimesForDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH)+1, c.get(Calendar.DAY_OF_MONTH));
    }

    public static int[] getTimesForDate(int year, int month, int day) {
        // Ramazon jadvali dan tekshiramiz
        String key = String.format("%04d-%02d-%02d", year, month, day);
        for (String[] row : RAMAZON) {
            if (row[0].equals(key)) {
                int[] times = new int[6];
                for (int i = 0; i < 6; i++) {
                    times[i] = parseTime(row[i+1]);
                }
                return times;
            }
        }
        // Matematik hisob
        return calcTimes(year, month, day);
    }

    static int[] calcTimes(int year, int month, int day) {
        int y = year, m = month;
        if (m <= 2) { y--; m += 12; }
        int A = y / 100, B = 2 - A + A / 4;
        double JD = (int)(365.25*(y+4716)) + (int)(30.6001*(m+1)) + day + B - 1524.5;
        double T = (JD - 2451545.0) / 36525.0;

        double L0 = (280.46646 + 36000.76983*T) % 360;
        double M  = (357.52911 + 35999.05029*T - 0.0001537*T*T) % 360;
        double Mr = Math.toRadians(M);
        double C  = (1.914602 - 0.004817*T)*Math.sin(Mr) + 0.019993*Math.sin(2*Mr) + 0.000289*Math.sin(3*Mr);
        double SL = L0 + C;
        double omega = Math.toRadians(125.04 - 1934.136*T);
        double lam = Math.toRadians(SL - 0.00569 - 0.00478*Math.sin(omega));
        double eps = Math.toRadians(23.439291 - 0.013004*T + 0.00256*Math.cos(omega));
        double decl = Math.asin(Math.sin(eps)*Math.sin(lam));

        double y3 = Math.pow(Math.tan(eps/2), 2);
        double L0r = Math.toRadians(L0);
        double EqT = 4*Math.toDegrees(
            y3*Math.sin(2*L0r) - 2*0.016709*Math.sin(Mr) +
            4*0.016709*y3*Math.sin(Mr)*Math.cos(2*L0r) -
            0.5*y3*y3*Math.sin(4*L0r) -
            1.25*0.016709*0.016709*Math.sin(2*Mr)
        ) / 60.0;

        double Dhuhr = 12.0 - EqT - LON/15.0 + TZ;
        double latr = Math.toRadians(LAT);

        double[] rawTimes = new double[6];
        rawTimes[0] = Dhuhr - ha(latr, decl, -18.0);      // Fajr
        rawTimes[1] = Dhuhr - ha(latr, decl, -0.8333);    // Sunrise
        rawTimes[2] = Dhuhr;                                // Dhuhr
        double asrA = Math.toDegrees(Math.atan(1.0/(2+Math.tan(Math.abs(latr-decl)))));
        rawTimes[3] = Dhuhr + ha(latr, decl, asrA);       // Asr
        rawTimes[4] = Dhuhr + ha(latr, decl, -0.8333);   // Maghrib
        rawTimes[5] = Dhuhr + ha(latr, decl, -17.0);     // Isha

        int[] result = new int[6];
        for (int i = 0; i < 6; i++) {
            double t = ((rawTimes[i] + OFFSETS[i]/60.0) % 24 + 24) % 24;
            result[i] = (int)(t * 60 + 0.5); // daqiqada
        }
        return result;
    }

    static double ha(double lat, double decl, double angle) {
        double cosH = (Math.sin(Math.toRadians(angle)) - Math.sin(lat)*Math.sin(decl))
                    / (Math.cos(lat)*Math.cos(decl));
        cosH = Math.max(-1.0, Math.min(1.0, cosH));
        return Math.toDegrees(Math.acos(cosH)) / 15.0;
    }

    static int parseTime(String t) {
        String[] p = t.split(":");
        return Integer.parseInt(p[0])*60 + Integer.parseInt(p[1]);
    }

    static String formatTime(int minutes) {
        return String.format("%02d:%02d", minutes/60, minutes%60);
    }
}
