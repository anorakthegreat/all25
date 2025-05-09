package org.team100.lib.controller.simple;

import java.util.function.DoubleUnaryOperator;

import org.team100.lib.dashboard.Glassy;
import org.team100.lib.framework.TimedRobot100;
import org.team100.lib.logging.Level;
import org.team100.lib.logging.LoggerFactory;
import org.team100.lib.logging.LoggerFactory.Control100Logger;
import org.team100.lib.logging.LoggerFactory.Model100Logger;
import org.team100.lib.profile.IncrementalProfile;
import org.team100.lib.profile.Profile100;
import org.team100.lib.state.Control100;
import org.team100.lib.state.Model100;

import edu.wpi.first.math.MathUtil;

/**
 * A profiled controller combines an incremental profile (e.g. trapezoid) and a
 * source of feedback (e.g. PID).
 * 
 * This class remembers the setpoint. When you call calculate(), you make a new
 * setpoint intended for the end of the next time step.
 * 
 * I think we have a habit of mixing up previous-step, current-step, and
 * future-step quantities when writing profile/control loops. This implements
 * the right way to do it.
 * 
 * You must call init() with the current measurement, prior to calculate().
 * 
 * Remember to re-initialize anytime the profile may diverge from the
 * measurement, or you'll introduce transients.
 * 
 * TODO: dedupe the two profiled controllers.
 */
public class IncrementalProfiledController implements ProfiledController, Glassy {
    private static final double kDt = TimedRobot100.LOOP_PERIOD_S;

    private IncrementalProfile m_profile;
    private final Feedback100 m_feedback;
    private final DoubleUnaryOperator m_modulus;
    private final double m_positionTolerance;
    private final double m_velocityTolerance;

    private final Model100Logger m_log_setpoint;
    private final Control100Logger m_log_control;

    private Model100 m_goal;
    private Model100 m_setpoint;

    public IncrementalProfiledController(
            LoggerFactory logger,
            IncrementalProfile profile,
            Feedback100 feedback,
            DoubleUnaryOperator modulus,
            double positionTolerance,
            double velocityTolerance) {
        m_profile = profile;
        m_feedback = feedback;
        m_modulus = modulus;
        m_positionTolerance = positionTolerance;
        m_velocityTolerance = velocityTolerance;
        // use the parent logger
        m_log_setpoint = logger.model100Logger(Level.TRACE, "setpoint");
        m_log_control = logger.control100Logger(Level.TRACE, "control");
    }

    /**
     * Initializes the setpoint.
     * 
     * @param measurement current-instant measurement
     */
    @Override
    public void init(Model100 measurement) {
        m_setpoint = measurement;
        m_log_setpoint.log(() -> m_setpoint);
        m_feedback.reset();
    }

    @Override
    public void setProfile(Profile100 profile){
        m_profile = profile;
    }



    /**
     * Calculates feedforward and feedback.
     * 
     * Feedback is based on the error between the current-instant measurement and
     * the setpoint calculated in the past, which was also intended for the current
     * instant.
     * 
     * Feedforward is based on the profile at the end of the next time step.
     * 
     * This remembers the feedforward and uses it on the next step.
     * 
     * @param measurement current-instant measurement
     * @param goal        final desired state
     */
    @Override
    public Result calculate(Model100 measurement, Model100 goal) {
        if (m_setpoint == null)
            throw new IllegalStateException("Null setpoint!");

        // use the goal nearest to the measurement.
        m_goal = new Model100(
                m_modulus.applyAsDouble(goal.x() - measurement.x()) + measurement.x(),
                goal.v());

        // adjust the setpoint based on the measurement.
        m_setpoint = new Model100(
                m_modulus.applyAsDouble(m_setpoint.x() - measurement.x()) + measurement.x(),
                m_setpoint.v());

        // Feedback compares the current measurement with the current profile.
        double u_FB = m_feedback.calculate(measurement, m_setpoint);

        // Profile result is for the next time step.
        Control100 u_FF = m_profile.calculate(kDt, m_setpoint, m_goal);
        m_log_control.log(() -> u_FF);

        m_setpoint = u_FF.model();
        m_log_setpoint.log(() -> m_setpoint);
        return new Result(u_FF, u_FB);
    }

    @Override
    public boolean profileDone() {
        // the only way to tell if an incremental profile is done is to compare the
        // setpoint to the goal.
        return MathUtil.isNear(
                m_goal.x(),
                m_setpoint.x(),
                m_positionTolerance)
                && MathUtil.isNear(
                        m_goal.v(),
                        m_setpoint.v(),
                        m_velocityTolerance);
    }

    @Override
    public Model100 getSetpoint() {
        return m_setpoint;
    }

    /**
     * The feedback controller error is within its tolerance, i.e. we are following
     * the profile well.
     */
    @Override
    public boolean atSetpoint() {
        boolean atSetpoint = m_feedback.atSetpoint();
        // Util.printf("profiled controller at setpoint %b\n", atSetpoint);
        return atSetpoint;
    }

    /**
     * The profile has reached the goal and the feedback error is within tolerance,
     * i.e. our path is complete.
     * 
     * This doesn't use current measurements, it uses whatever inputs we saw in
     * calculate() most recently.
     */
    @Override
    public boolean atGoal(Model100 goal) {
        return profileDone() && atSetpoint();
    }

    @Override
    public void close() {
        //
    }
}
