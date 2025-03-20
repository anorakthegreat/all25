package org.team100.frc2025.CommandGroups;

import org.team100.frc2025.Elevator.Elevator;
import org.team100.frc2025.Elevator.SetElevator;
import org.team100.frc2025.Elevator.SetElevatorPerpetually;
import org.team100.frc2025.Wrist.AlgaeGrip;
import org.team100.frc2025.Wrist.IntakeAlgaeGrip;
import org.team100.frc2025.Wrist.SetWrist;
import org.team100.frc2025.Wrist.Wrist2;
import org.team100.lib.framework.ParallelCommandGroup100;
import org.team100.lib.framework.ParallelDeadlineGroup100;
import org.team100.lib.logging.LoggerFactory;

import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;

public class ScoreBarge extends SequentialCommandGroup {
    public ScoreBarge(LoggerFactory parent, Elevator elevator, Wrist2 wrist, AlgaeGrip algae) {
        addCommands(
                new ParallelDeadlineGroup100(
                        parent,
                        new SetElevator(elevator, 54, false),
                        new SetWrist(wrist, 3.4, true),
                        new IntakeAlgaeGrip(algae, true)
                // new RunAlgaeGrip(algae)
                ),
                new ParallelCommandGroup100(
                        parent,
                        new SetElevatorPerpetually(elevator, 54)
                // new OuttakeAlgaeGrip(wrist, algae)
                )

        );
        // new OuttakeAlgaeGrip(wrist, algae)
    }
}
