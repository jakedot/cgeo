package cgeo.geocaching.wherigo;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.WherigoThingDetailsBinding;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.TextParam;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import cz.matejcik.openwig.Engine;
import cz.matejcik.openwig.Media;
import org.apache.commons.lang3.StringUtils;
import se.krka.kahlua.vm.LuaClosure;



/** Handles Wherigo/OpenWIG push dialog */
public class WherigoPushDialogProvider implements IWherigoDialogProvider {

    @NonNull private final String[] texts;
    @NonNull private final Media[] media;
    @NonNull private final String button1;
    @Nullable private final String button2;
    @Nullable private final LuaClosure callback;

    private WherigoThingDetailsBinding binding;
    private int page;


    /**
     * Handles display of an OpenWIG push dialog. The following description is copied for reference from OpenWIG code
     * <p>
     * Shows a multi-page dialog to the user.
     * <p>
     * If another dialog or input is open, it should be closed before displaying this dialog.
     * <p>
     * While the dialog is open, user should only be able to continue by clicking one of its buttons. Button1 flips to
     * next page, and when at end, invokes the callback with parameter "Button1", regardless of value of button1.
     * Button2 immediately closes the dialog and invokes callback with parameter "Button2".
     * If the dialog is closed by another API call, callback should be invoked with null parameter.
     * @param texts texts of individual pages of dialog
     * @param media pictures for individual pages of dialog
     * @param button1 label for primary button. If null, "OK" is used.
     * @param button2 label for secondary button. If null, the button is not displayed.
     * @param callback callback to call when closing the dialog, or null
     */
    WherigoPushDialogProvider(final String[] texts, final Media[] media, final String button1, final String button2, final LuaClosure callback) {
        this.texts = texts == null || texts.length == 0 ? new String[]{"no text provided" } :
                texts;
                // (texts.length == 1 ? new String[]{texts[0], "Second page test" } : texts); //´this line can be commented in for testing purposes
        this.media = media == null ? new Media[0] : media;
        this.button1 = StringUtils.isBlank(button1) ? "OK" : button1.trim();
        this.button2 = StringUtils.isBlank(button2) ? null : button2.trim();
        this.callback = callback;
        this.page = 0;
    }

    @Override
    public Dialog createDialog(final Activity activity) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.cgeo_fullScreenDialog);
        binding = WherigoThingDetailsBinding.inflate(LayoutInflater.from(activity));
        final AlertDialog dialog = builder.create();
        dialog.setView(binding.getRoot());

        refreshGui();

        final List<Boolean> options = button2 == null ? Collections.singletonList(TRUE) : Arrays.asList(TRUE, FALSE);

        WherigoUtils.setViewActions(options, binding.dialogActionlist, item -> TRUE.equals(item) ?
                TextParam.text(button1).setImage(ImageParam.id(R.drawable.ic_menu_done)) :
                TextParam.text(button2).setImage(ImageParam.id(R.drawable.ic_menu_cancel)),
            item -> {
                if (FALSE.equals(item)) {
                    WherigoDialogManager.get().clear();
                    if (callback != null) {
                        Engine.invokeCallback(callback, "Button2");
                    }
                } else if (this.page + 1 < texts.length) {
                    this.page ++;
                    refreshGui();
                } else {
                    WherigoDialogManager.get().clear();
                    if (callback != null) {
                        Engine.invokeCallback(callback, "Button1");
                    }
                }
            }
        );

        return dialog;
    }

    private void refreshGui() {
        final int page = this.page < 0 || this.page >= texts.length ? 0 : this.page;
        final String message = this.texts[this.page];
        final Media media = this.media == null || this.media.length == 0 ? null : (page >= this.media.length ? this.media[0] : this.media[page]);

        binding.description.setText(WherigoGame.get().toDisplayText(message));
        if (media != null) {
            binding.media.setMedia(media);
        }
        binding.debugBox.setVisibility(WherigoGame.get().isDebugModeForCartridge() ? VISIBLE : GONE);
        if (WherigoGame.get().isDebugModeForCartridge()) {
            binding.debugInfo.setText("DebugInfo: Wherigo Dialog");
        }

        binding.headerInformation.setVisibility(texts.length > 1 ? View.VISIBLE : View.GONE);
        binding.headerInformation.setText("Page " + (page + 1) + "/" + texts.length);
    }

}
