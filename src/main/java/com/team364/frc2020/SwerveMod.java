package com.team364.frc2020;

import static com.team364.frc2020.Conversions.*;
import static com.team364.frc2020.RobotMap.*;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix.motorcontrol.StatusFrame;
import com.ctre.phoenix.motorcontrol.SupplyCurrentLimitConfiguration;
import com.ctre.phoenix.motorcontrol.can.TalonFX;
import com.ctre.phoenix.motorcontrol.can.TalonFXConfiguration;
import com.ctre.phoenix.sensors.AbsoluteSensorRange;
import com.ctre.phoenix.sensors.CANCoder;
import com.ctre.phoenix.sensors.CANCoderConfiguration;
import com.ctre.phoenix.sensors.SensorInitializationStrategy;
import com.team364.frc2020.misc.math.Vector2;

import edu.wpi.first.wpilibj.geometry.Rotation2d;
import edu.wpi.first.wpilibj.kinematics.SwerveModuleState;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Subsystem;

public class SwerveMod implements Subsystem {
    public final int moduleNumber;
    
    private final CANCoder localTurn;
    private final TalonFX mAngleMotor;
    private final TalonFX mDriveMotor;
    private Vector2 modulePosition;
    private CANCoderConfiguration localCANConfig = new CANCoderConfiguration();

    public double smartAngle;
    public Vector2 velocity;
    public double currentAngle;
    private boolean driveInvert = false;
    public double offset;

    public PeriodicIO periodicIO = new PeriodicIO();

    public SupplyCurrentLimitConfiguration swerveAngleSupplyLimit = new SupplyCurrentLimitConfiguration(ANGLEENABLECURRENTLIMIT, ANGLECONTINUOUSCURRENTLIMIT, ANGLEPEAKCURRENT, ANGLEPEAKCURRENTDURATION);
    public SupplyCurrentLimitConfiguration swerveDriveSupplyLimit = new SupplyCurrentLimitConfiguration(DRIVEENABLECURRENTLIMIT, DRIVECONTINUOUSCURRENTLIMIT, DRIVEPEAKCURRENT, DRIVEPEAKCURRENTDURATION);

    //private final PIDController m_speedPIDController = new PIDController(1, 0, 0);
    //private final ProfiledPIDController m_anglePIDController = new ProfiledPIDController(1, 0, 0, new TrapezoidProfile.Constraints(SWERVEMAX_ANGLEVELOCITY, SWERVEMAX_ANGLEACCELERATION));

    public SwerveMod(int moduleNumber, Vector2 modulePosition, TalonFX angleMotor, CANCoder turnEncoder, TalonFX driveMotor,
            boolean invertDrive, boolean invertAngle, boolean invertAnglePhase, double offset) {
        this.moduleNumber = moduleNumber;
        this.modulePosition = modulePosition;
        mAngleMotor = angleMotor;
        mDriveMotor = driveMotor;
        currentAngle = 0;
        localTurn = turnEncoder;
        localCANConfig.absoluteSensorRange = AbsoluteSensorRange.Unsigned_0_to_360;
        localCANConfig.initializationStrategy = SensorInitializationStrategy.BootToAbsolutePosition;
        this.offset = offset;

        //Configure CANCoder
        localTurn.configAllSettings(localCANConfig, 30);
    //Configure CANCoder
    localCANConfig.absoluteSensorRange = AbsoluteSensorRange.Unsigned_0_to_360;
    localCANConfig.initializationStrategy = SensorInitializationStrategy.BootToAbsolutePosition;
    //Configure CANCoder
    localTurn.configAllSettings(localCANConfig, 30);

    //Drive Factory Default
    mDriveMotor.configFactoryDefault();
    TalonFXConfiguration driveConfig = new TalonFXConfiguration();

    //Configure Drive Supply Current
    driveConfig.supplyCurrLimit = swerveDriveSupplyLimit;

    // Configure Drive PID
    driveConfig.primaryPID.selectedFeedbackSensor = FeedbackDevice.IntegratedSensor;
    driveConfig.slot0.kP = 1;
    driveConfig.slot0.kI = 0;
    driveConfig.slot0.kD = 0;

    // Configure Drive Motor
    mDriveMotor.configFactoryDefault();
    mDriveMotor.configSelectedFeedbackSensor(FeedbackDevice.IntegratedSensor, SLOTIDX, SWERVETIMEOUT);
    mDriveMotor.setStatusFramePeriod(StatusFrame.Status_2_Feedback0, 20);
    mDriveMotor.setNeutralMode(NeutralMode.Brake);
    mDriveMotor.setInverted(invertDrive);


    //Angle Factory Default
    mAngleMotor.configFactoryDefault();
    TalonFXConfiguration angleConfig = new TalonFXConfiguration();

    //Configure Angle Supply Current
    angleConfig.supplyCurrLimit = swerveAngleSupplyLimit;

    // Configure Angle PID
    angleConfig.primaryPID.selectedFeedbackSensor = FeedbackDevice.RemoteSensor0;
    angleConfig.slot0.kP = ANGLEP;
    angleConfig.slot0.kI = ANGLEI;
    angleConfig.slot0.kD = ANGLED;

    // Configure rest of Angle Motor
    mAngleMotor.configAllSettings(angleConfig);
    mAngleMotor.configRemoteFeedbackFilter(localTurn, 0, SWERVETIMEOUT);
    mAngleMotor.setInverted(invertAngle);
    mAngleMotor.setSensorPhase(invertAnglePhase);
    mAngleMotor.setNeutralMode(NeutralMode.Coast);
    mAngleMotor.setStatusFramePeriod(StatusFrame.Status_2_Feedback0, 20);
    
    }

    public void runOutputs(boolean profiling){
      mDriveMotor.set(ControlMode.PercentOutput, (profiling) ? periodicIO.velocityDemand : periodicIO.speedDemand);
      mAngleMotor.set(ControlMode.Position, periodicIO.positionDemand);    
      SmartDashboard.putBoolean("velocity", profiling);

    }

    public void setVectorVelocity(Vector2 velocity, boolean speed) {
      setAngle(velocity.getAngle().toDegrees());
      if (speed) {
          setSpeed(velocity.length);
      } else {
          setSpeed(0);
      }
    }

    public synchronized void setAngle(double targetAngle) {
        targetAngle = modulate360(-targetAngle);
        double currentAngle = toDegrees(mAngleMotor.getSelectedSensorPosition());
        double currentAngleMod = modulate360(currentAngle - offset);
        if (currentAngleMod < 0) currentAngleMod += 360;

        double delta = currentAngleMod - targetAngle;
        if (delta > 180) {
            targetAngle += 360;
        } else if (delta < -180) {
            targetAngle -= 360;
        }

        delta = currentAngleMod - targetAngle;
        if (delta > 90 || delta < -90) {
            if(delta > 90){
                targetAngle += 180;
            }
            else if(delta < -90){
                targetAngle -= 180;
            }            
            driveInvert = true;
        } else{
            driveInvert = false;
        }

        
        targetAngle += currentAngle - currentAngleMod;        
        targetAngle = toCounts(targetAngle);
        periodicIO.setDemandPosition(targetAngle);
    }


    public synchronized void setSpeed(double fSpeed) {
        if(driveInvert) fSpeed *= -1;
        periodicIO.setDemandSpeed(fSpeed);
    }

    public synchronized void setVelocity(double velocity){
      periodicIO.setDemandVelocity(velocity);
    }

    public static class PeriodicIO {
        // Outputs
        public double positionDemand;
        public double speedDemand;
        public double velocityDemand;

        public void setDemandPosition(double positionDemand) {
            this.positionDemand = positionDemand;
        }

        public void setDemandSpeed(double speedDemand) {
            this.speedDemand = speedDemand;
        }

        public void setDemandVelocity(double velocityDemand){
          this.velocityDemand = velocityDemand * (kMaxSwerveDriveVelocity / kMaxSpeedMetersPerSecond);
        }
    }


    public SwerveModuleState getState(){
        double holdTurn = modulate360(localTurn.getPosition());
        if(holdTurn < 0) holdTurn *= -1;
        return new SwerveModuleState(mDriveMotor.getSelectedSensorVelocity() * (kMaxSpeedMetersPerSecond / kMaxSwerveDriveVelocity), new Rotation2d(holdTurn));
    }
    
    public Vector2 getModulePosition() {
        return modulePosition;
    }

    public double getCANCoderAngle(){
        double convert = modulate360(toDegrees(mAngleMotor.getSelectedSensorPosition()));
        if(convert < 0) convert += 360;
        return convert;
    }
    
    public double getDriveSpeed(){
        return mDriveMotor.getSelectedSensorVelocity();
    }

    public double getDriveDistance(){
        return mDriveMotor.getSelectedSensorPosition();
    }

    public void resetDriveDistance(){
        mDriveMotor.setSelectedSensorPosition(0);
    }

    public TalonFX getAngleMotor(){
        return mAngleMotor;
    }
    public void toFusionSwerve(SwerveModuleState state){
        setAngle(state.angle.getDegrees());
        setVelocity(state.speedMetersPerSecond);
    }

	public void stop() {
        mAngleMotor.set(ControlMode.Position, getCANCoderAngle());
        mDriveMotor.set(ControlMode.PercentOutput, 0);
	}
}
