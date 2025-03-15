// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package org.team100.frc2025.CommandGroups;

import org.team100.frc2025.Elevator.Elevator;
import org.team100.frc2025.Funnel.Funnel;
import org.team100.frc2025.Funnel.RunFunnel;
import org.team100.frc2025.Wrist.AlgaeGrip;
import org.team100.frc2025.Wrist.CoralTunnel;
import org.team100.frc2025.Wrist.ElevatorDutyCycle;
import org.team100.frc2025.Wrist.RunCoralTunnel;
import org.team100.frc2025.Wrist.Wrist2;

import edu.wpi.first.wpilibj2.command.ParallelCommandGroup;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;

// NOTE:  Consider using this command inline, rather than writing a subclass.  For more
// information, see:
// https://docs.wpilib.org/en/stable/docs/software/commandbased/convenience-features.html
public class RunFunnelHandoff extends ParallelCommandGroup {
  /** Creates a new RunFunnelHandoff. */
  public RunFunnelHandoff(Elevator elevator, Wrist2 wrist, Funnel funnel, CoralTunnel tunnel, AlgaeGrip grip) {
    // Add your commands in the addCommands() call, e.g.
    // addCommands(new FooCommand(), new BarCommand());
    addCommands(
        new PrepareFunnelHandoff(wrist, elevator),
        new RunFunnel(funnel),
        new RunCoralTunnel(tunnel, 1)
    );
  }
}
