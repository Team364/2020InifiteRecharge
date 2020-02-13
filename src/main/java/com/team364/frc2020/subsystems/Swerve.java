package com.team364.frc2020.subsystems;

import static com.team364.frc2020.Conversions.*;
import static com.team364.frc2020.RobotMap.*;
import static com.team364.frc2020.Configuration.SwerveJson;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.ctre.phoenix.motorcontrol.can.TalonFX;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;
import com.ctre.phoenix.sensors.CANCoder;
import com.ctre.phoenix.sensors.PigeonIMU;
import edu.wpi.first.wpilibj.geometry.Rotation2d;
import edu.wpi.first.wpilibj.geometry.Translation2d;
import edu.wpi.first.wpilibj.kinematics.SwerveDriveKinematics;
import edu.wpi.first.wpilibj.kinematics.SwerveDriveOdometry;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

import com.team364.frc2020.misc.math.Rotation2;
import com.team364.frc2020.misc.math.Vector2;
import com.team364.frc2020.SwerveMod;

public class Swerve extends SubsystemBase {

    private static Swerve Instance = null;
    public PigeonIMU pigeon;

	/*
	 * 0 is Front Right     
	 * 1 is Front Left
	 * 2 is Back Left
	 * 3 is Back Right
	 */
    private SwerveMod[] mSwerveModules;
    public List<SwerveMod> modules;
    public SwerveDriveOdometry m_odometry;
    public SwerveDriveKinematics m_kinematics;

    public Swerve() {
        configOffsets();
        SmartDashboard.putNumber("da offset", MOD1OFFSET);
        pigeon = new PigeonIMU(new TalonSRX(10));
        zeroGyro();
        mSwerveModules = new SwerveMod[] {
            new SwerveMod(1,
                new Vector2(TRACKWIDTH / 2.0, WHEELBASE / 2.0),
                new TalonFX(FLANGLE),
                new CANCoder(FLCAN),
                new TalonFX(FLDRIVE),
                MOD1DRIVEINVERT,
                MOD1ANGLEINVERT,
                MOD1ANGLEPHASE,
                MOD1OFFSET),
            new SwerveMod(2,
                new Vector2(-TRACKWIDTH / 2.0, WHEELBASE / 2.0),
                new TalonFX(FRANGLE),
                new CANCoder(FRCAN),
                new TalonFX(FRDRIVE),
                MOD2DRIVEINVERT,
                MOD1ANGLEINVERT,
                MOD1ANGLEPHASE,
                MOD2OFFSET),
            new SwerveMod(3,
                new Vector2(TRACKWIDTH / 2.0, -WHEELBASE / 2.0),
                new TalonFX(BLANGLE),
                new CANCoder(BLCAN),
                new TalonFX(BLDRIVE),
                MOD3DRIVEINVERT,
                MOD1ANGLEINVERT,
                MOD1ANGLEPHASE,
                MOD3OFFSET),
            new SwerveMod(4,
                new Vector2(-TRACKWIDTH / 2.0, -WHEELBASE / 2.0),
                new TalonFX(BRANGLE),
                new CANCoder(BRCAN),
                new TalonFX(BRDRIVE),
                MOD4DRIVEINVERT,
                MOD1ANGLEINVERT,
                MOD1ANGLEPHASE,
                MOD4OFFSET)
        };
        
        modules = Arrays.asList(mSwerveModules[0], mSwerveModules[1], mSwerveModules[2], mSwerveModules[3]);
        m_kinematics = new SwerveDriveKinematics(
            new Translation2d(TRACKWIDTH / 2.0, WHEELBASE / 2.0), 
            new Translation2d(-TRACKWIDTH / 2.0, WHEELBASE / 2.0),
            new Translation2d(TRACKWIDTH / 2.0, -WHEELBASE / 2.0), 
            new Translation2d(-TRACKWIDTH / 2.0, -WHEELBASE / 2.0) );
        m_odometry = new SwerveDriveOdometry(m_kinematics, getAngle());
    } 

    public synchronized static Swerve getInstance() {
        if (Instance == null) {
          Instance = new Swerve();
        }
        return Instance;
    }

    public void configOffsets(){
        Map<Object, Double> offsetsMap = SwerveJson.getMap();
            MOD1OFFSET = offsetsMap.get("1");
            MOD2OFFSET = offsetsMap.get("2");
            MOD3OFFSET = offsetsMap.get("3");
            MOD4OFFSET = offsetsMap.get("4");
    }

    public SwerveMod[] getSwerveModules() {
        return mSwerveModules;
    }

    public void openLoopOutputs(){
        modules.forEach(mod -> mod.openLoopOutput());
    }

    public void holonomicDrive(Vector2 translation, double rotation, boolean speed) {
        Vector2 velocity;
        for(SwerveMod mod : getSwerveModules()){
            Vector2 newTranslation = null;
            newTranslation = translation.rotateBy(Rotation2.fromDegrees(getYaw()));
            //newTranslation = translation;
            velocity = mod.getModulePosition().normal().scale(deadband(rotation)).add(newTranslation);
            mod.setVectorVelocity(velocity, speed);
        }        
    }

    public void updateKinematics(){
        for (SwerveMod mod : getSwerveModules()){
            mod.setAngle(mod.periodicIO.velocityPosition);
            mod.setSpeed(mod.periodicIO.velocitySpeed);
        }
        openLoopOutputs();
    }
    
    @Override
    public void periodic(){
        for(SwerveMod mod : modules){
            SmartDashboard.putNumber("CANCoder Mod " + mod.moduleNumber + " ", mod.getCANCoderAngle());
        }
    }

    public Rotation2d getAngle(){
        return new Rotation2d(getYaw());
    }


    public double getYaw(){
        double[] ypr = new double[3];
        pigeon.getYawPitchRoll(ypr);
        return ypr[0];
    }
    
    public void zeroGyro(){
        pigeon.setYaw(0);
    }

    public void updateOdometry() {
        
        m_odometry.update(
            getAngle(),
            mSwerveModules[0].getState(),
            mSwerveModules[1].getState(),
            mSwerveModules[2].getState(),
            mSwerveModules[3].getState()
        );
    }
    
}
