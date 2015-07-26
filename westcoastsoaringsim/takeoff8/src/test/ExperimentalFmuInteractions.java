package test;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.javafmi.wrapper.generic.Simulation;
import org.javafmi.wrapper.v2.Access;
import org.junit.Test;

public class ExperimentalFmuInteractions {
    protected double pitch_dot = 0.0;
    protected double x_dot = 0.0;
    protected double targetXdot = 0.0;
    protected double trim_angle = 0.0;
    protected double control_angle = 0.0;
    protected double timestep = 1.0;

    protected File fmuFile = new File("C:\\Users\\edd9fe\\AppData\\Local\\Temp\\OpenModelica\\OMEdit\\Autopilot2.fmu");
    //protected File fmuFile = new File("C:\\Users\\edd9fe\\Documents\\dev\\gitTakeoff\\takeoff8fmi\\target\\AutopilotFMU1.fmu");

    /**
     * for simple test of FMI wrapper in 64 bit vs. 32 bit runtime without
     * JavaFX timeouts
     *
     */
    @Test
    public void t1() {
	File fmuFile = new File("C:\\Users\\edd9fe\\AppData\\Local\\Temp\\OpenModelica\\OMEdit\\Autopilot2.fmu");
	//File fmuFile = new File("C:\\Users\\edd9fe\\Documents\\dev\\gitTakeoff\\takeoff8fmi\\target\\AutopilotFMU1.fmu");
	Simulation simulation = new Simulation(fmuFile.getAbsolutePath());
	simulation.init(0.0);
	simulation.write("pitch_dot").with(pitch_dot);
	simulation.write("x_dot").with(x_dot);
	simulation.write("targetXdot").with(targetXdot);
	simulation.write("trim_angle").with(trim_angle);
	simulation.write("control_angle").with(control_angle);
	simulation.doStep(timestep);
	trim_angle = simulation.read("trim_angle").asDouble();
	control_angle = simulation.read("control_angle").asDouble();
    }

    @Test
    public void t2() {

	Simulation simulation = new Simulation(fmuFile.getAbsolutePath());
	simulation.init(0.0);
	simulation.write("pitch_dot").with(pitch_dot);
	simulation.write("x_dot").with(x_dot);
	simulation.write("targetXdot").with(targetXdot);
	simulation.write("trim_angle").with(trim_angle);
	simulation.write("control_angle").with(control_angle);
	simulation.doStep(timestep);
	trim_angle = simulation.read("trim_angle").asDouble();
	control_angle = simulation.read("control_angle").asDouble();
	Access access = new Access(simulation);
	 
	List<String> unknownVariables = Arrays.asList("unknown", "variables");
	List<String> knownVariables = Arrays.asList("known", "variables");
	double[] knownDerivatives = new double[] { 1. };
//	access.getDirectionalDerivative(unknownVariables, knownVariables, knownDerivatives);
	simulation.terminate();
    }

}
