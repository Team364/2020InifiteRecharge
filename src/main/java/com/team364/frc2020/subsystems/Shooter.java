package com.team364.frc2020.subsystems;

import static com.team364.frc2020.RobotMap.*;
import static com.team364.frc2020.States.*;
import static com.team364.frc2020.RobotContainer.THE_SWITCH;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.ctre.phoenix.motorcontrol.InvertType;
import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix.motorcontrol.StatusFrame;
import com.ctre.phoenix.motorcontrol.SupplyCurrentLimitConfiguration;
import com.ctre.phoenix.motorcontrol.can.TalonFX;
import com.ctre.phoenix.sensors.CANCoder;

import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInWidgets;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class Shooter extends SubsystemBase {
    private double ShooterInput;
    public TalonFX mFlyWheelMotor;
    public TalonFX mSlaveFlyWheelMotor;
    
    private SupplyCurrentLimitConfiguration shootSupplyLimit = new SupplyCurrentLimitConfiguration(true, 35, 40, 0.1);

    private ShuffleboardTab shooterPID = Shuffleboard.getTab("Configuration");
        private NetworkTableEntry shooterkP;
        private NetworkTableEntry shooterkI;
        private NetworkTableEntry shooterkD;
        private NetworkTableEntry shooterkF;

    public Shooter() {
        mFlyWheelMotor = new TalonFX(SHOOTER);   
        mSlaveFlyWheelMotor = new TalonFX(SHOOTERSLAVE);
    
        // Configure Shooter Motor
        mFlyWheelMotor.configFactoryDefault();
        mSlaveFlyWheelMotor.configFactoryDefault();
        mFlyWheelMotor.configSelectedFeedbackSensor(FeedbackDevice.IntegratedSensor, 0, 20);
        mFlyWheelMotor.setNeutralMode(NeutralMode.Coast);
        mSlaveFlyWheelMotor.setNeutralMode(NeutralMode.Coast);
        mFlyWheelMotor.setStatusFramePeriod(StatusFrame.Status_2_Feedback0, 10, 10);
        
        mFlyWheelMotor.selectProfileSlot(0, 20);
        mFlyWheelMotor.config_kP(0, 5);
        mFlyWheelMotor.config_kI(0, 0);
        mFlyWheelMotor.config_kD(0, 0);
        mFlyWheelMotor.config_kF(0, 0);

        mFlyWheelMotor.setInverted(true);
        mSlaveFlyWheelMotor.follow(mFlyWheelMotor);
        mSlaveFlyWheelMotor.setInverted(InvertType.OpposeMaster);
        ShooterInput = 0;
 
        mFlyWheelMotor.configSupplyCurrentLimit(shootSupplyLimit, 20);
        mSlaveFlyWheelMotor.configSupplyCurrentLimit(shootSupplyLimit, 20);

        shooterkP = shooterPID.add("Shooter kP", 0.0).withWidget(BuiltInWidgets.kTextView).withPosition(4, 1).getEntry();
        shooterkI = shooterPID.add("Shooter kI", 0.0).withWidget(BuiltInWidgets.kTextView).withPosition(4, 2).getEntry();
        shooterkD = shooterPID.add("Shooter kD", 0.0).withWidget(BuiltInWidgets.kTextView).withPosition(4, 3).getEntry();
        shooterkF = shooterPID.add("Shooter kF", 0.0).withWidget(BuiltInWidgets.kTextView).withPosition(4, 4).getEntry();

    }

    public void setFlyWheelVel(double velocity) {
        ShooterInput = velocity;
        mFlyWheelMotor.set(ControlMode.Velocity, toSensorCounts(velocity));
    }

    public double getFlyWheelVel() {
        return mFlyWheelMotor.getSelectedSensorVelocity();
    }

    @Override
    public void periodic() {
        mFlyWheelMotor.config_kP(0, shooterkP.getDouble(1.0));
        mFlyWheelMotor.config_kI(0, shooterkI.getDouble(0.0));
        mFlyWheelMotor.config_kD(0, shooterkD.getDouble(0.0));
        mFlyWheelMotor.config_kF(0, shooterkF.getDouble(0.0));
        SmartDashboard.putNumber("Shooter velocity", fromSensorCounts(getFlyWheelVel()));
    }

    public void setFlyWheelOff(){
        mFlyWheelMotor.set(ControlMode.PercentOutput, 0);

    }

    public double toSensorCounts(double shooterRpm){
        double sensorCounts = shooterRpm * (18.0 / 40.0) * (2048.0 / 600.0);
        return sensorCounts;
    }

    public double fromSensorCounts(double sensorCounts){
        double shooterRpm = sensorCounts / (18.0 / 40.0) / (2048.0 / 600.0);
        return shooterRpm;
    }
} 