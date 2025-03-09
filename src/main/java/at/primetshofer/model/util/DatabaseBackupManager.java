package at.primetshofer.model.util;

import at.primetshofer.logic.tracing.verification.VerificationLogic;
import jakarta.persistence.EntityManager;
import org.apache.log4j.Logger;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.*;

public class DatabaseBackupManager {

    private final static Logger logger = Logger.getLogger(DatabaseBackupManager.class);

    private static final String BACKUP_DIR = "DB_Backup";

    public static void checkAndBackup(EntityManager em) {
        // Ensure backup directory exists
        File backupDir = new File(BACKUP_DIR);
        if (!backupDir.exists() && !backupDir.mkdirs()) {
            logger.error("Could not create backup directory: '" + BACKUP_DIR + "'");
            return;
        }

        LocalDate today = LocalDate.now();
        String backupFileName = "backup-" + today + ".zip";
        File backupFile = new File(backupDir, backupFileName);

        // Check if today's backup exists
        if (backupFile.exists()) {
            logger.warn("Backup already exists for today: '" + backupFile.getName() + "'");
        } else {
            performBackup(em, backupFile.getAbsolutePath());
        }

        // Clean up old backups based on retention rules
        cleanupBackups();
    }

    private static void performBackup(EntityManager em, String backupFilePath) {
        // Construct the SQL for the H2 backup command.
        // Note: The file path separator is normalized to '/'.
        String backupSql = "BACKUP TO '" + backupFilePath.replace("\\", "/") + "'";
        try {
            em.getTransaction().begin();
            em.createNativeQuery(backupSql).executeUpdate();
            em.getTransaction().commit();
            logger.info("Backup created: '" + backupFilePath + "'");
        } catch (Exception ex) {
            em.getTransaction().rollback();
            logger.error("Backup failed: '" + backupFilePath + "'", ex);
        }
    }

    private static void cleanupBackups() {
        File backupDir = new File(BACKUP_DIR);
        File[] backupFiles = backupDir.listFiles((dir, name) -> name.matches("backup-\\d{4}-\\d{2}-\\d{2}\\.zip"));
        if (backupFiles == null) {
            return;
        }

        // Build a list of backup entries with their dates
        List<BackupEntry> entries = new ArrayList<>();
        for (File file : backupFiles) {
            String name = file.getName();
            // Extract date part from filename: "backup-YYYY-MM-DD.zip"
            String dateStr = name.substring("backup-".length(), name.length() - ".zip".length());
            try {
                LocalDate backupDate = LocalDate.parse(dateStr);
                entries.add(new BackupEntry(backupDate, file));
            } catch (DateTimeParseException ex) {
                logger.error("Skipping file with invalid date format: '" + name + "'");
            }
        }

        // Sort backups by date descending (newest first)
        entries.sort(Comparator.comparing(BackupEntry::getDate).reversed());

        LocalDate today = LocalDate.now();
        // Maps to keep track of which backup to keep for weekly and monthly groups.
        Map<String, BackupEntry> weeklyMap = new HashMap<>();
        Map<String, BackupEntry> monthlyMap = new HashMap<>();

        // List of files to delete
        List<File> filesToDelete = new ArrayList<>();

        // Define retention thresholds:
        // Daily: backups <= 7 days old (keep all)
        // Weekly: backups older than 7 days but <= 90 days
        // Monthly: backups older than 90 days
        for (BackupEntry entry : entries) {
            LocalDate backupDate = entry.getDate();
            long daysOld = ChronoUnit.DAYS.between(backupDate, today);

            if (daysOld > 7) {
                if (daysOld <= 90) { // Weekly retention period
                    // Group by ISO week and year (e.g., "2025-15")
                    WeekFields weekFields = WeekFields.of(Locale.getDefault());
                    String weekKey = backupDate.getYear() + "-" + backupDate.get(weekFields.weekOfYear());
                    if (weeklyMap.containsKey(weekKey)) {
                        // Already have a backup for this week; mark this one for deletion.
                        filesToDelete.add(entry.getFile());
                    } else {
                        weeklyMap.put(weekKey, entry);
                    }
                } else {
                    // Monthly retention period: group by year and month (e.g., "2025-4")
                    String monthKey = backupDate.getYear() + "-" + backupDate.getMonthValue();
                    if (monthlyMap.containsKey(monthKey)) {
                        filesToDelete.add(entry.getFile());
                    } else {
                        monthlyMap.put(monthKey, entry);
                    }
                }
            }
        }

        // Delete the backups that are no longer needed.
        for (File file : filesToDelete) {
            if (file.delete()) {
                logger.info("Deleted old backup: '" + file.getName() + "'");
            } else {
                logger.error("Failed to delete old backup: '" + file.getName() + "'");
            }
        }
    }

    // Helper class to associate a backup file with its backup date.
    private static class BackupEntry {
        private final LocalDate date;
        private final File file;

        public BackupEntry(LocalDate date, File file) {
            this.date = date;
            this.file = file;
        }

        public LocalDate getDate() {
            return date;
        }

        public File getFile() {
            return file;
        }
    }
}
