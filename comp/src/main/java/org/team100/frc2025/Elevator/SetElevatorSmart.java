package org.team100.frc2025.Elevator;

import org.team100.lib.config.ElevatorUtil.ScoringPosition;
import org.team100.lib.experiments.Experiment;
import org.team100.lib.experiments.Experiments;

import edu.wpi.first.wpilibj2.command.Command;

public class SetElevatorSmart extends Command {
    Elevator m_elevator;
    double m_value;
    boolean finished = false;
    boolean m_perpetual;
    ScoringPosition m_height;

    public SetElevatorSmart(Elevator elevator, ScoringPosition height, boolean perpetual) {
        m_elevator = elevator;
        m_height = height;
        finished = false;
        m_perpetual = perpetual;
        addRequirements(m_elevator);
    }

    @Override
    public void initialize() {
        finished = false;
        // resetting forces the setpoint velocity to zero, which is not always what we
        // want
        // m_elevator.resetElevatorProfile();
    }

    @Override
    public void execute() {
        m_elevator.setPosition(m_height.getValue()); // 24.5 for l3

        double error = Math.abs(m_elevator.getPosition() - m_value);
        if (error < 0.5) {
            finished = true;
        }
    }

    @Override
    public void end(boolean interrupted) {
        m_elevator.stop();
        finished = false;

    }

    @Override
    public boolean isFinished() {
        if (m_perpetual) {
            return false;
        }
        if (Experiments.instance.enabled(Experiment.UseProfileDone))
            return finished && m_elevator.profileDone();
        return finished;
    }
}
