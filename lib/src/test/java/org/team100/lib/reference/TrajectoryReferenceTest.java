package org.team100.lib.reference;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.team100.lib.motion.drivetrain.SwerveModel;
import org.team100.lib.motion.drivetrain.kinodynamics.SwerveKinodynamics;
import org.team100.lib.motion.drivetrain.kinodynamics.SwerveKinodynamicsFactory;
import org.team100.lib.testing.Timeless;
import org.team100.lib.timing.TimingConstraint;
import org.team100.lib.timing.TimingConstraintFactory;
import org.team100.lib.trajectory.Trajectory100;
import org.team100.lib.trajectory.TrajectoryPlanner;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;

public class TrajectoryReferenceTest implements Timeless {
    private static final double kDelta = 0.001;
    SwerveKinodynamics swerveKinodynamics = SwerveKinodynamicsFactory.forRealisticTest();
    List<TimingConstraint> constraints = new TimingConstraintFactory(swerveKinodynamics).allGood();
    TrajectoryPlanner planner = new TrajectoryPlanner(constraints);

    @Test
    void testSimple() {
        Trajectory100 t = planner.restToRest(
                new Pose2d(0, 0, Rotation2d.kZero),
                new Pose2d(1, 0, Rotation2d.kZero));
        TrajectoryReference r = new TrajectoryReference(t);
        // measurement is irrelevant
        r.initialize(new SwerveModel());
        {
            SwerveModel c = r.current();
            assertEquals(0, c.velocity().x(), kDelta);
            assertEquals(0, c.pose().getX(), kDelta);
            SwerveModel n = r.next();
            assertEquals(0.033, n.velocity().x(), kDelta);
            assertEquals(0, n.pose().getX(), kDelta);
        }
        // no time step, nothing changes
        {
            SwerveModel c = r.current();
            assertEquals(0, c.velocity().x(), kDelta);
            assertEquals(0, c.pose().getX(), kDelta);
            SwerveModel n = r.next();
            assertEquals(0.033, n.velocity().x(), kDelta);
            // x is very small but not zero
            assertEquals(0.0003266, n.pose().getX(), 0.0000001);
        }
        // stepping time gets the next references
        stepTime();
        {
            SwerveModel c = r.current();
            assertEquals(0.033, c.velocity().x(), kDelta);
            assertEquals(0, c.pose().getX(), kDelta);
            SwerveModel n = r.next();
            assertEquals(0.065, n.velocity().x(), kDelta);
            assertEquals(0.001, n.pose().getX(), kDelta);
        }
        // way in the future, we're at the end.
        for (int i = 0; i < 500; ++i) {
            stepTime();
        }
        {
            SwerveModel c = r.current();
            assertEquals(0, c.velocity().x(), kDelta);
            assertEquals(1, c.pose().getX(), kDelta);
            SwerveModel n = r.next();
            assertEquals(0, n.velocity().x(), kDelta);
            assertEquals(1, n.pose().getX(), kDelta);
        }

    }

}
