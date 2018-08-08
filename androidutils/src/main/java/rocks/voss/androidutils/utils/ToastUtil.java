package rocks.voss.androidutils.utils;

import android.content.Context;
import android.widget.Toast;

public class ToastUtil {
    public static void createLongToast(Context context, String msg) {
        createToast(context, msg, Toast.LENGTH_LONG);
    }

    public static void createShortToast(Context context, String msg) {
        createToast(context, msg, Toast.LENGTH_SHORT);
    }

    public static void createToast(Context context, String msg, int length) {
        Toast.makeText(context, msg, length).show();
    }
}
