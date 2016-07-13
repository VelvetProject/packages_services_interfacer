package masquerade.substratum.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import masquerade.substratum.util.ReadOverlaysFile;
import masquerade.substratum.util.Root;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class Helper extends BroadcastReceiver {

    private List<String> state5overlays = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("Masquerade",
                "BroadcastReceiver has accepted Substratum's commands and is running now...");
        Root.requestRootAccess();

        if (intent.getStringExtra("substratum-check") != null) {
            if (intent.getStringExtra("substratum-check").equals("masquerade-ball")) {
                Intent runCommand = new Intent();
                runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                runCommand.setAction("projekt.substratum.MASQUERADE_BALL");
                runCommand.putExtra("substratum-check", "masquerade-ball");
                context.sendBroadcast(runCommand);
                Log.d("Masquerade",
                        "BroadcastReceiver was triggered to check for system integrity and " +
                                "service activation.");

            }
        }

        if (intent.getStringArrayListExtra("pm-uninstall") != null) {
            uninstall_handler(intent, "pm-uninstall", false);
        }

        if (intent.getStringArrayListExtra("pm-uninstall-specific") != null) {
            uninstall_handler(intent, "pm-uninstall-specific", true);
        }

        if (intent.getStringExtra("om-commands") != null) {
            Log.d("Masquerade", "Running command: \"" +
                    intent.getStringExtra("om-commands") + "\"");
            Root.runCommand(intent.getStringExtra("om-commands"));
        }
    }

    private boolean checkIfPackageActivated(String package_name) {
        return (state5overlays.contains(package_name));
    }

    private void uninstall_handler(Intent intent, String inheritor, boolean specific) {
        try {
            String final_commands_disable = "";
            String final_commands_uninstall = "";

            Root.runCommand(
                    "pm grant masquerade.substratum android.permission.READ_EXTERNAL_STORAGE");
            Root.runCommand(
                    "pm grant masquerade.substratum android.permission.WRITE_EXTERNAL_STORAGE");

            ArrayList<String> packages_to_uninstall =
                    new ArrayList<>(intent.getStringArrayListExtra(inheritor));
            File current_overlays = new File(Environment
                    .getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml");
            if (current_overlays.exists()) {
                Root.runCommand("rm " + Environment
                        .getExternalStorageDirectory().getAbsolutePath() +
                        "/.substratum/current_overlays.xml");
            }
            Root.runCommand("cp /data/system/overlays.xml " +
                    Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml");
            String[] state5initial = {Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml", "5"};
            state5overlays = ReadOverlaysFile.main(state5initial);

            for (int i = 0; i < packages_to_uninstall.size(); i++) {
                String current = packages_to_uninstall.get(i);

                Log.d("Masquerade", "Intent received to purge referendum package file \"" +
                        current + "\"");
                if (checkIfPackageActivated(packages_to_uninstall.get(i))) {
                    Log.d("Masquerade", "Package file \"" + current +
                            "\" requires an overlay disable prior to uninstall...");
                    if (final_commands_disable.length() == 0) {
                        final_commands_disable = "om disable " + current;
                    } else {
                        final_commands_disable = final_commands_disable + " " + current;
                    }

                    if (final_commands_uninstall.length() == 0) {
                        final_commands_uninstall = "pm uninstall " + current;
                    } else {
                        final_commands_uninstall = final_commands_uninstall +
                                " && pm uninstall " + current;
                    }
                } else {
                    Log.d("Masquerade", "\"" + current +
                            "\" has been redirected to the package manager in " +
                            "preparations of removal...");
                    Root.runCommand("pm uninstall " + current);
                }
            }

            if (final_commands_disable.length() > 0) {
                Log.d("Masquerade", "Disable commands: " + final_commands_disable);
                Root.runCommand(final_commands_disable);
            }
            if (final_commands_uninstall.length() > 0) {
                Log.d("Masquerade", "Uninstall commands: " + final_commands_uninstall);
                Root.runCommand(final_commands_uninstall);
            }

            if (!specific) {
                // Clear the resource idmapping files generated by OMS
                Log.d("Masquerade", "Cleaning up resource-cache directory...");
                Root.runCommand("rm /data/resource-cache/*");
                // Now clear the persistent overlays database
                Log.d("Masquerade", "Finalizing clean up of persistent overlays database...");
                Root.runCommand("rm -rf /data/system/overlays.xml");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}