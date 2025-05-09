package org.team100.lib.motion.servo;

import java.util.OptionalDouble;

import org.team100.lib.controller.simple.ProfiledController;
import org.team100.lib.encoder.CombinedEncoder;
import org.team100.lib.experiments.Experiment;
import org.team100.lib.experiments.Experiments;
import org.team100.lib.logging.Level;
import org.team100.lib.logging.LoggerFactory;
import org.team100.lib.logging.LoggerFactory.Control100Logger;
import org.team100.lib.logging.LoggerFactory.DoubleLogger;
import org.team100.lib.logging.LoggerFactory.Model100Logger;
import org.team100.lib.logging.LoggerFactory.OptionalDoubleLogger;
import org.team100.lib.motion.mechanism.RotaryMechanism;
import org.team100.lib.profile.Profile100;
import org.team100.lib.state.Control100;
import org.team100.lib.state.Model100;

import edu.wpi.first.math.MathUtil;

/**
 * Passthrough to outboard closed-loop angular control, using a profile with
 * velocity feedforward, also extra torque (e.g. for gravity). There's no
 * feedback at this level, and no feedforward calculation either, that's
 * delegated to the mechanism.
 * 
 * Must be used with a combined encoder, to "zero" the motor encoder.
 * 
 * 
 */
public class OutboardAngularPositionServo implements AngularPositionServo {
    private static final double kPositionTolerance = 0.05;
    private static final double kVelocityTolerance = 0.05;

    private final RotaryMechanism m_mechanism;
    private final CombinedEncoder m_encoder;

    // LOGGERS
    private final Model100Logger m_log_goal;
    private final DoubleLogger m_log_ff_torque;
    private final DoubleLogger m_log_measurement;
    private final Control100Logger m_log_setpoint;
    private final OptionalDoubleLogger m_log_position;

    private final ProfiledController m_controller;
    /**
     * Goal "winds up" i.e. it's it's [-inf, inf], not [-pi,pi]
     */
    private Model100 m_goal = new Model100(0, 0);
    /**
     * Setpoint "winds up" i.e. it's [-inf, inf], not [-pi,pi]
     */
    private Control100 m_setpoint = new Control100(0, 0);

    public OutboardAngularPositionServo(
            LoggerFactory parent,
            RotaryMechanism mech,
            CombinedEncoder encoder,
            ProfiledController controller) {
        LoggerFactory child = parent.child(this);
        m_mechanism = mech;
        m_encoder = encoder;
        m_controller = controller;
        m_log_goal = child.model100Logger(Level.TRACE, "goal (rad)");
        m_log_ff_torque = child.doubleLogger(Level.TRACE, "Feedforward Torque (Nm)");
        m_log_measurement = child.doubleLogger(Level.TRACE, "measurement (rad)");
        m_log_setpoint = child.control100Logger(Level.TRACE, "setpoint (rad)");
        m_log_position = child.optionalDoubleLogger(Level.TRACE, "Position");
    }

    @Override
    public void reset() {
        OptionalDouble position = getPosition();
        if (position.isEmpty())
            return;
        // using the current velocity sometimes includes a whole lot of noise, and then
        // the profile tries to follow that noise. so instead, use zero.
        // OptionalDouble velocity = getVelocity();
        // if (velocity.isEmpty())
        // return;
        m_setpoint = new Control100(position.getAsDouble(), 0);
        m_controller.init(new Model100(position.getAsDouble(), 0));
    }

    @Override
    public void setTorqueLimit(double torqueNm) {
        m_mechanism.setTorqueLimit(torqueNm);
    }

    @Override
    public void setDutyCycle(double dutyCycle) {
        m_mechanism.setDutyCycle(dutyCycle);
    }

    @Override
    public void setEncoderPosition(double value) {
        m_mechanism.setEncoderPosition(value);
    }

    /**
     * Sets the goal, updates the setpoint to the "next step" value towards it,
     * gives the setpoint to the outboard mechanism.
     * 
     * The outboard measurement does not wrap, but the goal does.
     * 
     * @param goalRad             [-pi, pi]
     * @param goalVelocityRad_S
     * @param feedForwardTorqueNm
     */
    @Override
    public void setPositionWithVelocity(double goalRad, double goalVelocityRad_S, double feedForwardTorqueNm) {
        OptionalDouble posOpt = m_encoder.getPositionRad();
        if (posOpt.isEmpty())
            return;
        m_log_position.log(() -> posOpt);

        // measurement is [-inf,inf]
        final double measurement = posOpt.getAsDouble();

        // choose a goal which is near the measurement
        // goal is [-inf, inf]
        m_goal = new Model100(MathUtil.angleModulus(goalRad - measurement) + measurement,
                goalVelocityRad_S);

        // m_goal = new Model100(goalRad,
        // goalVelocityRad_S);

        // setpoint is [-inf,inf], near the measurement
        m_setpoint = new Control100(
                MathUtil.angleModulus(m_setpoint.x() - measurement) + measurement,
                m_setpoint.v());

        // m_setpoint = new Control100(
        // m_setpoint.x(),
        // m_setpoint.v());

        // finally compute a new setpoint
        final ProfiledController.Result result = m_controller.calculate(
                m_setpoint.model(), m_goal);
        m_setpoint = result.feedforward();

        if (Experiments.instance.enabled(Experiment.LashCorrection)) {
            double lashError = m_encoder.getError();
            m_mechanism.setPosition(m_setpoint.x() - lashError, m_setpoint.v(), m_setpoint.a(), feedForwardTorqueNm);
        } else {
            m_mechanism.setPosition(m_setpoint.x(), m_setpoint.v(), m_setpoint.a(), feedForwardTorqueNm);
        }

        m_log_goal.log(() -> m_goal);
        m_log_ff_torque.log(() -> feedForwardTorqueNm);
        m_log_measurement.log(() -> measurement);
        m_log_setpoint.log(() -> m_setpoint);
    }

    @Override
    public void setPosition(double goal, double feedForwardTorqueNm) {
        setPositionWithVelocity(goal, 0.0, feedForwardTorqueNm);
    }

    /** Value is updated in Robot.robotPeriodic(). */
    @Override
    public OptionalDouble getPosition() {
        return m_encoder.getPositionRad();
    }

    /** Value is updated in Robot.robotPeriodic(). */
    @Override
    public OptionalDouble getVelocity() {
        return m_encoder.getRateRad_S();
    }

    /**
     * Compares robotPeriodic-updated measurements to the setpoint,
     * so you need to know when the setpoint was updated: is it for the
     * current Takt time, or the next step?
     */
    @Override
    public boolean atSetpoint() {
        OptionalDouble positionRad = getPosition();
        if (positionRad.isEmpty())
            return false;
        OptionalDouble velocityRad_S = getVelocity();
        if (velocityRad_S.isEmpty())
            return false;
        double positionError = MathUtil.angleModulus(m_setpoint.x() - positionRad.getAsDouble());
        double velocityError = m_setpoint.v() - velocityRad_S.getAsDouble();
        return Math.abs(positionError) < kPositionTolerance
                && Math.abs(velocityError) < kVelocityTolerance;
    }

    @Override
    public boolean profileDone() {
        return setpointAtGoal();
    }

    /**
     * Note the setpoint may reflect the curent time, or the next time, depending on
     * whether setPosition has been called during this cycle.
     */
    private boolean setpointAtGoal() {
        double positionError = MathUtil.angleModulus(m_goal.x() - m_setpoint.x());
        double velocityError = m_goal.v() - m_setpoint.v();
        return Math.abs(positionError) < kPositionTolerance
                && Math.abs(velocityError) < kVelocityTolerance;
    }

    /**
     * Note this is affected by the setpoint update.
     * 
     * It really makes the most sense to call this *before* updating the setpoint,
     * because the measurement comes from the recent-past Takt and the updated
     * setpoint will be aiming at the next one.
     */
    @Override
    public boolean atGoal() {
        return atSetpoint() && setpointAtGoal();
    }

    @Override
    public double getGoal() {
        return m_goal.x();
    }

    @Override
    public void stop() {
        m_mechanism.stop();
    }

    @Override
    public void close() {
        m_mechanism.close();
    }

    @Override
    public Control100 getSetpoint() {
        return m_setpoint;
    }

    @Override
    public void periodic() {
        m_mechanism.periodic();
        m_encoder.periodic();
    }

    @Override
    public void setStaticTorque(double feedForwardTorqueNm) {

    }

    @Override
    public void setProfile(Profile100 profile){
        m_controller.setProfile(profile);
    }

}
