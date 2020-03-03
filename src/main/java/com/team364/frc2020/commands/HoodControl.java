package com.team364.frc2020.commands;

import com.team364.frc2020.Configuration;
import com.team364.frc2020.Robot;
import com.team364.frc2020.misc.util.Function;
import com.team364.frc2020.subsystems.*;

import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInWidgets;
import edu.wpi.first.wpilibj2.command.CommandBase;

import static com.team364.frc2020.States.*;

public class HoodControl extends CommandBase {
    private double angle;
    private Hood s_Hood;
    private Configuration config;


    public HoodControl(double angle, Hood s_Hood, Configuration config) {
        addRequirements(s_Hood);
        this.angle = angle;
        this.s_Hood = s_Hood;
        this.config = config;
    }

    @Override
    public void initialize() {
        Robot.HoodControl.setValue(true);
    }

    @Override
    public void execute() {
        Function exe = new Function((configState == ConfigStates.TARGET) ? 
            () -> {s_Hood.setAngle(config.HoodAngle);} 
                : 
            () -> {s_Hood.setAngle(angle);}
        );
        exe.run();
    }

    @Override
    public boolean isFinished() {
        return false;
    }
    @Override
    public void end(boolean interrupted){
        s_Hood.setAngle(0);
        Robot.HoodControl.setValue(false);
    }


}
