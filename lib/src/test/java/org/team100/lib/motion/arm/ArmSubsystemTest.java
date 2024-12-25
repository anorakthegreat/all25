package org.team100.lib.motion.arm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.team100.lib.logging.LoggerFactory;
import org.team100.lib.logging.TestLoggerFactory;
import org.team100.lib.logging.primitive.TestPrimitiveLogger;
import org.team100.lib.testing.Timeless;

class ArmSubsystemTest implements Timeless {
    private static final double kDelta = 0.001;
    private static final LoggerFactory logger = new TestLoggerFactory(new TestPrimitiveLogger());

    // test simple direct motion
    @Test
    void testSimple() {
        ArmSubsystem armSubSystem = ArmFactory.get(logger);

        assertEquals(0, armSubSystem.getPosition().get().th1, kDelta);
        assertEquals(0, armSubSystem.getPosition().get().th2, kDelta);

        for (int i = 0; i < 100; ++i) {
            stepTime(0.02);
            // since i took out the limits this just spins
            armSubSystem.set(1, 1);
            // you have to call getPosition on the simulated sensor for it to do the integraiton.
            armSubSystem.getPosition();
            // ArmAngles angles = armSubSystem.getPosition().get();
            // System.out.printf("%d %5.3f %5.3f\n", i, angles.th1, angles.th2);
        }

        // how far did it spin?
        assertEquals(0.65, armSubSystem.getPosition().get().th1, 0.01);
        assertEquals(0.65, armSubSystem.getPosition().get().th2, 0.01);

        armSubSystem.close();
    }
}