package org.team100.frc2025.CommandGroups;

import org.team100.frc2025.Elevator.Elevator;
import org.team100.frc2025.Wrist.Wrist2;
import org.team100.lib.experiments.Experiment;
import org.team100.lib.experiments.Experiments;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;

public class PrePlaceCoralL4 extends Command {
    Wrist2 m_wrist;
    Elevator m_elevator;
    double m_elevatorGoal;
    double countElevator = 0;
    double countWrist = 0;
    boolean finished = false;
    boolean m_perpetual = false;
    private final CommandScheduler scheduler = CommandScheduler.getInstance();
    Command m_holdingCommand;

    public PrePlaceCoralL4(Wrist2 wrist, Elevator elevator, double elevatorValue, boolean perpetual) {
        m_wrist = wrist;
        m_elevator = elevator;
        m_elevatorGoal = elevatorValue;
        m_perpetual = perpetual;
        m_holdingCommand = null;
        addRequirements(m_wrist, m_elevator);
    }

    public PrePlaceCoralL4(Wrist2 wrist, Elevator elevator, double elevatorValue, boolean perpetual, Command holdingCommand) {
        m_wrist = wrist;
        m_elevator = elevator;
        m_elevatorGoal = elevatorValue;
        m_perpetual = perpetual;
        m_holdingCommand = holdingCommand;
        addRequirements(m_wrist, m_elevator);
    }

    @Override
    public void initialize() {
        countElevator = 0;
        countWrist = 0;
        finished = false;
        // resetting forces the setpoint velocity to zero, which is not always what we
        // want
        // m_wrist.resetWristProfile();
        // m_elevator.resetElevatorProfile();
    }

    @Override
    public void execute() {
        m_elevator.setPosition(m_elevatorGoal);
        if (m_elevatorGoal - 10 > m_elevator.getPosition()) {
            m_wrist.setAngleValue(0.4);
        } else {
            m_wrist.setAngleValue(1.25);
        }

        double errorElevator = Math.abs(m_elevator.getPosition() - m_elevatorGoal);
        double errorWrist = Math.abs(m_wrist.getAngle() - 1.25);

        if (errorElevator < 0.5) {
            countElevator++;
        } else {
            countElevator = 0;
        }

        if (errorWrist < 0.05) {
            countWrist++;
        } else {
            countWrist = 0;
        }

        if (countElevator >= 2 && countWrist >= 1) {
            finished = true;
        }
    }

    @Override
    public void end(boolean interrupted) {
        if(!interrupted){
            if(m_holdingCommand != null){
                scheduler.schedule(m_holdingCommand);
            }
        }

    }

    @Override
    public boolean isFinished() {
        if(!m_perpetual){
            if (Experiments.instance.enabled(Experiment.UseProfileDone)){
                return finished && m_wrist.profileDone() && m_elevator.profileDone();
            }
            return finished;
        } else {
            return false;
        }
        
    }
}
