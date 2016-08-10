package lite.navi.view;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;

import lite.navi.R;

/**
 * @author ylscat
 *         Date: 2016-08-07 10:31
 */
public class Utils {
    public static Dialog createWaitingDialog(Activity activity) {
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        dialog.setContentView(R.layout.waiting);
        dialog.setCancelable(false);
        return dialog;
    }
}
