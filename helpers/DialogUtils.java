package td.teladoum.baobab.helpers;

import android.app.Dialog;

public class DialogUtils {

    /**
     * Safely dismisses a Dialog to prevent IllegalStateException when the lifecycle state
     * of the Dialog (or ComponentDialog/AlertDialog) is INITIALIZED or when the Activity is finishing.
     *
     * @param dialog The Dialog instance to dismiss safely.
     */
    public static void safeDismiss(Dialog dialog) {
        if (dialog != null && dialog.isShowing()) {
            try {
                dialog.dismiss();
            } catch (Exception ignored) {
                // Ignore lifecycle or window detachment exceptions during dismiss
            }
        }
    }
}
