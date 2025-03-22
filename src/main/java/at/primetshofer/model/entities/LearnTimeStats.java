package at.primetshofer.model.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Duration;
import java.time.LocalDate;

@Entity
public class LearnTimeStats {

    @Id
    private LocalDate date;
    private Duration duration;
    private int exercisesCount;

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public int getExercisesCount() {
        return exercisesCount;
    }

    public void setExercisesCount(int exercisesCount) {
        this.exercisesCount = exercisesCount;
    }

    public void addDuration(Duration duration) {
        this.duration = this.duration.plus(duration);
    }

    public void incrementExercisesCount() {
        this.exercisesCount++;
    }
}
