package at.primetshofer.model.util;

import at.primetshofer.Main;
import de.jcm.discordgamesdk.Core;
import de.jcm.discordgamesdk.CreateParams;
import de.jcm.discordgamesdk.activity.Activity;
import org.apache.log4j.Logger;

import java.time.Instant;

public class DiscordActivityUtil {

    private static final Logger logger = Logger.getLogger(Main.class);

    private static Core core;
    private static boolean stop = false;
    private static Instant startTime;

    public static void startDiscordConnection(){
        stop = false;
        try(CreateParams params = new CreateParams()) {
            params.setClientID(1363117128659636485L);
            params.setFlags(CreateParams.getDefaultFlags());
            // Create the Core
            try{
                core = new Core(params);
            } catch (Exception e){
                logger.warn("Failed to initialize discord connection", e);
                return;
            }

            // Create the Activity
            try(Activity activity = new Activity())
            {
                activity.setDetails("Starting");

                activity.assets().setLargeImage("icon");

                // Setting a start time causes an "elapsed" field to appear
                startTime = Instant.now();
                activity.timestamps().setStart(startTime);

                // Finally, update the current activity to our activity
                core.activityManager().updateActivity(activity);
            }

            // Run callbacks forever
            Thread thread = new Thread(() -> {
                while (!stop) {
                    core.runCallbacks();
                    try {
                        // Sleep a bit to save CPU
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });

            thread.setDaemon(true);
            Thread.startVirtualThread(thread);
        }
    }

    public static void updateActivity(Activity activity){
        if(core.isOpen()){
            activity.assets().setLargeImage("icon");
            Thread updateActivityThread = new Thread(() -> core.activityManager().updateActivity(activity));

            updateActivityThread.setDaemon(true);
            Thread.startVirtualThread(updateActivityThread);
        }
    }

    public static void setActivityDetails(String details){
        Activity activity = new Activity();
        activity.timestamps().setStart(startTime);
        activity.setDetails(details);

        DiscordActivityUtil.updateActivity(activity);
    }

    public static void stopDiscordConnection(){
        core.close();
        stop = true;
    }

}
