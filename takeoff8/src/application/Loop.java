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
package application;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Stack;

import application.flightdynamics.FlightDynamics;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.EventHandler;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Transform;

public class Loop {
    FlightDynamics flight;
    WeatherModel weather;
    final int LOOPS_PER_UPDATE = 1;
    final int UPDATES_PER_S = 60;
    final double TIME_STEP = 1 / ((double) LOOPS_PER_UPDATE) / ((double) UPDATES_PER_S); // simulation update rate in seconds.
    final double PIXEL_PER_M = 20;

    Image glider0;
    Image glider10;
    Image glider20;
    Image glider30;
    Image glider40;
    Image glider50;
    Image collapse;
    Image collapse2;
    Image halfbar;
    Image fullbar;

    final int lines = 2;
    int[] posY;
    int[] posX;

    Boolean pause = false;
    Boolean display = false;
    Boolean autoPilot = false;
    int i;
    int time = 0;
    int markTime = 0;
    double markX = 0.0;
    double markZ = 0.0;

    int count = 0;

    private DoubleProperty mouseXProperty = new SimpleDoubleProperty();
    private DoubleProperty mouseYProperty = new SimpleDoubleProperty();
    private boolean keyPressed;
    private Canvas canvas;
    private GraphicsContext gc;

    void initialize(Canvas canvas) throws IOException {
	this.canvas = canvas;

	//JS:		size(400,400);
	flight = new FlightDynamics();
	weather = new WeatherModel();
	flight.loadPolar();
	setupGlider();

	glider0 = new Image("/media/pgsim_uk/glider0.gif");
	glider10 = new Image("/media/pgsim_uk/glider10.gif");
	glider20 = new Image("/media/pgsim_uk/glider20.gif");
	glider30 = new Image("/media/pgsim_uk/glider30.gif");
	glider40 = new Image("/media/pgsim_uk/glider40.gif");
	glider50 = new Image("/media/pgsim_uk/glider50.gif");
	collapse = new Image("/media/pgsim_uk/collapse.gif");
	collapse2 = new Image("/media/pgsim_uk/collapse2.gif");
	halfbar = new Image("/media/pgsim_uk/halfbar.gif");
	fullbar = new Image("/media/pgsim_uk/fullbar.gif");

	//JS:		imageMode(CENTER);
	posY = new int[lines];
	posX = new int[lines];

	for (i = 0; i < lines; i++) {
	    posY[i] = (int) (canvas.getHeight() / lines * i);
	    posX[i] = (int) (canvas.getWidth() / lines * i);
	}
	gc = canvas.getGraphicsContext2D();
	//JS:		colorMode(RGB);
	//JS:	   	stroke(255);
    }

    public DoubleProperty mouseYProperty() {
	return mouseYProperty;
    }

    void draw() {
	gc.setTransform(new Affine());
	matrixStack.clear();
	double height = canvas.getHeight();
	double width = canvas.getWidth();
	gc.clearRect(0, 0, width, height);
	double mouseY = mouseYProperty.get();
	if (!pause) {
	    time++;
	    if (autoPilot) {
		flight.autopilot_pgsim();
	    } else {
		if (mouseY > ((double) (height / 2))) {
		    flight.trim_angle = Math.toRadians(1.0);
		    flight.control_angle = (mouseY - height / 2) / height * 99.9;
		} else {
		    flight.control_angle = 0.0;
		    flight.trim_angle = Math.toRadians((mouseY - height / 2) / ((double) (height / 2)) * 5.0 + 1.0);
		}
	    }

	    weather.step(UPDATES_PER_S);

	    for (i = 0; i < LOOPS_PER_UPDATE; i++) {
		updateGlider(flight.control_angle, flight.trim_angle, weather.windX, weather.windZ);
	    }
	    flight.averageLD = 0.999 * flight.averageLD
		+ 0.001 * flight.lift / (flight.pilot_drag + flight.glider_drag); // average still air glide

	    if (display) {

		System.out.println("aoa: " + ff(Math.toDegrees(flight.angle_of_attack), 5, 1) + " trim: "
		    + ff(Math.toDegrees(flight.trim_angle), 4, 1) + " control: " + ff(flight.control_angle, 2, 0)
		    + " lift: " + ff(flight.lift, 5, 0) + " drag: " + ff(flight.glider_drag, 5, 0) + " tension: "
		    + ff(flight.line_tension, 5, 0) + " xCP: " + ff(flight.xCP * 100, 3, 0) + " rotation: "
		    + ff(flight.pitch_dot, 7, 3) + " speed: " + ff(flight.x_dot, 5, 1) + " sink: "
		    + ff(flight.z_dot, 5, 2) + " avgL/D: " + ff(flight.averageLD, 5, 2));
	    }

	    background();
	    stroke(1.0, 1.0, 1.0, 1.0);
	    for (i = 0; i < lines; i++) {
		double moved = flight.x_dot / UPDATES_PER_S * PIXEL_PER_M;
		posX[i] = (posX[i] - (int) moved);
		if (posX[i] > width)
		    posX[i] -= width;
		if (posX[i] < 0)
		    posX[i] += width;
		line(posX[i], 0, posX[i], height);

		moved = flight.z_dot / UPDATES_PER_S * PIXEL_PER_M;
		posY[i] = posY[i] - (int) moved;
		if (posY[i] > height)
		    posY[i] -= height;
		if (posY[i] < 0)
		    posY[i] += height;
		line(0, posY[i], width, posY[i]);
	    }
	    pushMatrix();
	    //translate(width / 2, 2 * height / 3);
	    translate(width / 2, height / 3);
	    rotate(-flight.pitch_angle);
	    if (flight.line_tension < -100.0)
		image(collapse2, 0, 0);
	    else if (flight.line_tension < 0.0)
		image(collapse, 0, 0);
	    else if (flight.control_angle <= 0) {
		if (Math.toDegrees(flight.trim_angle) > -1.3)
		    image(glider0, 0, 0);
		else if (Math.toDegrees(flight.trim_angle) > -2.6)
		    image(halfbar, 0, 0);
		else
		    image(fullbar, 0, 0);
	    } else if (flight.control_angle < 10)
		image(glider10, 0, 0);
	    else if (flight.control_angle < 20)
		image(glider20, 0, 0);
	    else if (flight.control_angle < 30)
		image(glider30, 0, 0);
	    else if (flight.control_angle < 40)
		image(glider40, 0, 0);
	    else
		image(glider50, 0, 0);

	    if (weather.windMode > 0) {
		popMatrix();
		strokeRGBA255(75, 225, 255, 255);
		translate(50, 50);
		rotate(Math.atan2(weather.windZ, weather.windX));
		int wind = (int) (4 * Math.sqrt(weather.windX * weather.windX + weather.windZ * weather.windZ));
		line(0, 0, wind, 0);
		line(wind, 0, 0.85 * wind, -0.1 * wind);
		line(wind, 0, 0.85 * wind, 0.1 * wind);
	    }
	}
    }

    private void strokeRGBA255(int r, int g, int b, int a) {
	stroke(r / 255.0, g / 255.0, b / 255.0, a / 255.0);

    }

    Stack<Affine> matrixStack = new Stack<>();

    private void popMatrix() {
	gc.setTransform(matrixStack.pop());

    }

    private void pushMatrix() {
	matrixStack.push(gc.getTransform());

    }

    private void translate(double x, double y) {
	gc.translate(x, y);
	//gc.translate(100, 100);
    }

    private void rotate(double angle) {
	gc.rotate(Math.toDegrees(angle));

    }

    private void stroke(double r, double g, double b, double a) {
	gc.setStroke(new Color(r, g, b, a));

    }

    public Loop() {
	super();
	FlightDynamics flight = new FlightDynamics();
	WeatherModel weather = new WeatherModel();
    }

    private void background() {
	gc.setFill(new Color(0.5, 0.5, 0.5, 1));
	gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

    }

    private void line(double x1, double y1, double x2, double y2) {
	gc.strokeLine(x1, y1, x2, y2);

    }

    private void image(Image img, int x, int y) {
	gc.drawImage(img, x, y);
    }

    //////////////////////////////////////////////////////////
    /**
     * You should only rely on the key char if the event is a key typed event.
     */
    public EventHandler<KeyEvent> createKeyEventHandler() {
	EventHandler<KeyEvent> handler = evt -> {
	    //KeyCode keycode = evt.getCode();
	    String key = evt.getCharacter();
	    if (key.equals("p") || key.equals("P"))
		pause = !pause;
	    else if (key.equals("a") || key.equals("A")) {
		autoPilot = !autoPilot;
		flight.targetXdot = flight.app_windX;
	    } else if (key.equals("t") || key.equals("T"))
		flight.pitch_dot = -6.0; // tumble!!
	    else if (key.equals("r") || key.equals("R"))
		flight.pitch_dot = 6.0; // reverse tumble!!
	    else if (key.equals("d") || key.equals("D"))
		display = !display;
	    else if (key.equals("w") || key.equals("W")) {
		weather.windMode = (weather.windMode + 1) % 4;
		weather.windPhase = 0;
		printWindMode();
	    } else if (key.equals("+") || key.equals("=")) {
		if (weather.windSpeed < 20.0)
		    weather.windSpeed++;
		printWindSpeed();
	    } else if (key.equals("-") || key.equals("_")) {
		if (weather.windSpeed > 0.0)
		    weather.windSpeed--;
		printWindSpeed();
	    } else if (key.equals("[") || key.equals("{")) {
		weather.windPhase = 0;
		weather.windPeriod++;
		printWindFreq();
	    } else if (key.equals("]") || key.equals("}")) {
		weather.windPhase = 0;
		if (weather.windPeriod > 1.0)
		    weather.windPeriod--;
		printWindFreq();
	    } else if (key.equals("h") || key.equals("H")) {
		flight.mass_pilot += 20;
		setupGlider();
		printPilotMass();
	    } else if (key.equals("b") || key.equals("B")) {
		if (flight.mass_pilot > 20)
		    flight.mass_pilot -= 20;
		setupGlider();
		printPilotMass();
	    } else if (key.equals("g") || key.equals("G")) {
		flight.mass_glider++;
		setupGlider();
		printGliderMass();
	    } else if (key.equals("v") || key.equals("V")) {
		if (flight.mass_glider > 0)
		    flight.mass_glider--;
		setupGlider();
		printGliderMass();
	    } else if (key.equals("f") || key.equals("F")) {
		flight.line_length++;
		setupGlider();
		printLineLength();
	    } else if (key.equals("c") || key.equals("C")) {
		if (flight.line_length > 1)
		    flight.line_length--;
		setupGlider();
		printLineLength();
	    } else if (key.equals("m") || key.equals("M")) {
		printMark();
		markTime = time;
		markX = flight.x;
		markZ = flight.z;
	    } else if (key.equals(" ")) { //reset
		display = false;
		flight.x_dot = 10.0;
		flight.z_dot = 1.0;
		flight.pitch_dot = 0.0;
		flight.x = 0.0;
		flight.z = 0.0;
		flight.pitch_angle = 0.0;
		weather.windX = 0.0;
		weather.windZ = 0.0;
		flight.mass_pilot = 80;
		flight.mass_glider = 4;
		flight.line_length = 7.0;
		weather.windSpeed = 5.0;
		weather.windMode = 0;
		setupGlider();
		System.out.println("Reset");
	    }
	};
	return handler;
    }

    void printMark() {
	display = false;
	double deltaT = (time - markTime) / UPDATES_PER_S;
	System.out.println("Time(s) " + ff(deltaT, 5, 1) + ", Dist(m) " + ff(flight.x - markX, 5, 1)
	    + ", Height loss(m) " + ff(markZ - flight.z, 5, 1) + ", Speed(km/h) "
	    + ff(3.6 * (flight.x - markX) / deltaT, 5, 1) + ", Sink(m/s) " + ff((markZ - flight.z) / deltaT, 6, 2)
	    + ", Glide " + ff((flight.x - markX) / (flight.z - markZ), 6, 2));
	return;
    }

    String ff(double num, int len, int decimals) {
	double scale = Math.pow(10, decimals);
	StringBuilder t = new StringBuilder("" + Math.round(num * scale) / scale);
	while (t.length() < len)
	    t.append(" ");
	return t.toString();
    }

    void printWindMode() {
	display = false;
	switch (weather.windMode) {
	case 0:
	    System.out.println("Calm");
	    break;
	case 1:
	    System.out.println("Wind vertical & sinusoidal");
	    break;
	case 2:
	    System.out.println("Wind horizontal & sinusoidal");
	    break;
	default:
	    System.out.println("Turbulent wind");
	    break;
	}
	return;
    }

    void printWindSpeed() {
	display = false;
	System.out.println("Max wind speed: " + Math.round(weather.windSpeed) + " m/s");
	return;
    }

    void printWindFreq() {
	display = false;
	System.out.println("Period of wind changes: " + Math.round(weather.windPeriod) + " s");
	return;
    }

    void printPilotMass() {
	display = false;
	System.out.println("Pilot Mass: " + Math.round(flight.mass_pilot) + " kg");
	return;
    }

    void printGliderMass() {
	display = false;
	System.out.println("Glider Mass: " + Math.round(flight.mass_glider) + " kg");
	return;
    }

    void printLineLength() {
	display = false;
	System.out.println("Line Length: " + Math.round(flight.line_length) + " m");
	return;
    }

    /////////////////////////////////////////////////////////////////////////////////////
    void setupGlider() {

	flight.cm_glider_length = flight.line_length * flight.mass_pilot / (flight.mass_glider + flight.mass_pilot);
	flight.cm_pilot_length = flight.line_length - flight.cm_glider_length;
	flight.total_glider_mass = flight.GLIDER_VOLUME * weather.AIR_DENSITY + flight.mass_glider;
	flight.total_mass = flight.total_glider_mass + flight.mass_pilot;
	flight.moment_of_inetia = flight.total_glider_mass * flight.cm_glider_length * flight.cm_glider_length
	    + flight.mass_pilot * flight.cm_pilot_length * flight.cm_pilot_length;
	return;
    }

    /////////////////////////////////////////////////////////////////////////////////////
    double pwl_lift_coef(double aoa_deg) { // aoa is in degrees

	if (aoa_deg < -180)
	    aoa_deg += 360;
	if (aoa_deg > 180)
	    aoa_deg -= 360;
	int i = (int) Math.floor((aoa_deg + 180.0) / 5.0); // find index into table
	// interpolate the output.
	double slope = (flight.LookUpTable[i + 1] - flight.LookUpTable[i]) / 5.0;
	return (slope * (aoa_deg - Math.floor(aoa_deg / 5.0) * 5.0) + flight.LookUpTable[i]);
    }

    /////////////////////////////////////////////////////////////////////////////////////

    void flight_coefs(double aoa_rad, double flap_angle) {

	double aoa_deg;
	int i, aoa, flap;
	double offset_aoa, offset_flap;
	double[] data0 = new double[3];
	double[] data1 = new double[3];
	double[] result = new double[3];

	aoa_deg = Math.toDegrees(aoa_rad);

	if (aoa_deg > flight.AOA_MAX || aoa_deg < flight.AOA_MIN) {
	    flight.eff_aoa_deg = (aoa_deg + flap_angle);
	    flight.lift_coef = pwl_lift_coef(flight.eff_aoa_deg);
	    flight.drag_coef = 0.5 - 0.2 * Math.cos(Math.toRadians(flight.eff_aoa_deg));
	    flight.xCP = 0.0;
	    return;
	}

	aoa = (int) Math.floor((aoa_deg - flight.AOA_MIN) / flight.AOA_STEP); // find index into table
	flap = (int) Math.floor((flap_angle - flight.FLAP_MIN) / flight.FLAP_STEP);

	offset_aoa = (aoa_deg - flight.aoa_list[aoa]) / flight.AOA_STEP;
	offset_flap = (flap_angle - flight.flap_list[flap]) / flight.FLAP_STEP;

	// bilinear interpolation
	for (i = 0; i < 3; i++) {
	    data0[i] = flight.polar[flap][aoa][i]
		+ offset_aoa * (flight.polar[flap][aoa + 1][i] - flight.polar[flap][aoa][i]);
	    data1[i] = flight.polar[flap + 1][aoa][i]
		+ offset_aoa * (flight.polar[flap + 1][aoa + 1][i] - flight.polar[flap + 1][aoa][i]);

	    result[i] = data0[i] + offset_flap * (data1[i] - data0[i]);
	}

	flight.lift_coef = result[0];
	flight.drag_coef = result[1];
	flight.xCP = result[2] - 0.25;
	return;
    }

    /////////////////////////////////////////////////////////////////////////////////////

    void updateGlider(double control_angle, double trim_angle, double windX, double windZ) {

	double common_factor;

	// The x-axis points through the nose of the glider.
	// The y-axis points to the right of the x-axis (facing in the pilot's direction of view), perpendicular to the x-axis.
	// The z-axis points down through the bottom the craft, perpendicular to the xy plane and satisfying the RH rule.
	// pitch is rotation around the y axis. An increasing pitch increases the aoa. A negative pitching moment lowers the pitch.
	// External winds use the same coordinate system. Positive winds blow towards +x and +z.

	////////////////////////////////////////////////////////
	// Apparent Wind - meaured at the center of mass
	// a poitive windX is a tail wind
	// a positive windZ is air blowing straight down
	// a positive app_windX is normal flow from front to back on the wing
	// a negative app_windZ is normal flow up at the wing (positive aoa)

	flight.app_windX = flight.x_dot - windX;
	flight.app_windZ = flight.z_dot - windZ;
	flight.app_wind_speed = Math.sqrt(flight.app_windX * flight.app_windX + flight.app_windZ * flight.app_windZ);
	// angle is measured realative to the x axis.
	// in normal flight (Vz < 0) the angle is > 0

	flight.app_wind_angle = Math.atan2(flight.app_windZ, flight.app_windX);

	////////////////////////////////////////////////////////
	// Apparent wind, meaured at the glider
	// positive pitch_dot rotates the wing back and lowers apparent wind in x
	// If flying straight down (pitch =-90, Vz > 0) the apparent wind is positive
	// A negative pitch_dot in this position will add to Vz and the apparent wind.
	//
	//

	flight.glider_windX = flight.app_windX
	    - flight.cm_glider_length * flight.pitch_dot * Math.cos(flight.pitch_angle);
	flight.glider_windZ = flight.app_windZ
	    + flight.cm_glider_length * flight.pitch_dot * Math.sin(flight.pitch_angle);

	////////////////////////////////////////////////////////
	// apparent wind at the glider rotated into glider coordinates
	// (parallel to chord line and perpendicular to chord line).
	// A positive glider_v_parallel has wind moving from front to back (normal flight)
	// A negative glider_v_perp gives the wing a positive AOA (normal flight)

	flight.glider_v_parallel = flight.glider_windX * Math.cos(flight.pitch_angle + trim_angle)
	    + -flight.glider_windZ * Math.sin(flight.pitch_angle + trim_angle);
	flight.glider_v_perp = flight.glider_windX * Math.sin(flight.pitch_angle + trim_angle)
	    + flight.glider_windZ * Math.cos(flight.pitch_angle + trim_angle);

	flight.glider_v = Math
	    .sqrt(flight.glider_v_parallel * flight.glider_v_parallel + flight.glider_v_perp * flight.glider_v_perp);
	flight.angle_of_attack = Math.atan2(flight.glider_v_perp, flight.glider_v_parallel);

	////////////////////////////////////////////////////////
	// this function does a trilinear extrapolation of polar data for a typical paraglider foil section
	// The extrapolation is over angle_of_attack, control_angle. XFLR5 was used to generate the polar data.
	//
	flight_coefs(flight.angle_of_attack, control_angle);
	common_factor = 0.50 * weather.AIR_DENSITY * flight.GLIDER_AREA * flight.glider_v * flight.glider_v;

	// LIFT Equation
	flight.lift = flight.lift_coef * common_factor * flight.LIFT_FACTOR;
	// determined LIFT_FACTOR in XFLR5 by comparing lift coef of a paraglider wing model to the lift coeficent of the foil section
	// The lower lift is due to the use of plan area as opposed to projected area and due to other inefficincies of a real wing.

	flight.induced_drag_coef = flight.INDUCED_DRAG_FACTOR * flight.LIFT_FACTOR * flight.LIFT_FACTOR
	    * flight.lift_coef * flight.lift_coef;
	flight.induced_drag = flight.induced_drag_coef * common_factor;
	// determined INDUCED_DRAG_FACTOR in XFLR5 by comparing induced drag coef with the lift coef^2 for a paraglider wing model

	flight.form_drag = (flight.drag_coef + flight.ROUGHNESS_COEF) * common_factor;
	// XFLR showed that the fudge factor to convert from a secion drag coef to the wing drag coef was near unity.

	flight.line_drag = flight.LINE_DRAG_COEF * 0.50 * weather.AIR_DENSITY * flight.LINE_AREA * flight.glider_v
	    * flight.glider_v;

	flight.glider_drag = flight.induced_drag + flight.form_drag + flight.line_drag;

	flight.line_tension = flight.lift * Math.cos(flight.angle_of_attack)
	    + flight.glider_drag * Math.sin(flight.angle_of_attack);
	if (flight.line_tension < 0.0) { // collapse!! zero the lift and increase the drag
	    flight.glider_drag -= flight.lift;
	    flight.lift = 0.0;
	}

	// changes in the center of pressure position slightly change the angle used to calculate the torque on the glider
	flight.cop_adjust_angle = Math.atan2(flight.xCP * flight.CHORD, flight.cm_glider_length);
	// this adjustment may help a bit with pitch stability.

	////////////////////////////////////////////////////////
	// The center of mass is so close to the pilot that the apparent wind is not signifficantly affected by pitch_dot
	// we will just use the apparent wind for pilot drag calculations

	flight.pilot_drag = flight.PILOT_DRAG_COEF * 0.5 * weather.AIR_DENSITY * flight.PILOT_AREA
	    * flight.app_wind_speed * flight.app_wind_speed;

	//////////////////////////////////////////////////////////////
	// apparent wind at glider angle relative to horizontal
	// should be equal to atan2(glider_windZ, glider_windX);

	flight.glider_wind_angle = (flight.angle_of_attack - flight.pitch_angle - trim_angle) % MathConstants.TWO_PI;

	//////////////////////////////////////////////////////////////
	// Equations of motion (F=ma)
	flight.x_dot_dot = (flight.lift * Math.sin(flight.glider_wind_angle)
	    - flight.glider_drag * Math.cos(flight.glider_wind_angle)
	    - flight.pilot_drag * Math.cos(flight.app_wind_angle)) / flight.total_mass;

	flight.z_dot_dot = flight.G - (flight.lift * Math.cos(flight.glider_wind_angle)
	    + flight.glider_drag * Math.sin(flight.glider_wind_angle)
	    + flight.pilot_drag * Math.sin(flight.app_wind_angle)) / flight.total_mass;

	flight.pitch_dot_dot = ((-flight.lift * Math.sin(flight.cop_adjust_angle + flight.angle_of_attack - trim_angle)
	    + flight.glider_drag * Math.cos(flight.cop_adjust_angle + flight.angle_of_attack - trim_angle))
	    * flight.cm_glider_length
	    - flight.pilot_drag * Math.cos(flight.app_wind_angle + flight.pitch_angle) * flight.cm_pilot_length)
	    / flight.moment_of_inetia;

	////////////////////////////////////////////////////////
	// update speed and position
	flight.x_dot = flight.x_dot + flight.x_dot_dot * TIME_STEP;
	flight.z_dot = flight.z_dot + flight.z_dot_dot * TIME_STEP;
	flight.pitch_dot = flight.pitch_dot + flight.pitch_dot_dot * TIME_STEP;

	flight.x = flight.x + flight.x_dot * TIME_STEP;
	flight.z = flight.z + flight.z_dot * TIME_STEP;
	flight.pitch_angle = (flight.pitch_angle + flight.pitch_dot * TIME_STEP) % MathConstants.TWO_PI;

	return;
    }

    //    /**
    //     * new member by Dennis
    //     */
    //    private double noise(double x, double y, double z) {
    //	return ImprovedNoise.noise(x, y, z);
    //    }

    /**
     * new member by Dennis
     * 
     * @throws IOException
     */
    public static RuntimeException uncheck(Throwable cause) {
	throw new RuntimeException(cause);
    }

}
