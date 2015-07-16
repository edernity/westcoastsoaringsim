////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Paraglider Glider Simulation and Visualization Program
// by Peter Spear. peter_spear@telus.net
// Nov 25, 2011
//
//    Copyright (C) 2011  Peter Spear
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU Affero General Public License as
//    published by the Free Software Foundation, either version 3 of the
//    License, or (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU Affero General Public License for more details.
//
//    To see a full copy of the GNU Affero General Public License
//    see <http://www.gnu.org/licenses/>.
//
//
// The physics of this simulation is as accurate as possible.
// Polar Data came from XFLR5 Foil analysis program.
//		http://xflr5.sourceforge.net/xflr5.htm
// The foil and glider design came from the GNULAB2 work of
// 	Pere H. Casellas Laboratori d'envol
//		http://www.laboratoridenvol.com/projects/gnuLAB2/gnuLAB2.en.html
// Big thanks to the Processing.js guys! What a great visualization tool!
//		http://processingjs.org/
// Beats hell out of my original C program that just spewed numbers.
//
// Feel free to modify and tweek this design as much as you like.
// Note that it took quite a bit to get the glider to stall and recover
// in a realistic manner. It is quite sensitive to values of the lift and
// drag coeficients. It is easy to break it. The XFLR5 polar data is only
// used between -16 degrees and +25 degrees aoa. Outside this range values
// for lift and drag were guessed at and tweeked.
//
// Further note to tweekers. Paragliders are almost pitch un-stable. Reduce the
// glider drag just little and the glider will become unstable. Play with it
// for a bit and you will soon give up on the "pendulum stability" idea.
//
// Have fun. Tune up your active flying, work on your infinite tumble and try a loop-de-loop
//

/* 
 * Copyright (c) 2015 Dennis Eder
 * This work is based on a paraglider simulation and visualization game by Peter Spear,
 * Director of the Westcoast Soaring Club in Canada, originally written in ProcessingJS 
 * (see http://www3.telus.net/cschwab/simPG/simGlider.pde), 
 * as announced here: http://www.paraglidingforum.com/viewtopic.php?t=43670 in 2011.
 * The goal is to port his game to JavaFX and use JavaFMI from SIANI/Spain to modularize 
 * pilot, glider and weather models showcasing FMI/FMU interoperability in a fun way.
 * It allows the user to design and exchange custom models and compare against others and even manual flight.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *  
 *  To see a full copy of the GNU Affero General Public License
 *  see <http://www.gnu.org/licenses/>.
 *
 * Contributors: 
 *  Dennis Eder - port of Peter Spear's Javascript-based paraglider simulator (http://www3.telus.net/cschwab/simPG/simGlider.pde) to JavaFX, some rapid drafts to offer my team a debuggable environment and jumpstart into single-day hackfest
 */
package application.flightdynamics;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;

public class FlightDynamics {
    //Polar curve parameters
    public final int FLAP_COUNT = 6;
    public final int AOA_COUNT = 83;
    public final double FLAP_MIN = 0.0;
    public final double FLAP_STEP = 10.0;
    final double FLAP_MAX = 50.0;
    public double[] flap_list;

    public final double AOA_MIN = -16.0;
    public final double AOA_STEP = 0.5;
    public final double AOA_MAX = 25.0;
    public double[] aoa_list;
    public double[][][] polar; //[flap][aoa][field]

    public final double LookUpTable[] = { -0.20, -0.40, -0.03, 0.00, 0.03, 0.060, 0.08, 0.09, 0.10, //-180 to -140
	    0.11, 0.11, 0.10, 0.09, 0.08, 0.07, 0.06, 0.05, 0.04, //-135 to -95
	    0.03, 0.02, 0.01, 0.00, -0.01, -0.02, -0.03, -0.04, -0.05, // -90 to -50
	    -0.06, -0.07, -0.08, -0.09, -0.10, -0.11, -0.12, -0.13, -0.14, // -45 to -5
	    -0.20, -0.40, -0.30, 0.00, 1.7, 1.6, 1.5, 1.4, 1.3, 1.2, 1.1, 1.0, 0.9, 0.80, 0.7, 0.60, 0.5, 0.4, 0.3, 0.2,
	    0.1, 0.00, -0.1, -0.2, -0.3, -0.4, -0.5, -0.6, -0.7, -0.8, -0.9, -1.0, -1.1, -1.2, -1.3, -1.4, // 130 to 175
	    -1.5, -0.6 }; // 180 to 185

    // glider model parameters

    /**
     * m/s^2 - horizontal acceleration of cm
     */
    public double x_dot_dot = 0.0;   
    /**
     * m/s^2 - vertical acceleration of cm
     */
    public double z_dot_dot = 0.0; 
    /**
     * radians/s/s - rotational acceleration around cm
     */
    public double pitch_dot_dot = 0.0;  
    /**
     * m/s - horizontal speed
     */
    public double x_dot = 10.0;   
    /**
     * m/s - vertical speed
     */
    public double z_dot = 1.0;   
    /**
     * radians/s - rotational speed
     */
    public double pitch_dot = 0.0;   
    /**
     * m - horizontal position
     */
    public double x = 0.0;   
    /**
     * m - vertical position
     */
    public double z = 0.0;  
    /**
     * radians - angle of incedence. Angle between the chord line and the horizon
     */
    public double pitch_angle = 0.0;   
    /**
     * radians - angle of attack. Angle between the chord line and the apparent wind
     */
    public double angle_of_attack;   
    /**
     *  m/s -- apparent wind speed at glider
     */
    public double glider_v;  
    /**
     *  m/s -- apparent wind speed at glider parallel to glider
     */
    public double glider_v_parallel;  
    /**
     * m/s -- apparent wind speed at glider perpendicular to glider
     */
    public double glider_v_perp;  
    /**
     * m/s -- horizontal component of apparent wind speed at pilot
     */
    double pilot_v_x;   
    /**
     * m/s -- vertical component of apparent wind speed at pilot
     */
    double pilot_v_z;   
    /**
     * m/s -- apparent wind speed at pilot
     */
    double pilot_v;  
    /**
     * radians -- angle of apparent wind at pilot relative to horizontal
     */
    double pilot_angle;  
    /**
     * m/s -- horizontal component of apparent wind speed at the center of mass (CM)
     */
    public double app_windX;   
    /**
     * m/s -- vertical component of apparent wind speed at CM
     */
    public double app_windZ;   
    /**
     *  m/s -- horizontal component of apparent wind speed at the center of mass (CM)
     */
    public double app_wind_speed;  
    /**
     * radians -- angle of apparent wind at CM relative to horizontal
     */
    public double app_wind_angle;   
    /**
     * m/s -- horizontal component of apparent wind speed at glider
     */
    public double glider_windX;  
    /**
     * m/s -- vertical component of apparent wind speed at glider
     */
    public double glider_windZ;   
    /**
     * radians -- angle of apparent wind at glider relative to horizontal
     */
    public double glider_wind_angle;  
    /**
     * section lift coeficint
     */
    public double lift_coef;  
    /**
     * section form and skin drag coeficint
     */
    public double drag_coef;  			 
    /**
     * -- shift in Center of Pressure away from 1/4 chord position + is rear ward, - is forward.
     */
    public double xCP;  				
    /**
     * Radians -- the change in COP position changes the angle at which torques are appled at the glider
     */
    public double cop_adjust_angle;  
    /**
     * N - glider lift
     */
    public double lift;  
    /**
     * N - glider induced drag (wing vorticies)
     */
    public double induced_drag;  
    /**
     * N - pressure & skin drag
     */
    public double form_drag;  				
    /**
     * N - total of form, skin and induced drag
     */
    public double glider_drag;  
    /**
     * N -- force on the pilot due to drag
     */
    public double pilot_drag;   
    /**
     * 
     */
    public double induced_drag_coef;
    /**
     * N
     */
    public double line_drag;   
    /**
     * N
     */
    public double line_tension;  
    /**
     * radians -- glider chord line tilt relative to a line perpendiucular to the 1/4 chord to pilot line <br>
     * positive trim tilts the glider back. Negative trim = speed bar.
     */
    public double trim_angle = 0.0;  
			    
    /**
     * in degrees on the trailing edge brake flap (76% of chord). XFLR5
     */
    public double control_angle = 0.0;   
    public double eff_aoa_deg;

    /**
     * kg
     */
    public double mass_pilot = 80;   
    /**
     * kg
     */
    public double mass_glider = 4;  
    /**
     * m
     */
    public double line_length = 7.0;   
    /**
     * m - center of mass to glider length
     */
    public double cm_glider_length;  
    /**
     * m - center of mass to pilot length
     */
    public double cm_pilot_length;  
    /**
     * kg
     */
    public double total_glider_mass;   
    /**
     * kg
     */
    public double total_mass;  
    /**
     * kg m^2
     */
    public double moment_of_inetia;   

    // the following constants were tweeked to try and get a reasonable polar response curve for the
    // glider: stall 22km/h, min sink 1.0 m/s, trim sink 1.3m/s @ 39km/h.

    public final double CHORD = 2.5;
    public final double G = 9.8; // m/s^2
    public final double GLIDER_AREA = 26.0; // m^2
    public final double PILOT_DRAG_COEF = 1.30;
    public final double LINE_DRAG_COEF = 1.10;
    public final double PILOT_AREA = 0.25; // m^2 - frontal area
    
    public final double LINE_AREA = 3 * 0.0014; // m^2
    public final double GLIDER_VOLUME = 2.5; // m^3
    public final double LIFT_FACTOR = 0.514; // converts section lift to wing lift
    public final double INDUCED_DRAG_FACTOR = 0.075; // induced drag coef is proportional to lift coef squared times this constant
    public final double ROUGHNESS_COEF = 0.008; // XFLR5 does't account for skin roughness which could be quite significant

    public double targetXdot;
    public double feedback;
    public double averageLD = 9.0;
    
    public void autopilot_pgsim() {
 	feedback = pitch_dot - (x_dot - targetXdot) / 100; // slowly adjust pitch angle to match target air speed
 	if (pitch_dot < 0.0) { // pitching forward
 	    if (trim_angle >= Math.toRadians(1.0)) {
 		control_angle -= Math.toDegrees(feedback / 20); // quickly adjust controls to minimize pitching motions
 	    } else {
 		trim_angle -= feedback / 200;
 	    }
 	} else { // pitching back
 	    if (control_angle <= 0.0) {
 		if (trim_angle > Math.toRadians(-4.0))
 		    trim_angle -= feedback / 200;
 	    } else {
 		control_angle -= Math.toDegrees(feedback / 20);
 	    }
 	}
 	if (control_angle > 49.9)
 	    control_angle = 49.9;
 	else if (control_angle < 0.0)
 	    control_angle = 0.0;
 	if (trim_angle > Math.toRadians(1.0))
 	    trim_angle = Math.toRadians(1.0);
 	else if (trim_angle < Math.toRadians(-4.0))
 	    trim_angle = Math.toRadians(-4.0);

     }
    /////////////////////////////////////////////////////////////////////////////////////
    public void loadPolar() throws IOException {
	int aoa, flap;
	 polar = new double[ FLAP_COUNT][ AOA_COUNT][3]; //[flap][aoa][field]
	String[] files = { "/data/flightmodels/pgsim_uk/polar18flap0.csv",
		"/data/flightmodels/pgsim_uk/polar18flap10.csv", "/data/flightmodels/pgsim_uk/polar18flap20.csv",
		"/data/flightmodels/pgsim_uk/polar18flap30.csv", "/data/flightmodels/pgsim_uk/polar18flap40.csv",
		"/data/flightmodels/pgsim_uk/polar18flap50.csv" };

	String[] temp = new String[3];

	for (flap = 0; flap < FLAP_COUNT; flap++) {
	    String lines[] = loadStrings(files[flap]);
	    for (aoa = 0; aoa < AOA_COUNT; aoa++) {
		//				println(lines[aoa]);
		temp = lines[aoa].split(",");
		for (int j = 0; j < 3; j++) {
		    polar[flap][aoa][j] = Double.parseDouble(temp[j]);
		}
	    }
	}
	flap_list = new double[FLAP_COUNT];
	aoa_list = new double[AOA_COUNT];
	for (flap = 0; flap < FLAP_COUNT; flap++)
	    flap_list[flap] = FLAP_MIN + FLAP_STEP * flap;
	for (aoa = 0; aoa < AOA_COUNT; aoa++)
	    aoa_list[aoa] = AOA_MIN + AOA_STEP * aoa;
	return;
    }
    /** 
     * @throws IOException
     */
    private String[] loadStrings(String filename) throws IOException {
	URL url = getClass().getResource(filename);
	File file = null;
	if (url == null)
	    throw new IOException("file '" + filename + "' not found");
	file = new java.io.File(url.getFile());
	if (!file.exists())
	    throw new IOException("file '" + filename + "' not found");
	return Files.readAllLines(file.toPath()).toArray(new String[0]);

    }
}
