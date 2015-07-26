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

/* @pjs pauseOnBlur="true";
preload="data/glider0.gif, data/glider10.gif, data/glider20.gif, data/glider30.gif, data/glider40.gif, data/glider50.gif, data/collapse.gif, data/collapse2.gif, data/halfbar.gif, data/fullbar.gif";
 globalKeyEvents="true";
 */

//Polar curve parameters
final int FLAP_COUNT = 6;
final int  AOA_COUNT = 83;
final float FLAP_MIN = 0.0;
final float FLAP_STEP = 10.0;
final float FLAP_MAX = 50.0;
float [] flap_list;

final float AOA_MIN = -16.0;
final float AOA_STEP = 0.5;
final float AOA_MAX = 25.0;
float [] aoa_list;
float [][][] polar; //[flap][aoa][field]

final float LookUpTable[] =
	{ -0.20, -0.40, -0.03, 0.00, 0.03, 0.060, 0.08, 0.09, 0.10, 					//-180 to -140
	 0.11, 0.11, 0.10, 0.09, 0.08, 0.07, 0.06, 0.05, 0.04,					//-135 to -95
	 0.03, 0.02, 0.01, 0.00, -0.01, -0.02, -0.03, -0.04, -0.05,  			// -90 to -50
	-0.06, -0.07, -0.08, -0.09, -0.10, -0.11, -0.12, -0.13, -0.14,   		// -45 to -5
	-0.20, -0.40, -0.30, 0.00, 1.7, 1.6, 1.5, 1.4, 1.3,
	 1.2, 1.1, 1.0, 0.9, 0.80, 0.7, 0.60, 0.5, 0.4,
	 0.3, 0.2, 0.1, 0.00, -0.1, -0.2, -0.3, -0.4, -0.5,
	 -0.6, -0.7, -0.8, -0.9, -1.0, -1.1, -1.2, -1.3, -1.4, 				// 130 to 175
	 -1.5, -0.6};																			// 180 to 185

// glider model parameters

float x_dot_dot = 0.0;			// m/s^2			- horizontal acceleration of cm
float z_dot_dot = 0.0;			// m/s^2			- vertical acceleration of cm
float pitch_dot_dot = 0.0;		// radians/s/s - rotational acceleration around cm
float x_dot = 10.0;				// m/s			- horizontal speed
float z_dot = 1.0;				// m/s			- vertical speed
float pitch_dot = 0.0;			// radians/s	- rotational speed
float x = 0.0;						// m				- horizontal position
float z = 0.0;						// m				- vertical position
float pitch_angle = 0.0;		// radians		- angle of incedence. Angle between the chord line and the horizon
float angle_of_attack;			// radians		- angle of attack. Angle between the chord line and the apparent wind
float glider_v;					// m/s			-- apparent wind speed at glider
float glider_v_parallel;		// m/s			-- apparent wind speed at glider parallel to glider
float glider_v_perp;				// m/s			-- apparent wind speed at glider perpendicular to glider
float pilot_v_x;					// m/s			-- horizontal component of apparent wind speed at pilot
float pilot_v_z;					// m/s			-- vertical component of apparent wind speed at pilot
float pilot_v;						// m/s			-- apparent wind speed at pilot
float pilot_angle;				// radians		-- angle of apparent wind at pilot relative to horizontal
float app_windX;					// m/s			-- horizontal component of apparent wind speed at the center of mass (CM)
float app_windZ;					// m/s			-- vertical component of apparent wind speed at CM
float app_wind_speed;			// m/s			-- horizontal component of apparent wind speed at the center of mass (CM)
float app_wind_angle;			// radians		-- angle of apparent wind at CM relative to horizontal
float glider_windX;				// m/s			-- horizontal component of apparent wind speed at glider
float glider_windZ;				// m/s			-- vertical component of apparent wind speed at glider
float glider_wind_angle;		// radians		-- angle of apparent wind at glider relative to horizontal

float lift_coef;					//				-- section lift coeficint
float drag_coef;					//				-- section form and skin drag coeficint
float xCP;							//				-- shift in Center of Pressure away from 1/4 chord position + is rear ward, - is forward.
float cop_adjust_angle;			//	Radians		-- the change in COP position changes the angle at which torques are appled at the glider
float lift;							// N				- glider lift
float induced_drag;				// N				- glider induced drag (wing vorticies)
float form_drag;					// N				- pressure & skin drag
float glider_drag;				// N				- total of form, skin and induced drag
float pilot_drag;					// N				-- force on the pilot due to drag
float induced_drag_coef;
float line_drag;					// N
float line_tension;				// N
float trim_angle = 0.0;			// radians 	-- glider chord line tilt relative to a line perpendiucular to the 1/4 chord to pilot line
										// positive trim tilts the glider back. Negative trim = speed bar.
float control_angle = 0.0;		// in degrees on the trailing edge brake flap (76% of chord). XFLR5
float eff_aoa_deg;

float windX = 0.0;				// m/s (+ is tail wind)
float windZ = 0.0;				// m/s (+ is down wind)
float mass_pilot = 80;			// kg
float mass_glider = 4;			// kg
float line_length = 7.0;		// m
float cm_glider_length;			// m - center of mass to glider length
float cm_pilot_length;			// m - center of mass to pilot length
float total_glider_mass;		// kg
float total_mass;					// kg
float moment_of_inetia;			// kg m^2


// the following constants were tweeked to try and get a reasonable polar response curve for the
// glider: stall 22km/h, min sink 1.0 m/s, trim sink 1.3m/s @ 39km/h.

final float CHORD = 2.5;
final float G =  9.8;			// m/s^2
final float GLIDER_AREA = 26.0;			// m^2
final float PILOT_DRAG_COEF = 1.30;
final float LINE_DRAG_COEF = 1.10;
final float PILOT_AREA = 0.25;			// m^2 - frontal area
final float AIR_DENSITY = 1.20;			// kg/m^3
final float LINE_AREA = 3 * 0.0014;	// m^2
final float GLIDER_VOLUME = 2.5;		// m^3
final float LIFT_FACTOR = 0.514;		// converts section lift to wing lift
final float INDUCED_DRAG_FACTOR = 0.075;		// induced drag coef is proportional to lift coef squared times this constant
float roughness_coef = 0.008;		// XFLR5 does't account for skin roughness which could be quite significant


final float UPDATES_PER_S = 60;
final float TIME_STEP = 1/UPDATES_PER_S;		// simulation update rate in seconds.
final float PIXEL_PER_M = 20;

PImage glider0;
PImage glider10;
PImage glider20;
PImage glider30;
PImage glider40;
PImage glider50;
PImage collapse;
PImage collapse2;
PImage halfbar;
PImage fullbar;

final int lines = 2;
float [] posY;
float [] posX;

Boolean pause = false;
Boolean display = false;
Boolean autoPilot = false;
int i;
int time = 0;
int markTime = 0;
float markX = 0.0;
float markZ = 0.0;

float targetXdot;
float feedback;
float averageLD = 9.0;
float avgGlide = 9.0;
float windSpeed = 5.0;
float windPeriod = 15.0;
int windMode = 0;
int windPhase = 0;
int count=0;
float noiseSpace = 0.0;

void setup() {
	size(400,400);
	loadPolar();
	setupGlider();
	println("");

	glider0 = loadImage("data/glider0.gif");
	glider10 = loadImage("data/glider10.gif");
	glider20 = loadImage("data/glider20.gif");
	glider30 = loadImage("data/glider30.gif");
	glider40 = loadImage("data/glider40.gif");
	glider50 = loadImage("data/glider50.gif");
	collapse = loadImage("data/collapse.gif");
	collapse2 = loadImage("data/collapse2.gif");
	halfbar = loadImage("data/halfbar.gif");
	fullbar = loadImage("data/fullbar.gif");


	imageMode(CENTER);
	posY = new float [lines];
	posX = new float [lines];

	for (i=0;i<lines;i++){
		posY[i] = float(height/lines*i);
		posX[i] = float(width/lines*i);
	}

	colorMode(RGB);
   stroke(255);
}


void draw(){

   if (count>0) count--;
   else if (keyPressed) {
		count = 20; // timeout to prevent typomatic multiple inputs
		getKeyInput();
   }


	if (!pause){
		time++;

		if (autoPilot){
			feedback = pitch_dot - (x_dot-targetXdot)/100;  // slowly adjust pitch angle to match target air speed
			if (pitch_dot < 0.0) {		// pitching forward
				if (trim_angle >= radians(1.0)) {
					control_angle-=degrees(feedback/20);		// quickly adjust controls to minimize pitching motions
				} else {
					trim_angle -=feedback/200;
				}
			} else {		// pitching back
				if (control_angle <= 0.0) {
					if (trim_angle > radians(-4.0)) trim_angle -=feedback/200;
				} else {
					control_angle-=degrees(feedback/20);
				}
			}
			if (control_angle > 49.9) control_angle = 49.9;
			else if (control_angle < 0.0) control_angle = 0.0;
			if (trim_angle > radians(1.0)) trim_angle = radians(1.0);
			else if (trim_angle < radians(-4.0)) trim_angle = radians(-4.0);
		} else {
			if (mouseY > float(height/2)){
				trim_angle = radians(1.0);
				control_angle = float(mouseY-height/2)/height * 99.9 ;
			} else {
				control_angle = 0.0;
				trim_angle = radians((mouseY-height/2)/float(height/2)*5.0+1.0);
			}
		}


		switch(windMode){
			case 1: // sinusoidal vertical variations with 10s period
				windX = 0.0;
				windPhase++;
				windZ = windSpeed * sin (TWO_PI * windPhase /UPDATES_PER_S / windPeriod);
				break;
			case 2: // sinusoidal horizontal variations with 10s period
				windZ = 0.0;
				windPhase++;
				windX = windSpeed * sin (TWO_PI * windPhase /UPDATES_PER_S / windPeriod);
				break;
			case 3: // Random vertical & horizontal wind
				noiseSpace+= 0.05/windPeriod;
				windX = windSpeed * 4.0 * (noise(noiseSpace, 0)-0.5);
				windZ = windSpeed * 4.0 * (noise(0, noiseSpace)-0.5);
				break;
			default:
				windZ = 0.0;
				windX = 0.0;
		}

	        updateGlider(control_angle, trim_angle, windX, windZ);

		averageLD = 0.999 * averageLD + 0.001 * lift/(pilot_drag+glider_drag); // average still air glide
		avgGlide = 0.999 * avgGlide + 0.001 * x_dot/z_dot; // average moving air glide

		if (display) {

			println("aoa: " + ff(degrees(angle_of_attack),5,1) + " trim: " + ff(degrees(trim_angle),4,1) + " control: " + ff(control_angle,2,0)
			+ " lift: " + ff(lift,5,0) + " drag: " + ff(glider_drag,5,0)+ " tension: " + ff(line_tension,5,0)
			+ " xCP: " + ff(xCP*100,3,0) + " speed: " + ff(x_dot,5,1)
			+ " sink: " + ff(z_dot,5,2) + " avgL/D: " + ff(averageLD,5,2) + " avgGlide: " + ff(avgGlide,5,2));
		}

		background(128);
	   stroke(255);
		for(i=0;i<lines;i++){
			posX[i] = posX[i] - x_dot/UPDATES_PER_S*PIXEL_PER_M;
			if (posX[i]>width) posX[i] -= float(width);
			if (posX[i]<0) posX[i] += float(width);
			line(int(posX[i]), 0 , int(posX[i]), height);

			posY[i] = posY[i] -  z_dot/UPDATES_PER_S*PIXEL_PER_M;
			if (posY[i]>height) posY[i] -= float(height);
			if (posY[i]<0) posY[i] += float(height);
			line(0, int(posY[i]), width , int(posY[i]));
		}
		pushMatrix();
		translate(width/2,2*height/3);
		rotate(-pitch_angle);
		if (line_tension < -100.0 ) image(collapse2,0,0);
		else if (line_tension < 0.0 ) image(collapse,0,0);
		else if (control_angle <= 0 ){
			if (degrees(trim_angle) >-1.3)  image(glider0,0,0);
			else if (degrees(trim_angle) >-2.6)  image(halfbar,0,0);
			else image(fullbar,0,0);
		}
		else if (control_angle < 10 ) image(glider10,0,0);
		else if (control_angle < 20 ) image(glider20,0,0);
		else if (control_angle < 30 ) image(glider30,0,0);
		else if (control_angle < 40 ) image(glider40,0,0);
		else image(glider50,0,0);
	        popMatrix();

		if (windMode != 0){
			stroke(255,0,0);
			translate(50,50);
			rotate(atan2(windZ,windX));
			int wind = int(4 * sqrt(windX*windX + windZ*windZ));
			line(0,0,wind,0);
			line(wind,0,0.85*wind,-0.1*wind);
			line(wind,0,0.85*wind,0.1*wind);
		}
	}
}




//////////////////////////////////////////////////////////

void getKeyInput(){
	if (key == 'p' || key == 'P')  pause = !pause;
	else if (key == 'a' || key == 'A'){
		autoPilot = !autoPilot;
		targetXdot = app_windX;
	}
	else if (key == 't' || key == 'T') pitch_dot = -6.0 ; // tumble!!
	else if (key == 'r' || key == 'R') pitch_dot = 6.0 ; // reverse tumble!!
	else if (key == 'd' || key == 'D') display = !display;
	else if (key == 'w' || key == 'W') {
		windMode = (windMode + 1)%4 ;
		windPhase = 0;
		printWindMode();
	}
	else if (key == '+' || key == '='){
		if (windSpeed < 20.0) windSpeed++;
		printWindSpeed();
	}
	else if (key == '-' || key == '_') {
		if (windSpeed > 0.0) windSpeed--;
		printWindSpeed();
	}
	else if (key == '[' || key == '{'){
		windPhase = 0;
		windPeriod++;
		printWindFreq();
	}
	else if (key == ']' || key == '}'){
		windPhase = 0;
		if (windPeriod > 1.0) windPeriod--;
		printWindFreq();
	}
	else if (key == 'h' || key == 'H'){
		mass_pilot+=20;
		setupGlider();
		printPilotMass();
	}
	else if (key == 'b' || key == 'B'){
		if (mass_pilot > 20) mass_pilot-=20;
		setupGlider();
		printPilotMass();
	}
	else if (key == 'g' || key == 'G'){
		mass_glider++;
		setupGlider();
		printGliderMass();
	}
	else if (key == 'v' || key == 'V'){
		if (mass_glider > 0) mass_glider--;
		setupGlider();
		printGliderMass();
	}
	else if (key == 'f' || key == 'F'){
		line_length++;
		setupGlider();
		printLineLength();
	}
	else if (key == 'c' || key == 'C'){
		if(line_length > 1) line_length--;
		setupGlider();
		printLineLength();
	}
	else if (key == 'j' || key == 'J'){
		roughness_coef+=0.001;
		printDrag();
	}
	else if (key == 'n' || key == 'N'){
		if(roughness_coef > 0.0) roughness_coef-=0.001;
		printDrag();
	}
	else if (key == 'm' || key == 'M'){
		printMark();
		markTime = time;
		markX = x;
		markZ = z;
	}
	else if (key == ' '){ //reset
		display = false;
		x_dot = 10.0;
		z_dot = 1.0;
		pitch_dot = 0.0;
		x = 0.0;
		z = 0.0;
		pitch_angle = 0.0;
		windX = 0.0;
		windZ = 0.0;
		mass_pilot = 80;
		mass_glider = 4;
		line_length = 7.0;
		windSpeed = 5.0;
		windMode = 0;
		roughness_coef=0.008;
		setupGlider();
		println("Reset");
	}
}

void printMark(){
	display = false;
	float deltaT = (time-markTime)/UPDATES_PER_S;
	println("Time(s) " + ff(deltaT,5,1) + ", Dist(m) " + ff(x-markX,5,1) + ", Height loss(m) " + ff(markZ-z,5,1) +
				", Speed(km/h) " + ff(3.6*(x-markX)/deltaT,5,1) + ", Sink(m/s) " + ff((markZ-z)/deltaT,6,2) +
				", Glide " + ff((x-markX)/(z-markZ),6,2));
	return;
}

String  ff(float num, int len, int decimals) {
	float scale = pow(10,decimals);
	String t = str(round(num*scale)/scale);
	while (t.length() < len) t = t + " ";
	return(t);
}


void printWindMode(){
	display = false;
	switch(windMode){
		case 0 : println("Calm"); break;
		case 1 : println("Wind vertical & sinusoidal"); break;
		case 2 : println("Wind horizontal & sinusoidal"); break;
		default : println("Turbulent wind"); break;
	}
	return;
}
void printWindSpeed(){
	display = false;
	println("Max wind speed: " + round(windSpeed) + " m/s");
	return;
}
void printDrag(){
	display = false;
	println("Drag: " + ff(roughness_coef,6,3));
	return;
}
void printWindFreq(){
	display = false;
	println("Period of wind changes: " + round(windPeriod) + " s");
	return;
}
void printPilotMass(){
	display = false;
	println("Pilot Mass: " + round(mass_pilot) + " kg");
	return;
}
void printGliderMass(){
	display = false;
	println("Glider Mass: " + round(mass_glider) + " kg");
	return;
}

void printLineLength(){
	display = false;
	println("Line Length: " + round(line_length) + " m");
	return;
}

/////////////////////////////////////////////////////////////////////////////////////
void setupGlider(){

	cm_glider_length = line_length*mass_pilot/(mass_glider+mass_pilot);
	cm_pilot_length = line_length-cm_glider_length;
	total_glider_mass = GLIDER_VOLUME*AIR_DENSITY + mass_glider;
	total_mass = total_glider_mass + mass_pilot;
	moment_of_inetia = total_glider_mass*cm_glider_length*cm_glider_length + mass_pilot*cm_pilot_length*cm_pilot_length;
//	println("moment_of_inetia "+moment_of_inetia);
	return;
}

/////////////////////////////////////////////////////////////////////////////////////
void loadPolar(){
	int aoa, flap;
	polar = new float [FLAP_COUNT][AOA_COUNT][3]; //[flap][aoa][field]
	String [] files = { "data/polar18flap0.csv",
								"data/polar18flap10.csv",
								"data/polar18flap20.csv",
								"data/polar18flap30.csv",
								"data/polar18flap40.csv",
								"data/polar18flap50.csv"};

	String [] temp = new String [3];

	for (flap=0; flap< FLAP_COUNT; flap++) {
		String lines[] = loadStrings(files[flap]);
		for (aoa=0; aoa<AOA_COUNT;aoa++) {
//			println(lines[aoa]);
			temp= split(lines[aoa], ',');
			for (int j=0; j<3; j++){
		  		polar[flap][aoa][j]=float(temp[j]);
			}
		}
	}
	flap_list = new float[FLAP_COUNT];
	aoa_list = new float[AOA_COUNT];
	for (flap=0; flap<FLAP_COUNT; flap++) flap_list[flap] = FLAP_MIN + FLAP_STEP*flap;
	for (aoa=0; aoa<AOA_COUNT;aoa++) aoa_list[aoa]=AOA_MIN+AOA_STEP*aoa;
	return;
}


/////////////////////////////////////////////////////////////////////////////////////
float pwl_lift_coef(float aoa_deg){ // aoa is in degrees
	int i;
	if (aoa_deg < - 180) aoa_deg+=360;
	if (aoa_deg > 180) aoa_deg-=360;
	i = (int) floor((aoa_deg+180.0)/5.0); // find index into table
		// interpolate the output.
	float slope = (LookUpTable[i+1] - LookUpTable[i])/5.0;
	return( slope * (aoa_deg - floor(aoa_deg/5.0)*5.0) + LookUpTable[i]);
}



/////////////////////////////////////////////////////////////////////////////////////

void flight_coefs(float aoa_rad, float flap_angle){

	float aoa_deg;
	int i, aoa, flap;
	float offset_aoa, offset_flap;
	float [] data0 = new float[3];
	float [] data1 = new float[3];
	float [] result = new float[3];

	aoa_deg = degrees(aoa_rad);

	if (aoa_deg >AOA_MAX || aoa_deg < AOA_MIN){
		eff_aoa_deg = (aoa_deg + flap_angle);
		lift_coef = pwl_lift_coef(eff_aoa_deg);
		drag_coef = 0.5 - 0.2 * cos(radians(eff_aoa_deg));
		xCP = 0.0;
		return;
	}

	aoa = (int) floor((aoa_deg - AOA_MIN)/AOA_STEP); 	// find index into table
	flap = (int) floor((flap_angle - FLAP_MIN)/FLAP_STEP);

	offset_aoa = (aoa_deg - aoa_list[aoa])/AOA_STEP;
	offset_flap = (flap_angle - flap_list[flap]) /FLAP_STEP;

	// bilinear interpolation
	for(i=0;i<3;i++){
		data0[i] = polar[flap][aoa][i] + offset_aoa * (polar[flap][aoa+1][i]-polar[flap][aoa][i]);
		data1[i] = polar[flap+1][aoa][i] + offset_aoa * (polar[flap+1][aoa+1][i]-polar[flap+1][aoa][i]);

		result[i] = data0[i] + offset_flap*(data1[i]-data0[i]);
	}

	lift_coef = result[0];
	drag_coef = result[1];
	xCP = result[2]-0.25;
	return;
}



/////////////////////////////////////////////////////////////////////////////////////

void updateGlider(float control_angle, float trim_angle, float windX, float windZ){

	float common_factor;

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

	app_windX = x_dot - windX;
	app_windZ = z_dot - windZ;
	app_wind_speed = sqrt(app_windX*app_windX + app_windZ*app_windZ);
// angle is measured realative to the x axis.
// in normal flight (Vz < 0) the angle is > 0

	app_wind_angle = atan2(app_windZ, app_windX);

////////////////////////////////////////////////////////
// Apparent wind, meaured at the glider
// positive pitch_dot rotates the wing back and lowers apparent wind in x
// If flying straight down (pitch =-90, Vz > 0) the apparent wind is positive
// A negative pitch_dot in this position will add to Vz and the apparent wind.
//
//

	glider_windX = app_windX - cm_glider_length * pitch_dot * cos(pitch_angle);
	glider_windZ = app_windZ + cm_glider_length * pitch_dot * sin(pitch_angle);

////////////////////////////////////////////////////////
// apparent wind at the glider rotated into glider coordinates
// (parallel to chord line and perpendicular to chord line).
// A positive glider_v_parallel has wind moving from front to back (normal flight)
// A negative glider_v_perp gives the wing a positive AOA (normal flight)

	glider_v_parallel = glider_windX * cos(pitch_angle+trim_angle) +
							  -glider_windZ * sin(pitch_angle+trim_angle);
	glider_v_perp     = glider_windX * sin(pitch_angle+trim_angle) +
							  glider_windZ * cos(pitch_angle+trim_angle);


	glider_v = sqrt(glider_v_parallel*glider_v_parallel + glider_v_perp*glider_v_perp);
	angle_of_attack = atan2(glider_v_perp, glider_v_parallel);

////////////////////////////////////////////////////////
// this function does a trilinear extrapolation of polar data for a typical paraglider foil section
// The extrapolation is over angle_of_attack, control_angle. XFLR5 was used to generate the polar data.
//
	flight_coefs(angle_of_attack, control_angle);
	common_factor = 0.50 * AIR_DENSITY * GLIDER_AREA * glider_v * glider_v;

// LIFT Equation
	lift = lift_coef * common_factor * LIFT_FACTOR;
  // determined LIFT_FACTOR in XFLR5 by comparing lift coef of a paraglider wing model to the lift coeficent of the foil section
  // The lower lift is due to the use of plan area as opposed to projected area and due to other inefficincies of a real wing.

	induced_drag_coef = INDUCED_DRAG_FACTOR * LIFT_FACTOR * LIFT_FACTOR * lift_coef *lift_coef;
	induced_drag = induced_drag_coef * common_factor;
  // determined INDUCED_DRAG_FACTOR in XFLR5 by comparing induced drag coef with the lift coef^2 for a paraglider wing model

	form_drag = (drag_coef + roughness_coef) * common_factor;
	// XFLR showed that the fudge factor to convert from a secion drag coef to the wing drag coef was near unity.

	line_drag = LINE_DRAG_COEF * 0.50 * AIR_DENSITY * LINE_AREA * glider_v * glider_v;

	glider_drag = induced_drag + form_drag + line_drag;

	line_tension = lift * cos(angle_of_attack) + glider_drag * sin(angle_of_attack);
	if (line_tension < 0.0){	// collapse!! zero the lift and increase the drag
		glider_drag -= lift;
		lift = 0.0;
	}

// changes in the center of pressure position slightly change the angle used to calculate the torque on the glider
	cop_adjust_angle = atan2(xCP * CHORD,cm_glider_length);
// this adjustment may help a bit with pitch stability.

////////////////////////////////////////////////////////
// The center of mass is so close to the pilot that the apparent wind is not signifficantly affected by pitch_dot
// we will just use the apparent wind for pilot drag calculations

	pilot_drag = PILOT_DRAG_COEF * 0.5 * AIR_DENSITY * PILOT_AREA * app_wind_speed * app_wind_speed;

//////////////////////////////////////////////////////////////
// apparent wind at glider angle relative to horizontal
// should be equal to atan2(glider_windZ, glider_windX);

	glider_wind_angle = (angle_of_attack - pitch_angle - trim_angle)%TWO_PI;

//////////////////////////////////////////////////////////////
// Equations of motion (F=ma)
	x_dot_dot= (lift*sin(glider_wind_angle) -
		glider_drag*cos(glider_wind_angle)-
		pilot_drag*cos(app_wind_angle)) /total_mass;

	z_dot_dot= G -(lift*cos(glider_wind_angle)+
		glider_drag*sin(glider_wind_angle)+
		pilot_drag*sin(app_wind_angle)) /total_mass;

	pitch_dot_dot= ((-lift*sin(cop_adjust_angle+angle_of_attack-trim_angle) +
		glider_drag*cos(cop_adjust_angle+angle_of_attack-trim_angle))* cm_glider_length -
		pilot_drag*cos(app_wind_angle+pitch_angle)*cm_pilot_length)/moment_of_inetia;

////////////////////////////////////////////////////////
// update speed and position
	x_dot = x_dot + x_dot_dot * TIME_STEP;
	z_dot = z_dot + z_dot_dot * TIME_STEP;
	pitch_dot = pitch_dot + pitch_dot_dot * TIME_STEP;

	x = x + x_dot * TIME_STEP;
	z = z + z_dot * TIME_STEP;
	pitch_angle = (pitch_angle + pitch_dot * TIME_STEP) % TWO_PI;

	return;
}

