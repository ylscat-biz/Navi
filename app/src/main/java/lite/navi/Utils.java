package lite.navi;

import android.location.Location;
import android.location.LocationManager;

/**
 * @author ylscat
 *         Date: 2016-08-19 22:34
 */
public class Utils {
    public static boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        if(location.getAccuracy() > 3000)
            return false;

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > 3000;
        boolean isSignificantlyOlder = timeDelta < -3000;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isSignificantlyMoreAccurate = accuracyDelta < -20;
        boolean isSignificantlyLessAccurate = accuracyDelta > 20;

        // Determine location quality using a combination of timeliness and accuracy
        if(isSignificantlyMoreAccurate)
            return true;
        if(isNewer && !isSignificantlyLessAccurate)
            return true;

        return false;
    }

    public static boolean isBetterLocation2(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        if(location.getAccuracy() > 3000)
            return false;

        return true;

//        // Check whether the new location fix is newer or older
//        long timeDelta = location.getTime() - currentBestLocation.getTime();
//        boolean isSignificantlyNewer = timeDelta > 5000;
//        boolean isSignificantlyOlder = timeDelta < -5000;
//        boolean isNewer = timeDelta > 0;
//
//        // If it's been more than two minutes since the current location, use the new location
//        // because the user has likely moved
//        if (isSignificantlyNewer) {
//            return true;
//            // If the new location is more than two minutes older, it must be worse
//        } else if (isSignificantlyOlder) {
//            return false;
//        }
//
//        // Check whether the new location fix is more or less accurate
//        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
//        boolean isSignificantlyMoreAccurate = accuracyDelta < -20;
//        boolean isSignificantlyLessAccurate = accuracyDelta > 20;
//
//        // Determine location quality using a combination of timeliness and accuracy
//        if(isSignificantlyMoreAccurate)
//            return true;
//        if(isNewer && !isSignificantlyLessAccurate)
//            return true;
//
//        return false;
    }
}
